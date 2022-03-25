package com.example.psqljwt.resources;

import com.example.psqljwt.Constants;
import com.example.psqljwt.repositories.PoaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/subcontractor")
public class PoaController {

    @Autowired
    PoaRepository userRepository;

    @Autowired
    RestTemplate restTemplate;

    @Value("${ah_onboarding_uri}")
    private String AH_ONBOARDING_URI;

    @GetMapping("/poa-onboarding-user-request")
    public ResponseEntity<String> fetchOnboardingPoa() {
        HttpEntity<String> response;
        try {
            response = restTemplate.getForEntity(AH_ONBOARDING_URI, String.class);
        } catch (Exception ex) {
            System.out.println(ex);
            return new ResponseEntity<>(
                    "Failed to retrieve PoA from Arrowhead PoaOnboarding controller",
                    HttpStatus.BAD_GATEWAY);
        }
        final String poa = response.getBody();
        System.out.println(poa);
        // TODO: Some minimal error checking
        userRepository.writePoa(poa);
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    @PostMapping("/poa-subcontractor")
    public ResponseEntity<Map<String, String>> registerUser() {
        return new ResponseEntity<>(generateJWTToken(), HttpStatus.OK);
    }
    
    private Map<String, String> generateJWTToken() {
        long timestamp = System.currentTimeMillis();
        final String poa = userRepository.readLatestPoa();
        // TODO: Handle the case where this is null.

        final String token = Jwts.builder().signWith(SignatureAlgorithm.HS256, Constants.API_SECRET_KEY)
                .setIssuedAt(new Date(timestamp))
                .setExpiration(new Date(timestamp + Constants.TOKEN_VALIDITY))
                // TODO: Change these claims
                .claim("destinationNetworkId", "xyz")
                .claim("metadata", "abc")
                .claim("transferable", "0")
                .claim("proofOfChain", poa) // TODO: What do we do here?
                .compact();
                
        return Map.of("token", token);
    }
}
