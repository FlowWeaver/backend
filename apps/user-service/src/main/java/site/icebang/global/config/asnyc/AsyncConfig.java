package site.icebang.global.config.asnyc;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  @Bean("traceExecutor")
  public Executor traceExecutor() {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
    executor.setVirtualThreads(true);
    executor.setThreadNamePrefix("trace-");
    executor.setTaskDecorator(new ContextPropagatingTaskDecorator()); // 컨텍스트(MDC 등) 전파 설정
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
