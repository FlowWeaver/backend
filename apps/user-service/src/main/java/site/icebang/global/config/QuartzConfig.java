package site.icebang.global.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Quartz Scheduler의 핵심 설정을 담당하는 Configuration 클래스입니다.
 *
 * <p>이 클래스는 Quartz Scheduler가 클러스터 환경에서 안전하게 동작하고, Spring Bean을 Job 내에서 주입받을 수 있도록
 * 스케줄러 인스턴스를 구성합니다.
 *
 * <h2>주요 기능:</h2>
 *
 * <ul>
 *   <li>Quartz 전용 DataSource 분리 설정
 *   <li>클러스터링 활성화를 위한 Quartz Properties 구성
 *   <li>Spring Bean 주입이 가능한 JobFactory 등록
 * </ul>
 *
 * <h2>클러스터링 동작 원리:</h2>
 *
 * <p>여러 애플리케이션 인스턴스(Pod)가 동일한 DB를 공유하며, Quartz 테이블(QRTZ_*)을 통해 Job 실행 상태를 동기화합니다.
 * 각 인스턴스는 주기적으로(기본 20초) 체크인하며, 특정 시점에 하나의 인스턴스만 Job을 실행하도록 보장합니다.
 *
 * @author bwnfo0702@gmail.com
 * @since v0.1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuartzConfig {

    private final ApplicationContext applicationContext;
    private final QuartzProperties quartzProperties;

    /**
     * Spring Bean을 Quartz Job에서 사용할 수 있도록 하는 JobFactory를 생성합니다.
     *
     * <p>기본 Quartz JobFactory는 Spring의 ApplicationContext를 인식하지 못하므로, Spring Bean 주입이
     * 불가능합니다. 이 Bean을 통해 Job 클래스 내에서 {@code @Autowired}를 사용할 수 있게 됩니다.
     *
     * @return Spring Bean 주입이 가능한 JobFactory
     */
    @Bean
    public JobFactory jobFactory() {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        log.info("Spring Bean 주입 가능한 JobFactory 생성 완료");
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            DataSource dataSource,
            JobFactory jobFactory) {

        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        // 1. 메인 DataSource 사용 (Quartz 전용 DataSource 제거)
        factory.setDataSource(dataSource);

        // 2. Spring Bean 주입 가능한 JobFactory 설정
        factory.setJobFactory(jobFactory);

        // 3. Quartz Properties 설정 (클러스터링 포함)
        Properties properties = new Properties();
        properties.putAll(quartzProperties.getProperties());
        properties.setProperty("org.quartz.threadPool.threadCount", "10");

        factory.setQuartzProperties(properties);
        factory.setApplicationContextSchedulerContextKey("applicationContext");
        factory.setAutoStartup(true);
        factory.setOverwriteExistingJobs(false);

        log.info("Quartz SchedulerFactoryBean 설정 완료 (Clustering: {})",
                properties.getProperty("org.quartz.jobStore.isClustered"));

        return factory;
    }
}