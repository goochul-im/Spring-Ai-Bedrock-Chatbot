package com.chatbot.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring AI Bedrock 설정 클래스
 *
 * Spring AI는 다양한 AI 모델 제공자(OpenAI, Anthropic, AWS Bedrock 등)에 대해
 * 통일된 추상화 계층을 제공합니다. 이 설정 클래스에서는 AWS Bedrock을 통해
 * Claude 모델을 사용하기 위한 Bean들을 정의합니다.
 *
 * 주요 개념:
 * - ChatModel: AI 모델과의 기본 통신 인터페이스 (자동 주입됨)
 * - ChatClient: ChatModel을 감싸는 고수준 API, 플루언트 빌더 패턴 제공
 * - ChatMemory: 대화 기록을 저장하고 관리하는 인터페이스
 * - Advisor: 요청/응답을 가로채서 추가 기능(메모리, 로깅 등)을 적용
 */
@Configuration
class BedrockConfig {

    /**
     * ChatMemory Bean 생성
     *
     * ChatMemory는 대화 기록을 저장하는 인터페이스입니다.
     * MessageWindowChatMemory는 최근 N개의 메시지만 유지하는 슬라이딩 윈도우 방식입니다.
     *
     * 메모리 전략 옵션:
     * - MessageWindowChatMemory: 최근 N개 메시지 유지 (토큰 효율적)
     * - InMemoryChatMemory: 모든 메시지를 메모리에 저장 (세션 기반)
     * - JdbcChatMemoryRepository: DB에 영구 저장 (재시작 후에도 유지)
     *
     * maxMessages 설정 시 고려사항:
     * - Claude 모델의 컨텍스트 윈도우 크기 (200K 토큰)
     * - 너무 많으면 비용 증가, 너무 적으면 맥락 손실
     * - 일반적으로 10-20개가 적절
     */
    @Bean
    fun chatMemory(): ChatMemory {
        return MessageWindowChatMemory.builder()
            .maxMessages(20)  // 최근 20개 메시지(사용자+AI)를 컨텍스트로 유지
            .build()
    }

    /**
     * ChatClient Bean 생성
     *
     * ChatClient는 Spring AI의 핵심 컴포넌트로, AI 모델과 상호작용하기 위한
     * 플루언트 API를 제공합니다.
     *
     * @param chatModel - Spring AI가 자동 구성한 Bedrock ChatModel
     *                   application.properties의 설정을 기반으로 자동 주입됨
     * @param chatMemory - 위에서 정의한 대화 기록 저장소
     *
     * Advisor 패턴:
     * - Advisor는 AOP의 Advice와 유사한 개념
     * - 요청 전/후에 추가 로직을 적용할 수 있음
     * - MessageChatMemoryAdvisor: 자동으로 대화 기록을 프롬프트에 추가
     *
     * defaultSystem 사용 예시:
     * .defaultSystem("당신은 친절한 AI 어시스턴트입니다.")
     */
    @Bean
    fun chatClient(
        chatModel: ChatModel,
        chatMemory: ChatMemory
    ): ChatClient {
        return ChatClient.builder(chatModel)
            // MessageChatMemoryAdvisor: 매 요청마다 자동으로 이전 대화 기록을 포함시킴
            // conversationId 파라미터로 여러 대화 세션을 분리할 수 있음
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .build()
            )
            // 시스템 프롬프트 설정 - 모든 대화에 적용될 기본 지시사항
            .defaultSystem("""
                당신은 터미널 환경에서 동작하는 AI 어시스턴트입니다.
                - 간결하고 명확하게 답변해주세요
                - 코드 예시가 필요한 경우 마크다운 코드 블록을 사용하세요
                - 한국어로 답변해주세요
            """.trimIndent())
            .build()
    }
}
