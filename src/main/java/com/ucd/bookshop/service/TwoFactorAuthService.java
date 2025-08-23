package com.ucd.bookshop.service;

import com.ucd.bookshop.model.User;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorAuthService {
    
    private static final String APP_NAME = "BookShop";
    
    public String generateSecret() {
        return Base32.random();
    }
    
    public String generateQRUrl(User user) {
        String secretKey = user.getSecret();
        String username = user.getUsername();
        
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            APP_NAME,
            username,
            secretKey,
            APP_NAME
        );
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
