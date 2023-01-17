package com.example.worker_fifo.service;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
public class IntegrationListener {
    private final Logger log = LoggerFactory.getLogger(IntegrationListener.class);
    @Autowired
    private ConnectionFactory factory;

    @Autowired
    private RabbitAdmin rbAdmin;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private InfoService infoService;

    private String instanceID;

    @Value("${props.strategy}")
    String strategy;

    public IntegrationListener(){
        instanceID = UUID.randomUUID().toString();
    }

    @Async
    public CompletableFuture<Object> runFifo(String tenant) throws IOException, TimeoutException {
        if (strategy.equalsIgnoreCase("dl")) {
            lockAndConsume(tenant);
        } else {
            consumeSAC(tenant);
        }

        return CompletableFuture.completedFuture(null);

    }

    private void lockAndConsume(String tenant) throws IOException, TimeoutException {
        String resource = infoService.getRsQueue(tenant);

        if (rbAdmin.getQueueInfo(resource) == null){
            return;
        }

        Connection connection = factory.createConnection();
        Channel channel = connection.createChannel(false);

        channel.queueDeclarePassive(resource);
        GetResponse response = channel.basicGet(resource, false);
        if (response != null) {
            try {
                consumeDL(tenant);
            } finally {
                channel.basicReject(response.getEnvelope().getDeliveryTag(), true);
            }
        }

        channel.close();
        connection.close();
    }

    private void consumeDL(String tenant) throws TimeoutException, IOException {
        String queue = infoService.getFifoQueue(tenant);

        if (rbAdmin.getQueueInfo(queue) == null){
            return;
        }


        Connection connection = factory.createConnection();
        Channel channel = connection.createChannel(false);

        channel.queueDeclarePassive(queue);
        GetResponse response = channel.basicGet(queue, false);
        if (response != null) {
            String message = new String(response.getBody());

            long redelivery = 0;
            if (response.getProps().getHeaders().containsKey("x-delivery-count")) {
                redelivery = (long) response.getProps().getHeaders().get("x-delivery-count");
            }

            try {
                integrationService.processIntegration(message, tenant, instanceID, redelivery);
            } catch (Exception e){
                channel.basicReject(response.getEnvelope().getDeliveryTag(), true);
            } finally {
                channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
            }
        }

        channel.close();
        connection.close();
    }

    private void consumeSAC(String tenant) throws TimeoutException, IOException {

        String queue = infoService.getFifoQueue(tenant);

        if (rbAdmin.getQueueInfo(queue) == null) {
            return;
        }


        Connection connection = factory.createConnection();
        Channel channel = connection.createChannel(false);
        try {

            channel.basicQos(1);
            channel.queueDeclarePassive(queue);

            if (channel.messageCount(queue) == 0 || rbAdmin.getQueueInfo(queue).getConsumerCount() > 0)
                return;

            CompletableFuture<Object> x = new CompletableFuture<>();
            channel.basicConsume(queue, false, instanceID, new IntegrationConsumer(channel, integrationService, tenant, instanceID, x));

            x.join();

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            channel.close();
            connection.close();
        }

    }
}
