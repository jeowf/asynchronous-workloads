package com.example.worker_fifo.service;

import com.example.worker_fifo.model.TestFifo;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class QueueService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    private ConnectionFactory factory;

    @Autowired
    private RabbitAdmin rbAdmin;

    @Autowired
    private InfoService infoService;

    @Value("${props.tenants}")
    private int tenants;

    @Value("${props.strategy}")
    String strategy;

    public void cleanQueues(int tenants){
        for (int i = 0; i < tenants; i++){
            String tenant = infoService.getTenantName(i);
            String rs = infoService.getRsQueue(tenant);
            String fifo = infoService.getFifoQueue(tenant);

            rbAdmin.deleteQueue(rs);
            rbAdmin.deleteQueue(fifo);
        }

    }

    public void setupQueues(int tenants){
        if (strategy.equalsIgnoreCase("dl")){
            setupQueuesDL(tenants);
        } else {
            setupQueuesSAC(tenants);
        }

    }

    private void setupQueuesDL(int tenants){

        System.out.println("DL Setup");

        for (int i = 0; i < tenants; i++){
            String tenant = infoService.getTenantName(i);
            String rs = infoService.getRsQueue(tenant);
            String fifo = infoService.getFifoQueue(tenant);

            Map<String, Object> arguments = new HashMap<>();
            arguments.put("x-queue-type", "quorum");
            arguments.put("x-delivery-limit", 10);

            Map<String, Object> arguments2 = new HashMap<>();
            arguments.put("x-queue-type", "quorum");

            rbAdmin.declareQueue(new Queue(fifo, true, false, false, arguments));
            rbAdmin.declareQueue(new Queue(rs, true, false, false, arguments2));
            rabbitTemplate.convertAndSend(rs,fifo);
        }
    }

    public void setupQueuesSAC(int tenants){

        System.out.println("SAC Setup");

        for (int i = 0; i < tenants; i++){
            String tenant = infoService.getTenantName(i);
            String fifo = infoService.getFifoQueue(tenant);

            Map<String, Object> arguments = new HashMap<>();
            arguments.put("x-queue-type", "quorum");
            arguments.put("x-single-active-consumer", true);
            arguments.put("x-delivery-limit", 10);

            rbAdmin.declareQueue(new Queue(fifo, true, false, false, arguments));
        }
    }

    @Async
    public void enqueueTestes(List<TestFifo> testsFifo){

        for (TestFifo testFifo : testsFifo){
            enqueueTestes(testFifo);

        }

    }

    private void enqueueTestes(TestFifo testFifo){
        Random random = new Random(testFifo.seed());

        for (int t = 0; t < testFifo.tests(); t++){

            long waitTime = 0;

            for (int i = 0; i < testFifo.quantity(); i++){

                String tenant = infoService.getTenantName(random.nextInt(0, testFifo.tenants()));
                String fifo = infoService.getFifoQueue(tenant);
                long randomDuration = random.nextLong(testFifo.minDuration(), testFifo.maxDuration());
                int id = i + t*testFifo.quantity();
                rabbitTemplate.convertAndSend(fifo, "" + id + "," + randomDuration + "," + testFifo.seed() + "," + testFifo.error() + "," + testFifo.testName());

                waitTime += randomDuration;
            }

            System.out.println(testFifo);

            System.out.println(">>> Wait END " + OffsetDateTime.now());
            waitForFinish();
            System.out.println(">>> .... END" + OffsetDateTime.now());

        }

    }

    public int getTenants(){
        return tenants;
    }

    private void waitForMillis(long millis){

        try {
            Thread.sleep((long)(millis * 1.5));
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private void waitForFinish(){

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        while (!isTestsFinished()){
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isTestsFinished(){
        for (int i = 0; i < tenants; i++) {
            String tenant = infoService.getTenantName(i);
            String fifo = infoService.getFifoQueue(tenant);

            try {
                int messageCount = rbAdmin.getQueueInfo(fifo).getMessageCount();
                if (messageCount > 0){
                    return false;
                }
            } catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

}
