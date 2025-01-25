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
public class SystemService {
    private final LoadBalancer loadBalancer;
    private final ServiceConnectionRepository serviceConnectionRepository;
    
    @Transactional
    public Integer createAndRegisterService(ServiceConnectionDTO serviceConnectionDTO){
        ServiceConnection serviceConnection=ServiceConnectionDTO.toEntity(serviceConnectionDTO);
        serviceConnection.setCreatedAt(LocalDateTime.now());
        Integer idNewEntity=serviceConnectionRepository.save(serviceConnection).getId();
        loadBalancer.registerService(serviceConnection);
        return idNewEntity;
    }
}
