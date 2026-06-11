🗒️ 설명
---

운영 환경(`https://lab.suhsaechan.kr/`)에서 페이지는 HTTPS로 로드되지만, 인증이 필요한 요청에 대해 서버가 `http://` 스킴으로 302 리다이렉트를 발행한다. 이로 인해 브라우저가 Mixed Content로 요청을 차단하여 로그인 및 clientHash 조회가 실패한다.

콘솔 에러:
```
Mixed Content: The page at 'https://lab.suhsaechan.kr/' was loaded over HTTPS,
but requested an insecure XMLHttpRequest endpoint 'http://lab.suhsaechan.kr/login'.
This request has been blocked
common.js: Unable to fetch client hash
```

🔄 재현 방법
---

1. `https://lab.suhsaechan.kr/` 접속
2. 미인증 상태에서 페이지가 `/api/member/client-hash` 등 인증 필요 요청 수행
3. 서버가 `Location: http://lab.suhsaechan.kr/login` 으로 응답 → 브라우저가 Mixed Content로 차단

📸 참고 자료
---

서버 직접 확인 결과 (`X-Forwarded-Proto: https` 명시했음에도 http 리다이렉트):
```
$ curl -sI https://lab.suhsaechan.kr/api/member/client-hash
HTTP/2 302
location: http://lab.suhsaechan.kr/login     # https로 들어왔는데 http로 리다이렉트

# Spring 컨테이너에 직접 X-Forwarded-Proto: https 주입해도 동일
Location: http://localhost:8080/login
```

**근본 원인**: `application.yml`의 `server.forward-headers-strategy: framework` 설정과 `WebSecurityConfig`의 수동 `ForwardedHeaderFilter` `@Bean` 등록이 중복된다. 수동 `@Bean`은 order를 지정하지 않아 `LOWEST_PRECEDENCE`로 등록되고, 이것이 Spring Boot의 자동 등록(`HIGHEST_PRECEDENCE`)을 `@ConditionalOnMissingBean`으로 가로챈다. 결과적으로 `ForwardedHeaderFilter`가 Spring Security 필터 체인보다 늦게 실행되어, Security가 리다이렉트 URL을 생성하는 시점에는 아직 `X-Forwarded-Proto`가 반영되지 않아 원본 스킴(`http`)이 사용된다.

✅ 예상 동작
---

- HTTPS로 접속한 사용자는 리다이렉트 `Location` 헤더도 `https://`로 생성되어야 한다.
- 미인증 요청 시 `https://lab.suhsaechan.kr/login`으로 정상 리다이렉트되어 Mixed Content 차단이 발생하지 않아야 한다.

**해결 방안**: `WebSecurityConfig`의 수동 `ForwardedHeaderFilter` `@Bean`을 제거하고, `application.yml`의 `forward-headers-strategy: framework` 전략만 유지한다. 이렇게 하면 Spring Boot가 올바른 최고 우선순위로 필터를 자동 등록하여 Security 필터보다 먼저 실행된다. (deprecated된 `use-forward-headers` 도 함께 정리)

⚙️ 환경 정보
---

- **OS**: 운영 서버 (Synology NAS, Docker)
- **인프라**: Cloudflare(HTTPS 종단) → Traefik v3.0(:80 web entrypoint) → Spring(8080)
- **브라우저**: 전체 (Mixed Content는 모든 모던 브라우저 공통)

🙋‍♂️ 담당자
---

- **백엔드**: Cassiiopeia
