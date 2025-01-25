package org.example.lbn.core.statistic;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@RequiredArgsConstructor
public class ServiceStatistic {
    private final Map<String, BackendStats> statsMap = new ConcurrentHashMap<>();

    public void recordRequest(String backendAddress, long latency, boolean isError) {
        statsMap.computeIfAbsent(backendAddress, key -> new BackendStats())
                .update(latency, isError);
    }

    public Map<String, BackendStats> getAllStats() {
        return statsMap;
    }
}
