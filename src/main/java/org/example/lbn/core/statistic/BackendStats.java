package org.example.lbn.core.statistic;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class BackendStats {
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger errorRequests = new AtomicInteger(0);
    private final AtomicInteger totalLatency = new AtomicInteger(0);

    public void update(long latency, boolean isError) {
        totalRequests.incrementAndGet();
        totalLatency.addAndGet((int) latency);
        if (isError) {
            errorRequests.incrementAndGet();
        }
    }

    public int getTotalRequests() {
        return totalRequests.get();
    }

    public int getErrorRequests() {
        return errorRequests.get();
    }

    public double getAverageLatency() {
        int totalReqs = totalRequests.get();
        return totalReqs == 0 ? 0 : (double) totalLatency.get() / totalReqs;
    }

    @Override
    public String toString() {
        return String.format("Requests: %d, Errors: %d, Avg Latency: %.2fms",
                getTotalRequests(), getErrorRequests(), getAverageLatency());
    }
}
