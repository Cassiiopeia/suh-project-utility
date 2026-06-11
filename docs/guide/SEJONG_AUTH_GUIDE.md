세종대학교 인증 모듈 가이드

모듈 소개

세종대학교 인증 모듈 Sejong University Auth Module은 세종대학교 포털 인증을 간편하게 처리할 수 있는 자바 라이브러리입니다. 이 라이브러리를 사용하면 단 3줄의 코드로 세종대학교 학생 인증을 구현할 수 있습니다. SSO 인증, 세션 관리, 데이터 파싱을 자동으로 처리하여 개발자의 부담을 줄여줍니다.

라이브러리 버전은 1.2.0이며 Java 17 이상과 Spring Boot 3.x 환경에서 사용할 수 있습니다. MIT 라이센스로 배포되어 자유롭게 사용하고 수정할 수 있습니다.

GitHub 저장소 주소는 https://github.com/Cassiiopeia/SUH-sejong-univ-auth 입니다. Nexus 저장소에서도 의존성을 받을 수 있으며 주소는 https://nexus.suhsaechan.kr 입니다.


개발자 정보

개발자는 서새찬 Suh Saechan입니다. 소만사 SOMANSA에서 DLP 솔루션 서버 모듈 팀 연구원으로 근무하고 있습니다. 세종대학교 바이오융합공학과를 졸업했습니다. GitHub 프로필은 https://github.com/Cassiiopeia 이며 이메일은 chan4760@gmail.com 입니다.


기술 스택

라이브러리는 Java 17과 Spring Boot 3.x를 기반으로 합니다. HTTP 통신에는 OkHttp 라이브러리를 사용합니다. HTML 파싱에는 Jsoup를 사용하고 JSON 파싱에는 Jackson을 사용합니다. SSL 인증서 검증 우회 기능을 제공하여 세종대학교 포털의 인증서 문제를 해결합니다.


테스트 페이지 사용 방법

SUH Project Utility 사이트에서 세종대학교 인증 모듈의 테스트 페이지를 제공합니다. 이 페이지를 통해 라이브러리의 기능을 직접 테스트해볼 수 있습니다.

테스트 페이지 접근 방법은 다음과 같습니다. 먼저 https://lab.suhsaechan.kr 사이트에 접속합니다. 상단 메뉴에서 세종대 인증을 클릭하거나 대시보드의 Dev Tools 섹션에서 세종대 인증 카드를 클릭합니다. 또는 직접 https://lab.suhsaechan.kr/sejong-auth 주소로 접속합니다.

테스트 페이지 사용 방법입니다. 왼쪽 입력 카드에서 학번을 입력합니다. 예시로 18010561 형식입니다. 비밀번호는 세종대학교 포털 비밀번호를 입력합니다. 인증 방식을 선택합니다. 통합 인증이 기본값이며 모든 정보를 조회합니다. 인증하기 버튼을 클릭합니다.

인증 결과는 오른쪽 결과 카드에 표시됩니다. 테이블 탭에서는 항목별로 정리된 결과를 볼 수 있습니다. JSON 탭에서는 원본 JSON 응답을 확인할 수 있습니다. JSON 결과 우측 상단의 복사 버튼을 클릭하면 클립보드에 복사됩니다.

보안 안내로 이 테스트 페이지는 입력된 학번과 비밀번호를 저장하지 않습니다. 인증 요청은 서버를 통해 세종대학교 포털로 전달되며 결과만 화면에 표시됩니다.


인증 방식 설명

라이브러리는 5가지 인증 방식을 제공합니다.

통합 인증 INTEGRATED는 DHC와 SIS 모든 정보를 조회합니다. 가장 많은 정보를 얻을 수 있어 추천하는 방식입니다. 학생 기본 정보, 학과, 학년, 재학 상태, 고전독서 인증 정보, 이메일, 전화번호, 영어 이름을 모두 조회합니다.

DHC 전용 인증은 대양휴머니티칼리지 시스템에서 정보를 조회합니다. classic.sejong.ac.kr 서버에 접속합니다. 학생 기본 정보와 학년, 재학 상태, 고전독서 인증 정보를 조회합니다.

SIS 전용 인증은 학사정보시스템에서 정보를 조회합니다. sjpt.sejong.ac.kr 서버에 접속합니다. 학생 기본 정보와 이메일, 전화번호, 영어 이름을 조회합니다.

DHC Raw HTML 인증은 DHC 인증 결과와 함께 원본 HTML 응답을 포함합니다. 디버깅이나 추가 데이터 파싱이 필요할 때 사용합니다.

SIS Raw JSON 인증은 SIS 인증 결과와 함께 원본 JSON 응답을 포함합니다. API 응답 구조를 확인하거나 추가 데이터가 필요할 때 사용합니다.


라이브러리 설치 방법

Gradle 프로젝트에서는 build.gradle 파일에 의존성을 추가합니다. repositories 블록에 maven url https://nexus.suhsaechan.me/repository/maven-releases/ 를 추가합니다. dependencies 블록에 implementation kr.suhsaechan:sejong-univ-auth:1.2.0 을 추가합니다.

Maven 프로젝트에서는 pom.xml 파일에 repository와 dependency를 추가합니다. repository id는 suh-nexus이고 url은 https://nexus.suhsaechan.me/repository/maven-releases/ 입니다. dependency의 groupId는 kr.suhsaechan이고 artifactId는 sejong-univ-auth이며 version은 1.2.0입니다.


라이브러리 사용 방법

Spring Boot 프로젝트에서 SuhSejongAuthEngine 빈을 주입받아 사용합니다. RequiredArgsConstructor 어노테이션을 사용하면 생성자 주입으로 간편하게 사용할 수 있습니다.

통합 인증은 authEngine.authenticate 메서드를 호출합니다. 첫 번째 인자로 학번을, 두 번째 인자로 비밀번호를 전달합니다. SejongAuthResult 객체가 반환됩니다.

DHC 전용 인증은 authEngine.authenticateWithDHC 메서드를 호출합니다. SejongDhcAuthResult 객체가 반환됩니다.

SIS 전용 인증은 authEngine.authenticateWithSIS 메서드를 호출합니다. SejongSisAuthResult 객체가 반환됩니다.

원본 데이터가 필요한 경우 authenticateWithDHCRaw 또는 authenticateWithSISRaw 메서드를 사용합니다. 각각 rawHtml 또는 rawJson 필드에 원본 응답이 포함됩니다.


응답 데이터 구조

인증 성공 시 반환되는 데이터입니다. isSuccess는 인증 성공 여부를 나타내는 불리언 값입니다. authType은 사용된 인증 방식입니다. studentId는 학번입니다. name은 이름입니다. major는 학과입니다. grade는 학년입니다. status는 재학 상태입니다. email은 이메일 주소이며 SIS 인증 시에만 포함됩니다. phoneNumber는 전화번호이며 SIS 인증 시에만 포함됩니다. englishName은 영어 이름이며 SIS 인증 시에만 포함됩니다. classicReading은 고전독서 인증 정보이며 DHC 인증 시에만 포함됩니다. authenticatedAt은 인증 시간입니다.

인증 실패 시에는 isSuccess가 false이고 errorCode와 errorMessage가 포함됩니다.


에러 코드 설명

AUTHENTICATION_FAILED는 학번 또는 비밀번호가 일치하지 않을 때 발생합니다.

INVALID_INPUT은 입력값이 비어있거나 유효하지 않을 때 발생합니다.

CONNECTION_FAILED는 세종대학교 포털 연결에 실패했을 때 발생합니다.

CONNECTION_TIMEOUT은 연결 시간이 초과되었을 때 발생합니다.

DATA_FETCH_FAILED는 학생 정보 조회에 실패했을 때 발생합니다.

PARSE_ERROR는 HTML 또는 JSON 파싱 중 오류가 발생했을 때 발생합니다.

SESSION_ERROR는 세션 처리 중 오류가 발생했을 때 발생합니다.

SSL_CONFIGURATION_ERROR는 SSL 설정 오류가 발생했을 때 발생합니다.


내부 동작 원리

인증 요청이 들어오면 먼저 입력값 유효성을 검사합니다. 학번과 비밀번호가 비어있으면 INVALID_INPUT 오류를 반환합니다.

DHC 인증 과정입니다. classic.sejong.ac.kr 서버에 HTTPS 연결을 수립합니다. 로그인 폼에 학번과 비밀번호를 POST 방식으로 전송합니다. 성공 시 세션 쿠키를 획득합니다. 학생 정보 페이지를 요청하여 HTML을 받아옵니다. Jsoup으로 HTML을 파싱하여 학생 정보를 추출합니다.

SIS 인증 과정입니다. sjpt.sejong.ac.kr 서버에 HTTPS 연결을 수립합니다. SSO 인증을 통해 세션을 획득합니다. 학생 정보 API를 호출하여 JSON 응답을 받습니다. Jackson으로 JSON을 파싱하여 학생 정보를 추출합니다.

통합 인증은 DHC와 SIS 인증을 순차적으로 수행하고 결과를 병합합니다.


보안 고려사항

이 라이브러리는 사용자의 학번과 비밀번호를 세종대학교 포털로 직접 전송합니다. 라이브러리 자체에서는 인증 정보를 저장하거나 로깅하지 않습니다. 사용하는 애플리케이션에서 인증 정보 처리에 주의해야 합니다.

SSL 인증서 검증 우회 기능은 세종대학교 포털의 인증서 문제를 해결하기 위해 제공됩니다. 이 기능은 신뢰할 수 있는 환경에서만 사용해야 합니다.


통계 기능

SUH Project Utility 대시보드에서 세종대 인증 사용 통계를 확인할 수 있습니다. 서버 통계 섹션에서 세종대 인증 카드를 통해 총 인증 횟수와 오늘 인증 횟수를 확인할 수 있습니다.

인증 성공 시에만 통계에 카운트됩니다. 인증 실패는 통계에 포함되지 않습니다.


관련 링크

테스트 페이지는 https://lab.suhsaechan.kr/sejong-auth 입니다.

GitHub 저장소는 https://github.com/Cassiiopeia/SUH-sejong-univ-auth 입니다.

Nexus 저장소는 https://nexus.suhsaechan.kr 입니다.

개발자 프로필 페이지는 https://lab.suhsaechan.kr/profile 입니다.
