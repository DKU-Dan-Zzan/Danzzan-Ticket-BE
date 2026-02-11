package com.danzzan.ticketing.infra.dku;

import com.danzzan.ticketing.infra.dku.exception.DkuFailedCrawlingException;
import com.danzzan.ticketing.infra.dku.model.DkuAuth;
import com.danzzan.ticketing.infra.dku.model.StudentInfo;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
public class DkuStudentService {

    private static final String WEBINFO_URL = "https://webinfo.dankook.ac.kr";
    private static final String STUDENT_INFO_PATH = "/tiac/univ/srec/srlm/views/findScregBasWeb.do?_view=ok";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final WebClient webClient;

    public DkuStudentService() {
        HttpClient httpClient = HttpClient.create()
                .followRedirect(false);

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    public StudentInfo crawlStudentInfo(DkuAuth auth) {
        try {
            MultiValueMap<String, String> cookies = auth.getCookies();
            log.info("크롤링 시작. 쿠키: {}", cookies.keySet());

            // 학생 정보 페이지 요청
            String html = fetchStudentInfo(cookies);

            if (html == null || html.isEmpty()) {
                throw new DkuFailedCrawlingException("학생 정보 페이지를 가져올 수 없습니다.");
            }

            log.info("학생 정보 HTML 길이: {}", html.length());
            // 디버그: HTML을 파일에 저장
            try {
                java.nio.file.Files.writeString(
                    java.nio.file.Path.of("/tmp/dku_student_info.html"), html);
            } catch (Exception ex) {
                log.warn("HTML 파일 저장 실패: {}", ex.getMessage());
            }

            return parseStudentInfo(html);

        } catch (Exception e) {
            log.error("학생 정보 크롤링 실패: {}", e.getMessage(), e);
            // 에러 정보를 파일에 저장
            try {
                java.nio.file.Files.writeString(
                    java.nio.file.Path.of("/tmp/dku_error.txt"),
                    "Error: " + e.getClass().getName() + ": " + e.getMessage() + "\nCookies: " + auth.getCookies());
            } catch (Exception ignored) {}
            throw new DkuFailedCrawlingException("학생 정보 크롤링 중 오류가 발생했습니다.");
        }
    }

    // Cookie 헤더 문자열 생성
    private String toCookieHeader(MultiValueMap<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        cookies.forEach((name, values) -> {
            for (String value : values) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(name).append("=").append(value);
            }
        });
        return sb.toString();
    }

    // 학생정보 페이지 직접 요청 (Cookie 헤더로 전달)
    private String fetchStudentInfo(MultiValueMap<String, String> cookies) {
        String cookieHeader = toCookieHeader(cookies);
        log.info("학생정보 요청 Cookie 헤더: {}", cookieHeader);

        ResponseEntity<String> response = webClient.get()
                .uri(WEBINFO_URL + STUDENT_INFO_PATH)
                .header(HttpHeaders.COOKIE, cookieHeader)
                .header(HttpHeaders.REFERER, WEBINFO_URL + "/")
                .retrieve()
                .onStatus(status -> false, resp -> null)
                .toEntity(String.class)
                .block();

        if (response == null) return null;

        HttpStatus status = (HttpStatus) response.getStatusCode();
        log.info("학생정보 응답 상태: {}", status);

        if (status == HttpStatus.OK) {
            return response.getBody();
        }

        // 302면 SSO 리다이렉트를 따라가야 함
        if (status == HttpStatus.FOUND || status == HttpStatus.MOVED_TEMPORARILY) {
            URI location = response.getHeaders().getLocation();
            log.info("학생정보 리다이렉트: {}", location);

            if (location != null) {
                // SSO 리다이렉트 체인 따라가기
                collectCookies(response.getHeaders(), cookies);
                cookieHeader = toCookieHeader(cookies);

                for (int i = 0; i < 5; i++) {
                    log.info("학생정보 리다이렉트 {}단계: {}", i + 1, location);
                    ResponseEntity<String> rResponse = webClient.get()
                            .uri(location)
                            .header(HttpHeaders.COOKIE, cookieHeader)
                            .header(HttpHeaders.REFERER, WEBINFO_URL + "/")
                            .retrieve()
                            .onStatus(s -> false, resp -> null)
                            .toEntity(String.class)
                            .block();

                    if (rResponse == null) return null;
                    collectCookies(rResponse.getHeaders(), cookies);
                    cookieHeader = toCookieHeader(cookies);

                    HttpStatus rStatus = (HttpStatus) rResponse.getStatusCode();
                    if (rStatus == HttpStatus.OK) {
                        return rResponse.getBody();
                    }

                    location = rResponse.getHeaders().getLocation();
                    if (location == null) {
                        return rResponse.getBody();
                    }
                }
            }
        }

        return response.getBody();
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

    private StudentInfo parseStudentInfo(String html) {
        Document doc = Jsoup.parse(html);

        String studentName = getElementValue(doc, "nm");
        String studentId = getElementValue(doc, "stuid");
        String academicStatus = getElementValue(doc, "scregStaNm");
        String affiliation = getElementValue(doc, "pstnOrgzNm");
        String yearStr = getElementValue(doc, "etrsYy");

        if (studentId == null || studentId.isEmpty()) {
            log.error("학생 정보 파싱 실패. HTML 앞 500자: {}", html.substring(0, Math.min(500, html.length())));
            throw new DkuFailedCrawlingException("학생 정보를 파싱할 수 없습니다. 로그인 상태를 확인해주세요.");
        }

        String college = "";
        String major = "";
        if (affiliation != null && !affiliation.isEmpty()) {
            int spaceIdx = affiliation.lastIndexOf(' ');
            if (spaceIdx > 0) {
                college = affiliation.substring(0, spaceIdx).trim();
                major = affiliation.substring(spaceIdx + 1).trim();
            } else {
                major = affiliation.trim();
            }
        }

        int yearOfAdmission = 0;
        if (yearStr != null && !yearStr.isEmpty()) {
            try {
                yearOfAdmission = Integer.parseInt(yearStr.replaceAll("[^0-9]", "").substring(0, 4));
            } catch (Exception e) {
                log.warn("입학년도 파싱 실패: {}", yearStr);
            }
        }

        log.info("학생 정보 파싱 성공: 학번={}, 이름={}, 학적={}", studentId, studentName, academicStatus);

        return new StudentInfo(
                studentName != null ? studentName : "",
                studentId,
                college,
                major,
                academicStatus != null ? academicStatus : "",
                yearOfAdmission
        );
    }

    private String getElementValue(Document doc, String id) {
        Element element = doc.getElementById(id);
        if (element != null) {
            String value = element.val();
            if (value != null && !value.isEmpty()) {
                return value.trim();
            }
            String text = element.text();
            if (text != null && !text.isEmpty()) {
                return text.trim();
            }
        }

        Element byName = doc.selectFirst("[name=" + id + "]");
        if (byName != null) {
            String value = byName.val();
            if (value != null && !value.isEmpty()) {
                return value.trim();
            }
        }

        return null;
    }
}
