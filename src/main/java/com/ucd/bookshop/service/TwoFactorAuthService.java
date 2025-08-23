package com.ucd.bookshop.service;

import com.ucd.bookshop.model.User;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TwoFactorAuthService {
    
    private static final String APP_NAME = "BookShop";
    
    public String generateSecret() {
        return Base32.random();
    }
    
    public String generateQRUrl(User user) {
        String issuer = APP_NAME;
        String label = user.getUsername();
        
        String otpauth = "otpauth://totp/"
                + URLEncoder.encode(issuer + ":" + label, StandardCharsets.UTF_8)
                + "?secret=" + URLEncoder.encode(user.getSecret(), StandardCharsets.UTF_8)
                + "&issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8);

        return "https://quickchart.io/qr?size=200&text=" +
                URLEncoder.encode(otpauth, StandardCharsets.UTF_8);
    }
    
    public void enableTwoFactorAuth(User user) {
        user.setSecret(generateSecret());
        user.setUsing2FA(true);
    }
    
    public void disableTwoFactorAuth(User user) {
        user.setSecret(null);
        user.setUsing2FA(false);
    }
}
