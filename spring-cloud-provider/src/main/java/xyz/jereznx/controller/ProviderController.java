package xyz.jereznx.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
