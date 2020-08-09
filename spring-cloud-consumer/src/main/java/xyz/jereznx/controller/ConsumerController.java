package xyz.jereznx.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import xyz.jereznx.service.InfoService;

import javax.annotation.PostConstruct;

/**
 * @author LQL
 * @since Create in 2020/8/8 17:23
 */
@RequestMapping("/consumer")
@RestController
public class ConsumerController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private InfoService infoService;

    @Value("${name}")
    private String name;

    @HystrixCommand(fallbackMethod = "fallbackInfo")
    @GetMapping("/info")
    public String info(String name) {
        return restTemplate.getForObject("http://PROVIDER/provider/info?name=" + name, String.class);
    }

    @GetMapping("/infoByFeign")
    public String infoByFeign(String name) {
        return infoService.info(name);
    }

    public String fallbackInfo(String name) {
        return name + "fallbackInfo";
    }

    @PostConstruct
    @GetMapping("/config")
    public String config() {
        System.out.println("name: " + name);
        return name;
    }
}
