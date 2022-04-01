package com.example.subcontractor.resources;

import java.util.Map;
import com.example.subcontractor.domain.PoaGenerator;
import com.example.subcontractor.domain.PoaParser;
import com.example.subcontractor.exceptions.BadGatewayException;
import com.example.subcontractor.repositories.PoaRepository;
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
import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/subcontractor")
public class Controller {

    @Autowired
    PoaRepository poaRepository;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    PoaParser poaParser;

    @Autowired
    PoaGenerator poaGenerator;

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

    @PostMapping("/poa-subcontractor")
    public ResponseEntity<Map<String, String>> generatePoaSubcontractor(
            final @RequestBody String request) {
        final Map<String, String> responseBody = Map.of("token", poaGenerator.generate(request));
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

}
