package xyz.jereznx.config;

import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
