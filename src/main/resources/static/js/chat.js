/**
 * Terminal Chat Client
 *
 * SSE(Server-Sent Events)를 사용하여 서버로부터 실시간 스트리밍 응답을 받습니다.
 *
 * SSE 작동 방식:
 * 1. EventSource 객체로 서버에 연결
 * 2. 서버가 "data: {텍스트}\n\n" 형식으로 데이터 전송
 * 3. onmessage 이벤트로 각 청크 수신
 * 4. 연결이 끊어지면 자동 재연결 시도 (브라우저 기본 동작)
 */

// 대화 세션 ID (서버에서 발급받거나 로컬에서 생성)
let conversationId = localStorage.getItem('conversationId') || generateUUID();

// DOM 요소 참조
const chatContainer = document.getElementById('chatContainer');
const messageInput = document.getElementById('messageInput');

// 현재 스트리밍 중인 EventSource 객체
let currentEventSource = null;

/**
 * UUID v4 생성
 * 대화 세션을 고유하게 식별하기 위해 사용
 */
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

/**
 * 세션 ID 저장 및 갱신
 */
function saveConversationId() {
    localStorage.setItem('conversationId', conversationId);
}

/**
 * 메시지를 채팅 컨테이너에 추가
 * @param {string} type - 메시지 타입: 'user', 'ai', 'system', 'error'
 * @param {string} content - 메시지 내용
 * @param {boolean} isStreaming - 스트리밍 중인 메시지인지 여부
 * @returns {HTMLElement} - 생성된 메시지 요소
 */
function addMessage(type, content, isStreaming = false) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}${isStreaming ? ' streaming' : ''}`;

    const promptSpan = document.createElement('span');
    promptSpan.className = `prompt ${type}-prompt`;

    // 프롬프트 텍스트 설정
    const promptTexts = {
        user: 'user',
        ai: 'claude',
        system: 'system',
        error: 'error'
    };
    promptSpan.textContent = promptTexts[type] || type;

    const contentSpan = document.createElement('span');
    contentSpan.className = 'content';
    contentSpan.textContent = content;

    messageDiv.appendChild(promptSpan);
    messageDiv.appendChild(contentSpan);
    chatContainer.appendChild(messageDiv);

    // 자동 스크롤
    scrollToBottom();

    return messageDiv;
}

/**
 * 채팅 컨테이너를 맨 아래로 스크롤
 */
function scrollToBottom() {
    chatContainer.scrollTop = chatContainer.scrollHeight;
}

/**
 * 입력 필드 활성화/비활성화
 * @param {boolean} enabled - 활성화 여부
 */
function setInputEnabled(enabled) {
    messageInput.disabled = !enabled;
    if (enabled) {
        messageInput.focus();
    }
}

/**
 * SSE를 통해 AI에게 메시지 전송 및 스트리밍 응답 수신
 *
 * SSE 장점:
 * - HTTP 기반으로 방화벽/프록시 친화적
 * - 자동 재연결 기능 내장
 * - 단순한 텍스트 프로토콜
 *
 * @param {string} message - 사용자 메시지
 */
function sendMessage(message) {
    // 사용자 메시지 표시
    addMessage('user', message);

    // 입력 비활성화 (응답 완료까지)
    setInputEnabled(false);

    // AI 응답 메시지 요소 생성 (스트리밍 모드)
    const aiMessageDiv = addMessage('ai', '', true);
    const contentSpan = aiMessageDiv.querySelector('.content');

    // URL 인코딩하여 쿼리 파라미터 구성
    const params = new URLSearchParams({
        message: message,
        conversationId: conversationId
    });

    // EventSource로 SSE 연결
    // GET 요청만 지원하므로 쿼리 파라미터로 데이터 전송
    const eventSource = new EventSource(`/api/chat?${params.toString()}`);
    currentEventSource = eventSource;

    let fullResponse = '';

    /**
     * 메시지 수신 핸들러
     * 서버에서 "data: {텍스트}" 형식으로 전송한 데이터 수신
     */
    eventSource.onmessage = function(event) {
        // 각 청크를 누적하여 전체 응답 구성
        fullResponse += event.data;
        contentSpan.textContent = fullResponse;
        scrollToBottom();
    };

    /**
     * 에러 핸들러
     * 연결 오류 또는 서버 오류 처리
     */
    eventSource.onerror = function(error) {
        console.error('SSE Error:', error);

        // 연결 종료 (정상 종료인 경우도 onerror 호출됨)
        eventSource.close();
        currentEventSource = null;

        // 스트리밍 완료 표시 (커서 제거)
        aiMessageDiv.classList.remove('streaming');

        // 응답이 비어있으면 에러 메시지 표시
        if (fullResponse === '') {
            contentSpan.textContent = '응답을 받지 못했습니다. 네트워크 연결을 확인해주세요.';
            aiMessageDiv.classList.add('error');
        }

        // 입력 다시 활성화
        setInputEnabled(true);
    };

    /**
     * 연결 성공 핸들러
     */
    eventSource.onopen = function() {
        console.log('SSE Connection opened');
    };
}

/**
 * 대화 기록 초기화
 */
async function clearConversation() {
    try {
        const response = await fetch(`/api/clear?conversationId=${conversationId}`, {
            method: 'POST'
        });

        if (response.ok) {
            // 화면의 메시지 삭제 (시스템 메시지 제외)
            const messages = chatContainer.querySelectorAll('.message:not(.system)');
            messages.forEach(msg => msg.remove());

            addMessage('system', '대화 기록이 초기화되었습니다.');
        }
    } catch (error) {
        console.error('Clear error:', error);
        addMessage('error', '대화 기록 초기화에 실패했습니다.');
    }
}

/**
 * 새 세션 시작
 */
async function startNewSession() {
    try {
        // 기존 대화 기록 초기화
        await clearConversation();

        // 새 세션 ID 발급
        const response = await fetch('/api/session');
        const data = await response.json();

        conversationId = data.conversationId;
        saveConversationId();

        addMessage('system', `새 세션이 시작되었습니다. (ID: ${conversationId.substring(0, 8)}...)`);
    } catch (error) {
        console.error('New session error:', error);
        addMessage('error', '새 세션 시작에 실패했습니다.');
    }
}

/**
 * 명령어 처리
 * @param {string} input - 사용자 입력
 * @returns {boolean} - 명령어였는지 여부
 */
function handleCommand(input) {
    const command = input.trim().toLowerCase();

    switch (command) {
        case '/clear':
            clearConversation();
            return true;
        case '/new':
            startNewSession();
            return true;
        case '/help':
            addMessage('system', '사용 가능한 명령어:');
            addMessage('system', '  /clear - 대화 기록 초기화');
            addMessage('system', '  /new   - 새 세션 시작');
            addMessage('system', '  /help  - 도움말 표시');
            return true;
        default:
            return false;
    }
}

/**
 * 입력 이벤트 핸들러
 */
messageInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        const message = messageInput.value.trim();

        if (message === '') return;

        // 입력 필드 초기화
        messageInput.value = '';

        // 명령어 확인
        if (message.startsWith('/')) {
            if (handleCommand(message)) {
                return;
            }
        }

        // AI에게 메시지 전송
        sendMessage(message);
    }
});

/**
 * 페이지 로드 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
    // 세션 ID 저장
    saveConversationId();

    // 세션 정보 표시
    addMessage('system', `세션 ID: ${conversationId.substring(0, 8)}...`);

    // 입력 필드에 포커스
    messageInput.focus();
});

/**
 * 페이지 종료 시 정리
 */
window.addEventListener('beforeunload', function() {
    // 진행 중인 SSE 연결 종료
    if (currentEventSource) {
        currentEventSource.close();
    }
});
