package org.example.lbn.controler;


import lombok.RequiredArgsConstructor;
import org.example.lbn.dto.ServiceConnectionDTO;
import org.example.lbn.service.RegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system")
public class RegisterController {
    private final RegisterService registerService;

    @PostMapping("/register-service")
    public ResponseEntity<Integer> createAndRegisterService(@RequestBody ServiceConnectionDTO serviceConnectionDTO) {
        return new ResponseEntity<>(registerService.createAndRegisterService(serviceConnectionDTO), HttpStatus.CREATED);
    }
}
