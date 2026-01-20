package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import org.springframework.stereotype.Service;

@Service
public class DummyService {

    @PerformanceMonitor(value = "fastMethod", logParams = true, logResult = true)
    public String fastMethod(String input) {
        return input + "-success";
    }

    @PerformanceMonitor(value = "slowMethod", slowThreshold = 100)
    public String slowMethod(String input) throws InterruptedException {
        if ("error".equals(input)) {
            throw new IllegalArgumentException("Invalid input");
        }
        Thread.sleep(100);
        return input + "-slow";
    }

    @PerformanceMonitor(value = "methodWithLogging", logParams = true, logResult = true)
    public String methodWithLogging(String param1, int param2) {
        return param1 + "-" + param2;
    }
}
