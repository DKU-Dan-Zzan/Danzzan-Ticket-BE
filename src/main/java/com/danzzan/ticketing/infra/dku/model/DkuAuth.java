package com.danzzan.ticketing.infra.dku.model;

import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

// 단국대 포털 인증 정보
// 로그인 후 발급받은 세션 쿠키를 저장하고 관리
@Getter
public class DkuAuth {

    private final MultiValueMap<String, String> cookies;

    public DkuAuth() {
        this.cookies = new LinkedMultiValueMap<>();
    }

    public DkuAuth(MultiValueMap<String, String> cookies) {
        this.cookies = new LinkedMultiValueMap<>();
        this.cookies.addAll(cookies);
    }

    public void addCookies(MultiValueMap<String, String> newCookies) {
        this.cookies.addAll(newCookies);
    }

    public void addCookie(String name, String value) {
        this.cookies.add(name, value);
    }

    // 쿠키를 HTTP 요청 헤더 형식으로 변환
    // 예: "JSESSIONID=xxx; WMONID=yyy"
    public String toCookieHeader() {
        StringBuilder sb = new StringBuilder();
        cookies.forEach((name, values) -> {
            for (String value : values) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(name).append("=").append(value);
            }
        });
        return sb.toString();
    }
}
