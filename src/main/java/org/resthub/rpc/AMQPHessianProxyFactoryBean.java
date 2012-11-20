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

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.FactoryBean;

/**
 * FactoryBean for Sprimg AMQP Hessian proxies. Exposes the proxied service
 * for use as a bean reference using the specified service interface.
 * 
 * @author Antoine Neveu
 *
 */
public class AMQPHessianProxyFactoryBean extends AMQPHessianProxyFactory
    implements FactoryBean<Object> {
    
    private Object serviceProxy;
    
    @Override
    public void afterPropertiesSet(){
        super.afterPropertiesSet();
        if (null == this.serviceInterface || ! this.serviceInterface.isInterface()){
            throw new IllegalArgumentException("Property 'serviceInterface' is required");
        }
        AMQPHessianProxy handler = new AMQPHessianProxy(this);
        this.serviceProxy =  Proxy.newProxyInstance(
                this.serviceInterface.getClassLoader(), new Class[]{this.serviceInterface}, handler);
    }

    public Object getObject() throws Exception {
        return this.serviceProxy;
    }

    public Class<?> getObjectType() {
        return this.serviceInterface;
    }

    public boolean isSingleton() {
        return true;
    }

}
