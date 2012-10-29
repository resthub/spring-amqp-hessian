spring-amqp-hessian
===================

spring-amqp-hessian is based on qpid-hessian component (https://github.com/ebourg/qpid-hessian)
and adapted for spring-amqp.

spring-amqp-hessian is a component helping the creation of Hessian services over AMQP
using spring-amqp.


Usage
-----

Server side :
Declare your service implementation and the endpoint
<bean id="echoServiceImpl" class="org.resthub.rpc.service.EchoServiceImpl"/>
<bean id="echoEndpoint" 
   class="org.resthub.rpc.HessianEndpoint">
   <constructor-arg index="0" ref="echoServiceImpl"/>
   <property name="connectionFactory" ref="connectionFactory" />
</bean>

Client side :
Declare the proxy
<bean id="echoService" class="org.resthub.rpc.AMQPHessianProxyFactoryBean">
    <property name="connectionFactory" ref="connectionFactory"/>
    <property name="serviceInterface" value="org.resthub.rpc.service.EchoService"/>
</bean>

You can now consume the service :
String echo = service.echo("Hello Hessian!");