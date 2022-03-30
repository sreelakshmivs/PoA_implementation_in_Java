package com.example.subcontractor.resources;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
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
import java.util.Date;
import java.util.Map;
import com.example.subcontractor.Constants;
import com.example.subcontractor.domain.Poa;
import com.example.subcontractor.exceptions.BadGatewayException;
import com.example.subcontractor.exceptions.InternalServerErrorException;
import com.example.subcontractor.repositories.PoaRepository;

@RestController
@RequestMapping("/subcontractor")
public class Controller {

    @Autowired
    PoaRepository poaRepository;

    @Autowired
    RestTemplate restTemplate;

    @Value("${ah_onboarding_uri}")
    private String AH_ONBOARDING_URI;

    @Value("${poa-onboarding-public-key}")
    Resource ONBOARDING_PUBLIC_KEY;

    @GetMapping("/echo")
    public String echo() {
        return "OK";
    }

    @GetMapping("/poa-onboarding-user-request")
    public String fetchOnboardingPoa() {
        HttpEntity<String> response;
        try {
            response = restTemplate.getForEntity(AH_ONBOARDING_URI, String.class);
        } catch (Exception ex) {
            System.out.println(ex);
            throw new BadGatewayException(
                    "Failed to retrieve PoA from Arrowhead PoaOnboarding controller");
        }
        final Claims poa = getClaims(response.getBody());
        final String destinationNetworkId = poa.get("destinationNetworkId", String.class);
        // TODO: Some minimal error checking
        poaRepository.write(destinationNetworkId);
        return "PoA successfully retrieved from the PoA Onboarding controller.";
    }

    @GetMapping("/poa-subcontractor")
    public ResponseEntity<Map<String, String>> generatePoaSubcontractor() {
        final Map<String, String> responseBody = Map.of("token", generatePoa());
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    private String generatePoa() {
        long timestamp = System.currentTimeMillis();
        // TODO: Handle the case where this is null.
        final Poa poaOnboarding = poaRepository.readLatest();

        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, Constants.API_SECRET_KEY)
                .setIssuedAt(new Date(timestamp))
                .setExpiration(new Date(timestamp + Constants.TOKEN_VALIDITY))
                // TODO: Change these claims
                .claim("destinationNetworkId", poaOnboarding.getDestinationNetworkId())
                .claim("metadata", "abc")
                .claim("transferable", "0")
                .claim("proofOfChain", "<Not yet available>")
                .compact();
    }

    private Claims getClaims(final String token) {
        final PublicKey onboardingControllerPublicKey = readPublicKey();
        return Jwts.parser()
                .setSigningKey(onboardingControllerPublicKey)
                .parseClaimsJws(token)
                .getBody();
    }

    private PublicKey readPublicKey() {
        final String keyString = readPublicKeyAsString()
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("\\n", "")
                .replace("-----END PUBLIC KEY-----", "");
        final byte[] keyBytes = Base64.getDecoder().decode(keyString);

        try {
            final KeyFactory fact = KeyFactory.getInstance("RSA");
            final X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyBytes);
            return fact.generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println(e);
            throw new InternalServerErrorException("Failed to read PoaOnboarding public key");
        }
    }

    public String readPublicKeyAsString() {
        try (Reader reader =
                new InputStreamReader(ONBOARDING_PUBLIC_KEY.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            System.out.println(e);
            throw new InternalServerErrorException("Failed to read PoA onboarding controller public key");
        }
    }
}
