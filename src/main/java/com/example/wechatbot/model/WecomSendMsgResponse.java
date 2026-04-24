package com.example.wechatbot.model;

import lombok.Data;

@Data
/**
 * 描述：封装企业微信客服发送消息接口响应。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class WecomSendMsgResponse {

    private Integer errcode;
    private String errmsg;
    private String msgid;
}
