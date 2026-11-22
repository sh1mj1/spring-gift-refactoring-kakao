# spring-gift-refactoring

## 로컬에서 실행하기

JDK 21을 사용합니다. 프로젝트 루트에서 다음 명령을 실행합니다.

```bash
./gradlew bootRun
```

- [상품 관리](http://localhost:8080/admin/products)
- [회원 관리 및 포인트 충전](http://localhost:8080/admin/members)
- [상품 목록 API](http://localhost:8080/api/products)

기본 주소 `/`에는 화면이 없으므로 위 주소로 접속합니다. 별도 DB 설정 없이 내장 H2가 사용됩니다. 8080 포트가 이미 사용 중이라면 `./gradlew bootRun --args='--server.port=8081'`로 실행하고 주소의 포트를 8081로 바꿉니다.

카카오 설정 없이 관리자 화면과 이메일 회원가입·로그인, 위시리스트, 포인트 주문을 사용할 수 있습니다. 위시리스트와 주문 API에는 이메일 로그인으로 발급받은 JWT를 `Authorization: Bearer <JWT>` 헤더에 넣습니다. 주문에 필요한 포인트는 회원 관리 화면에서 충전합니다.

카카오 로그인을 연습할 때는 `KAKAO_CLIENT_ID` 등 카카오 앱 설정을 환경 변수로 지정합니다. 카카오톡 주문 알림은 회원에게 저장된 카카오 액세스 토큰이 있을 때 발송을 시도합니다.

## 용어 사전

[프로젝트 용어 사전](docs/glossary.md)
