package com.stars.starsapiclientsdk;

import com.stars.starsapiclientsdk.client.StarsApiClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * StarsApiClientConfig类是StarsAPI客户端的配置类。
 *
 * @author stars
 */
@Configuration
@ConfigurationProperties("starsapi.client")
@Data
@ComponentScan
public class StarsApiClientConfig {

    private String accessKey;
    private String secretKey;

    /**
     * 创建并返回StarsApiClient的Bean。
     *
     * @return StarsApiClient的实例
     */
    @Bean
    public StarsApiClient starsApiClient() {
        return new StarsApiClient(accessKey, secretKey);
    }
}
