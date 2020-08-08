## 1.前言

## 2.项目环境搭建
使用springboot 脚手架搭建一个空的父工程 spring-cloud-examples，然后将自动生成的src文件夹删除

## 3.Eureka 注册中心

### 3.1 概述

### 3.2 服务端
#### 3.2.1. 创建模块

在项目中新建一个maven 子 Module：spring-cloud-eureka-server ，可以将步骤2中删除的src拷到这个子module中，修改一下启动类名称。

#### 3.2.2 添加依赖

在pom文件中添加依赖：
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```
#### 3.2.3 在启动类上添加注解 @EnableEurekaServer

#### 3.2.4 配置文件

```
server:
  port: 6001
eureka:
  instance:
    hostname: 127.0.0.1
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
spring:
  application:
    name: EUREKA-SERVER
```
启动Server服务，此时访问 [localhost:6001](localhost:6001),即可看到eureka的监控页面。

### 3.3 客户端
#### 3.3.1 创建模块

同上，模块名 spring-cloud-eureka-client

#### 3.3.2 添加依赖

```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

#### 3.3.3 在启动类上添加注解 @EnableEurekaClient

#### 3.3.4 配置文件

```
spring:
  application:
    #    注册进eureka的名字
    name: EUREKA-CLIENT
eureka:
  client:
    serviceUrl:
      #      eureka的注册中心地址
      defaultZone: http://127.0.0.1:6001/eureka/
```

启动Client服务，再次访问[localhost:6001](localhost:6001)，会发现页面上Client已成功注册。

## 4.Ribbon 负载均衡
### 4.1 概述

### 4.2 环境准备
#### 4.2.1 创建模块
此时我们的工程有根模块，下面有一个EUREKA服务端模块和一个EUREKA客户端模块，将客户端模块再复制2份，分别命名为 spring-cloud-provider ， spring-cloud-consumer 。应用名分别为 PROVIDER ，CONSUMER 作为服务的提供者和消费者。
在父工程中添加web依赖
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

#### 4.2.2 spring-cloud-provider
添加controller，访问后返回该服务的端口号

```
/**
 * @author LQL
 * @since Create in 2020/8/8 17:19
 */
@RequestMapping("/provider")
@RestController
public class ProviderController {

    @Value("${server.port}")
    private int port;

    @GetMapping("/info")
    public String info(String name) {
        return name + " port:" + port;
    }

}
```
配置文件：
```
spring:
  application:
    name: PROVIDER
eureka:
  client:
    serviceUrl:
      defaultZone: http://127.0.0.1:6001/eureka/
server:
  port: 7001
```
此时启动项目，访问 [http://127.0.0.1:7001/provider/info](http://127.0.0.1:7001/provider/info),能看到返回 port:7001。
#### 4.2.3 spring-cloud-consumer
配置RestTemplate

```
/**
 * @author LQL
 * @since Create in 2020/8/8 17:25
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(simpleClientHttpRequestFactory());
    }

    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(5000);
        factory.setConnectTimeout(15000);
        return factory;
    }

}
```
添加controller
```
/**
 * @author LQL
 * @since Create in 2020/8/8 17:23
 */
@RequestMapping("/consumer")
@RestController
public class ConsumerController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/info")
    public String  info(){
        return restTemplate.getForObject("http://127.0.0.1:7001/provider/info?name=" + name, String.class);
    }

}
```
配置文件
```
spring:
  application:
    name: CONSUMER
eureka:
  client:
    serviceUrl:
      defaultZone: http://127.0.0.1:6001/eureka/
server:
  port: 8000
```
此时启动项目，访问 [http://127.0.0.1:8000/consumer/info](http://127.0.0.1:8000/consumer/info),能看到返回 port:7001。

### 4.3 使用上下文替代指定的ip、端口
上面消费者是直接用IP端口请求的提供者，此时我们修改消费者代码：

```
return restTemplate.getForObject("http://PROVIDER/provider/info?name=" + name, String.class);
```
将写死的 **127.0.0.1:7001** 修改为 **PROVIDER**，此处的PROVIDER即服务端配置 文件中，指定的spring.application.name。
此时重启消费者，再次访问 [http://127.0.0.1:8000/consumer/info](http://127.0.0.1:8000/consumer/info),依旧能看到返回 port:7001。
### 4.4 Ribbon负载均衡
#### 4.4.1 启动多个服务提供者
使用IDEA，启动3个服务提供者的实例，分别指定端口为7001,7002,7003。

#### 4.4.2 配置服务消费者负载均衡
修改服务消费者的 RestTemplate 配置：
```
/**
 * @author LQL
 * @since Create in 2020/8/8 17:25
 */
@Configuration
public class RestTemplateConfig {

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(simpleClientHttpRequestFactory());
    }

    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(5000);
        factory.setConnectTimeout(15000);
        return factory;
    }

}
```
即在RestTemplate 上添加 @LoadBalanced 注解。

#### 4.4.3 重启消费者
重启消费者，不停访问 [http://127.0.0.1:8000/consumer/info](http://127.0.0.1:8000/consumer/info),会看到返回结果在 port:7001，port:7002，port:7003 之间循环。

### 4.5 Ribbon负载均衡策略
默认提供了7种策略：
![image](https://note.youdao.com/yws/public/resource/11adcf7cea60edbc1c04bf7808992e8c/xmlnote/WEBRESOURCEa284bf73641e051246b2936b66da1548/5568)
#### 4.5.1 配置方式一： 代码配置:Bean
添加了 IRule Bean的配置，此策略即随机策略。

```
@Bean
public IRule ribbonRule() {
    return new RandomRule();
}
```
重启消费者，不停访问 [http://127.0.0.1:8000/consumer/info](http://127.0.0.1:8000/consumer/info),会看到返回结果在 port:7001，port:7002，port:7003 之间随机出现。
#### 4.5.2 配置方法二： 代码配置:注解
在消费者的启动类上添加注解：
```
@RibbonClient(name="PROVIDER",configuration = RandomRule.class)
```
也可达到同样的效果。
#### 4.5.3 配置方式三： 配置文件配置
去除方式一中的配置，在消费者配置文件中，添加：
```
PROVIDER:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
```
开头的 **PROVIDER** ，即服务提供者的应用名。

### 4.6 自定义Ribbon负载均衡策略

## 5.Feigin 基于接口和注解进行请求
### 5.1 概述
在4中，使用RestTemplate进行rest请求，还要注意参数传递及返回值的转型，比较繁琐。则引入Feigin 简化操作。

### 5.2 代码
#### 5.2.1 添加依赖
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```
#### 5.2.2 启动类添加注解 @EnableFeignClients

#### 5.2.3 创建接口

```
/**
 * @author LQL
 * @since Create in 2020/8/8 20:06
 */
@FeignClient(name = "PROVIDER")
public interface InfoFeignClient {

//    参数必须添加@RequestParam,否则feigin会把他方法请求体中，把请求方法改为post
    @GetMapping("/provider/info")
    String info(@RequestParam("name") String name);

}
```
这里有个注意点，如果是get请求，请求参数必须加 @RequestParam 注解，而非springmvc那种可以不加。如果不加，feigin会把它放到请求体中，请求方法变成了Post，从而导致405错误。

#### 5.2.4 修改ConsumerController

```
@Autowired
private InfoFeignClient infoFeignClient;

@GetMapping("/infoByFeigin")
public String infoByFeigin(String name) {
    return infoFeignClient.info(name);
}
```
#### 5.2.5 重启测试
重新启动消费者，访问 [http://localhost:8000/consumer/infoByFeigin?name=consumer](https://note.youdao.com/),可正常返回。不停方法，发现ribbon的负载策略依旧有效。