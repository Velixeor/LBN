package org.example.lbn.controler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.lbn.service.RequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/backend-request")
public class BackendRequestController {
    private final RequestService requestService;

    @GetMapping("/{typeService}/**")
    public ResponseEntity<?> proxyRequest(@PathVariable String typeService, HttpServletRequest request) {
        return requestService.proxyRequest(typeService, request);
    }
}
