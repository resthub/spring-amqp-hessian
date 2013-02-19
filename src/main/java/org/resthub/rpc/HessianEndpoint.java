/**
 * Copyright 2010 Emmanuel Bourg
 * Copyright 2012 resthub.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.resthub.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.caucho.hessian.io.SerializerFactory;

/**
 * Endpoint for serving Hessian services.
 * 
 * This class is derived from {@link com.caucho.hessian.server.HessianServlet}. 
 * 
 * @author Emmanuel Bourg
 * @author Antoine Neveu
 */
public class HessianEndpoint implements InitializingBean, DisposableBean
{
    private static final Logger logger = LoggerFactory.getLogger(HessianEndpoint.class);
    
    private Class<?> serviceAPI;
    private Object serviceImpl;
    private SerializerFactory serializerFactory;
    private ConnectionFactory connectionFactory;
    private SimpleMessageListenerContainer listenerContainer;
    private AmqpAdmin admin;
    
    private int concurentConsumers;

    /** The prefix of the queue created to receive the hessian requests */
    private String queuePrefix;

    /**
     * Creates an hessian endpoint.
     */
    public HessianEndpoint()
    {
        // Initialize the service
        setServiceAPI(findRemoteAPI(getClass()));
        setServiceImpl(this);
    }

    /**
     * Creates an hessian endpoint for the specified service.
     * 
     * @param serviceImpl The remote object to be exposed by the endpoint
     */
    public HessianEndpoint(Object serviceImpl)
    {
        // Initialize the service
        setServiceAPI(findRemoteAPI(serviceImpl.getClass()));
        setServiceImpl(serviceImpl);
    }

    /**
     * Specifies the interface of the service.
     */
    public void setServiceAPI(Class<?> serviceAPI)
    {
        this.serviceAPI = serviceAPI;
    }

    /**
     * Specifies the object implementing the service.
     */
    public void setServiceImpl(Object serviceImpl)
    {
        this.serviceImpl = serviceImpl;
        
    }

    /**
     * Sets the serializer factory.
     */
    public void setSerializerFactory(SerializerFactory factory)
    {
        serializerFactory = factory;
    }

    /**
     * Gets the serializer factory.
     */
    public SerializerFactory getSerializerFactory()
    {
        if (serializerFactory == null)
        {
            serializerFactory = new SerializerFactory();
        }

        return serializerFactory;
    }

    /**
     * Returns the prefix of the queue created to receive the hessian requests.
     */
    public String getQueuePrefix()
    {
        return queuePrefix;
    }

    /**
     * Sets the prefix of the queue created to receive the hessian requests.
     */
    public void setQueuePrefix(String prefix)
    {
        queuePrefix = prefix;
    }

    /**
     * Get the connectionFactory
     * @return connectionFactory
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Set the connectionFactory
     * @param connectionFactory
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Get the number of concurent consumers
     * @return concurentConsumers
     */
    public int getConcurentConsumers() {
        return concurentConsumers;
    }

    /**
     * Set the number of concurent consumers
     * @param concurentConsumers
     */
    public void setConcurentConsumers(int concurentConsumers) {
        this.concurentConsumers = concurentConsumers;
    }

    /**
     * Sets the serializer send collection java type.
     */
    public void setSendCollectionType(boolean sendType)
    {
        getSerializerFactory().setSendCollectionType(sendType);
    }

    private Class<?> findRemoteAPI(Class<?> implClass)
    {
        if (implClass == null)
        {
            return null;
        }
        
        Class<?>[] interfaces = implClass.getInterfaces();

        if (interfaces.length > 0)
        {
            return interfaces[0];
        }

        return findRemoteAPI(implClass.getSuperclass());
    }

    /**
     * Return the name of the request queue for the service.
     * The queue name is based on the class of the API implemented.
     */
    private String getRequestQueueName(Class<?> cls)
    {
        String requestQueue = cls.getSimpleName();
        if (queuePrefix != null)
        {
            requestQueue = queuePrefix + "." + requestQueue;
        }
        
        return requestQueue;
    }

    /**
     * Create a queue.
     * 
     * @param admin AMQPAdmin
     * @param name    the name of the queue
     */
    private void createQueue(AmqpAdmin admin, String name)
    {
        Queue requestQueue = new Queue(name, false, false, false);
        admin.declareQueue(requestQueue);
    }

    /**
     * Starts the endpoint on the connection specified.
     * A listener container is launched on a created queue. The listener will
     * respond to hessian requests. The endpoint is closed by calling the destroy 
     * method.
     */
    public void run()
    {
        logger.debug("Launching endpoint for service : " + serviceAPI.getSimpleName());
        admin = new RabbitAdmin(connectionFactory);
        // Add connectionListener to recreate queue when connection fall
        connectionFactory.addConnectionListener(new ConnectionListener() {
            public void onCreate(Connection connection) {
                 createQueue(admin, getRequestQueueName(serviceAPI));
            }
            
            public void onClose(Connection connection) {
            }
            
        });
        
        // Create the queue normaly the first time
        this.createQueue(admin, getRequestQueueName(serviceAPI));
        
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(
                new RawMessageDelegate(serviceAPI, serviceImpl, serializerFactory));
        listenerAdapter.setMessageConverter(null);
        listenerAdapter.setMandatoryPublish(false);
        
        listenerContainer = new SimpleMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.setQueueNames(getRequestQueueName(serviceAPI));
        listenerContainer.setMessageListener(listenerAdapter);
        if (this.concurentConsumers > 0){
            listenerContainer.setConcurrentConsumers(concurentConsumers);
        }
        listenerContainer.start();
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.connectionFactory == null){
            throw new IllegalArgumentException("Property 'connectionFactory' is required");
        }
        this.run();
    }
    
    /**
     * Destroys the listenerContainer instance.
     */
    public void destroy() {
        this.listenerContainer.destroy();
    }

    

}
