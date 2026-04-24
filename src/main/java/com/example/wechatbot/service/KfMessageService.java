package com.example.wechatbot.service;

import com.example.wechatbot.config.WecomProperties;
import com.example.wechatbot.model.WecomSendMsgResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 描述：负责通过企业微信客服接口发送消息。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class KfMessageService {

    private final WecomTokenService tokenService;
    private final WecomProperties wecomProperties;
    private final RestTemplate restTemplate;

    /**
     * 描述：向指定企业微信外部联系人发送文本消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void sendText(String externalUserId, String content) {
        String accessToken = tokenService.getAccessToken();
        String url = "https://qyapi.weixin.qq.com/cgi-bin/kf/send_msg?access_token=" + accessToken;

        Map<String, Object> payload = Map.of(
                "touser", externalUserId,
                "open_kfid", wecomProperties.getKfAccountId(),
                "msgtype", "text",
                "text", Map.of("content", content)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        WecomSendMsgResponse response = restTemplate.postForObject(url, entity, WecomSendMsgResponse.class);
        log.info("send_msg result={}", response);
        if (response == null || response.getErrcode() == null || response.getErrcode() != 0) {
            throw new IllegalStateException("failed to send wecom kf message: " + response);
        }
    }
}
