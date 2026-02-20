package com.danzzan.ticketing.infra.dku;

import com.danzzan.ticketing.infra.dku.exception.DkuFailedLoginException;
import com.danzzan.ticketing.infra.dku.model.DkuAuth;
import lombok.extern.slf4j.Slf4j;
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
// 1. webinfo.dankook.ac.kr/member/logon.do?sso=ok 로 POST (username, password, tabIndex=0)
// 2. 302 → pmi-sso.jsp → portal → pmi-sso2.jsp → 리다이렉트 체인 따라가기
// 3. webinfo 학생정보 페이지 접근하여 세션 쿠키 확보
@Slf4j
@Service
public class DkuAuthenticationService {

    private static final String WEBINFO_URL = "https://webinfo.dankook.ac.kr";
    private static final String LOGIN_URL = WEBINFO_URL + "/member/logon.do?sso=ok";
    private static final int MAX_REDIRECTS = 10;
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

        // 0단계: webinfo 접속하여 초기 쿠키 수집
        collectInitialCookies(cookies);

        // 1단계: webinfo의 logon.do에 직접 POST (SSO 로그인 페이지가 아닌 webinfo로!)
        String formData = makeFormData(studentId, password);
        ResponseEntity<String> loginResponse = postLogin(LOGIN_URL, formData, cookies);

        HttpStatus loginStatus = (HttpStatus) loginResponse.getStatusCode();
        collectCookies(loginResponse.getHeaders(), cookies);

        log.info("로그인 POST 응답: {}, Location: {}", loginStatus, loginResponse.getHeaders().getLocation());

        // 200 OK = 로그인 실패 (로그인 페이지가 다시 렌더링됨)
        if (loginStatus == HttpStatus.OK) {
            log.warn("DKU 로그인 실패: 200 OK (잘못된 학번/비밀번호)");
            throw new DkuFailedLoginException();
        }

        // 302가 아니면 예상 외 응답
        if (!isRedirect(loginStatus)) {
            log.error("DKU 로그인 예상 외 응답: {}", loginStatus);
            throw new DkuFailedLoginException();
        }

        // 2단계: SSO 리다이렉트 체인 따라가기 (pmi-sso.jsp → portal → pmi-sso2.jsp → ...)
        URI location = loginResponse.getHeaders().getLocation();
        if (location == null) {
            log.error("DKU 로그인: Location 헤더 없음");
            throw new DkuFailedLoginException();
        }

        followRedirectChain(location, cookies);

        // 3단계: webinfo 학생정보 페이지에 접근하여 세션 확보
        try {
            URI studentInfoUri = URI.create(WEBINFO_URL + "/tiac/univ/srec/srlm/views/findScregBasWeb.do?_view=ok");
            followRedirectChain(studentInfoUri, cookies);
        } catch (Exception e) {
            log.warn("학생정보 페이지 사전 접근 실패 (무시): {}", e.getMessage());
        }

        log.info("DKU 로그인 완료. 수집된 쿠키 키: {}", cookies.keySet());
        return new DkuAuth(cookies);
    }

    // webinfo 초기 접속으로 쿠키 수집
    private void collectInitialCookies(MultiValueMap<String, String> cookies) {
        try {
            URI currentUri = URI.create(WEBINFO_URL + "/");
            for (int i = 0; i < MAX_REDIRECTS; i++) {
                ResponseEntity<String> response = doGet(currentUri, cookies);
                collectCookies(response.getHeaders(), cookies);

                HttpStatus status = (HttpStatus) response.getStatusCode();
                if (!isRedirect(status)) break;

                URI nextLocation = response.getHeaders().getLocation();
                if (nextLocation == null) break;
                if (!nextLocation.isAbsolute()) {
                    nextLocation = currentUri.resolve(nextLocation);
                }
                currentUri = nextLocation;
            }
        } catch (Exception e) {
            log.warn("초기 쿠키 수집 중 오류 (무시): {}", e.getMessage());
        }
    }

    // 리다이렉트 체인 따라가기
    private void followRedirectChain(URI startLocation, MultiValueMap<String, String> cookies) {
        URI location = startLocation;
        for (int i = 0; i < MAX_REDIRECTS; i++) {
            log.info("리다이렉트 {}단계: {}", i + 1, location);
            ResponseEntity<String> response = doGet(location, cookies);
            collectCookies(response.getHeaders(), cookies);

            HttpStatus status = (HttpStatus) response.getStatusCode();
            if (!isRedirect(status)) break;

            URI nextLocation = response.getHeaders().getLocation();
            if (nextLocation == null) break;
            if (!nextLocation.isAbsolute()) {
                nextLocation = location.resolve(nextLocation);
            }
            location = nextLocation;
        }
    }

    private boolean isRedirect(HttpStatus status) {
        return status == HttpStatus.FOUND || status == HttpStatus.MOVED_TEMPORARILY
                || status == HttpStatus.MOVED_PERMANENTLY || status == HttpStatus.TEMPORARY_REDIRECT;
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

    // GET 요청
    private ResponseEntity<String> doGet(URI location, MultiValueMap<String, String> cookies) {
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
            log.error("GET 요청 실패: {}", e.getMessage());
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
