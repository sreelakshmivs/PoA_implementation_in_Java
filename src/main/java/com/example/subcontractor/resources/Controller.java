package com.example.subcontractor.resources;

import java.util.Date;
import java.util.Map;
import com.example.subcontractor.Constants;
import com.example.subcontractor.domain.Poa;
import com.example.subcontractor.domain.PoaParser;
import com.example.subcontractor.exceptions.BadGatewayException;
import com.example.subcontractor.repositories.PoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@RestController
@RequestMapping("/subcontractor")
public class Controller {

    @Autowired
    PoaRepository poaRepository;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    PoaParser poaParser;

    @Value("${ah_onboarding_uri}")
    private String AH_ONBOARDING_URI;

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
        final Claims poaClaims = poaParser.getClaims(response.getBody());
        final String destinationNetworkId = poaClaims.get("destinationNetworkId", String.class);

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

}
