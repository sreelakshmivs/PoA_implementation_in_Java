package com.example.subcontractor.domain;

import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import com.example.subcontractor.Constants;
import com.example.subcontractor.exceptions.InternalServerErrorException;
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
    private String KEY_STORE_PASSWORD;

    @Autowired
    PoaRepository poaRepository;

    public String generate() {
        long timestamp = System.currentTimeMillis();
        // TODO: Handle the case where this is null.
        final Poa poaOnboarding = poaRepository.readLatest();

        String publicKey;
        String principalName;

        try {
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(KEY_STORE.getInputStream(), KEY_STORE_PASSWORD.toCharArray());
            principalName = keyStore.aliases().nextElement();
            publicKey = getPublicKey(keyStore, principalName);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            e.printStackTrace();
            throw new InternalServerErrorException("Failed to read public data from key store");
        }

        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, Constants.API_SECRET_KEY)
                .setIssuedAt(new Date(timestamp))
                .setExpiration(new Date(timestamp + Constants.TOKEN_VALIDITY))
                .claim("destinationNetworkId", poaOnboarding.getDestinationNetworkId())
                .claim("transferable", "0")
                .claim("proofOfChain", "<Not yet available>") // TODO: Change
                .claim("principlePublicKey", publicKey)
                .claim("agentPublicKey", "<Not yet available>")
                .claim("metadata", generateMetadata(principalName))
                .compact();
    }

    private Map<String, String> generateMetadata(final String principalName) {
        return Map.of(
                "principalName", principalName,
                "agentName", "<AGENT NAME>",
                "credentials", "Submit PoA to the Destination Network ID");
    }

    private String getPublicKey(final KeyStore keyStore, final String alias) throws KeyStoreException {
        final Certificate certificate = keyStore.getCertificate(alias);
        final PublicKey publicKey = certificate.getPublicKey();
        final byte[] encodedPublicKey = publicKey.getEncoded();
        return Base64.getMimeEncoder().encodeToString(encodedPublicKey);
    }

}
