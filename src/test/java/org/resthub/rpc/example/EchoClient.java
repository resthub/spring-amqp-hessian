/**
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
package org.resthub.rpc.example;

import org.resthub.rpc.service.EchoService;
import org.springframework.context.support.GenericXmlApplicationContext;

public class EchoClient {
    
    public static void main(String[] args) throws Exception {
//        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost", 5672);
//        connectionFactory.setUsername("guest");
//        connectionFactory.setPassword("guest");
//        
//        AMQPHessianProxyFactory factory = new AMQPHessianProxyFactory();
//        factory.setReadTimeout(3000);
//        EchoService service = factory.create(EchoService.class, connectionFactory, null);
//        
//        System.out.println(service.echo("Hello AMQP!"));
//        connectionFactory.destroy();
        
        GenericXmlApplicationContext context = new GenericXmlApplicationContext(
                "classpath:/applicationContext-client.xml");
        
        EchoService service = (EchoService) context.getBean("echoService");
        System.out.println(service.echo("Hello AMQP!"));
        
//        FailingService failing = (FailingService) context.getBean("serializationError");
//        failing.getNotSerializable();
        
        context.close();
        
    }
}
