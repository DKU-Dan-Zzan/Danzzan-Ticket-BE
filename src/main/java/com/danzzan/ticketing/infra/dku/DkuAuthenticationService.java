package com.danzzan.ticketing.infra.dku;

import com.danzzan.ticketing.infra.dku.exception.DkuFailedLoginException;
import com.danzzan.ticketing.infra.dku.model.DkuAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// 단국대 포털 인증 서비스
// webinfo.dankook.ac.kr에 로그인하여 인증 쿠키 획득
@Slf4j
@Service
public class DkuAuthenticationService {

    private static final String WEBINFO_URL = "https://webinfo.dankook.ac.kr";
    private static final String LOGIN_PATH = "/member/logIn.do";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final WebClient webClient;

    public DkuAuthenticationService() {
        this.webClient = WebClient.builder()
                .baseUrl(WEBINFO_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .build();
    }

    // 단국대 웹정보시스템 로그인
    // @param studentId 학번
    // @param password 비밀번호
    // @return 인증된 쿠키 정보
    public DkuAuth login(String studentId, String password) {
        DkuAuth auth = new DkuAuth();

        // 1단계: 초기 페이지 접속하여 세션 쿠키 획득
        getInitialCookies(auth);

        // 2단계: 로그인 요청
        boolean loginSuccess = performLogin(auth, studentId, password);
        if (!loginSuccess) {
            throw new DkuFailedLoginException();
        }

        return auth;
    }

    // 초기 페이지 접속하여 세션 쿠키 획득
    private void getInitialCookies(DkuAuth auth) {
        try {
            ClientResponse response = webClient.get()
                    .uri("/")
                    .exchange()
                    .block();

            if (response != null) {
                extractCookies(response, auth);
            }
        } catch (Exception e) {
            log.warn("초기 쿠키 획득 실패: {}", e.getMessage());
        }
    }

    // 실제 로그인 수행
    private boolean performLogin(DkuAuth auth, String studentId, String password) {
        try {
            // 로그인 파라미터 구성
            String encodedId = URLEncoder.encode(studentId, StandardCharsets.UTF_8);
            String encodedPwd = URLEncoder.encode(password, StandardCharsets.UTF_8);
            String formData = "username=" + encodedId + "&password=" + encodedPwd + "&tabIndex=0";

            ClientResponse response = webClient.post()
                    .uri(LOGIN_PATH)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .header(HttpHeaders.COOKIE, auth.toCookieHeader())
                    .header(HttpHeaders.REFERER, WEBINFO_URL + "/")
                    .bodyValue(formData)
                    .exchange()
                    .block();

            if (response == null) {
                return false;
            }

            extractCookies(response, auth);

            // 로그인 성공 여부 확인
            int statusCode = response.statusCode().value();
            String body = response.bodyToMono(String.class).block();

            // 실패 시 에러 메시지 포함됨
            if (body != null && (body.contains("실패") || body.contains("일치하지") || body.contains("error"))) {
                return false;
            }

            // 302 리다이렉트 또는 200 OK면 성공
            return statusCode == 200 || statusCode == 302;

        } catch (Exception e) {
            log.error("로그인 요청 실패: {}", e.getMessage());
            return false;
        }
    }

    // 응답 헤더에서 쿠키 추출
    private void extractCookies(ClientResponse response, DkuAuth auth) {
        List<String> setCookieHeaders = response.headers().header(HttpHeaders.SET_COOKIE);
        MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();

        for (String setCookie : setCookieHeaders) {
            // "JSESSIONID=xxx; Path=/; HttpOnly" 형태에서 이름=값만 추출
            String[] parts = setCookie.split(";")[0].split("=", 2);
            if (parts.length == 2) {
                cookies.add(parts[0].trim(), parts[1].trim());
            }
        }

        auth.addCookies(cookies);
    }
}
