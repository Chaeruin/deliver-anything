package com.deliveranything.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Virtual Threads (Project Loom) 설정 1. Tomcat 요청 처리 스레드 2. @Async 비동기 작업 3. Redis Message Listener
 */
@Slf4j
@Configuration
@EnableAsync
public class VirtualThreadConfig implements AsyncConfigurer {

  /**
   * Tomcat의 모든 요청을 Virtual Thread로 처리 - 기존 Platform Thread Pool 대신 Virtual Thread 사용
   */
  @Bean
  public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
    return protocolHandler -> {
      log.info("Configuring Tomcat to use Virtual Threads");
      protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    };
  }

  /**
   * @Async 비동기 작업을 Virtual Thread로 처리 - Redis Subscriber, Notification 발송 등
   */
  @Override
  @Bean(name = {TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
      "asyncTaskExecutor"})
  public AsyncTaskExecutor getAsyncExecutor() {
    log.info("Configuring @Async to use Virtual Threads");
    return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
  }

  /**
   * Delivery 패키지 전용 Virtual Thread Executor - 명시적으로 Virtual Thread 사용이 필요한 경우
   */
  @Bean(name = "deliveryVirtualThreadExecutor")
  public Executor deliveryVirtualThreadExecutor() {
    log.info("Creating delivery-specific Virtual Thread Executor");
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}

