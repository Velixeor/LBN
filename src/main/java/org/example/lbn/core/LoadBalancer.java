package org.example.lbn.core;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lbn.core.statistic.ServiceStatistic;
import org.example.lbn.entity.ServiceConnection;
import org.example.lbn.repository.ServiceConnectionRepository;
import org.example.lbn.service.RedisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoadBalancer {
    private final RestTemplate restTemplate;
    private final ServiceStatistic serviceStatistic;
    private final RedisService redisService;

    public String getCurrentServiceByType(String typeService) {
        AtomicInteger currentService = redisService.getCounter(typeService);
        List<ServiceConnection> allServiceConnection = redisService.getBackends(typeService);
        log.debug("Поиск сервиса для типа: {}", typeService);
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
                log.debug("Выбран доступный сервис: {}", address);
                return address;
            }
            attempts++;
        }

        log.warn("Не удалось найти доступный сервис.");
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
                redisService.saveCounter(currentConnection.getTypeService(), currentService);
                log.debug("Сервис {} доступен, задержка: {} мс", address, latency);
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
        log.info("Сервис {} недоступен, помечаем как мертвый", address);
        serviceStatistic.recordRequest(address, 0, true);
        handleDeadService(currentConnection);
    }

    private boolean healthCheck(ServiceConnection currentConnection) {
        String url = currentConnection.getAddress() + "/health";
        try {
            ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);
            return response.getStatusCode().equals(HttpStatus.OK);
        } catch (Exception e) {
            return false;
        }
    }

    private void handleDeadService(ServiceConnection currentConnection) {
        List<ServiceConnection> deadServices = redisService.getDeadBackends();
        deadServices.add(currentConnection);
        redisService.saveDeadBackends(deadServices);

        List<ServiceConnection> backends = redisService.getBackends(currentConnection.getTypeService());
        backends.remove(currentConnection);
        redisService.saveBackends(currentConnection.getTypeService(), backends);
    }

    @Scheduled(fixedDelay = 10000000)
    private void reviveDeadServices() {
        log.info("Начинаем проверку и восстановление мертвых сервисов...");
        List<ServiceConnection> deadServices = redisService.getDeadBackends();
        List<ServiceConnection> updatedDeadServices = processDeadServices(deadServices);
        redisService.saveDeadBackends(updatedDeadServices);
        log.info("Процесс восстановления завершен. Осталось мертвых сервисов: {}", updatedDeadServices.size());
    }

    private List<ServiceConnection> processDeadServices(List<ServiceConnection> deadServices) {
        Iterator<ServiceConnection> iterator = deadServices.iterator();

        while (iterator.hasNext()) {
            ServiceConnection service = iterator.next();
            if (healthCheck(service)) {
                registerService(service);
                iterator.remove();
                log.info("Сервис {} успешно восстановлен и зарегистрирован.", service.getId());
            }
        }

        return deadServices;
    }

    public void registerService(ServiceConnection server) {
        List<ServiceConnection> services = redisService.getBackends(server.getTypeService());
        services.add(server);
        redisService.saveBackends(server.getTypeService(), services);
        log.info("Сервис {} зарегистрирован в системе.", server.getId());
    }
}
