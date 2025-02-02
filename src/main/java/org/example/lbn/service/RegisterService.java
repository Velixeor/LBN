package org.example.lbn.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lbn.core.LoadBalancer;
import org.example.lbn.dto.ServiceConnectionDTO;
import org.example.lbn.entity.ServiceConnection;
import org.example.lbn.repository.ServiceConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterService {
    private final LoadBalancer loadBalancer;
    private final ServiceConnectionRepository serviceConnectionRepository;

    @Transactional
    public Integer createAndRegisterService(ServiceConnectionDTO serviceConnectionDTO) {
        log.info("Создание и регистрация сервиса начата: {}", serviceConnectionDTO);
        ServiceConnection serviceConnection = ServiceConnectionDTO.toEntity(serviceConnectionDTO);
        serviceConnection.setCreatedAt(LocalDateTime.now());
        ServiceConnection savedServiceConnection = serviceConnectionRepository.save(serviceConnection);
        log.info("Сервис успешно сохранен в БД: {}", savedServiceConnection);
        loadBalancer.registerService(savedServiceConnection);
        log.info("Сервис зарегистрирован в балансировщике с ID: {}", savedServiceConnection.getId());
        return savedServiceConnection.getId();
    }
}
