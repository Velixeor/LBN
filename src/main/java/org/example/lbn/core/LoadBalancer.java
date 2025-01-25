package org.example.lbn.core;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lbn.core.statistic.ServiceStatistic;
import org.example.lbn.entity.ServiceConnection;
import org.example.lbn.repository.ServiceConnectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoadBalancer {
    private final ServiceConnectionRepository serviceConnectionRepository;
    private final RestTemplate restTemplate;
    private final ServiceStatistic serviceStatistic;

    private static final Map<String, List<ServiceConnection>> backends = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private static final List<ServiceConnection> deadBackends = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        List<ServiceConnection> allServices = serviceConnectionRepository.findAll();
        allServices.forEach(service -> {
            registerService(service);
            log.info("Successfully initialized handler for type: {}", service.getTypeService());
        });
        log.info("All service handlers initialized successfully.");
    }

    public String getCurrentServiceByType(String typeService) {
        AtomicInteger currentService = counters.get(typeService);
        List<ServiceConnection> allServiceConnection = backends.get(typeService);
        return getHealthAddress(currentService, allServiceConnection);
    }

    private String getHealthAddress(AtomicInteger currentService, List<ServiceConnection> allServiceConnection) {
        int attempts = 0;
        int maxAttempts = allServiceConnection.size();

        while (attempts < maxAttempts) {
            int currentIndex = getNextServiceIndex(currentService, allServiceConnection.size());
            ServiceConnection currentConnection = allServiceConnection.get(currentIndex);
            String address = currentConnection.getAddress();

            if (tryToUseService(currentConnection, address, currentService)) {
                return address;
            }

            attempts++;
        }

        throw new IllegalStateException("No available services");
    }

    private int getNextServiceIndex(AtomicInteger currentService, int totalServices) {
        return currentService.getAndUpdate(index -> (index + 1) % totalServices);
    }

    private boolean tryToUseService(ServiceConnection currentConnection, String address, AtomicInteger currentService) {
        long start = System.currentTimeMillis();
        try {
            if (healthCheck(currentConnection)) {
                long latency = System.currentTimeMillis() - start;
                serviceStatistic.recordRequest(address, latency, false);
                counters.put(currentConnection.getTypeService(), currentService);
                return true;
            } else {
                handleUnavailableService(currentConnection, address);
                return false;
            }
        } catch (Exception e) {
            handleUnavailableService(currentConnection, address);
            return false;
        }
    }

    private void handleUnavailableService(ServiceConnection currentConnection, String address) {
        serviceStatistic.recordRequest(address, 0, true);
        handleDeadService(currentConnection);
    }

    private boolean healthCheck(ServiceConnection currentConnection) {
        String url = currentConnection.getAddress() + "/health";
        try {
            ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);
            updateLastChecked(currentConnection);
            return response.getStatusCode().equals(HttpStatus.OK);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateLastChecked(ServiceConnection currentConnection) {
        currentConnection.setLastChecked(LocalDateTime.now());
        serviceConnectionRepository.save(currentConnection);
    }

    private void handleDeadService(ServiceConnection currentConnection) {
        deadBackends.add(currentConnection);
        backends.computeIfPresent(currentConnection.getTypeService(), (key, connections) -> {
            connections.remove(currentConnection);
            return connections;
        });
    }

    @Scheduled(fixedDelay = 10000000)
    private void reviveDeadServices() {
        List<ServiceConnection> revivedServices = deadBackends.stream().filter(this::healthCheck).toList();
        revivedServices.forEach(this::registerService);
        deadBackends.removeAll(revivedServices);
    }

    public void registerService(ServiceConnection server) {
        backends.computeIfAbsent(server.getTypeService(), k -> new ArrayList<>()).add(server);
    }
}
