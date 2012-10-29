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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.TimeoutException;

import org.resthub.rpc.service.EchoService;
import org.resthub.rpc.service.EchoServiceEndpoint;
import org.resthub.rpc.service.FailingService;
import org.resthub.rpc.service.FailingServiceEndpoint;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


public class AMQPHessianProxyTest
{
    protected final String HOSTNAME = "localhost";

    protected CachingConnectionFactory connectionFactory;

    @BeforeTest
    protected void setUp() throws Exception
    {
        connectionFactory = new CachingConnectionFactory("localhost", 5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
    }

//    @AfterTest
//    protected void tearDown() throws Exception
//    {
//        connectionFactory.destroy();
//    }

    protected void startEndpoint()
    {
        EchoServiceEndpoint endpoint = new EchoServiceEndpoint();
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.run();
    }

    @Test
    public void testEcho() throws Exception
    {
        startEndpoint();
        
        AMQPHessianProxyFactoryBean factory = new AMQPHessianProxyFactoryBean();
        factory.setReadTimeout(5000);
        factory.setServiceInterface(EchoService.class);
        factory.setConnectionFactory(connectionFactory);
        factory.afterPropertiesSet();
        EchoService service = (EchoService) factory.getObject();
        String message = "Hello Hessian!";

        assertEquals(message, service.echo(message));
        assertEquals(message, service.echo(message));
    }

    @Test
    public void testException() throws Exception
    {
        startEndpoint();
        
        AMQPHessianProxyFactoryBean factory = new AMQPHessianProxyFactoryBean();
        factory.setReadTimeout(5000);
        factory.setCompressed(true);
        factory.setServiceInterface(EchoService.class);
        factory.setConnectionFactory(connectionFactory);
        factory.afterPropertiesSet();
        EchoService service = (EchoService) factory.getObject();
        String message = "Hello Hessian!";

        try
        {
            service.exception(message);
            fail("No exception thrown");
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            assertEquals("Exception message", message, e.getMessage());
        }
    }

    @Test
    public void testTimeout() throws Exception
    {
        FailingServiceEndpoint endpoint = new FailingServiceEndpoint();
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.run();
        
        AMQPHessianProxyFactoryBean factory = new AMQPHessianProxyFactoryBean();
        factory.setReadTimeout(3000);
        factory.setServiceInterface(FailingService.class);
        factory.setConnectionFactory(connectionFactory);
        factory.afterPropertiesSet();
        FailingService service = (FailingService) factory.getObject();
        System.out.println(service);
        
        try
        {
            service.timeout(5000);
            fail("UndeclaredThrowableException expected");
        }
        catch (UndeclaredThrowableException e)
        {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof TimeoutException);
            Thread.sleep(3000);
        }
    }

    @Test
    public void testSerializationError() throws Exception
    {
        FailingServiceEndpoint endpoint = new FailingServiceEndpoint();
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.run();
        
        AMQPHessianProxyFactoryBean factory = new AMQPHessianProxyFactoryBean();
        factory.setReadTimeout(5000);
        factory.setServiceInterface(FailingService.class);
        factory.setConnectionFactory(connectionFactory);
        factory.afterPropertiesSet();
        FailingService service = (FailingService) factory.getObject();
        
        try
        {
            service.getNotSerializable();
            fail("IllegalStateException expected");
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.getMessage().contains("must implement java.io.Serializable"));
        }
    }
    
    @Test
    public void testDoNothing() throws Exception
    {
        EchoServiceEndpoint endpoint = new EchoServiceEndpoint();
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.run();
        
        AMQPHessianProxyFactoryBean factory = new AMQPHessianProxyFactoryBean();
        factory.setReadTimeout(5000);
        factory.setServiceInterface(EchoService.class);
        factory.setConnectionFactory(connectionFactory);
        factory.afterPropertiesSet();
        EchoService service = (EchoService) factory.getObject();
        
        try
        {
            service.doNothing();
        }
        catch (Exception e)
        {
            throw e;
        }
    }
}
