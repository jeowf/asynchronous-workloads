package com.example.worker_fifo.service;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class IntegrationConsumer implements Consumer {
    private final Channel channel;
    private volatile String consumerTag;
    private IntegrationService integrationService;
    private String tenant;

    private String instanceID;

    private CompletableFuture<Object> future;

    public IntegrationConsumer(Channel channel, IntegrationService integrationService, String tenant, String instanceID, CompletableFuture<Object> future) {

        this.channel = channel;
        this.integrationService = integrationService;
        this.tenant = tenant;
        this.instanceID = instanceID;
        this.future = future;

    }

    public void handleConsumeOk(String consumerTag) {
        this.consumerTag = consumerTag;
    }

    public void handleCancelOk(String consumerTag) {
    }

    public void handleCancel(String consumerTag) throws IOException {
        channel.basicCancel(consumerTag);
    }

    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {

    }

    public void handleRecoverOk(String consumerTag) {
    }

    public Channel getChannel() {
        return this.channel;
    }

    public String getConsumerTag() {
        return this.consumerTag;
    }

    @Override
    public void handleDelivery(String consumerTag,
                               Envelope envelope,
                               AMQP.BasicProperties properties,
                               byte[] body)
            throws IOException
    {
        String routingKey = envelope.getRoutingKey();
        String contentType = properties.getContentType();
        long deliveryTag = envelope.getDeliveryTag();

        String message = new String(body);

        long redelivery = 0;
        if (properties.getHeaders().containsKey("x-delivery-count")) {
            redelivery = (long) properties.getHeaders().get("x-delivery-count");
        }

        try {
            integrationService.processIntegration(message, tenant, consumerTag, redelivery);
            channel.basicCancel(consumerTag);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e){
            channel.basicReject(deliveryTag, true);
        } finally {
            future.complete(null);
        }

    }
}