package xyz.jereznx.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import xyz.jereznx.service.InfoService;

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
}
