package com.example.subcontractor;

import javax.net.ssl.SSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

@Configuration
public class HttpConfig {

    @Value("${server.ssl.key-store}")
    private String KEY_STORE;

    @Value("${server.ssl.trust-store}")
    private String TRUST_STORE;

    @Value("${server.ssl.key-password}")
    private String KEY_PASSWORD;

    @Value("${server.ssl.key-store-password}")
    private String KEY_STORE_PASSWORD;

    @Value("${server.ssl.trust-store-password}")
    private String TRUST_STORE_PASSWORD;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) throws Exception {
        final SSLContext sslContext = SSLContextBuilder
                .create()
                .loadKeyMaterial(
                        ResourceUtils.getFile(KEY_STORE),
                        KEY_STORE_PASSWORD.toCharArray(),
                        KEY_PASSWORD.toCharArray())
                .loadTrustMaterial(
                        ResourceUtils.getFile(TRUST_STORE),
                        TRUST_STORE_PASSWORD.toCharArray())
                .build();

        final HttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        return builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
                .build();
    }

}
