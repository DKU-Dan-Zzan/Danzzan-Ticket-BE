package com.danzzan.ticketing.infra.dku;

import com.danzzan.ticketing.infra.dku.exception.DkuFailedCrawlingException;
import com.danzzan.ticketing.infra.dku.model.DkuAuth;
import com.danzzan.ticketing.infra.dku.model.StudentInfo;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

// 단국대 학생 정보 스크래핑 서비스
@Slf4j
@Service
public class DkuStudentService {

    private static final String WEBINFO_URL = "https://webinfo.dankook.ac.kr";
    private static final String STUDENT_INFO_PATH = "/member/getMember.do";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final WebClient webClient;

    public DkuStudentService() {
        this.webClient = WebClient.builder()
                .baseUrl(WEBINFO_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    // 인증된 세션으로 학생 정보 크롤링
    // @param auth 인증 쿠키
    // @return 학생 정보
    public StudentInfo crawlStudentInfo(DkuAuth auth) {
        try {
            String html = webClient.get()
                    .uri(STUDENT_INFO_PATH)
                    .header(HttpHeaders.COOKIE, auth.toCookieHeader())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (html == null || html.isEmpty()) {
                throw new DkuFailedCrawlingException("학생 정보 페이지를 가져올 수 없습니다.");
            }

            return parseStudentInfo(html);

        } catch (DkuFailedCrawlingException e) {
            throw e;
        } catch (Exception e) {
            log.error("학생 정보 크롤링 실패: {}", e.getMessage());
            throw new DkuFailedCrawlingException("학생 정보 크롤링 중 오류가 발생했습니다.");
        }
    }

    // HTML에서 학생 정보 파싱
    private StudentInfo parseStudentInfo(String html) {
        Document doc = Jsoup.parse(html);

        // 단국대 웹정보시스템 HTML 구조에 맞게 파싱
        // 실제 구조에 따라 수정 필요할 수 있음
        String studentName = getValueById(doc, "userName", "name");
        String studentId = getValueById(doc, "userNumber", "hakbun");
        String college = getValueById(doc, "college", "collNm");
        String major = getValueById(doc, "major", "hakgwa");
        String academicStatus = getValueById(doc, "status", "hakjeok");
        String yearStr = getValueById(doc, "admissionYear", "ipYear");

        // 값이 없으면 테이블에서 추출 시도
        if (studentId == null || studentId.isEmpty()) {
            studentName = getValueFromTable(doc, "성명", "이름");
            studentId = getValueFromTable(doc, "학번");
            college = getValueFromTable(doc, "단과대학", "대학");
            major = getValueFromTable(doc, "학과", "전공");
            academicStatus = getValueFromTable(doc, "학적상태", "학적");
            yearStr = getValueFromTable(doc, "입학년도");
        }

        if (studentId == null || studentId.isEmpty()) {
            throw new DkuFailedCrawlingException("학생 정보를 파싱할 수 없습니다. 로그인 상태를 확인해주세요.");
        }

        int yearOfAdmission = 0;
        if (yearStr != null && !yearStr.isEmpty()) {
            try {
                yearOfAdmission = Integer.parseInt(yearStr.replaceAll("[^0-9]", "").substring(0, 4));
            } catch (Exception e) {
                log.warn("입학년도 파싱 실패: {}", yearStr);
            }
        }

        return new StudentInfo(
                studentName != null ? studentName : "",
                studentId,
                college != null ? college : "",
                major != null ? major : "",
                academicStatus != null ? academicStatus : "",
                yearOfAdmission
        );
    }

    // ID나 name 속성으로 값 추출
    private String getValueById(Document doc, String... ids) {
        for (String id : ids) {
            Element element = doc.getElementById(id);
            if (element != null) {
                String value = element.val();
                if (value != null && !value.isEmpty()) {
                    return value.trim();
                }
                value = element.text();
                if (value != null && !value.isEmpty()) {
                    return value.trim();
                }
            }

            // name 속성으로 검색
            element = doc.selectFirst("[name=" + id + "]");
            if (element != null) {
                String value = element.val();
                if (value != null && !value.isEmpty()) {
                    return value.trim();
                }
            }

            // class로 검색
            element = doc.selectFirst("." + id);
            if (element != null) {
                return element.text().trim();
            }
        }
        return null;
    }

    // 테이블에서 라벨로 값 추출
    // 예: <th>학번</th><td>32100000</td>
    private String getValueFromTable(Document doc, String... labels) {
        for (String label : labels) {
            // th-td 구조
            Element th = doc.selectFirst("th:contains(" + label + ")");
            if (th != null) {
                Element td = th.nextElementSibling();
                if (td != null && td.tagName().equals("td")) {
                    return td.text().trim();
                }
            }

            // dt-dd 구조
            Element dt = doc.selectFirst("dt:contains(" + label + ")");
            if (dt != null) {
                Element dd = dt.nextElementSibling();
                if (dd != null && dd.tagName().equals("dd")) {
                    return dd.text().trim();
                }
            }

            // label-span 구조
            Element labelEl = doc.selectFirst("label:contains(" + label + ")");
            if (labelEl != null) {
                Element parent = labelEl.parent();
                if (parent != null) {
                    Element span = parent.selectFirst("span");
                    if (span != null) {
                        return span.text().trim();
                    }
                }
            }
        }
        return null;
    }
}
