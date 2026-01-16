package com.chatbot.controller

import com.chatbot.service.ChatService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.util.UUID

/**
 * 챗봇 컨트롤러
 *
 * 웹 인터페이스와 AI 서비스를 연결하는 컨트롤러입니다.
 * SSE(Server-Sent Events)를 사용하여 실시간 스트리밍 응답을 제공합니다.
 *
 * 엔드포인트 구조:
 * - GET  /           : 메인 채팅 페이지 (Thymeleaf)
 * - GET  /api/chat   : SSE 스트리밍 채팅 API
 * - POST /api/chat   : 동기 채팅 API (선택적)
 * - POST /api/clear  : 대화 기록 초기화
 * - GET  /api/session: 새 세션 ID 생성
 *
 * SSE vs WebSocket:
 * - SSE: 단방향(서버→클라이언트), HTTP 기반, 자동 재연결, 간단한 구현
 * - WebSocket: 양방향, 별도 프로토콜, 더 복잡하지만 유연함
 * - 챗봇처럼 서버가 주로 데이터를 보내는 경우 SSE가 적합
 */
@Controller
class ChatController(
    private val chatService: ChatService
) {

    /**
     * 메인 채팅 페이지를 렌더링합니다.
     *
     * Thymeleaf 템플릿 엔진이 templates/index.html을 찾아 렌더링합니다.
     * 반환값 "index"는 src/main/resources/templates/index.html에 매핑됩니다.
     */
    @GetMapping("/")
    fun index(): String {
        return "index"
    }

    /**
     * SSE 스트리밍 채팅 API
     *
     * Server-Sent Events를 사용하여 AI 응답을 실시간으로 스트리밍합니다.
     *
     * MediaType.TEXT_EVENT_STREAM_VALUE:
     * - Content-Type: text/event-stream
     * - 브라우저의 EventSource API와 호환
     * - 각 데이터 청크가 "data: {content}\n\n" 형식으로 전송됨
     *
     * Flux<String> 반환:
     * - Spring WebFlux가 자동으로 SSE 형식으로 변환
     * - 각 문자열이 개별 SSE 이벤트로 전송됨
     * - 클라이언트는 실시간으로 텍스트를 받아 화면에 표시
     *
     * @param message 사용자 메시지 (URL 파라미터)
     * @param conversationId 대화 세션 ID (URL 파라미터)
     * @return Flux<String> - SSE 스트림으로 전송되는 응답 텍스트
     */
    @GetMapping(
        path = ["/api/chat"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    @ResponseBody
    fun streamChat(
        @RequestParam message: String,
        @RequestParam conversationId: String
    ): Flux<String> {
        // ChatService에서 반환된 Flux가 그대로 SSE 스트림으로 전송됨
        // Spring이 자동으로 각 요소를 SSE 이벤트로 변환
        return chatService.streamChat(message, conversationId)
    }

    /**
     * 동기 채팅 API (비스트리밍)
     *
     * 스트리밍이 필요 없는 경우를 위한 대체 API입니다.
     * 전체 응답이 완성된 후 한 번에 반환합니다.
     *
     * 사용 사례:
     * - 짧은 응답이 예상되는 경우
     * - SSE를 지원하지 않는 클라이언트
     * - 백엔드 간 통신
     *
     * @param request 채팅 요청 DTO
     * @return ChatResponse - 완성된 응답
     */
    @PostMapping("/api/chat")
    @ResponseBody
    fun chat(@RequestBody request: ChatRequest): ChatResponse {
        val response = chatService.chat(request.message, request.conversationId)
        return ChatResponse(response)
    }

    /**
     * 대화 기록 초기화 API
     *
     * 특정 세션의 대화 기록을 모두 삭제합니다.
     * 사용자가 새로운 대화를 시작하고 싶을 때 호출됩니다.
     *
     * @param conversationId 초기화할 세션 ID
     * @return ResponseEntity - 성공 응답
     */
    @PostMapping("/api/clear")
    @ResponseBody
    fun clearConversation(@RequestParam conversationId: String): ResponseEntity<Map<String, String>> {
        chatService.clearConversation(conversationId)
        return ResponseEntity.ok(mapOf("status" to "cleared"))
    }

    /**
     * 새 세션 ID 생성 API
     *
     * 클라이언트가 새로운 대화를 시작할 때 고유한 세션 ID를 발급합니다.
     * UUID를 사용하여 충돌 없는 고유 식별자를 생성합니다.
     *
     * @return Map - 새로 생성된 세션 ID
     */
    @GetMapping("/api/session")
    @ResponseBody
    fun newSession(): Map<String, String> {
        return mapOf("conversationId" to UUID.randomUUID().toString())
    }
}

/**
 * 채팅 요청 DTO
 *
 * @property message 사용자 메시지
 * @property conversationId 대화 세션 ID
 */
data class ChatRequest(
    val message: String,
    val conversationId: String
)

/**
 * 채팅 응답 DTO
 *
 * @property content AI 응답 내용
 */
data class ChatResponse(
    val content: String
)
