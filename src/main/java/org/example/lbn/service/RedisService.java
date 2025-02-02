package org.example.lbn.service;


import lombok.RequiredArgsConstructor;
import org.example.lbn.entity.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BACKENDS_KEY = "loadbalancer:backends";
    private static final String COUNTERS_KEY = "loadbalancer:counters";
    private static final String DEAD_BACKENDS_KEY = "loadbalancer:dead";

    public void saveBackends(String typeService, List<ServiceConnection> services) {
        redisTemplate.opsForHash().put(BACKENDS_KEY, typeService, services);
    }

    public List<ServiceConnection> getBackends(String typeService) {
        return (List<ServiceConnection>) redisTemplate.opsForHash().get(BACKENDS_KEY, typeService);
    }

    public void saveCounter(String typeService, AtomicInteger counter) {
        redisTemplate.opsForHash().put(COUNTERS_KEY, typeService, counter.get());
    }

    public AtomicInteger getCounter(String typeService) {
        Integer value = (Integer) redisTemplate.opsForHash().get(COUNTERS_KEY, typeService);
        return new AtomicInteger(value != null ? value : 0);
    }

    public void saveDeadBackends(List<ServiceConnection> deadServices) {
        redisTemplate.opsForValue().set(DEAD_BACKENDS_KEY, deadServices);
    }

    public List<ServiceConnection> getDeadBackends() {
        return (List<ServiceConnection>) redisTemplate.opsForValue().get(DEAD_BACKENDS_KEY);
    }
}
