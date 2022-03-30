package com.example.subcontractor.domain;

import java.util.Date;
import java.util.Map;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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

        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, Constants.API_SECRET_KEY)
                .setIssuedAt(new Date(timestamp))
                .setExpiration(new Date(timestamp + Constants.TOKEN_VALIDITY))
                .claim("destinationNetworkId", poaOnboarding.getDestinationNetworkId())
                .claim("transferable", "0")
                .claim("proofOfChain", "<Not yet available>") // TODO: Change
                .claim("metadata", generateMetadata())
                .compact();
    }

    private Map<String, String> generateMetadata() {
        return Map.of(
                "Principal name", getNameFromKeyStore(),
                "Agent name", "<AGENT NAME>",
                "Credentials", "Submit PoA to the Destination Network ID");
    }

    private String getNameFromKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(KEY_STORE.getInputStream(), KEY_STORE_PASSWORD.toCharArray());
            return keyStore.aliases().nextElement();
        } catch (CertificateException | IOException | NoSuchAlgorithmException
                | KeyStoreException e) {
            System.out.println(e);
            throw new InternalServerErrorException("Cannot get common name from cert");
        }
    }

}
