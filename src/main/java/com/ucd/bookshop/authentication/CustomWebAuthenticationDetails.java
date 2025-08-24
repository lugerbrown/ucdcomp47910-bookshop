package com.ucd.bookshop.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import java.util.Objects;

public class CustomWebAuthenticationDetails extends WebAuthenticationDetails {

    private String verificationCode;

    public CustomWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        verificationCode = request.getParameter("code");
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        CustomWebAuthenticationDetails that = (CustomWebAuthenticationDetails) obj;
        return Objects.equals(verificationCode, that.verificationCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), verificationCode);
    }
}
