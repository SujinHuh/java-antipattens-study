# 004 TIL

## 문제 주제

포인트 환불 레거시 코드 리뷰

## 오늘 잡은 키워드

- DI와 직접 생성
- DTO/Entity 분리
- Transaction boundary
- Optional
- 반복 Repository 조회
- 예외 정책
- static mutable collection
- Util 책임 분리
- 테스트 가능성

## 내가 잘한 부분

Controller에서 Entity를 직접 받는 문제, Service/Repository 직접 생성, 반복 조회, RuntimeException, static List, 테스트 부실을 발견했다.

## 내가 보완할 부분

- Service에서 `ResponseEntity`를 반환하자고 말하지 않는다.
- DTO 개선을 Lombok이 아니라 API 계약/검증 책임으로 설명한다.
- JPA N+1과 반복 Repository 조회를 구분한다.
- 없는 결제를 요청 객체로 보정하는 문제를 더 명확히 잡는다.
- 인메모리 예제와 실제 DB 전제를 구분해서 트랜잭션을 말한다.
- 결제/환불 도메인에서는 소유자 검증을 핵심 문제로 본다.

## 면접용 문장

```text
이 코드는 Controller가 내부 모델을 직접 받고 반환하고, Service가 클라이언트가 보낸 상태를 신뢰해 환불을 처리합니다.
환불은 서버에 저장된 결제 상태를 기준으로 소유자와 상태를 검증해야 하며, 실제 DB 기반 Repository라면 트랜잭션 경계 안에서 처리해야 합니다.
또한 반복 Repository 조회, RuntimeException 직접 사용, static mutable List, 실행되지 않는 테스트가 있어 운영과 테스트 안정성이 떨어집니다.
```

## 안티패턴 지침화 후보

- Entity 직접 Request/Response 사용
- 조회 실패를 요청 객체로 보정
- 소유자 검증 누락
- 반복 Repository 조회
- static mutable collection 저장소
- Service가 HTTP 응답 문자열 생성
