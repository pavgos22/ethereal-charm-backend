package com.ethereal.auth.services;


import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    public Cookie generateCookie(String name, String value, int exp) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(exp);
        cookie.setHttpOnly(true);
        return cookie;
    }

    public Cookie removeCookie(Cookie[] cookies, String name) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                cookie.setPath("/");
                cookie.setMaxAge(0);
                cookie.setHttpOnly(true);
                return cookie;
            }
        }
        return null;
    }
}
