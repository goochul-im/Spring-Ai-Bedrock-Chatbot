# Spring AI Bedrock Chatbot

AWS Bedrock의 Claude 모델을 활용한 실시간 스트리밍 챗봇 애플리케이션입니다.
Spring AI를 학습하기 위한 목적으로 제작되었으며, 모든 핵심 코드에 상세한 한글 주석이 포함되어 있습니다.

## 목적

- **Spring AI 학습**: Spring AI의 ChatClient, ChatMemory, Advisor 패턴 이해
- **AWS Bedrock 연동**: AWS Bedrock Converse API를 통한 Claude 모델 사용법 학습
- **SSE 스트리밍**: Server-Sent Events를 활용한 실시간 응답 스트리밍 구현
- **대화 컨텍스트 관리**: 세션 기반 대화 기록 유지 방법 학습

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Kotlin 1.9.25 |
| Framework | Spring Boot 3.5.9 |
| AI | Spring AI 1.1.2, AWS Bedrock Converse API |
| Model | Claude (Bedrock) |
| Template | Thymeleaf |
| Reactive | Spring WebFlux (SSE 스트리밍) |
| Cache | Caffeine |
| Build | Gradle (Kotlin DSL) |

## 프로젝트 구조

```
src/main/kotlin/com/chatbot/
├── ChatbotApplication.kt           # Spring Boot 진입점
├── config/
│   ├── BedrockConfig.kt            # ChatClient, ChatMemory 설정
│   └── CacheConfig.kt              # Caffeine 캐시 설정
├── service/
│   └── ChatService.kt              # 챗봇 비즈니스 로직
└── controller/
    └── ChatController.kt           # REST API 엔드포인트

src/main/resources/
├── application.properties          # 애플리케이션 설정
├── templates/
│   └── index.html                  # 채팅 UI 템플릿
└── static/
    ├── css/terminal.css            # 터미널 스타일
    └── js/chat.js                  # SSE 클라이언트
```

## 시작하기

### 사전 요구사항

- JDK 17 이상
- AWS 계정 및 Bedrock 액세스 권한
- Bedrock에서 Claude 모델 활성화

### 1. 환경변수 설정

```bash
# Windows (PowerShell)
$env:AWS_ACCESS_KEY_ID="your-access-key"
$env:AWS_SECRET_ACCESS_KEY="your-secret-key"
$env:AWS_REGION="us-east-1"
$env:BEDROCK_MODEL_ID="your-claude-model-id"

# Linux/macOS
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_REGION=us-east-1
export BEDROCK_MODEL_ID=your-claude-model-id
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 접속

브라우저에서 http://localhost:8080 접속

## 사용 가능한 Claude 모델 ID

| 모델 | Model ID |
|------|----------|
| Claude 3.5 Sonnet v2 | `anthropic.claude-3-5-sonnet-20241022-v2:0` |
| Claude 3.5 Sonnet v1 | `anthropic.claude-3-5-sonnet-20240620-v1:0` |
| Claude 3 Opus | `anthropic.claude-3-opus-20240229-v1:0` |
| Claude 3 Haiku | `anthropic.claude-3-haiku-20240307-v1:0` |

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/` | 채팅 UI 페이지 |
| GET | `/api/chat` | SSE 스트리밍 채팅 |
| POST | `/api/chat` | 동기 채팅 |
| POST | `/api/clear` | 대화 기록 초기화 |
| GET | `/api/session` | 새 세션 ID 발급 |

## 터미널 UI 명령어

- `/clear` - 대화 기록 초기화
- `/new` - 새 세션 시작
- `/help` - 도움말 표시

## 학습 포인트

각 파일에 상세한 주석이 포함되어 있습니다:

- **BedrockConfig.kt**: ChatClient 빌더 패턴, ChatMemory 전략, Advisor 개념
- **ChatService.kt**: 스트리밍 vs 블로킹 호출, Flux 사용법
- **ChatController.kt**: SSE MediaType, 리액티브 엔드포인트
- **chat.js**: EventSource API, SSE 클라이언트 구현
