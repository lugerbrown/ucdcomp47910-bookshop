package com.ucd.bookshop.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ucd.bookshop.model.User;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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

        try {
            return generateQRCodeDataUrl(otpauth);
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
    
    private String generateQRCodeDataUrl(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200, hints);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        byte[] qrCodeBytes = outputStream.toByteArray();
        String base64QRCode = Base64.getEncoder().encodeToString(qrCodeBytes);
        
        return "data:image/png;base64," + base64QRCode;
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
