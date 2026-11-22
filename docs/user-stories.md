# 사용자 스토리와 흐름

JDK 21로 별도 로컬 서버(`localhost:18080`)를 띄우고, 초기 데이터가 들어 있는 인메모리 H2에서 아래 요청을 직접 보냈다. 리다이렉트는 따라가지 않고 첫 응답의 상태 코드와 `Location`을 확인했다. 스토리의 목적은 기능에서 추론한 사용자 목표이며, 코드에서 근거를 찾을 수 없는 동작의 의도는 [확인 필요 목록](open-questions.md)에 남겼다.

## API 사용자 스토리

| 누가 | 무엇을 왜 하는가 | 관련 API | 실제 응답 |
| --- | --- | --- | --- |
| 방문자 o| 주문할 상품을 고르기 위해 상품 목록과 상세 정보를 본다. | `GET /api/products`, `GET /api/products/{id}` | 목록 `200`과 상품 6개. 상세 `200`과 상품 정보. 없는 상품은 `404`. |
| 상품 관리 담당자 | 판매 목록을 최신 상태로 유지하기 위해 상품을 등록·수정·삭제한다. | `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}` | 등록 `201`과 `Location: /api/products/7`. 수정 `200`과 변경된 상품. 삭제 `204`, 삭제한 상품의 상세 조회 `404`. 모두 토큰 없이 확인했다. |
| 방문자 | 상품을 분류별로 알아보기 위해 카테고리를 조회한다. | `GET /api/categories` | `200`과 카테고리 3개. |
| 상품 관리 담당자 | 상품을 분류할 기준을 관리하기 위해 카테고리를 등록·수정·삭제한다. | `POST /api/categories`, `PUT /api/categories/{id}`, `DELETE /api/categories/{id}` | 등록 `201`과 `Location: /api/categories/4`. 수정 `200`과 변경된 카테고리. 삭제 `204`. |
| 방문자 | 원하는 선택지가 있는지 확인하기 위해 상품의 옵션과 재고를 본다. | `GET /api/products/{productId}/options` | `200`과 검증용 상품의 옵션 2개. |
| 상품 관리 담당자 | 구매할 수 있는 선택지를 관리하기 위해 옵션을 추가하거나 삭제한다. | `POST /api/products/{productId}/options`, `DELETE /api/products/{productId}/options/{optionId}` | 추가 `201`과 생성된 옵션. 옵션이 2개일 때 하나를 삭제하면 `204`. 마지막 옵션 삭제는 `400`과 “옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.” |
| 비회원 | 위시와 주문을 이용할 계정을 만들기 위해 이메일로 가입한다. | `POST /api/members/register` | `201`과 인증 토큰. 같은 이메일로 다시 가입하면 `400`과 `Email is already registered.` |
| 회원 | 자신의 위시와 주문에 접근하기 위해 이메일과 비밀번호로 로그인한다. | `POST /api/members/login` | `200`과 인증 토큰. 잘못된 비밀번호는 `400`과 `Invalid email or password.` |
| 방문자 | 카카오 계정으로 서비스에 로그인한다. | `GET /api/auth/kakao/login`, `GET /api/auth/kakao/callback` | 로그인 시작 `302`와 카카오 인가 주소. 인가 코드 없이 콜백을 호출하면 `400`. 카카오 로그인·동의 후 인증 토큰을 받는 과정은 미확인. |
| 회원 | 나중에 다시 볼 상품을 모아 두기 위해 위시를 조회하고, 상품을 담거나 뺀다. | `GET /api/wishes`, `POST /api/wishes`, `DELETE /api/wishes/{id}` | 조회 `200`, 처음 담기 `201`, 같은 상품 다시 담기 `200`과 같은 위시 ID, 제거 `204`. 다른 회원의 위시 제거는 `403`. |
| 회원 | 원하는 옵션을 포인트로 구매하고 필요한 경우 선물 메시지를 남긴다. | `POST /api/orders` | `201`과 주문 수량 1, 선물 메시지, `Location: /api/orders/5`. 성공한 요청에서 재고는 49→48, 포인트는 30,000→5,000이었다. 별도 포인트 부족 요청은 `500`이었고 재고만 50→49로 줄었다. |
| 회원 | 구매한 내용을 다시 확인하기 위해 자신의 주문을 조회한다. | `GET /api/orders` | 주문 전 `200`과 빈 목록. 주문 후 `200`과 주문 1건. 생성 응답의 `Location`을 조회하면 `404`. |

위시·주문은 인증 토큰으로 호출했다. 위시 조회에서는 인증 헤더가 없으면 `400`, 잘못된 토큰이면 `401`이었다. 위시와 주문 조회 모두 인증 헤더에 유효한 토큰만 넣고 `Bearer `를 생략해도 `200`이었다.

## 관리자 화면 사용자 스토리

관리자는 화면에서 맡는 역할을 가리킨다. 아래 요청은 인증 헤더나 로그인 쿠키 없이 보냈다. 화면의 폼 전송과 동일하게 등록·수정·삭제·포인트 충전에 폼 데이터를 사용했다.

| 누가 | 무엇을 왜 하는가 | 관리자 화면의 요청 | 실제 응답 |
| --- | --- | --- | --- |
| 관리자 | 판매 목록을 최신 상태로 유지하기 위해 상품을 조회하고 등록·수정·삭제한다. | `GET /admin/products`, `GET /admin/products/new`, `GET /admin/products/{id}/edit` 및 `POST /admin/products`, `POST /admin/products/{id}/edit`, `POST /admin/products/{id}/delete` | 목록·추가·수정 화면 모두 `200`과 HTML. 등록·수정·삭제는 각각 `302`와 `Location: /admin/products`. 이후 API에서 수정된 이름과 삭제 후 `404`를 확인했다. |
| 관리자 | 서비스에서 관리하는 회원 정보를 확인하고 등록·수정·삭제한다. | `GET /admin/members`, `GET /admin/members/new`, `GET /admin/members/{id}/edit` 및 `POST /admin/members`, `POST /admin/members/{id}/edit`, `POST /admin/members/{id}/delete` | 목록·추가·수정 화면 모두 `200`과 HTML. 등록·수정·삭제는 각각 `302`와 `Location: /admin/members`. 목록에서 이메일 변경과 삭제를 확인했다. |
| 관리자 | 회원이 주문할 수 있도록 포인트를 충전한다. | `POST /admin/members/{id}/charge-point` | `302`와 `Location: /admin/members`. 회원 목록에서 포인트가 0→30,000으로 늘었다. 0 포인트 충전 요청은 `500`. |

## 토큰 없이 상품을 등록한 예

```http
POST /api/products
Content-Type: application/json

{"name":"무인증 상품","price":1000,"imageUrl":"http://example.com/a.png","categoryId":1}
```

실제 응답은 `201 Created`였고 `Location: /api/products/7`이 반환됐다. 같은 상품을 토큰 없이 삭제하자 `204 No Content`가 반환됐다. 카테고리·옵션 변경과 관리자 화면도 토큰 없이 실행됐다. 공개 접근을 허용한 의도는 [확인 필요 목록](open-questions.md)에 적었다.
