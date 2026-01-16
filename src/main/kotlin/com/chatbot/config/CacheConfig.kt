package com.chatbot.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * 캐시 설정 클래스
 *
 * 대화 세션 정보와 메타데이터를 캐시에 저장하여 빠른 접근을 제공합니다.
 * Caffeine은 고성능 Java 캐시 라이브러리로, Guava Cache의 후속작입니다.
 *
 * 캐시 사용 목적:
 * 1. 세션 메타데이터 저장 (생성 시간, 마지막 활동 시간 등)
 * 2. 대화 통계 정보 캐싱
 * 3. 자주 사용되는 응답 캐싱 (선택적)
 *
 * 참고: ChatMemory(대화 기록)는 Spring AI의 MessageWindowChatMemory가 관리하고,
 * 이 캐시는 그 외의 세션 관련 정보를 저장하는 데 사용됩니다.
 */
@Configuration
@EnableCaching  // Spring Cache 추상화 활성화 - @Cacheable, @CacheEvict 등 사용 가능
class CacheConfig {

    /**
     * Caffeine Cache Manager 설정
     *
     * Caffeine 주요 설정:
     * - expireAfterAccess: 마지막 접근 후 만료 시간
     * - expireAfterWrite: 작성 후 만료 시간
     * - maximumSize: 캐시 최대 항목 수 (LRU 방식으로 제거)
     * - recordStats: 캐시 통계 기록 활성화
     *
     * 세션 캐시 전략:
     * - 30분 동안 접근이 없으면 세션 만료
     * - 최대 1000개 세션 동시 관리
     * - 메모리 효율을 위해 약한 참조 사용 가능 (weakKeys, weakValues)
     */
    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()

        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                // 마지막 접근 후 30분이 지나면 캐시 항목 만료
                // 사용자가 30분간 대화하지 않으면 세션 정보 삭제
                .expireAfterAccess(30, TimeUnit.MINUTES)
                // 최대 1000개의 캐시 항목 유지
                // 초과 시 가장 오래 접근하지 않은 항목부터 제거 (LRU)
                .maximumSize(1000)
                // 캐시 히트율 등 통계 정보 기록 (모니터링용)
                .recordStats()
        )

        return cacheManager
    }
}
