package com.doumi.donation.chatbot.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.JedisPooled;

@Configuration
public class ChatbotConfig {

    @Bean
    public JedisPooled jedisPooled(@Value("${chatbot.redis.host}") String host,
                                   @Value("${chatbot.redis.port}") int port) {
        return new JedisPooled(host, port);
    }

    /**
     * 캠페인 벡터 저장소.
     * category/region/status를 TAG 필드로 색인해야 검색 시 필터 표현식을 쓸 수 있음
     * (필드 정의가 properties로는 불가능해서 빈을 직접 구성)
     * 반환 타입이 RedisVectorStore여야 자동설정(RedisVectorStoreAutoConfiguration)이 이 빈을 보고 물러남
     */
    @Bean
    public RedisVectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("campaign-index")
                .prefix("campaign:")
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("category"),
                        RedisVectorStore.MetadataField.tag("region"),
                        RedisVectorStore.MetadataField.tag("status"),
                        RedisVectorStore.MetadataField.numeric("campaignId"))
                .initializeSchema(true)
                .build();
    }

    /** 세션별 멀티턴 대화 기록 (서버 메모리, 최근 20개 메시지 유지) */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }
}
