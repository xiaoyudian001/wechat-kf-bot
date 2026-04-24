package com.example.wechatbot.config;

import com.example.wechatbot.repository.CallbackEventRepository;
import com.example.wechatbot.repository.ChatMessageRepository;
import com.example.wechatbot.service.PersistentCallbackEventService;
import com.example.wechatbot.service.PersistentConversationMemoryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * 描述：在仓储 Bean 存在时装配持久化服务门面。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class PersistentStorageConfig {

    @Bean
    @ConditionalOnBean(ChatMessageRepository.class)
    /**
     * 描述：创建持久化会话记忆服务。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public PersistentConversationMemoryService persistentConversationMemoryService(
            ChatMessageRepository chatMessageRepository) {
        return new PersistentConversationMemoryService(chatMessageRepository);
    }

    @Bean
    @ConditionalOnBean(CallbackEventRepository.class)
    /**
     * 描述：创建持久化回调事件服务。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public PersistentCallbackEventService persistentCallbackEventService(
            CallbackEventRepository callbackEventRepository) {
        return new PersistentCallbackEventService(callbackEventRepository);
    }
}
