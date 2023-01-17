package com.example.worker_fifo.controller;

import com.example.worker_fifo.model.TestFifo;
import com.example.worker_fifo.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @PostMapping("clean/{tenants}")
    public void cleanQueues(@PathVariable(name = "tenants") int tenants){
        queueService.cleanQueues(tenants);
    }

    @PostMapping("clean/")
    public void cleanQueues(){
        queueService.cleanQueues(100);
    }

    @PostMapping("setup/{tenants}")
    public void setupQueues(@PathVariable(name = "tenants") int tenants){
        queueService.setupQueues(tenants);
    }

    @PostMapping("test")
    public void enqueueTestes(@RequestBody List<TestFifo> testsFifo){
        queueService.enqueueTestes(testsFifo);
    }

}
