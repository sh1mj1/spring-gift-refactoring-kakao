# 작업 목록

## 작동 변경 후보

구조 변경 중 발견한 작동은 현재 증거와 결정할 정책을 함께 기록한다. 각 항목의 배경은 [확인 필요 목록](open-questions.md)에 있다. 아래 코드 링크는 리팩토링 전 위치를 가리킨다.

| 작동 | 실제 확인 | 코드에 있는 이유와 위치 | 확인할 질문 |
| --- | --- | --- | --- |
| 같은 상품을 위시에 다시 담으면 기존 위시가 반환됨 | 처음 `201`, 다시 담기 `200`과 같은 위시 ID | 중복을 확인하고 기존 위시를 반환한다. 왜 이 응답을 택했는지는 설명이 없다. [위시 중복 처리](../src/main/java/gift/wish/WishController.java#L68) | 중복 담기를 성공으로 보고 기존 위시를 돌려주는 정책을 유지할까? |
| 옵션이 하나뿐인 상품은 마지막 옵션을 삭제할 수 없음 | 마지막 옵션 삭제 `400`. 새 상품 등록 직후 옵션 조회는 `200`과 `[]`. | 상품에는 항상 옵션이 하나 이상 있어야 한다는 주석이 있고, 삭제에서 이를 검사한다. 상품 등록 직후에는 그 규칙을 충족하지 않는다. [옵션 정책 주석](../src/main/java/gift/option/OptionController.java#L20) · [옵션 삭제](../src/main/java/gift/option/OptionController.java#L79) · [상품 등록](../src/main/java/gift/product/ProductController.java#L48) | 옵션이 없는 상품은 등록 단계에서만 허용하는가? 주문 가능한 상품으로 공개하려면 옵션이 필수인가? |
| 카카오 알림 발송이 실패해도 주문은 성공 | 발송 실패는 미재현. 카카오 액세스 토큰이 없는 회원의 주문은 `201`. | `best-effort kakao notification` 주석과 `sendKakaoMessageIfPossible` 이름이 발송을 시도하는 정책을 드러낸다. 주문 저장 후 발송하고 예외를 무시한다. 왜 실패 기록이나 재시도가 없는지는 설명이 없다. [발송 정책과 예외 처리](../src/main/java/gift/order/OrderController.java#L95) | 주문 성공을 유지하더라도 발송 실패를 기록하거나 재시도해야 할까? |
| 없는 상품을 삭제해도 `204` | 상품 삭제 후 같은 주소로 삭제를 반복하니 `204`. 상세 조회는 `404`. | 존재 여부를 따로 검사하지 않고 삭제를 요청한 뒤 `204`를 보낸다. 상태 코드 선택의 이유는 없다. [상품 삭제](../src/main/java/gift/product/ProductController.java#L84) | 이미 없는 상품도 삭제 완료로 응답하는 정책을 유지할까? |
| 토큰 없이 상품을 등록·삭제할 수 있음 | 토큰 없이 상품 등록 `201`, 삭제 `204`. 카테고리·옵션 변경과 관리자 폼도 인증 없이 성공. | 상품 변경 API와 관리자 화면에서 인증이나 권한을 검사하지 않는다. 공개 접근의 이유는 없다. [상품 API](../src/main/java/gift/product/ProductController.java#L48) · [상품 관리 화면](../src/main/java/gift/product/AdminProductController.java#L27) · [회원 관리 화면](../src/main/java/gift/member/AdminMemberController.java#L28) | 어떤 조회가 공개여야 하며, 상품 변경과 포인트 충전은 어떤 역할에 허용할까? |
| 주문이 있는 상품을 삭제하면 `500` | 주문이 참조하는 옵션을 가진 상품 2 삭제 시 `500`. 이후 상품 조회는 `200`. 서버 로그에는 주문이 옵션을 참조한다는 외래 키 제약 위반이 남음. | 외래 키 제약은 실패 원인을 설명한다. 삭제를 막을 때 `500`으로 응답하는 정책 설명은 없다. [주문과 옵션의 참조 관계](../src/main/resources/db/migration/V1__Initialize_project_tables.sql#L47) · [상품 삭제](../src/main/java/gift/product/ProductController.java#L84) | 주문이 있는 상품은 판매만 중단해야 할까? 삭제를 거부할 경우 어떤 응답을 돌려줘야 할까? |
| 포인트가 부족하면 주문은 실패하고 재고만 줄어듦 | `500`, 재고 50→49, 포인트 차감과 새 주문 생성 없음. | 포인트를 확인하기 전에 재고를 저장한다. 실패 시 재고를 남겨야 한다는 설명은 없다. [재고·포인트 처리 순서](../src/main/java/gift/order/OrderController.java#L86) | 주문이 실패하면 재고와 포인트를 주문 전 상태로 함께 되돌려야 할까? |
| 주문한 상품이 위시에 남음 | 해당 상품을 위시에 담고 주문한 뒤에도 같은 위시가 조회됨. | 위시 정리 단계는 주석에 있다. 주문 저장부터 응답까지 위시를 삭제하는 동작은 없다. [주문 흐름 주석](../src/main/java/gift/order/OrderController.java#L61) · [주문 저장 이후 처리](../src/main/java/gift/order/OrderController.java#L95) | 주문 성공 시 위시를 자동으로 정리하는 것이 현재 정책에도 맞을까? |
| 주문 생성 응답의 `Location` 주소로 조회하면 `404` | 생성 `201`에 포함된 `/api/orders/5`를 같은 토큰으로 조회하면 `404`. 주문 목록에는 해당 주문이 있음. | 생성 응답은 개별 주문 주소를 넣지만 조회는 회원의 주문 목록만 제공한다. 이유는 없다. [주문 조회](../src/main/java/gift/order/OrderController.java#L47) · [생성 응답](../src/main/java/gift/order/OrderController.java#L95) | 주문 상세 조회를 제공할까? 생성 응답에 어떤 주소를 넣어야 할까? |
| 인증 헤더에서 `Bearer`를 생략해도 인증됨 | 유효한 토큰만 넣어 위시와 주문을 조회하니 각각 `200`. | 접두사를 제거하고 남은 값을 검증한다. 접두사를 필수로 요구하지 않는 이유는 없다. [인증 토큰 처리](../src/main/java/gift/auth/AuthenticationResolver.java#L25) | 토큰만 전달하는 형식도 지원해야 할까? |
| 0 포인트 충전은 `500` | 관리자 화면의 충전 요청에 0을 보내니 `500`. | 0 이하를 거부하는 조건은 있다. 화면에 입력 오류를 돌려주는 처리나 `500`을 선택한 이유는 확인되지 않는다. [포인트 충전 규칙](../src/main/java/gift/member/Member.java#L49) · [관리자 충전 요청](../src/main/java/gift/member/AdminMemberController.java#L75) | 잘못된 충전 요청에 어떤 안내를 보여줘야 할까? |
| 상품에 옵션이 하나 이하일 때 삭제 대상보다 옵션 개수를 먼저 확인함 | 코드에서 확인. 존재하지 않거나 다른 상품 소속인 옵션 ID에도 마지막 옵션 삭제 제한이 먼저 적용될 수 있다. | 옵션 ID 조회 전에 옵션 개수를 검사한다. [옵션 삭제](../src/main/java/gift/option/OptionService.java#L46) | 삭제 대상이 없으면 옵션 개수와 관계없이 먼저 `404`를 반환할까? |
| 상품 수정의 존재 확인과 이름 검증 순서가 관리자 화면과 API에서 다름 | 코드에서 확인. 없는 상품에 잘못된 이름을 보내면 관리자 화면은 상품 조회가 먼저이고, API는 이름 검증이 먼저다. | 두 수정 경로에서 조회와 검증의 실행 순서가 다르다. [관리자 수정](../src/main/java/gift/product/AdminProductController.java#L62) · [API 수정](../src/main/java/gift/product/ProductController.java#L53) | 상품이 없고 이름도 잘못된 경우 어느 오류를 우선할까? |

### 주문 작동 결정

- 포인트 부족 시 재고가 줄어드는 작동과 카카오 발송 실패를 무시하는 작동은 [주문 트랜잭션 ADR](adr/0001-order-transaction-boundary.md)의 정책을 적용한다.
- 카카오 로그인은 회원·카카오 토큰을 저장한 뒤 서비스 JWT를 발급한다. 토큰 발급 실패 후 회원 변경이 남아야 할지 후속 검토한다.
- 옵션 개수 검사와 삭제 사이에 동시 요청이 들어오는 경우의 마지막 옵션 정책과 동시성 제어를 후속 검토한다.
- 주문한 상품이 위시에 남는 작동은 [위시 정리 ADR](adr/0002-order-wish-cleanup.md)의 정책을 적용한다.
