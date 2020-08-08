package xyz.jereznx.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import xyz.jereznx.config.InfoFeignClient;

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
    private InfoFeignClient infoFeignClient;

    @GetMapping("/info")
    public String info(String name) {
        return restTemplate.getForObject("http://PROVIDER/provider/info?name=" + name, String.class);
    }

    @GetMapping("/infoByFeigin")
    public String infoByFeigin(String name) {
        return infoFeignClient.info(name);
    }
}
