package com.doumi.donation.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Redis 캐시 설정.
 * 통계 대시보드처럼 조회 빈도는 높고 실시간성 요구는 낮은 응답을 캐싱한다.
 * 신선도는 TTL 5분으로만 관리한다 — 통계는 플랫폼 전체 누적치라 최대 5분 지연이
 * 사용자에게 드러나지 않고(기부 반영 여부는 캠페인 상세의 모금액으로 확인한다),
 * 쓰기마다 무효화하면 캐시가 계속 비어 캐싱 효과 자체가 사라지기 때문.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String STATS_CACHE = "stats";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
