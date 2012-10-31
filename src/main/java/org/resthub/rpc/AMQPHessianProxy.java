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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeoutException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.services.server.AbstractSkeleton;

/**
 * Proxy implementation for Hessian clients. Applications will generally
 * use {@link AMQPHessianProxyFactory} to create proxy clients.
 * 
 * @author Emmanuel Bourg
 * @author Scott Ferguson
 * @author Antoine Neveu
 */
public class AMQPHessianProxy implements InvocationHandler
{
    private AMQPHessianProxyFactory _factory;
    
    AMQPHessianProxy(){}

    AMQPHessianProxy(AMQPHessianProxyFactory factory)
    {
        _factory = factory;
    }

    /**
     * Handles the object invocation.
     *
     * @param proxy  the proxy object to invoke
     * @param method the method to call
     * @param args   the arguments to the proxy object
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        String methodName = method.getName();
        Class<?>[] params = method.getParameterTypes();

        // equals and hashCode are special cased
        if (methodName.equals("equals") && params.length == 1 && params[0].equals(Object.class))
        {
            Object value = args[0];
            if (value == null || !Proxy.isProxyClass(value.getClass()))
            {
                return Boolean.FALSE;
            }

            AMQPHessianProxy handler = (AMQPHessianProxy) Proxy.getInvocationHandler(value);

            return _factory.equals(handler._factory);
        }
        else if (methodName.equals("hashCode") && params.length == 0)
        {
            return _factory.hashCode();
        }
        else if (methodName.equals("toString") && params.length == 0)
        {
            return "[HessianProxy " + proxy.getClass() + "]";
        }
        
        ConnectionFactory connectionFactory = _factory.getConnectionFactory();
        
        try
        {
            Message response = sendRequest(connectionFactory, method, args);
            
            if (response == null) {
                throw new TimeoutException();
            }
            
            MessageProperties props = response.getMessageProperties();
            boolean compressed = "deflate".equals(props.getContentEncoding());
            
            AbstractHessianInput in;
            
            InputStream is = new ByteArrayInputStream(response.getBody());
            if (compressed) {
                is = new InflaterInputStream(is, new Inflater(true));
            }
            
            int code = is.read();

            if (code == 'H')
            {
                int major = is.read();
                int minor = is.read();

                in = _factory.getHessian2Input(is);

                return in.readReply(method.getReturnType());
            }
            else if (code == 'r')
            {
                int major = is.read();
                int minor = is.read();

                in = _factory.getHessianInput(is);

                in.startReplyBody();

                Object value = in.readObject(method.getReturnType());

                in.completeReply();

                return value;
            }
            else
            {
                throw new HessianProtocolException("'" + (char) code + "' is an unknown code");
            }
        }
        catch (HessianProtocolException e)
        {
            throw new HessianRuntimeException(e);
        }
    }
    
    /**
     * Send request message
     * 
     * @param connectionFactory Spring connection factory
     * @param method Method to call
     * @param args Method arguments
     * @return Response to the sent request
     * @throws IOException
     */
    private Message sendRequest(ConnectionFactory connectionFactory, Method method, Object[] args) throws IOException
    {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        if (_factory.getReadTimeout() > 0){
            template.setReplyTimeout(_factory.getReadTimeout());
        }
        
        this.createQueue(connectionFactory, this.getRequestQueueName(method.getDeclaringClass()));
        
        byte[] payload = createRequestBody(method, args);

        MessageProperties messageProperties = new MessageProperties();
//        messageProperties.setReplyToAddress(new Address("amq.direct", "", replyQueue.getName()));
        messageProperties.setContentType("x-application/hessian");
        if (_factory.isCompressed())
        {
            messageProperties.setContentEncoding("deflate");
        }
        
        Message message = new Message(payload, messageProperties);
        Message response = template.sendAndReceive(getRequestExchangeName(method.getDeclaringClass()), message);
        
        return response;
    }
    
    
    /**
     * Return the name of the request exchange for the service.
     */
    private String getRequestExchangeName(Class<?> cls)
    {
        String requestExchange = cls.getSimpleName();
        if (_factory.getQueuePrefix() != null)
        {
            requestExchange = _factory.getQueuePrefix() + "." + requestExchange;
        }
        
        return requestExchange;
    }
    
    /**
     * Return the name of the request queue for the service.
     */
    private String getRequestQueueName(Class<?> cls)
    {
        String requestQueue = cls.getSimpleName();
        if (_factory.getQueuePrefix() != null)
        {
            requestQueue = _factory.getQueuePrefix() + "." + requestQueue;
        }
        
        return requestQueue;
    }

    /**
     * Create a queue.
     * 
     * @param connectionFactory
     * @param name    the name of the queue
     */
    private void createQueue(ConnectionFactory connectionFactory, String name)
    {
        AmqpAdmin admin = new RabbitAdmin(connectionFactory);
        Queue requestQueue = new Queue(name, true, false, false);
        admin.declareQueue(requestQueue);
        DirectExchange requestExchange = new DirectExchange(name, true, false);
        admin.declareExchange(requestExchange);
        Binding requestBinding = BindingBuilder.bind(requestQueue).to(requestExchange).with(name);
        admin.declareBinding(requestBinding);
    }
    
    /**
     * Create the request message body
     * @param method
     * @param args
     * @return
     * @throws IOException
     */
    private byte[] createRequestBody(Method method, Object[] args) throws IOException
    {
        String methodName = method.getName();
        
        if (_factory.isOverloadEnabled() && args != null && args.length > 0)
        {
            methodName = AbstractSkeleton.mangleName(method, false);
        }
        
        ByteArrayOutputStream payload = new ByteArrayOutputStream(256);
        OutputStream os;
        if (_factory.isCompressed())
        {
            Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            os = new DeflaterOutputStream(payload, deflater);
        }
        else
        {
            os = payload;
        }
        
        AbstractHessianOutput out = _factory.getHessianOutput(os);
        
        out.call(methodName, args);
        if (os instanceof DeflaterOutputStream)
        {
            ((DeflaterOutputStream) os).finish();
        }
        out.flush();
        
        return payload.toByteArray();
    }
}
