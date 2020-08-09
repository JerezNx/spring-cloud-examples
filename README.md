[toc]
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

## 5.Feign 基于接口和注解进行请求
### 5.1 概述
在4中，使用RestTemplate进行rest请求，还要注意参数传递及返回值的转型，比较繁琐。则引入Feign 简化操作。

### 5.2 基础使用
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
public interface InfoService {

//    参数必须添加@RequestParam,否则feign会把他方法请求体中，把请求方法改为post
    @GetMapping("/provider/info")
    String info(@RequestParam("name") String name);

}
```
这里有个注意点，如果是get请求，请求参数必须加 @RequestParam 注解，而非springmvc那种可以不加。如果不加，feign会把它放到请求体中，请求方法变成了Post，从而导致405错误。

#### 5.2.4 修改ConsumerController

```
@Autowired
private InfoService infoService;

@GetMapping("/infoByFeign")
public String infoByFeign(String name) {
    return infoService.info(name);
}
```
#### 5.2.5 重启测试
重新启动消费者，访问 [http://localhost:8000/consumer/infoByFeign?name=consumer](http://localhost:8000/consumer/infoByFeign?name=consumer),可正常返回。不停方法，发现ribbon的负载策略依旧有效。

## 6.Hystrix 服务熔断
### 6.1 概述

### 6.2 环境准备
在之前基础上
#### 6.2.1 添加依赖

```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
```
#### 6.2.2 启动类添加注解  @EnableHystrix

### 6.3 Ribbon结合Hystrix
修改ConsumerController 
```
@HystrixCommand(fallbackMethod = "fallbackInfo")
@GetMapping("/info")
public String info(String name) {
    return restTemplate.getForObject("http://PROVIDER/provider/info?name=" + name, String.class);
}

public String fallbackInfo(String name) {
    return name + "fallbackInfo";
}
```
在方法上添加注解 **@HystrixCommand(fallbackMethod = "fallbackInfo")** ,其中 fallbackInfo 即指的当调用服务提供者出错时，fallback的方法。

此时启动消费者，访问 [http://127.0.0.1:8000/consumer/info](http://127.0.0.1:8000/consumer/info)，一切正常。此时将提供者的7003的实例停掉，继续访问，发现7001和7002的正常返回，7003时，会返回 “consumerfallbackInfo”。

### 6.5 Ribbon结合Feign
修改配置文件，添加配置
```
feign:
  hystrix:
    enabled: true
```
新增 InfoService 的实现类
```
/**
 * @author LQL
 * @since Create in 2020/8/9 15:48
 */
@Service
public class InfoServiceImpl implements InfoService {
    @Override
    public String info(String name) {
        return name + "fallbackOfFeign";
    }
}
```
修改 InfoService 上的 @FeignClient 注解，添加 fallback 属性
```
@FeignClient(name = "PROVIDER", fallback = InfoServiceImpl.class)
```
重启消费者，访问 [http://localhost:8000/consumer/infoByFeign?name=consumer](http://localhost:8000/consumer/infoByFeign?name=consumer)，结果和上面的一样。

## 7.Zuul 服务网关
### 7.1 概述
1、网关访问方式
　　通过zuul访问服务的，URL地址默认格式为：http://zuulHostIp:port/要访问的服务名称/服务中的URL

　　服务名称：properties配置文件中的spring.application.name。

　　服务的URL：就是对应的服务对外提供的URL路径监听。
　　  
参考：[https://www.cnblogs.com/jing99/p/11696192.html](https://www.cnblogs.com/jing99/p/11696192.html)  

### 7.2 基础使用
#### 7.2.1 创建模块
将之前的eureka client 拷贝1份，模块名为 spring-cloud-zuul
#### 7.2.2 添加依赖
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
</dependency>
```
#### 7.2.3 启动类添加注解 @EnableZuulProxy
#### 7.2.4 配置文件

```
spring:
  application:
    name: ZUUL
server:
  port: 9000
eureka:
  client:
    serviceUrl:
      defaultZone: http://127.0.0.1:6001/eureka/
zuul:
  routes:
#    自定义名称
    provider:
#      请求路径为 /a/**的，会被路由到 PROVIDER 服务
      path: /a/**
#      即 注册到 eureka 的名称
      serviceId: PROVIDER
    consumer:
      path: /b/**
      serviceId: CONSUMER
```
#### 7.2.5 测试
启动zuul，分别访问 [http://localhost:9000/a/provider/info](http://localhost:9000/a/provider/info)，和
[http://localhost:9000/b/consumer/info](http://localhost:9000/b/consumer/info)。
都可正常返回。

### 7.3 结合ribbon
#### 7.3.1 概述
通过访问上面网关的2个接口，发现访问Provider时是有负载均衡的，但策略好像是轮询。访问Consumer时是其中的配置的随机。
#### 7.3.2 配置轮询策略
方法同之前4.5 有3种方式，此处演示配置文件的方式。修改配置文件：
```
PROVIDER:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
```
重启Zuul，再次访问[http://localhost:9000/a/provider/info](http://localhost:9000/a/provider/info)，会发现负载策略变为了随机。
### 7.4 结合Hystrix
#### 7.4.1 概述
当我们把Provider的7003实例停止后，通过网关访问Provider是等待比较长的时间后返回500，Consumer是很快返回了504 Gateway Timeout，打断点发现进入了Hystric的fallback方法，但没有正常返回错误信息。

在Edgware版本之前，Zuul提供了接口ZuulFallbackProvider用于实现fallback处理。从Edgware版本开始，Zuul提供了ZuulFallbackProvider的子接口FallbackProvider来提供fallback处理。
　　Zuul的fallback容错处理逻辑，只针对timeout异常处理，当请求被Zuul路由后，只要服务有返回（包括异常），都不会触发Zuul的fallback容错逻辑。

　　因为对于Zuul网关来说，做请求路由分发的时候，结果由远程服务运算的。那么远程服务反馈了异常信息，Zuul网关不会处理异常，因为无法确定这个错误是否是应用真实想要反馈给客户端的。
#### 7.4.2 代码
新建FallbackProvider类，其中getRoute表示哪个服务名， 可以用 * 表示所有。
```
/**
 * @author LQL
 * @since Create in 2020/8/9 17:16
 */
public class CustomZuulFallbackProvider implements FallbackProvider {

    private String applicationName;

    public CustomZuulFallbackProvider(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * 当前的fallback容错处理逻辑处理的是哪一个服务。可以使用通配符‘*’代表为全部的服务提供容错处理。
     * @return
     */
    @Override
    public String getRoute() {
        return this.applicationName;
    }

    /**
     * 当服务发生错误的时候，如何容错。
     * @param route
     * @param cause
     * @return
     */
    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        cause.printStackTrace();
        return this.executeFallback(HttpStatus.OK, route + "服务熔断",
                "application", "json", "utf-8");
    }

    /**
     * 具体处理过程。
     *
     * @param status       容错处理后的返回状态，如200正常GET请求结果，201正常POST请求结果，404资源找不到错误等。
     *                     使用spring提供的枚举类型对象实现。HttpStatus
     * @param contentMsg   自定义的响应内容。就是反馈给客户端的数据。
     * @param mediaType    响应类型，是响应的主类型， 如： application、text、media。
     * @param subMediaType 响应类型，是响应的子类型， 如： json、stream、html、plain、jpeg、png等。
     * @param charsetName  响应结果的字符集。这里只传递字符集名称，如： utf-8、gbk、big5等。
     * @return ClientHttpResponse 就是响应的具体内容。
     * 相当于一个HttpServletResponse。
     */
    private final ClientHttpResponse executeFallback(final HttpStatus status,
                                                     String contentMsg, String mediaType, String subMediaType, String charsetName) {
        return new ClientHttpResponse() {

            /**
             * 设置响应的头信息
             */
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders header = new HttpHeaders();
                MediaType mt = new MediaType(mediaType, subMediaType, Charset.forName(charsetName));
                header.setContentType(mt);
                return header;
            }

            /**
             * 设置响应体
             * zuul会将本方法返回的输入流数据读取，并通过HttpServletResponse的输出流输出到客户端。
             */
            @Override
            public InputStream getBody() throws IOException {
                String content = contentMsg;
                return new ByteArrayInputStream(content.getBytes());
            }

            /**
             * ClientHttpResponse的fallback的状态码 返回String
             */
            @Override
            public String getStatusText() throws IOException {
                return this.getStatusCode().getReasonPhrase();
            }

            /**
             * ClientHttpResponse的fallback的状态码 返回HttpStatus
             */
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return status;
            }

            /**
             * ClientHttpResponse的fallback的状态码 返回int
             */
            @Override
            public int getRawStatusCode() throws IOException {
                return this.getStatusCode().value();
            }

            /**
             * 回收资源方法
             * 用于回收当前fallback逻辑开启的资源对象的。
             * 不要关闭getBody方法返回的那个输入流对象。
             */
            @Override
            public void close() {
            }
        };
    }
}

```

新增配置类，ZuulFallbackConfig
```
/**
 * @author LQL
 * @since Create in 2020/8/9 17:23
 */
@Configuration
public class ZuulFallbackConfig {

    @Bean
    public FallbackProvider fallbackProvider() {
        return new CustomZuulFallbackProvider("PROVIDER");
    }

    @Bean
    public FallbackProvider fallbackConsumer() {
        return new CustomZuulFallbackProvider("CONSUMER");
    }

}
```
#### 7.4.3 测试
重启Zuul，此时再次访问 [http://localhost:9000/a/provider/info](http://localhost:9000/a/provider/info)，和
[http://localhost:9000/b/consumer/info](http://localhost:9000/b/consumer/info)，会发现当访问到7003时，返回了熔断信息。

## 8. Config 配置中心
### 8.1 概述

### 8.2 服务端-数据库实现
#### 8.2.1 新建 spring-cloud-config-server-db 模块
将之前的client拷贝一份
#### 8.2.2 添加依赖
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```
此处为了方便，数据库使用的是h2内存数据库。引入jdbc starter用于数据源的自动装配。  
注：spring-cloud-config-server 依赖必须单独放在该模块，之前本地测试偷懒将所有依赖都放在根pom中，结果config server能起来，但client一直读不到配置。

#### 8.2.3 数据库表结构
设计一张表 SYS_CONFIG ，存放配置信息。表结构(schema-h2.sql):
```
DROP TABLE IF EXISTS SYS_CONFIG;

CREATE TABLE SYS_CONFIG
(
	SERVICE_NAME VARCHAR(32) NOT NULL COMMENT '服务名',
	ENV VARCHAR(16) NULL DEFAULT 'dev' COMMENT '环境，如dev prod test',
	PROPERTY_KEY VARCHAR(128) NOT NULL COMMENT '属性KEY',
	PROPERTY_VALUE VARCHAR(128) NULL DEFAULT NULL COMMENT '属性值',
	PROPERTY_DEFAULT_VALUE VARCHAR(50) NULL DEFAULT NULL COMMENT '属性默认值',
	REMARK VARCHAR(50) NULL DEFAULT NULL COMMENT '备注',
	LABEL VARCHAR(50) NOT NULL DEFAULT 'master' COMMENT '标签'
);
```
添2条测试数据(data-h2.sql)：
```
DELETE FROM SYS_CONFIG;

INSERT INTO SYS_CONFIG (SERVICE_NAME, ENV, PROPERTY_KEY, PROPERTY_VALUE, PROPERTY_DEFAULT_VALUE, REMARK, LABEL) VALUES
('CONSUMER', 'dev', 'test', 'test1@baomidou.com','','test','master'),
('CONSUMER', 'dev', 'name', null ,'123','name','master');
```
#### 8.2.4 配置文件
配置application.yml:
```
spring:
  profiles:
#     数据库形式配置 必须为 jdbc, 自动实现 JdbcEnvironmentRepository。
    active: jdbc
  datasource:
    driver-class-name: org.h2.Driver
    schema: classpath:db/schema-h2.sql
    data: classpath:db/data-h2.sql
    url: jdbc:h2:mem:test
    username: root
    password: test
  application:
    #    注册进eureka的名字
    name: CONFIG-SERVER-DB
  cloud:
    config:
      enabled: true
      server:
        default-label: dev
        jdbc:
#          查询结果集必须只有2个字段，表示key 和 value 
          sql: SELECT PROPERTY_KEY , NVL(PROPERTY_VALUE,PROPERTY_DEFAULT_VALUE) FROM SYS_CONFIG WHERE SERVICE_NAME=? AND ENV=? AND LABEL=?
eureka:
  client:
    serviceUrl:
      #      eureka的注册中心地址
      defaultZone: http://127.0.0.1:6001/eureka/
server:
  port: 10001
```
注1：以数据库为配置中心时，spring.profiles.active 必须为 **jdbc**。  
注2：查询sql:
> SELECT PROPERTY_KEY , NVL(PROPERTY_VALUE,PROPERTY_DEFAULT_VALUE) FROM SYS_CONFIG WHERE SERVICE_NAME=? AND ENV=? AND LABEL=?

结果集必须只有2个字段，表示key 和 value 。  
查询条件至少有3个：
1. 第1个是服务名，对应于客户端中的 ==**spring.cloud.config.name**==，此处对应到数据库的字段 **SERVICE_NAME**。
2. 第2个是当前环境，对应于客户端中的 ==**spring.cloud.config.profile**==，此处对应到数据库的字段 **ENV**
3. 第3个是当前当前分支，一般使用git时指定，对应于客户端中的 ==**spring.cloud.config.label**==，此处对应到数据库的字段 **LABEL**

还有两个参数是version、state，暂不作研究。

#### 8.2.5 启动类添加 @EnableConfigServer 注解
#### 8.2.6 测试
启动config server，访问 [http://localhost:10001/CONSUMER/dev/master](http://localhost:10001/CONSUMER/dev/master)。即可看到配置属性：
```
{
  "name": "CONSUMER",
  "profiles": [
    "dev"
  ],
  "label": "master",
  "version": null,
  "state": null,
  "propertySources": [
    {
      "name": "CONSUMER-dev",
      "source": {
        "test": "test1@baomidou.com",
        "name": "123"
      }
    }
  ]
}
```
url规则：http://[==config-server-ip==]:[==config-server-port==]/[==service-name==]/[==env==]/[==label==]

### 8.3 服务端-git实现
 
### 8.4 客户端
直接在之前的consumer模块进行调整
#### 8.4.1 添加依赖
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

#### 8.4.2 新建 bootstrap.yml
```
spring:
  cloud:
    config:
      discovery:
        enabled: true
#        config-server的应用名
        serviceId: CONFIG-SERVER-DB
#        3个查询条件
      name: CONSUMER
      profile: dev
      label: master
      fail-fast: true
  application:
    name: CONSUMER
eureka:
  client:
    serviceUrl:
      defaultZone: http://127.0.0.1:6001/eureka/
server:
  port: 8000
```

#### 8.4.3 测试
修改ConsumerController，添加代码：
```
@Value("${name}")
private String name;

@PostConstruct
@GetMapping("/config")
public String config() {
    System.out.println("name: " + name);
    return name;
}
```
启动consumer，控制台成打印name的值。

