package com.example.wechatbot.service;

import com.example.wechatbot.config.WecomProperties;
import com.example.wechatbot.model.WecomTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
/**
 * 描述：负责获取和缓存企业微信 access_token。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class WecomTokenService {

    private final WecomProperties wecomProperties;
    private final RestTemplate restTemplate;
    private final ObjectProvider<RedisCacheService> redisCacheServiceProvider;

    private volatile String cachedToken;
    private volatile Instant expireAt = Instant.EPOCH;

    /**
     * 描述：获取可用的企业微信 access_token。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expireAt.minusSeconds(120))) {
            return cachedToken;
        }

        RedisCacheService redisCacheService = redisCacheServiceProvider.getIfAvailable();
        if (redisCacheService != null) {
            var redisToken = redisCacheService.getAccessToken();
            if (redisToken.isPresent()) {
                cachedToken = redisToken.get();
                expireAt = Instant.now().plusSeconds(300);
                return cachedToken;
            }
        }

        String url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s"
                .formatted(wecomProperties.getCorpId(), wecomProperties.getSecret());

        WecomTokenResponse response = restTemplate.getForObject(url, WecomTokenResponse.class);
        if (response == null || response.getErrcode() == null || response.getErrcode() != 0) {
            throw new IllegalStateException("failed to get access token: " + response);
        }

        cachedToken = response.getAccess_token();
        int expiresIn = response.getExpires_in() == null ? 7200 : response.getExpires_in();
        expireAt = Instant.now().plusSeconds(expiresIn);
        if (redisCacheService != null) {
            redisCacheService.saveAccessToken(cachedToken, expiresIn);
        }
        return cachedToken;
    }
}
