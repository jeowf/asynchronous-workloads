package com.example.worker_fifo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class IntegrationService {

    private ConcurrentMap<String, Boolean> lock;

    @Autowired
    private InfoService infoService;

    @Autowired
    private QueueService queueService;

    @PostConstruct
    public void initMap(){
        lock = new ConcurrentHashMap<>();

        int tenants = queueService.getTenants();

        for (int i = 0; i < tenants; i++){
            lock.put(infoService.getTenantName(i), false);
        }
    }

    public boolean startIntegration(String tenant){
        if (lock.get(tenant))
            return false;

        lock.replace(tenant, true);
        return true;
    }

    public void endIntegration(String tenant){
        lock.replace(tenant, false);
    }

    public Boolean isRunning(String tenant){
        return lock.get(tenant);
    }

    public void processIntegration(String message, String tenant, String instanceID, long redelivery) throws IOException {

        File file = new File(System.getProperty("java.io.tmpdir") + "worker-" + instanceID + ".txt");
        file.createNewFile();
        FileOutputStream oFile = new FileOutputStream(file, true);

        String[] integration = message.split(",");
        long id = Long.parseLong(integration[0]);
        long sleepTime = Long.parseLong(integration[1]);
        long seed = Long.parseLong(integration[2]);
        double error = Double.parseDouble(integration[3]);
        String testName = integration[4];

        log(oFile, "LOG:START," + instanceID + "," + tenant + "," + id + "," + sleepTime + "," + testName + "," + LocalDateTime.now());

        //System.out.println("ERROR " + error);

        sleep(sleepTime);
        if (error > 0.0f){
            Random random = new Random(seed);
            random = new Random(random.nextLong() * (redelivery + id));
            double chance = random.nextDouble();

            Random rng2 = new Random(seed);
            if (chance <= error){
                log(oFile, "LOG:ERROR," + instanceID + "," + tenant + "," + id + "," + sleepTime  + "," + testName + "," + LocalDateTime.now());
                throw new RuntimeException("Simulated Error");
            }

        }

        log(oFile, "LOG:END," + instanceID + "," + tenant + "," + id + "," + sleepTime + "," + testName + "," + LocalDateTime.now());
        oFile.close();

    }

    private void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void log(FileOutputStream oFile, String message) throws IOException {

        System.out.println(message);
        oFile.write((message + "\n").getBytes(StandardCharsets.UTF_8));
    }

}
