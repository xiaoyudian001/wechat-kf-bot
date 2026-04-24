package com.example.wechatbot.config;

import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
/**
 * 描述：集中创建项目运行所需的通用 Bean。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class AppConfig {

    private final WecomProperties wecomProperties;
    private final HttpClientProperties httpClientProperties;

    @Bean
    /**
     * 描述：创建带连接超时和读取超时的 RestTemplate。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(httpClientProperties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(httpClientProperties.getReadTimeoutMs()));
        return new RestTemplate(factory);
    }

    @Bean
    /**
     * 描述：初始化企业微信 SDK 服务实例。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public WxCpService wxCpService() {
        WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
        config.setCorpId(wecomProperties.getCorpId());
        config.setCorpSecret(wecomProperties.getSecret());
        config.setToken(wecomProperties.getToken());
        config.setAesKey(wecomProperties.getAesKey());

        WxCpService service = new WxCpServiceImpl();
        service.setWxCpConfigStorage(config);
        return service;
    }
}
