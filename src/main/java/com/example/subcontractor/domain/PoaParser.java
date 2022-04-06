package com.example.subcontractor.domain;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import com.example.subcontractor.exceptions.InternalServerErrorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Service
public class PoaParser {

    @Value("${poa-onboarding-public-key}")
    Resource ONBOARDING_PUBLIC_KEY;

    public Claims getClaims(final String token) {
        final PublicKey onboardingControllerPublicKey = readOnboadingControllerPublicKey();
        return Jwts.parser()
                .setSigningKey(onboardingControllerPublicKey)
                .parseClaimsJws(token)
                .getBody();
    }

    private PublicKey readOnboadingControllerPublicKey() {
        final String keyString = readOnboardingControllerPublicKeyAsString()
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("\\n", "")
                .replace("-----END PUBLIC KEY-----", "");
        final byte[] keyBytes = Base64.getDecoder().decode(keyString);

        try {
            final KeyFactory fact = KeyFactory.getInstance("RSA");
            final X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyBytes);
            return fact.generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            throw new InternalServerErrorException("Failed to read PoaOnboarding public key");
        }
    }

    public String readOnboardingControllerPublicKeyAsString() {
        try (Reader reader =
                new InputStreamReader(ONBOARDING_PUBLIC_KEY.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            e.printStackTrace();
            throw new InternalServerErrorException("Failed to read PoA onboarding controller public key");
        }
    }

}
