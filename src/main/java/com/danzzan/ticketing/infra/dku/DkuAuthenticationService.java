package com.danzzan.ticketing.infra.dku;

import com.danzzan.ticketing.infra.dku.exception.DkuFailedLoginException;
import com.danzzan.ticketing.infra.dku.model.DkuAuth;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// 단국대 포털 인증 서비스
// 실제 흐름:
// 1. webinfo.dankook.ac.kr 접속 → SSO 리다이렉트 → 로그인 폼(logon.do?sso=ok) 도착
// 2. 로그인 폼 action URL로 POST (username, password, tabIndex=0)
// 3. 302 → SSO 콜백 리다이렉트 따라가기 → 인증 쿠키 획득
@Slf4j
@Service
public class DkuAuthenticationService {

    private static final String WEBINFO_URL = "https://webinfo.dankook.ac.kr";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final WebClient webClient;

    public DkuAuthenticationService() {
        HttpClient httpClient = HttpClient.create()
                .followRedirect(false);

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .build();
    }

    public DkuAuth login(String studentId, String password) {
        MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();

        // 0단계: webinfo 접속 → SSO 리다이렉트 따라가서 로그인 폼 도달
        String loginFormUrl = navigateToLoginForm(cookies);
        log.info("로그인 폼 URL: {}", loginFormUrl);

        // 1단계: 로그인 폼에 credentials POST
        String formData = makeFormData(studentId, password);
        ResponseEntity<String> loginResponse = postLogin(loginFormUrl, formData, cookies);

        HttpStatus loginStatus = (HttpStatus) loginResponse.getStatusCode();
        collectCookies(loginResponse.getHeaders(), cookies);

        log.info("로그인 POST 응답: {}, Location: {}", loginStatus, loginResponse.getHeaders().getLocation());
        // 디버그: 로그인 응답 저장
        try {
            String debugInfo = "Status: " + loginStatus
                + "\nLocation: " + loginResponse.getHeaders().getLocation()
                + "\nCookies: " + cookies
                + "\nBody:\n" + (loginResponse.getBody() != null ? loginResponse.getBody().substring(0, Math.min(2000, loginResponse.getBody().length())) : "null");
            java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/dku_login_response.txt"), debugInfo);
        } catch (Exception ignored) {}

        // 200 OK = 로그인 실패 (로그인 페이지가 다시 렌더링됨)
        if (loginStatus == HttpStatus.OK) {
            log.warn("DKU 로그인 실패: 200 OK (잘못된 학번/비밀번호)");
            throw new DkuFailedLoginException();
        }

        // 302가 아니면 예상 외 응답
        if (loginStatus != HttpStatus.FOUND && loginStatus != HttpStatus.MOVED_TEMPORARILY) {
            log.error("DKU 로그인 예상 외 응답: {}", loginStatus);
            throw new DkuFailedLoginException();
        }

        // 2단계: SSO 리다이렉트 따라가기 (여러 번일 수 있음)
        URI location = loginResponse.getHeaders().getLocation();
        if (location == null) {
            log.error("DKU 로그인: Location 헤더 없음");
            throw new DkuFailedLoginException();
        }

        // 리다이렉트 체인 따라가기 (최대 5번)
        for (int i = 0; i < 5; i++) {
            log.info("리다이렉트 {}단계: {}", i + 1, location);
            ResponseEntity<String> redirectResponse = followRedirect(location, cookies);
            collectCookies(redirectResponse.getHeaders(), cookies);

            HttpStatus redirectStatus = (HttpStatus) redirectResponse.getStatusCode();
            if (redirectStatus == HttpStatus.OK) {
                // 최종 페이지 도달
                break;
            }

            URI nextLocation = redirectResponse.getHeaders().getLocation();
            if (nextLocation == null) {
                break;
            }
            location = nextLocation;
        }

        log.info("DKU 로그인 성공. 수집된 쿠키: {}", cookies);

        // 3단계: 로그인 후 webinfo 메인 페이지에 접근하여 세션 쿠키 확보
        try {
            ResponseEntity<String> mainResponse = followRedirect(
                    URI.create(WEBINFO_URL + "/main.do"), cookies);
            collectCookies(mainResponse.getHeaders(), cookies);
            HttpStatus mainStatus = (HttpStatus) mainResponse.getStatusCode();
            log.info("메인 페이지 응답: {}", mainStatus);

            // 메인 페이지도 리다이렉트될 수 있음 (SSO 재인증)
            if (mainStatus == HttpStatus.FOUND || mainStatus == HttpStatus.MOVED_TEMPORARILY) {
                URI mainLocation = mainResponse.getHeaders().getLocation();
                if (mainLocation != null) {
                    for (int i = 0; i < 5; i++) {
                        log.info("메인 리다이렉트 {}단계: {}", i + 1, mainLocation);
                        ResponseEntity<String> r = followRedirect(mainLocation, cookies);
                        collectCookies(r.getHeaders(), cookies);
                        HttpStatus s = (HttpStatus) r.getStatusCode();
                        if (s == HttpStatus.OK) break;
                        mainLocation = r.getHeaders().getLocation();
                        if (mainLocation == null) break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("메인 페이지 접근 실패 (무시): {}", e.getMessage());
        }

        log.info("최종 쿠키: {}", cookies);
        return new DkuAuth(cookies);
    }

    // webinfo 접속 → SSO 리다이렉트 → 로그인 폼 페이지까지 따라가기
    // 로그인 폼의 action URL 반환
    private String navigateToLoginForm(MultiValueMap<String, String> cookies) {
        try {
            URI currentUri = URI.create(WEBINFO_URL + "/");

            // 리다이렉트 체인 따라가기 (최대 5번)
            String lastBody = null;
            for (int i = 0; i < 5; i++) {
                ResponseEntity<String> response = followRedirect(currentUri, cookies);
                collectCookies(response.getHeaders(), cookies);

                HttpStatus status = (HttpStatus) response.getStatusCode();
                if (status == HttpStatus.OK) {
                    lastBody = response.getBody();
                    break;
                }

                URI nextLocation = response.getHeaders().getLocation();
                if (nextLocation == null) {
                    lastBody = response.getBody();
                    break;
                }

                // 상대 경로 처리
                if (!nextLocation.isAbsolute()) {
                    nextLocation = currentUri.resolve(nextLocation);
                }
                currentUri = nextLocation;
            }

            // 로그인 폼 HTML에서 action URL 추출
            if (lastBody != null && !lastBody.isEmpty()) {
                Document doc = Jsoup.parse(lastBody);
                Element form = doc.getElementById("logonForm");
                if (form != null) {
                    String action = form.attr("action");
                    if (action != null && !action.isEmpty()) {
                        // 상대 경로면 절대 경로로 변환
                        if (action.startsWith("/")) {
                            return currentUri.getScheme() + "://" + currentUri.getHost()
                                    + (currentUri.getPort() > 0 ? ":" + currentUri.getPort() : "")
                                    + action;
                        }
                        return action;
                    }
                }
            }

            // form을 못 찾으면 기본 logon.do URL 사용
            log.warn("로그인 폼을 찾지 못함. 기본 URL 사용");
            return WEBINFO_URL + "/member/logon.do";

        } catch (Exception e) {
            log.error("로그인 폼 탐색 실패: {}", e.getMessage());
            return WEBINFO_URL + "/member/logon.do";
        }
    }

    // 로그인 POST 요청
    private ResponseEntity<String> postLogin(String url, String formData, MultiValueMap<String, String> cookies) {
        try {
            return webClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .header(HttpHeaders.ORIGIN, WEBINFO_URL)
                    .header(HttpHeaders.REFERER, url)
                    .cookies(c -> cookies.forEach((name, values) -> values.forEach(v -> c.add(name, v))))
                    .bodyValue(formData)
                    .retrieve()
                    .onStatus(status -> false, resp -> null)
                    .toEntity(String.class)
                    .block();
        } catch (Exception e) {
            log.error("로그인 POST 요청 실패: {}", e.getMessage());
            throw new DkuFailedLoginException();
        }
    }

    // GET 리다이렉트 따라가기
    private ResponseEntity<String> followRedirect(URI location, MultiValueMap<String, String> cookies) {
        try {
            return webClient.get()
                    .uri(location)
                    .header(HttpHeaders.REFERER, WEBINFO_URL + "/")
                    .cookies(c -> cookies.forEach((name, values) -> values.forEach(v -> c.add(name, v))))
                    .retrieve()
                    .onStatus(status -> false, resp -> null)
                    .toEntity(String.class)
                    .block();
        } catch (Exception e) {
            log.error("리다이렉트 요청 실패: {}", e.getMessage());
            throw new DkuFailedLoginException();
        }
    }

    private String makeFormData(String studentId, String password) {
        String encodedId = URLEncoder.encode(studentId, StandardCharsets.UTF_8);
        String encodedPwd = URLEncoder.encode(password, StandardCharsets.UTF_8);
        return "username=" + encodedId + "&password=" + encodedPwd + "&tabIndex=0";
    }

    private void collectCookies(HttpHeaders headers, MultiValueMap<String, String> cookies) {
        List<String> setCookieHeaders = headers.get(HttpHeaders.SET_COOKIE);
        if (setCookieHeaders == null) return;

        for (String setCookie : setCookieHeaders) {
            String[] parts = setCookie.split(";")[0].split("=", 2);
            if (parts.length == 2) {
                String name = parts[0].trim();
                String value = parts[1].trim();
                cookies.remove(name);
                cookies.add(name, value);
            }
        }
    }
}
