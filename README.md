# Deliver Anything
---

## 🌟 프로젝트 소개

`Deliver Anything`은 사용자의 위치를 기반으로 실시간 배달 서비스를 제공하는 플랫폼입니다.

- **고객(소비자)**: 주변 상점을 검색하고, 원하는 상품을 주문하며, 실시간으로 배달 상태를 추적할 수 있습니다.
- **점주(판매자)**: 자신의 상점과 상품을 등록하고, 들어온 주문을 관리하며, 정산 내역을 확인할 수 있습니다.
- **라이더(배달원)**: 배달 요청을 수락하고, 실시간 위치 정보를 공유하며, 배달 완료 후 정산받을 수 있습니다.

## 🛠️ 주요 기술 스택

- **Backend**: Java, Spring Boot, Spring Security, JPA
- **Database**: MySQL, Redis
- **Infra**: AWS (EC2, S3, RDS), Docker, Terraform
- **CI/CD**: GitHub Actions

<br>

<details>
<summary>📊 ERD (Entity-Relationship Diagram)</summary>

```mermaid
erDiagram
    User {
        Long id PK
        String email
        String password
        String username
        String phoneNumber
        SocialProvider socialProvider
        String socialId
        Long currentActiveProfile_id FK
        Boolean isEmailVerified
        Boolean isEnabled
        Boolean isAdmin
        LocalDateTime lastLoginAt
    }

    Profile {
        Long id PK
        Long user_id FK
        ProfileType type
        Boolean isActive
    }

    CustomerProfile {
        Long id PK "Profile ID"
        Long defaultAddressId
        String customerPhoneNumber
        String nickname
        String profileImageUrl
    }

    RiderProfile {
        Long id PK "Profile ID"
        RiderToggleStatus toggleStatus
        String area
        String licenseNumber
        String bankName
        String bankAccountNumber
        String bankAccountHolderName
        String riderPhoneNumber
        String nickname
        String profileImageUrl
    }

    SellerProfile {
        Long id PK "Profile ID"
        String businessName
        String businessCertificateNumber
        String businessPhoneNumber
        String bankName
        String accountNumber
        String accountHolder
        String nickname
        String profileImageUrl
    }

    CustomerAddress {
        Long id PK
        Long customer_profile_id FK
        String addressName
        String address
        Point location
    }

    Store {
        Long id PK
        Long seller_profile_id FK
        Long store_category_id FK
        String imageUrl
        String name
        String description
        String roadAddr
        Point location
        StoreStatus status
    }

    StoreCategory {
        Long id PK
        String name
    }

    StoreBlocklist {
        Long id PK
        Long store_id FK
        Long customer_profile_id FK
    }

    Product {
        Long id PK
        Long store_id FK
        String name
        String description
        Integer price
        String imageUrl
    }

    Stock {
        Long id PK
        Long product_id FK
        Integer version
        Integer totalQuantity
        Integer heldQuantity
    }

    Order {
        Long id PK
        Long store_id FK
        Long cusotomer_id FK
        Long delivery_id FK
        OrderStatus status
        String merchantId
        String address
        Point destination
        Long totalPrice
    }

    OrderItem {
        Long id PK
        Long order_id FK
        Long product_id FK
        Integer price
        Integer quantity
    }

    Delivery {
        Long id PK
        Double expectedTime
        String requested
        DeliveryStatus status
        LocalDateTime startedAt
        LocalDateTime completedAt
        Integer charge
        Long store_id FK
        Long review_id FK
        Long rider_profile_id FK
        Long customer_profile_id FK
    }

    Review {
        Long id PK
        Integer rating
        String comment
        ReviewTargetType targetType
        Long targetId "FK to Store or RiderProfile"
        Long customer_profile_id FK
    }

    ReviewPhoto {
        Long id PK
        Long review_id FK
        String photoUrl
    }

    Payment {
        Long id PK
        String merchantUid "FK to Order"
        String paymentKey
        Long amount
        PaymentStatus status
    }

    Notification {
        Long id PK
        Long recipientId "Profile ID"
        NotificationType type
        String message
        String data
        Boolean isRead
    }

    VerificationToken {
        Long id PK
        String identifier
        String verificationCode
        VerificationType verificationType
        VerificationPurpose purpose
        LocalDateTime expireAt
        Boolean isUsed
    }

    SettlementBatch {
        Long id PK
        Long targetId
        Long targetTotalAmount
        Integer transactionCount
        Long totalPlatformFee
        Long settledAmount
        LocalDate settlementDate
    }

    SettlementDetail {
        Long id PK
        Long orderId
        Long targetId
        Long targetAmount
        Long platformFee
        SettlementStatus status
        Long batchId
    }

    User ||--o{ Profile : "has"
    User ||--|{ Profile : "current active"
    Profile ||--|{ CustomerProfile : "is a"
    Profile ||--|{ RiderProfile : "is a"
    Profile ||--|{ SellerProfile : "is a"

    CustomerProfile ||--o{ CustomerAddress : "has"
    CustomerProfile ||--o{ Order : "places"
    CustomerProfile ||--o{ Review : "writes"
    CustomerProfile ||--o{ StoreBlocklist : "blocks"
    CustomerProfile ||--o{ Delivery : "receives"

    RiderProfile ||--o{ Delivery : "delivers"

    SellerProfile ||--o{ Store : "owns"

    StoreCategory ||--o{ Store : "categorizes"
    Store ||--o{ Product : "sells"
    Store ||--o{ Order : "receives"
    Store ||--o{ Delivery : "requests"
    Store ||--o{ StoreBlocklist : "is blocked by"

    Product ||--|| Stock : "has"
    Product ||--o{ OrderItem : "is in"

    Order ||--o{ OrderItem : "contains"
    Order ||--|| Delivery : "has one"
    Order }o--|| Payment : "is paid by"

    Delivery ||--|| Review : "can have"

    Review ||--o{ ReviewPhoto : "has"
```

</details>

<br>

<details>
<summary>📄 API Specification</summary>

### Auth API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/auth/signup` | 회원가입 |
| `POST` | `/api/v1/auth/login` | 로그인 |
| `POST` | `/api/v1/auth/logout` | 단일 로그아웃 (현재 기기) |
| `POST` | `/api/v1/auth/logout/all` | 전체 로그아웃 (모든 기기) |
| `POST` | `/api/v1/auth/refresh` | 토큰 재발급 |

### Delivery API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `PATCH` | `/api/v1/deliveries/status` | 라이더 토글 전환 |
| `POST` | `/api/v1/deliveries/area` | 배달 가능 지역 설정 |
| `PATCH` | `/api/v1/deliveries/{deliveryId}/delivery-status` | 배달 상태 변경 |
| `POST` | `/api/v1/deliveries/decision` | 라이더 배달 수락/거절 결정 |
| `GET` | `/api/v1/deliveries/today` | 오늘의 배달 내역 조회 |
| `GET` | `/api/v1/deliveries/in-progress` | 진행 중인 배달 조회 |
| `GET` | `/api/v1/deliveries/in-progress/{deliveryId}` | 진행 중인 배달 단건 상세 조회 |
| `GET` | `/api/v1/deliveries/total` | 총 배달 내역 요약 조회 + 배달 완료 리스트 조회 |

### Rider Location WebSocket API
| Type | Endpoint | Description |
| :--- | :--- | :--- |
| `@MessageMapping` | `/location` | 라이더 위치 정보 업데이트 (WebSocket) |

### Media API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/media/presigned-url` | 파일 업로드를 위한 Pre-signed URL 생성 |

### Notification API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/notifications/stream` | SSE 구독 |
| `GET` | `/api/v1/notifications` | 알림 목록 조회 |
| `POST` | `/api/v1/notifications/{id}/read` | 알림 읽음 처리 |
| `GET` | `/api/v1/notifications/unread-count` | 읽지 않은 알림 수 조회 |

### Customer Order API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/customer/orders` | 주문 생성 |
| `GET` | `/api/v1/customer/orders` | 주문 내역 조회 |
| `GET` | `/api/v1/customer/orders/{orderId}` | 주문 단일 조회 |
| `GET` | `/api/v1/customer/orders/in-progress` | 진행중인 주문 조회 |
| `GET` | `/api/v1/customer/orders/completed` | 배달 완료된 주문 조회 |
| `POST` | `/api/v1/customer/orders/{merchantUid}/pay` | 주문 결제 |
| `POST` | `/api/v1/customer/orders/{orderId}/cancel` | 주문 취소 |

### Store Order API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/stores/{storeId}/orders/history` | 주문 내역 조회 |
| `GET` | `/api/v1/stores/{storeId}/orders/pending` | 주문 수락 대기 목록 조회 |
| `GET` | `/api/v1/stores/{storeId}/orders/accepted` | 주문 현황 목록 조회 |
| `PATCH` | `/api/v1/stores/{storeId}/orders/{orderId}/accept` | 주문 수락 |
| `PATCH` | `/api/v1/stores/{storeId}/orders/{orderId}/reject` | 주문 거절 |

### Product API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/stores/{storeId}/products` | 상품 생성 |
| `GET` | `/api/v1/stores/{storeId}/products` | 상품 목록 조회 |
| `GET` | `/api/v1/stores/{storeId}/products/{productId}` | 상품 상세 조회 |
| `PUT` | `/api/v1/stores/{storeId}/products/{productId}` | 상품 정보 수정 |
| `DELETE` | `/api/v1/stores/{storeId}/products/{productId}` | 상품 삭제 |

### Review API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/reviews` | 리뷰 생성 |
| `DELETE` | `/api/v1/reviews/{reviewId}` | 리뷰 삭제 |
| `PATCH` | `/api/v1/reviews/{reviewId}` | 리뷰 수정 |
| `GET` | `/api/v1/reviews/{reviewId}` | 리뷰 조회 |
| `GET` | `/api/v1/me/reviews` | 내 리뷰 리스트 & 평점 조회 |
| `GET` | `/api/v1/stores/{storeId}/reviews` | 특정 상점 리뷰 리스트 & 평점 조회 |
| `POST` | `/api/v1/reviews/{reviewId}/like` | 리뷰 좋아요 등록 |
| `DELETE` | `/api/v1/reviews/{reviewId}/like` | 리뷰 좋아요 취소 |
| `GET` | `/api/v1/reviews/{reviewId}/likes` | 리뷰 좋아요 수 조회 |

### Store Search API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/search/stores` | 상점 검색 |

### Rider Settlement API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/rider/settlements/day` | 정산 일별 조회 |
| `GET` | `/api/v1/rider/settlements/week` | 정산 주간별 조회 |
| `GET` | `/api/v1/rider/settlements/month` | 정산 월별 조회 |
| `GET` | `/api/v1/rider/settlements/period` | 정산 기간 조회 |
| `GET` | `/api/v1/rider/settlements/summary` | 정산 요약 조회 |

### Store Settlement API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/store/settlements/{storeId}/day` | 정산 일별 조회 |
| `GET` | `/api/v1/store/settlements/{storeId}/week` | 정산 주간별 조회 |
| `GET` | `/api/v1/store/settlements/{storeId}/month` | 정산 월별 조회 |
| `GET` | `/api/v1/store/settlements/{storeId}/period` | 정산 기간 조회 |

### Store Category API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/store-categories` | 상점 카테고리 목록 조회 |

### Store API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/stores` | 상점 생성 |
| `GET` | `/api/v1/stores/{storeId}` | 상점 단건 조회 |
| `PUT` | `/api/v1/stores/{storeId}` | 상점 정보 수정 |
| `DELETE` | `/api/v1/stores/{storeId}` | 상점 삭제 |
| `POST` | `/api/v1/stores/{storeId}/toggle-status` | 상점 영업상태 변경 |

### Customer Profile API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/users/me/customer` | 내 고객 프로필 조회 |
| `PUT` | `/api/v1/users/me/customer` | 내 고객 프로필 수정 |
| `GET` | `/api/v1/users/me/customer/addresses` | 내 배송지 목록 조회 |
| `GET` | `/api/v1/users/me/customer/addresses/{addressId}` | 특정 배송지 조회 |
| `POST` | `/api/v1/users/me/customer/addresses` | 배송지 추가 |
| `PUT` | `/api/v1/users/me/customer/addresses/{addressId}` | 배송지 수정 |
| `DELETE` | `/api/v1/users/me/customer/addresses/{addressId}` | 배송지 삭제 |
| `PUT` | `/api/v1/users/me/customer/addresses/{addressId}/default` | 기본 배송지 설정 |
| `GET` | `/api/v1/users/me/customer/addresses/default` | 기본 배송지 조회 |

### Rider Profile API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/users/me/rider` | 내 배달원 프로필 조회 |
| `PUT` | `/api/v1/users/me/rider` | 내 배달원 프로필 수정 |
| `POST` | `/api/v1/users/me/rider/toggle` | 배달 상태 토글 |
| `PUT` | `/api/v1/users/me/rider/status` | 배달 상태 설정 |
| `GET` | `/api/v1/users/me/rider/available` | 배달 가능 여부 조회 |
| `PUT` | `/api/v1/users/me/rider/area` | 활동 지역 수정 |
| `GET` | `/api/v1/users/me/rider/area` | 활동 지역 조회 |
| `PUT` | `/api/v1/users/me/rider/account-info` | 정산 계좌 정보 수정 |

### Seller Profile API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/users/me/seller` | 내 판매자 프로필 조회 |
| `PUT` | `/api/v1/users/me/seller` | 내 판매자 프로필 수정 |
| `PUT` | `/api/v1/users/me/seller/business-info` | 사업자 정보 수정 |
| `PUT` | `/api/v1/users/me/seller/account-info` | 정산 계좌 정보 수정 |

### User API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | 내 정보 조회 |
| `PUT` | `/api/v1/users/me` | 내 정보 수정 |
| `PUT` | `/api/v1/users/me/password` | 비밀번호 변경 |
| `POST` | `/api/v1/users/me/profiles` | 프로필 생성 |
| `POST` | `/api/v1/users/me/profile/switch` | 프로필 전환 |
| `GET` | `/api/v1/users/me/profiles` | 사용 가능한 프로필 목록 조회 |

</details>

<br>

<details>
<summary>✅ Workflow Guide</summary>

## 1. Issue → Branch
- **이슈는 반드시 GitHub Project 보드에서 생성**
  - Projects → Buddy App → **New issue** 버튼 클릭
  - 이슈 템플릿(`Type`, `Scope`, `Summary`, `Details`)에 맞춰 작성
- 규칙에 맞는 이슈만 자동 브랜치 생성됨
- 브랜치 네이밍 규칙:
  ```
  {type}/{scope}/{issue_number}
  ```
  예) `feat/be/12`

### Type
- `feat` : 새로운 기능
- `fix` : 버그 수정
- `refactor` : 리팩터링
- `docs` : 문서 작업
- `chore` : 환경/설정/잡일
- `test` : 테스트 코드

### Scope
- `fe` : Frontend
- `be` : Backend
- `infra` : Infra / 배포 / 환경

---

## 2. Pull Request
- 브랜치 작업 완료 후 → **PR 생성**
- **PR 제목 자동 동기화**: 이슈 제목 + 번호  
  예)  
  ```
  feat(be): 로그인 API 추가 (#12)
  ```

### PR 병합 규칙
- `dev` 브랜치로 머지:  
  - 관련 이슈 자동 close  
  - 작업 브랜치 자동 삭제
- `main` 브랜치로 머지:  
  - 배포 파이프라인(CD) 실행

---

## 3. Branch Strategy
- `main` : 배포용 브랜치 (Release 태그, Docker 빌드/푸시, 배포 실행)  
- `dev` : 통합 개발 브랜치 (이슈별 브랜치가 합쳐지는 곳)  
- `feat/*`, `fix/*`, `refactor/*`, `docs/*`, `chore/*`, `test/*` :  
  → 이슈 단위 작업 브랜치 (머지 후 자동 삭제)

---

## 4. CI/CD
### CI (Backend CI)
- **트리거**: `dev`, `main` 브랜치에서 push & PR  
- **동작**:
  - Gradle 빌드 & 테스트 실행
  - Redis 컨테이너 서비스 지원
  - `.env` 파일 GitHub Secrets 기반 로드

### CD (Backend CD)
- **트리거**: `main` 브랜치 push  
- **동작**:
  - Git Tag + Release 생성
  - Docker 이미지 빌드 & GHCR Push
  - AWS EC2 Blue/Green 배포 (SSM SendCommand 이용)

---

## 5. Issue Template
- 하나의 공통 템플릿 제공
  - **Type** : feat / fix / refactor / docs / chore / test  
  - **Scope** : fe / be / infra  
  - **Summary** : 간단 요약 (브랜치명/PR 제목 반영)  
  - **Details** : 작업 설명 & 완료 기준

---

## ✅ Workflow 요약
1. **Issue 생성 (Projects 보드에서만)**  
2. 규칙에 맞으면 **브랜치 자동 생성**  
3. 작업 후 **PR 생성 → PR 제목 자동 동기화**  
4. **PR 병합**
   - `dev`: 이슈 닫기 + 브랜치 삭제  
   - `main`: CD 실행 (배포)  
5. **Release & 배포** → Docker + AWS EC2 Blue/Green

</details>
