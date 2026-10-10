🗒️ 설명
---

세종대학교 인증 모듈(`kr.suhsaechan:sejong-univ-auth:1.2.0`)을 사용하는 페이지([https://lab.suhsaechan.kr/sejong-auth](https://lab.suhsaechan.kr/sejong-auth))에서 **모든 인증 방식이 실패**한다.

- 영향 범위: `INTEGRATED` / `DHC` / `SIS` / `DHC_RAW` / `SIS_RAW` **전체 인증 방식**
- 인증 흐름(컨트롤러 → 라이브러리)은 정상 동작하나, **라이브러리가 세종대 포털에 연결하는 단계에서 실패**하여 `CONNECTION_FAILED`(`세종대학교 포털에 연결할 수 없습니다.`)를 반환한다.
- 처음엔 HTTP 403(CSRF) 문제로 의심했으나, 실제 요청을 캡처해보니 **응답은 200 OK**였고 본문에 `CONNECTION_FAILED`가 담겨 있었다. 즉 인증 인가/CSRF 문제가 아니라 **라이브러리의 세종대 포털 아웃바운드 연결 문제**다.

🔄 재현 방법
---

1. [https://lab.suhsaechan.kr/sejong-auth](https://lab.suhsaechan.kr/sejong-auth) 접속
2. 학번/비밀번호 입력 후 임의의 인증 방식(통합/DHC/SIS 등) 선택하여 인증 실행
3. 인증 결과로 `CONNECTION_FAILED` 반환 확인 (어떤 학번/비밀번호를 넣어도 동일)

📸 참고 자료
---

**실제 요청/응답 캡처 (브라우저 F12 Network)**

- 요청: `POST https://lab.suhsaechan.kr/api/sejong-auth/authenticate`
- 응답 상태: **`200 OK`** (CSRF/403 문제 아님)
- 요청 헤더: `x-csrf-token` 정상 전송됨, `JSESSIONID` 정상, `content-type: multipart/form-data`
- 응답 본문:

```json
{
  "isSuccess": false,
  "authType": "INTEGRATED",
  "major": null,
  "studentId": null,
  "name": null,
  "grade": null,
  "status": null,
  "email": null,
  "phoneNumber": null,
  "englishName": null,
  "classicReading": null,
  "rawHtml": null,
  "rawJson": null,
  "authenticatedAt": null,
  "errorCode": "CONNECTION_FAILED",
  "errorMessage": "세종대학교 포털에 연결할 수 없습니다."
}
```

**데이터 소스 (라이브러리가 접속하는 외부 호스트)**

- DHC: `classic.sejong.ac.kr`
- SIS: `sjpt.sejong.ac.kr`

**추정 원인 (서버 진단 전 — 미확정 가설)**

아래는 아직 서버에서 확인하지 않은 가설 목록이다. 우선순위 순.

1. **운영 컨테이너 아웃바운드 차단 (가장 유력)** — 운영 컨테이너(`suh-project-utility-blue`)에서 `*.sejong.ac.kr`로의 아웃바운드 연결이 방화벽/내부망 정책으로 막혀 있을 가능성.
2. **컨테이너 내 DNS 해석 실패** — 컨테이너에서 `classic.sejong.ac.kr` / `sjpt.sejong.ac.kr` DNS 해석이 안 되는 경우.
3. **세종대 포털 측 변경** — 엔드포인트 URL/SSL 인증서 변경 등. `1.2.0` 라이브러리가 구버전이라 변경에 대응 못 할 가능성.
4. **세종대 포털 일시 장애** — 외부 포털 자체의 일시적 다운.

**조사 방향 (제안)**

- 운영 컨테이너 내부에서 다음을 실행해 1·2번을 먼저 가른다:
  - `curl -v https://classic.sejong.ac.kr` / `curl -v https://sjpt.sejong.ac.kr`
  - `nslookup classic.sejong.ac.kr` (DNS 해석 확인)
- 라이브러리가 던지는 `CONNECTION_FAILED`의 **내부 실제 예외**(`SocketTimeoutException` / `UnknownHostException` / `SSLHandshakeException` 중 무엇인지) 스택트레이스를 확인해 원인 카테고리를 좁힌다.
- 라이브러리가 catch한 원인 예외를 로그/에러 메시지에 노출하면 진단이 빨라진다 (개선 포인트).

✅ 예상 동작
---

- 정상 학번/비밀번호로 인증 시 세종대 포털에 연결되어 학생 정보(이름, 학과, 학년 등)가 정상 반환되어야 한다.
- 연결 실패가 불가피한 환경이라면, `CONNECTION_FAILED`만 던지지 말고 **원인 구분이 가능한 에러 메시지/로그**(DNS 실패 / 타임아웃 / SSL 실패)를 제공해 디버깅을 돕는 것이 바람직하다.

⚙️ 환경 정보
---

- **OS**: 운영 컨테이너 `suh-project-utility-blue` (Docker)
- **브라우저**: Chrome 142 (재현 클라이언트)
- **기기**: -
- **라이브러리**: `kr.suhsaechan:sejong-univ-auth:1.2.0`
- **서비스**: suh-project-utility (Spring Boot 3.x, Java 17)

🙋‍♂️ 담당자
---

- **백엔드**: Cassiiopeia
- **프론트엔드**: -
- **디자인**: -
