package com.example.worker_fifo.service;

import org.springframework.stereotype.Service;

@Service
public class InfoService {

    public String getTenantName(int i){
        return "t" + i;
    }

    public String getFifoQueue(String tenant){
        return "z_paper_fifo_" + tenant;
    }

    public String getRsQueue(String tenant){
        return "z_paper_rs_" + tenant;
    }
}
