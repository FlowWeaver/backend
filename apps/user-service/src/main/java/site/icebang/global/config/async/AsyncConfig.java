package site.icebang.global.config.async;

import java.lang.reflect.Method;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  @Bean("traceExecutor")
  public ThreadPoolTaskExecutor traceExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(100);
    executor.setTaskDecorator(new ContextPropagatingTaskDecorator()); // 필수
    executor.setThreadNamePrefix("trace-");
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncUncaughtExceptionHandler() {
      @Override
      public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error(
            "Async method execution failed - Method: {}.{}, Params: {}",
            method.getDeclaringClass().getSimpleName(),
            method.getName(),
            params,
            ex);
      }
    };
  }
}

// package site.icebang.global.config.async;
//
// import java.lang.reflect.Method;
// import java.util.concurrent.Executor;
//
// import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.task.SimpleAsyncTaskExecutor;
// import org.springframework.core.task.TaskDecorator;
// import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
// import org.springframework.scheduling.annotation.AsyncConfigurer;
// import org.springframework.scheduling.annotation.EnableAsync;
//
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
//
// @Slf4j
// @Configuration
// @EnableAsync
// @RequiredArgsConstructor
// public class AsyncConfig implements AsyncConfigurer {
//
//  private final SemaphoreTaskDecorator semaphoreTaskDecorator;
//
//  @Bean("traceExecutor")
//  public Executor traceExecutor() {
//    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
//    executor.setVirtualThreads(true);
//    executor.setThreadNamePrefix("trace-");
//
//    // MDC 전파 데코레이터 생성
//    TaskDecorator contextDecorator = new ContextPropagatingTaskDecorator();
//
//    // 두 데코레이터의 조합:
//    // Context 설정(MDC 복사) 후 Semaphore 제어가 적용되도록 구성
//    executor.setTaskDecorator(
//        runnable -> contextDecorator.decorate(semaphoreTaskDecorator.decorate(runnable)));
//
//    return executor;
//  }
//
//  @Override
//  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
//    return new AsyncUncaughtExceptionHandler() {
//      @Override
//      public void handleUncaughtException(Throwable ex, Method method, Object... params) {
//        log.error(
//            "Async method execution failed - Method: {}.{}, Params: {}",
//            method.getDeclaringClass().getSimpleName(),
//            method.getName(),
//            params,
//            ex);
//      }
//    };
//  }
// }
