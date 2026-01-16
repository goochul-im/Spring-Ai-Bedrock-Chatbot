package com.chatbot.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * 챗봇 서비스 클래스
 *
 * Spring AI의 ChatClient를 사용하여 AWS Bedrock Claude 모델과 통신합니다.
 * 이 서비스는 동기(blocking)와 비동기(streaming) 두 가지 방식의 채팅을 지원합니다.
 *
 * 주요 기능:
 * 1. 스트리밍 응답 - SSE를 통해 실시간으로 응답 전송
 * 2. 대화 컨텍스트 유지 - conversationId로 세션별 대화 기록 관리
 * 3. 새 대화 시작 - 기존 대화 기록 초기화
 */
@Service
class ChatService(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory
) {

    /**
     * 스트리밍 방식으로 AI 응답을 받아옵니다.
     *
     * Flux<String>은 리액티브 스트림으로, 데이터가 생성되는 대로 클라이언트에게
     * 실시간으로 전송됩니다. 이는 긴 응답도 사용자가 즉시 볼 수 있게 해줍니다.
     *
     * ChatClient 사용법:
     * 1. prompt() - 새로운 프롬프트 시작
     * 2. user(message) - 사용자 메시지 설정
     * 3. advisors(a -> ...) - Advisor에 파라미터 전달 (여기서는 conversationId)
     * 4. stream() - 스트리밍 모드로 실행
     * 5. content() - 응답 내용만 추출 (Flux<String> 반환)
     *
     * conversationId 작동 방식:
     * - MessageChatMemoryAdvisor가 이 ID를 사용해 대화 기록을 분리
     * - 같은 ID = 같은 대화 세션 (이전 대화 기록 포함)
     * - 다른 ID = 새로운 대화 세션 (빈 상태에서 시작)
     *
     * @param message 사용자의 입력 메시지
     * @param conversationId 대화 세션을 구분하는 고유 ID (예: UUID)
     * @return Flux<String> - 스트리밍으로 전송되는 AI 응답 텍스트 조각들
     */
    fun streamChat(message: String, conversationId: String): Flux<String> {
        return chatClient.prompt()
            .user(message)
            // ChatMemory.CONVERSATION_ID 파라미터를 통해 대화 세션 지정
            // 이 값은 MessageChatMemoryAdvisor에서 사용됨
            .advisors { advisorSpec ->
                advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId)
            }
            // stream() 호출로 스트리밍 모드 활성화
            // 내부적으로 Bedrock의 InvokeModelWithResponseStream API 사용
            .stream()
            // content()는 응답에서 텍스트 내용만 추출
            // chatResponse()를 사용하면 메타데이터(토큰 수 등)도 포함
            .content()
    }

    /**
     * 동기(블로킹) 방식으로 AI 응답을 받아옵니다.
     *
     * 스트리밍이 필요 없는 경우(짧은 응답, 백그라운드 처리 등)에 사용합니다.
     * 전체 응답이 완성될 때까지 대기한 후 한 번에 반환합니다.
     *
     * @param message 사용자의 입력 메시지
     * @param conversationId 대화 세션 ID
     * @return String - 완성된 AI 응답 텍스트
     */
    fun chat(message: String, conversationId: String): String {
        return chatClient.prompt()
            .user(message)
            .advisors { advisorSpec ->
                advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId)
            }
            // call()은 동기 방식으로 전체 응답을 기다림
            .call()
            // content()로 응답 텍스트 추출 (null인 경우 빈 문자열 반환)
            .content() ?: ""
    }

    /**
     * 특정 대화 세션의 기록을 삭제하여 새로운 대화를 시작합니다.
     *
     * ChatMemory.clear() 메서드를 호출하여 해당 conversationId의
     * 모든 이전 메시지를 제거합니다.
     *
     * 사용 시나리오:
     * - 사용자가 "새 대화" 버튼 클릭
     * - 세션 타임아웃
     * - 대화 컨텍스트 리셋 필요 시
     *
     * @param conversationId 초기화할 대화 세션 ID
     */
    fun clearConversation(conversationId: String) {
        chatMemory.clear(conversationId)
    }
}
