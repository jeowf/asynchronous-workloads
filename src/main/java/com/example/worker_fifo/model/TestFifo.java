package com.example.worker_fifo.model;

public record TestFifo(int tenants, int quantity, long seed, long minDuration, long maxDuration, double error, int tests, String testName) {
}
