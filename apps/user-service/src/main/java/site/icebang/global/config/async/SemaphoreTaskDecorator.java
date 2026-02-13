package site.icebang.global.config.async;

import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SemaphoreTaskDecorator implements TaskDecorator {

  @Value("${spring.datasource.hikari.maximum-pool-size:10}")
  private int maximumPoolSize;

  private Semaphore semaphore;

  @PostConstruct
  public void init() {
    this.semaphore = new Semaphore(maximumPoolSize);
    log.info("SemaphoreTaskDecorator 초기화: 동시 실행 제한 수(maximumPoolSize) = {}", maximumPoolSize);
  }

  @Override
  public Runnable decorate(Runnable runnable) {
    return () -> {
      try {
        semaphore.acquire();
        runnable.run();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("비동기 작업 실행 대기 중 인터럽트 발생", e);
      } finally {
        semaphore.release();
      }
    };
  }
}
