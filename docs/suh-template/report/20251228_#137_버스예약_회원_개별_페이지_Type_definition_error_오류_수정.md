# 버스예약 회원 개별 페이지 Type definition error 오류 수정

**이슈**: [#137](https://github.com/Cassiiopeia/suh-project-utility/issues/137)  
**작업일**: 2025-12-28  
**담당자**: @Cassiiopeia

---

## 📌 작업 개요

버스예약 회원 개별 페이지(`/somansa/bus/member/{memberId}`) 진입 시 발생하는 `Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]` 오류 수정

**핵심 문제**: Jackson이 Hibernate LAZY 프록시 객체를 직렬화하려다 실패하여 500 에러 발생

---

## 🔍 문제 분석

### 오류 발생 원인

1. **API 호출**: `POST /api/somansa-bus/history/list/by-member`
2. **에러 메시지**: 
   ```
   No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor
   ```

3. **근본 원인**:
   - `SomansaBusReservationHistory` 엔티티가 `somansaBusMember`, `somansaBusRoute`를 `@ManyToOne(fetch = FetchType.LAZY)`로 참조
   - 트랜잭션 종료 후 Jackson이 프록시 객체를 JSON으로 직렬화하려 시도
   - Jackson이 Hibernate 프록시를 인식하지 못해 `ByteBuddyInterceptor`를 직렬화하려다 실패

### 왜 첫 번째 API는 성공하고 두 번째는 실패했나?

- **첫 번째 API** (`/schedule/list/by-member`): 데이터가 없어 빈 배열 반환 → 프록시 접근 없음 → 성공
- **두 번째 API** (`/history/list/by-member`): 실제 데이터 존재 → 프록시 객체 직렬화 시도 → 실패

---

## ✅ 구현 내용

### 1. Hibernate Module 의존성 추가

**파일**: `Suh-Common/build.gradle`

```gradle
api 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate6:2.18.2'
```

**목적**: Jackson이 Hibernate 6.x 프록시 객체를 안전하게 처리할 수 있도록 지원

---

### 2. Jackson 설정에 Hibernate Module 등록

**파일**: `Suh-Web/src/main/java/me/suhsaechan/web/config/JacksonConfig.java`

**변경 내용**:
```java
// Hibernate 프록시 처리 모듈 등록
Hibernate6Module hibernate6Module = new Hibernate6Module();
hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
objectMapper.registerModule(hibernate6Module);
```

**동작 방식**:
- `FORCE_LAZY_LOADING = false`: 프록시를 강제로 로드하지 않음 (성능 최적화)
- 로드되지 않은 프록시는 null로 직렬화
- 로드된 프록시는 실제 값으로 직렬화

---

### 3. Repository에 JOIN FETCH 쿼리 추가

**파일**: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/repository/SomansaBusReservationHistoryRepository.java`

**추가된 메서드**:
```java
@Query("SELECT h FROM SomansaBusReservationHistory h " +
    "JOIN FETCH h.somansaBusMember " +
    "JOIN FETCH h.somansaBusRoute " +
    "WHERE h.somansaBusMember.somansaBusMemberId = :memberId " +
    "ORDER BY h.executedAt DESC")
List<SomansaBusReservationHistory> findByMemberIdWithDetails(@Param("memberId") UUID memberId);
```

**목적**:
- `JOIN FETCH`로 연관 엔티티를 한 번의 쿼리로 로드
- 트랜잭션 내에서 실제 엔티티가 로드되므로 프록시 문제 해결
- N+1 쿼리 문제도 함께 방지

**특이사항**:
- 기존 메서드는 유지하고 새로운 메서드 추가 (하위 호환성)
- 최근 10개 조회용 메서드도 동일하게 추가

---

### 4. Service 메서드 수정

**파일**: `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusReservationService.java`

**변경 내용**:
```java
// 기존: findBySomansaBusMemberSomansaBusMemberIdOrderByExecutedAtDesc(memberId)
// 변경: findByMemberIdWithDetails(memberId)
List<SomansaBusReservationHistory> histories = 
    historyRepository.findByMemberIdWithDetails(memberId);
```

**결과**: 연관 엔티티가 로드된 상태로 조회되어 프론트엔드에서 `history.somansaBusRoute.description` 사용 가능

---

### 5. Schedule도 동일하게 수정

**파일**: 
- `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/repository/SomansaBusScheduleRepository.java`
- `Suh-Domain-Somansa-Bus/src/main/java/me/suhsaechan/somansabus/service/SomansaBusScheduleService.java`

**목적**: Schedule도 향후 데이터가 생기면 같은 문제 발생 가능성 있어 예방 차원에서 동일하게 수정

---

## 🔧 주요 변경사항 상세

### Hibernate6Module의 역할

Jackson이 Hibernate 프록시를 만났을 때:
1. 프록시인지 확인
2. 로드되지 않았으면 null로 직렬화
3. 로드되었으면 실제 값으로 직렬화
4. `ByteBuddyInterceptor` 같은 내부 필드는 무시

### JOIN FETCH의 효과

**기존 방식** (N+1 문제):
```sql
-- 1. History 조회
SELECT * FROM somansa_bus_reservation_history WHERE member_id = ?

-- 2. 각 History마다 Member 조회 (N번)
SELECT * FROM somansa_bus_member WHERE id = ?

-- 3. 각 History마다 Route 조회 (N번)
SELECT * FROM somansa_bus_route WHERE id = ?
```

**개선 방식** (1번의 쿼리):
```sql
SELECT h.*, m.*, r.*
FROM somansa_bus_reservation_history h
JOIN somansa_bus_member m ON h.member_id = m.id
JOIN somansa_bus_route r ON h.route_id = r.id
WHERE m.id = ?
ORDER BY h.executed_at DESC
```

---

## 📦 의존성 변경

**추가된 의존성**:
- `com.fasterxml.jackson.datatype:jackson-datatype-hibernate6:2.18.2`

**이유**: Spring Boot 3.x는 Hibernate 6.x를 사용하므로 `hibernate6` 버전 필요

---

## 🧪 테스트 및 검증

### 테스트 방법

1. 애플리케이션 실행
   ```bash
   ./gradlew bootRun
   ```

2. 브라우저에서 테스트
   - URL: `http://localhost:8080/somansa/bus/member/{memberId}`
   - 예약 기록 테이블이 정상적으로 표시되는지 확인
   - 노선 정보(`somansaBusRoute.description`)가 표시되는지 확인

3. 개발자 도구에서 확인
   - Network 탭에서 `/api/somansa-bus/history/list/by-member` 응답 확인
   - 500 에러가 발생하지 않는지 확인
   - 응답 JSON에 `somansaBusRoute` 정보가 포함되어 있는지 확인

### 예상 결과

**이전 (에러 발생)**:
```json
{
    "errorCode": "INTERNAL_SERVER_ERROR",
    "message": "Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]"
}
```

**이후 (정상 동작)**:
```json
{
    "histories": [
        {
            "somansaBusReservationHistoryId": "7938a735-983e-4197-888e-d07039918d4c",
            "somansaBusMember": {
                "somansaBusMemberId": "cb1d1c3f-5bea-43f0-9a27-ba420f3eefc5",
                "loginId": "chan4760@somansa.com",
                "displayName": "chan4760"
            },
            "somansaBusRoute": {
                "description": "07:30 당산역 2호 - 출근",
                "departureTime": "07:30",
                "station": "당산역"
            },
            "reservationDate": "2025-12-28",
            "isSuccess": true,
            "executedAt": "2025-12-28T21:13:57"
        }
    ],
    "totalCount": 1
}
```

---

## 📌 참고사항

### 왜 두 가지 방법을 모두 사용했나?

1. **Hibernate6Module (필수)**:
   - 전체 프로젝트에서 프록시 직렬화 문제 방지
   - 다른 API에서도 같은 문제 발생 방지
   - 향후 확장성 보장

2. **JOIN FETCH (필수)**:
   - 프론트엔드가 필요한 데이터(`somansaBusRoute.description`) 제공
   - N+1 쿼리 문제 해결로 성능 개선
   - 트랜잭션 내에서 실제 엔티티 로드

**결론**: 두 방법을 함께 사용해야 완전한 해결

### 향후 주의사항

- 새로운 엔티티에서 `@ManyToOne(fetch = FetchType.LAZY)` 사용 시:
  - Hibernate6Module이 자동으로 안전하게 처리
  - 프론트엔드에서 연관 엔티티 정보가 필요하면 JOIN FETCH 쿼리 추가

### 성능 영향

- **긍정적**: N+1 쿼리 문제 해결로 성능 개선
- **주의**: JOIN FETCH는 중복 데이터를 가져올 수 있으므로 페이징 시 주의 필요

---

## ✅ 완료 체크리스트

- [x] Hibernate6Module 의존성 추가
- [x] Jackson 설정에 Hibernate6Module 등록
- [x] ReservationHistory Repository에 JOIN FETCH 쿼리 추가
- [x] Schedule Repository에 JOIN FETCH 쿼리 추가
- [x] Service 메서드에서 새로운 쿼리 사용
- [x] 린터 에러 없음 확인
- [ ] 로컬 환경에서 테스트 완료
- [ ] 프로덕션 배포 후 모니터링

---

**작성일**: 2025-12-28  
**작성자**: AI Assistant (Claude)

