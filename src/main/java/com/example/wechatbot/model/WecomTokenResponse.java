package com.example.wechatbot.model;

import lombok.Data;

@Data
/**
 * 描述：封装企业微信 access_token 接口响应。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class WecomTokenResponse {

    private Integer errcode;
    private String errmsg;
    private String access_token;
    private Integer expires_in;
}
