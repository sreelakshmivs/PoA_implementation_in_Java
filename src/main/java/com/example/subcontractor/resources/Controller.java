package com.example.subcontractor.resources;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.util.Date;
import java.util.Map;
import com.example.subcontractor.Constants;
import com.example.subcontractor.domain.Config;
import com.example.subcontractor.repositories.ConfigRepository;
import com.example.subcontractor.repositories.PoaRepository;

@RestController
@RequestMapping("/subcontractor")
public class Controller {

    @Autowired
    PoaRepository poaRepository;

    @Autowired
    ConfigRepository configRepository;

    @Autowired
    RestTemplate restTemplate;

    @Value("${ah_onboarding_uri}")
    private String AH_ONBOARDING_URI;

    @GetMapping("/echo")
    public String echo() {
        return "OK";
    }

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
        // TODO: Some minimal error checking
        poaRepository.write(poa);
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    @GetMapping("/config")
    public Config getConfig() {
        return configRepository.readLatest();
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, String>> createConfig(
            @RequestBody final Map<String, String> config) {
        configRepository.write(
                config.get("destinationNetworkId"),
                config.get("transferable"),
                config.get("metadata"));
        return new ResponseEntity<>(generateJWTToken(), HttpStatus.OK);
    }

    @GetMapping("/poa-subcontractor")
    public ResponseEntity<Map<String, String>> generatePoaSubcontractor() {
        return new ResponseEntity<>(generateJWTToken(), HttpStatus.OK); // Pass along the device's
                                                                        // public key
    }

    private Map<String, String> generateJWTToken() {
        long timestamp = System.currentTimeMillis();
        // TODO: Handle the case where this is null.
        final String poa = poaRepository.readLatest().getPoa();

        final String token =
                Jwts.builder().signWith(SignatureAlgorithm.HS256, Constants.API_SECRET_KEY)
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
