package org.example.lbn.service;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lbn.core.LoadBalancer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {
    private final LoadBalancer loadBalancer;
    @Value("${request.connect-timeout}")
    private int connectTimeout;
    @Value("${request.read-timeout}")
    private int readTimeout;

    public ResponseEntity<?> proxyRequest(String typeService, HttpServletRequest request) {
        try {
            return attemptRequest(typeService, request, false);
        } catch (Exception e) {
            log.warn("First attempt failed for service '{}', retrying on another node...", typeService);
            try {
                return attemptRequest(typeService, request, true);
            } catch (Exception retryException) {
                log.error("Retry attempt failed for service '{}': {}", typeService, retryException.getMessage(), retryException);
                return handleError(typeService, retryException);
            }
        }
    }

    private RestTemplate createRestTemplateWithTimeout() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(requestFactory);
    }

    private ResponseEntity<?> attemptRequest(String typeService, HttpServletRequest request, boolean isRetry) {
        String serviceAddress = getServiceAddress(typeService, isRetry);
        String targetUrl = buildTargetUrl(serviceAddress, typeService, request);
        try {
            ResponseEntity<String> response = sendRequestToTarget(targetUrl);
            return createClientResponse(response, targetUrl);
        } catch (RestClientException e) {
            log.warn("Request to target URL '{}' failed: {}", targetUrl, e.getMessage(), e);
            throw e;
        }
    }

    private String getServiceAddress(String typeService, boolean isRetry) {
        String serviceAddress = loadBalancer.getCurrentServiceByType(typeService);
        log.info("Resolved service address for type '{}' (isRetry={}): {}", typeService, isRetry, serviceAddress);
        return serviceAddress;
    }

    private String buildTargetUrl(String serviceAddress, String typeService, HttpServletRequest request) {
        String targetPath = request.getRequestURI().replaceFirst("/" + typeService, "");
        String targetUrl = serviceAddress + targetPath;
        log.info("Built target URL for service of type '{}': {}", typeService, targetUrl);
        return targetUrl;
    }

    private ResponseEntity<String> sendRequestToTarget(String targetUrl) {
        log.info("Sending request to target URL: {}", targetUrl);
        RestTemplate restTemplate = createRestTemplateWithTimeout();
        return restTemplate.getForEntity(targetUrl, String.class);
    }

    private ResponseEntity<?> createClientResponse(ResponseEntity<String> response, String targetUrl) {
        log.info("Received response with status: {} from target URL: {}", response.getStatusCode(), targetUrl);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private ResponseEntity<?> handleError(String typeService, Exception e) {
        log.error("Error while routing request to service of type '{}': {}", typeService, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("No available service for type: " + typeService);
    }

}
