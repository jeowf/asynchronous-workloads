package com.example.worker_fifo.controller;

import com.example.worker_fifo.service.InfoService;
import com.example.worker_fifo.service.IntegrationListener;
import com.example.worker_fifo.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Controller
public class ScheduleController {

    @Autowired
    IntegrationListener integrationListener;

    @Autowired
    InfoService infoService;

    @Autowired
    QueueService queueService;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void executeFifo() throws Exception{
        int tenants = queueService.getTenants();
        for (int i = 0; i < tenants; i++){
            String tenant = infoService.getTenantName(i);
            CompletableFuture<Object> exec = integrationListener.runFifo(tenant);
            CompletableFuture.allOf(exec).join();
        }
    }



}
