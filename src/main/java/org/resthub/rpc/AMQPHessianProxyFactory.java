/**
 * Copyright 2010 Emmanuel Bourg
 * Copyright 2012 Resthub.org
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.InitializingBean;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianDebugInputStream;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.HessianRemoteResolver;
import com.caucho.hessian.io.SerializerFactory;

/**
 * Factory for creating Hessian client stubs. The returned stub will
 * call the remote object for all methods.
 *
 * After creation, the stub can be like a regular Java class. Because
 * it makes remote calls, it can throw more exceptions than a Java class.
 * In particular, it may throw protocol exceptions.
 * 
 * This class is derived from {@link com.caucho.hessian.client.HessianProxyFactory}. 
 * 
 * @author Emmanuel Bourg
 * @author Scott Ferguson
 * @author Antoine Neveu
 */
public class AMQPHessianProxyFactory implements InitializingBean
{
    private SerializerFactory _serializerFactory;
    private HessianRemoteResolver _resolver;
    private ConnectionFactory connectionFactory;
    
    private String queuePrefix;

    private boolean isOverloadEnabled = false;

    private boolean isHessian2Reply = true;
    private boolean isHessian2Request = true;

    private boolean debug = false;

    private long readTimeout = -1;
    
    private boolean compressed;

    /**
     * Creates the new proxy factory.
     */
    public AMQPHessianProxyFactory()
    {
        
    }

    /**
     * Returns the prefix of the queue that receives the hessian requests.
     */
    public String getQueuePrefix()
    {
        return queuePrefix;
    }

    /**
     * Sets the prefix of the queue that receives the hessian requests.
     */
    public void setQueuePrefix(String queuePrefix)
    {
        this.queuePrefix = queuePrefix;
    }

    /**
     * Sets the debug mode.
     */
    public void setDebug(boolean isDebug)
    {
        this.debug = isDebug;
    }

    /**
     * Gets the debug mode.
     */
    public boolean isDebug()
    {
        return debug;
    }

    /**
     * Returns true if overloaded methods are allowed (using mangling)
     */
    public boolean isOverloadEnabled()
    {
        return isOverloadEnabled;
    }

    /**
     * set true if overloaded methods are allowed (using mangling)
     */
    public void setOverloadEnabled(boolean isOverloadEnabled)
    {
        this.isOverloadEnabled = isOverloadEnabled;
    }

    /**
     * Returns the socket timeout on requests in milliseconds.
     */
    public long getReadTimeout()
    {
        return readTimeout;
    }

    /**
     * Sets the socket timeout on requests in milliseconds.
     */
    public void setReadTimeout(long timeout)
    {
        readTimeout = timeout;
    }

    /**
     * Indicates if the requests/responses should be compressed.
     */
    public boolean isCompressed()
    {
        return compressed;
    }

    /**
     * Specifies if the requests/responses should be compressed.
     */
    public void setCompressed(boolean compressed) 
    {
        this.compressed = compressed;
    }

    /**
     * True if the proxy can read Hessian 2 responses.
     */
    public void setHessian2Reply(boolean isHessian2)
    {
        isHessian2Reply = isHessian2;
    }

    /**
     * True if the proxy should send Hessian 2 requests.
     */
    public void setHessian2Request(boolean isHessian2)
    {
        isHessian2Request = isHessian2;

        if (isHessian2)
        {
            isHessian2Reply = true;
        }
    }

    /**
     * Returns the remote resolver.
     */
    public HessianRemoteResolver getRemoteResolver()
    {
        return _resolver;
    }

    /**
     * Sets the serializer factory.
     */
    public void setSerializerFactory(SerializerFactory factory)
    {
        _serializerFactory = factory;
    }

    /**
     * Gets the serializer factory.
     */
    public SerializerFactory getSerializerFactory()
    {
        if (_serializerFactory == null)
        {
            _serializerFactory = new SerializerFactory();
        }

        return _serializerFactory;
    }
    
    /**
     * Get the connectionFactory
     * @return
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
     * Creates a new proxy from the specified interface.
     * @param api the interface
     * @return the proxy to the object with the specified interface
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> api){
        this.afterPropertiesSet();
        if (null == api || ! api.isInterface()){
            throw new IllegalArgumentException("Parameter 'api' is required");
        }
        
        AMQPHessianProxy handler = new AMQPHessianProxy(this);
        return (T) Proxy.newProxyInstance(api.getClassLoader(), new Class[]{api}, handler);
    }
    
    AbstractHessianInput getHessianInput(InputStream is)
    {
        return getHessian2Input(is);
    }

    AbstractHessianInput getHessian1Input(InputStream is)
    {
        AbstractHessianInput in;

        if (debug)
        {
            is = new HessianDebugInputStream(is, new PrintWriter(System.out));
        }

        in = new HessianInput(is);

        in.setRemoteResolver(getRemoteResolver());

        in.setSerializerFactory(getSerializerFactory());

        return in;
    }

    AbstractHessianInput getHessian2Input(InputStream is)
    {
        AbstractHessianInput in;

        if (debug)
        {
            is = new HessianDebugInputStream(is, new PrintWriter(System.out));
        }

        in = new Hessian2Input(is);

        in.setRemoteResolver(getRemoteResolver());

        in.setSerializerFactory(getSerializerFactory());

        return in;
    }

    AbstractHessianOutput getHessianOutput(OutputStream os)
    {
        AbstractHessianOutput out;

        if (isHessian2Request)
        {
            out = new Hessian2Output(os);
        }
        else
        {
            HessianOutput out1 = new HessianOutput(os);
            out = out1;

            if (isHessian2Reply)
            {
                out1.setVersion(2);
            }
        }

        out.setSerializerFactory(getSerializerFactory());

        return out;
    }
    
    public void afterPropertiesSet(){
        if (this.connectionFactory == null){
            throw new IllegalArgumentException("Property 'connectionFactory' is required");
        }
    }
}

