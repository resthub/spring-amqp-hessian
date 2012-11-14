/**
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
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.HessianFactory;
import com.caucho.hessian.io.HessianInputFactory;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;

/**
 * Requests processing
 * @author Antoine Neveu
 *
 */
public class RawMessageDelegate {
    
    private static final Logger logger = LoggerFactory.getLogger(RawMessageDelegate.class);
    
    private Class<?> serviceAPI;
    private Object serviceImpl;
    private SerializerFactory serializerFactory;
    
    public RawMessageDelegate(){
        
    }
    
    public RawMessageDelegate(Class<?> serviceAPI, Object serviceImpl, SerializerFactory serializerFactory){
        this.serviceAPI = serviceAPI;
        this.serviceImpl = serviceImpl;
        this.serializerFactory = serializerFactory;
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
     * Message processing
     * @param message
     * @return
     */
    public Message handleMessage(Message message){
        MessageProperties props = message.getMessageProperties();
        boolean compressed = "deflate".equals(props.getContentEncoding());

        
        byte[] response;
        try
        {
            response = createResponseBody(message.getBody(), compressed);
        }
        catch (Exception e)
        {
            logger.error("Exception occurs during method call", e);
            e.printStackTrace();
            compressed = false;
            response = createFaultBody(message.getBody(), e);
        }
        
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("x-application/hessian");
        if (compressed)
        {
            messageProperties.setContentEncoding("deflate");
        }
        return new Message(response, messageProperties);
    }
    
    /**
     * Execute a request.
     */
    private byte[] createResponseBody(byte[] request, boolean compressed) throws Exception
    {
        InputStream in = new ByteArrayInputStream(request);
        if (compressed)
        {
            in = new InflaterInputStream(new ByteArrayInputStream(request), new Inflater(true));
        }
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStream out;
        if (compressed)
        {
            Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            out = new DeflaterOutputStream(bout, deflater);
        }
        else
        {
            out = bout;
        }
        
        HessianSkeleton skeleton = new HessianSkeleton(serviceImpl, serviceAPI);
        skeleton.invoke(in, out, getSerializerFactory());
        
        if (out instanceof DeflaterOutputStream)
        {
            ((DeflaterOutputStream) out).finish();
        }
        out.flush();
        out.close();

        return bout.toByteArray();
    }

    private byte[] createFaultBody(byte[] request, Throwable cause)
    {
        try
        {
            ByteArrayInputStream is = new ByteArrayInputStream(request);
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            AbstractHessianOutput out = createHessianOutput(new HessianInputFactory().readHeader(is), os);

            out.writeFault(cause.getClass().getSimpleName(), cause.getMessage(), cause);
            out.close();

            return os.toByteArray();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private AbstractHessianOutput createHessianOutput(HessianInputFactory.HeaderType header, OutputStream os)
    {
        AbstractHessianOutput out;
        
        HessianFactory hessianfactory = new HessianFactory();
        switch (header)
        {
            case CALL_1_REPLY_1:
                out = hessianfactory.createHessianOutput(os);
                break;

            case CALL_1_REPLY_2:
            case HESSIAN_2:
                out = hessianfactory.createHessian2Output(os);
                break;

            default:
                throw new IllegalStateException(header + " is an unknown Hessian call");
        }
        
        return out;
    }

}
