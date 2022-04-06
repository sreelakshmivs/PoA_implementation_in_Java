package com.example.subcontractor.domain;

import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import com.example.subcontractor.Constants;
import com.example.subcontractor.exceptions.InternalServerErrorException;
import com.example.subcontractor.exceptions.NotFoundException;
import com.example.subcontractor.repositories.PoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class PoaGenerator {

    @Value("${server.ssl.key-store}")
    private Resource KEY_STORE;

    @Value("${server.ssl.key-password}")
    private String KEY_PASSWORD;

    @Value("${server.ssl.key-store-password}")
    private String KEY_STORE_PASSWORD;

    @Autowired
    PoaRepository poaRepository;

    public String generate(final String agentName, final String agentPublicKey) {
        long timestamp = System.currentTimeMillis();
        // TODO: Handle the case where this is null.
        final Poa poaOnboarding = poaRepository.readLatest();

        if (poaOnboarding == null) {
            throw new NotFoundException("PoA onboarding not found in database");
        }

        try {
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(KEY_STORE.getInputStream(), KEY_STORE_PASSWORD.toCharArray());
            final String principalName = keyStore.aliases().nextElement();
            final String principalPublicKey = toString(getPublicKey(keyStore, principalName));

            return Jwts.builder()
                .signWith(SignatureAlgorithm.RS256, getPrivateKey(keyStore, principalName))
                .setIssuedAt(new Date(timestamp))
                .setExpiration(new Date(timestamp + Constants.TOKEN_VALIDITY))
                .claim("destinationNetworkId", poaOnboarding.getDestinationNetworkId())
                .claim("transferable", "0")
                .claim("proofOfChain", "<Not yet available>") // TODO: Change
                .claim("principalPublicKey", principalPublicKey)
                .claim("agentPublicKey", agentPublicKey)
                .claim("metadata", generateMetadata(agentName, principalName))
                .compact();
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException e) {
            e.printStackTrace();
            // TODO: Improve exception handling
            throw new InternalServerErrorException("Failed to read public data from key store");
        }
    }

    private Map<String, String> generateMetadata(final String agentName, final String principalName) {
        return Map.of(
                "principalName", principalName,
                "agentName", agentName,
                "credentials", "Submit PoA to the Destination Network ID");
    }

    private PublicKey getPublicKey(final KeyStore keyStore, final String alias) throws KeyStoreException {
        final Certificate certificate = keyStore.getCertificate(alias);
        return certificate.getPublicKey();
    }

    private Key getPrivateKey(final KeyStore keyStore, final String alias) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        return keyStore.getKey(alias, KEY_STORE_PASSWORD.toCharArray());
    }

    private String toString(final Key key) {
        final byte[] encodedKey = key.getEncoded();
        return Base64.getEncoder().encodeToString(encodedKey);
    }

}
