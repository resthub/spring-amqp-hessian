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

package org.resthub.rpc.service;

import org.resthub.rpc.HessianEndpoint;



/**
 * Echo service implementation as a subclass of HessianEndpoint.
 * 
 * @author Emmanuel Bourg
 */
public class EchoServiceEndpoint extends HessianEndpoint implements EchoService
{
    public String echo(String message)
    {
        return message;
    }

    public void exception(String message) throws Exception
    {
        throw new Exception(message);
    }
    
    public void doNothing(){
        
    }
}
