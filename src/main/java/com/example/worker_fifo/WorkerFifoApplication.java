package com.example.worker_fifo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class WorkerFifoApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkerFifoApplication.class, args);
	}

}
