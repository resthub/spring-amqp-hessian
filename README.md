spring-amqp-hessian
===================

spring-amqp-hessian is based on qpid-hessian component (https://github.com/ebourg/qpid-hessian)
and adapted for spring-amqp.

spring-amqp-hessian is a component helping the creation of Hessian services over AMQP
using spring-amqp.


Usage
-----

### Server side :

Declare your service implementation and the endpoint in your Spring configuration file :

    <bean id="echoServiceImpl" class="org.resthub.rpc.service.EchoServiceImpl"/>
    <bean id="echoEndpoint" class="org.resthub.rpc.HessianEndpoint">
        <constructor-arg index="0" ref="echoServiceImpl"/>
        <property name="connectionFactory" ref="connectionFactory" />
    </bean>

### Client side :

Declare the proxy in your Spring configuration file :

    <bean id="echoService" class="org.resthub.rpc.AMQPHessianProxyFactoryBean">
        <property name="connectionFactory" ref="connectionFactory"/>
        <property name="serviceInterface" value="org.resthub.rpc.service.EchoService"/>
    </bean>

You can now consume the service :

    String echo = echoService.echo("Hello Hessian!");
    

Specifications
--------------

### Exceptions management

Exceptions on server side when calling a method are catched and sent to the client. 
Exceptions can be catched on client side :

    try {
        String echo = echoService.echo("Hello Hessian!");
    } catch (Exception e) {
        //do stuff
    }
    
### Partial serialization

Objects on server side can have more attributes than same objects on client side.

Indicative performance
----------------------

Comparative bench between spring-amqp-hessian with RabbitMQ and REST/JSON.

Test with spring-amqp-hessian and RabbitMQ 2.6.1 :

    * Average of 2800 requests per second
    
Same test with REST/JSON :

    * Average of 1800 requests per second