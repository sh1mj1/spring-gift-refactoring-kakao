# 스타일 정리

Gradle Checkstyle을 운영 Java에 적용한다. `FileTabCharacter`, `RegexpSingleline`(줄 끝 공백), `AvoidStarImport`, `UnusedImports`, `Indentation`을 사용한다. 블록과 줄바꿈의 기본 들여쓰기는 4칸으로 두고 `forceStrictCondition=false`를 적용한다.

린트 도입과 용어 정리는 별도 커밋으로 진행한다. [용어 사전](../../glossary.md)에 맞춰 주문 금액 지역 변수에 `totalPrice`를 사용하고 서비스 JWT와 카카오 토큰을 `accessToken`, `kakaoAccessToken`으로 구분한다. JPA 엔티티 클래스·필드·접근자, DB 테이블·컬럼, 요청·응답 DTO, 오류 메시지와 카카오 알림 문구를 보존한다.

## 실행과 검증

- 메인 세션(Medium 설정 요청)에서 승인과 검증 결과를 관리한다. 이 작업은 별도의 GPT-6.2-Sol Low 워커에서 직전 작업의 커밋을 바탕으로 실행한다.
- 각 커밋 전에 Java 21로 `sh ./gradlew test --rerun-tasks`를 실행한다. 기존 인수 테스트 5개 클래스의 48건이 모두 통과해야 한다. 테스트 소스와 실행 설정을 유지한다.
- 작업 완료 시 `sh ./gradlew checkstyleMain --rerun-tasks` 결과를 보고한다. 수동 린트 수정이 필요하면 파일·줄·규칙을 보고하고 사용자 지시를 기다린다.
- 커밋에는 목적 하나를 담고 `refactor: [구조 변경] …` 형식의 한 줄 메시지를 사용한다.
- 전·중·후에 발견하는 작동 변경 후보는 [작업 목록](../../tasks.md)에 기록한다.
