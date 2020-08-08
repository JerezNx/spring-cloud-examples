package xyz.jereznx.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
