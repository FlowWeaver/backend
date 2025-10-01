# 블로그 콘텐츠 자동화 플랫폼

### AI 기반 워크플로우 오케스트레이터를 활용한 RAG 기반 블로그 콘텐츠 자동화 시스템

-----

### 목차

1.  서비스 개요
2.  시스템 개요
3.  시스템 아키텍처
4.  유스케이스 다이어그램
5.  시퀀스 다이어그램
6.  기술 스택
7.  주요 구성 요소 및 역할
8.  프로젝트 디렉토리 구조
9.  환경 변수 관리 전략

-----

## 1\. 서비스 개요

최근 커머스 업계의 AI 기반 콘텐츠 자동화 트렌드에도 불구하고, 여전히 트렌드 분석, 상품 조사, 콘텐츠 생성 및 발행 등 각 단계에서 시간 소모적인 수작업이 필요합니다. 본 프로젝트는 이러한 비효율을 해결하기 위해, **RAG(검색 증강 생성)** 기술을 기반으로 블로그 콘텐츠 생성의 전 과정을 자동화하는 워크플로우 플랫폼을 구축하는 것을 목표로 합니다.

-----

## 2\. 시스템 개요

본 시스템은 `Spring Boot` 기반의 오케스트레이터와 `Python(FastAPI)` 기반의 AI 워커로 구성된 **이중 레이어 아키텍처**를 채택하여, 각 서비스의 역할을 명확히 분리했습니다.

* **워크플로우 자동화**: 네이버 데이터 랩의 실시간 트렌드 키워드를 자동 수집하고, 싸다구몰(1688)에서 유사도 기반 검색을 통해 관련 상품을 매칭합니다.
* **AI 콘텐츠 생성**: 수집된 상품 정보와 이미지를 OCR 및 번역 기술로 분석한 후, RAG(검색 증강 생성) 기술을 통해 SEO에 최적화된 블로그 콘텐츠를 자동으로 생성합니다.
* **관리 및 모니터링**: 관리자 전용 대시보드에서 워크플로우의 실행, 스케줄 제어, 실행 이력 및 결과를 실시간으로 모니터링할 수 있습니다. 또한, Grafana를 통해 서버 리소스와 API 상태를 시각적으로 확인할 수 있습니다.

-----

## 3\. 시스템 아키텍처

역할과 책임을 명확히 분리하기 위해 `Spring Boot`가 Orchestrator, `FastAPI`가 Worker 역할을 수행하는 이중 레이어 아키텍처를 채택했습니다.

* **Spring Boot (Orchestrator)**: `Workflow → Job → Task`의 계층적 구조를 기반으로 전체 비즈니스 흐름을 제어합니다. 스케줄링(`Quartz`), 상태 관리, 데이터 영속성, 사용자 인증/인가 등 핵심 로직을 담당합니다.
* **FastAPI (Worker)**: 키워드 추출, 상품 검색, 웹 크롤링, AI 연동(RAG), OCR 등 Python 생태계에 특화된 무거운 실제 작업을 API 형태로 제공합니다.

![System Architecture](assets/시스템 아키텍처1.png)

![System Architecture](assets/시스템 아키텍처2.png)

-----

## 4\. 유스케이스 다이어그램

시스템의 주요 액터는 \*\*관리자(Admin)\*\*와 \*\*스케줄러(Scheduler)\*\*입니다. 관리자는 워크플로우와 스케줄을 관리하고, 수동으로 워크플로우를 실행할 수 있습니다. 스케줄러는 정해진 시간에 워크플로우를 자동으로 실행합니다.

![Usecase Diagram](assets/유스케이스 다이어그램.png)

-----

## 5\. 시퀀스 다이어그램

### 5.1. 워크플로우 실행 흐름 (스케줄/수동)

1.  **트리거**: `Quartz` 스케줄러 또는 사용자의 API 요청(`POST /v0/workflows/{id}/run`)으로 워크플로우 실행이 시작됩니다.
2.  **비동기 실행**: `WorkflowController`는 `WorkflowExecutionService`를 \*\*비동기(`@Async`)\*\*로 호출하고, 즉시 `202 Accepted`를 응답하여 사용자 경험을 향상시킵니다.
3.  **오케스트레이션**: `WorkflowExecutionService`가 `Job`과 `Task`를 순차적으로 실행하며, `TaskExecutionService`에 재시도 로직을 위임합니다.
4.  **외부 API 호출**: `FastApiTaskRunner`와 `FastApiAdapter`를 거쳐 실제 FastAPI 서버와 통신합니다.
5.  **결과 기록**: 모든 실행 결과는 DB에 기록되며, 실패 시에도 다음 작업은 계속 진행됩니다.

#### 수동 실행
![Sequence Diagram](assets/시퀀스 다이어그램(수동 실행).png)

#### 스케줄 실행
![Sequence Diagram](assets/시퀀스 다이어그램(스케줄 실행).png)


### 5.2. CI/CD 파이프라인

GitHub Actions를 사용하여 `main` 또는 `pre-processing` 브랜치에 Push 또는 PR이 발생했을 때 CI/CD 파이프라인이 자동으로 실행됩니다. 빌드, 테스트, Docker 이미지 빌드 및 푸시, EC2 배포까지의 과정이 자동화되어 있습니다.

![Sequence Diagram](assets/CICD 시퀀스 다이어그램1.png)

![Sequence Diagram](assets/CICD 시퀀스 다이어그램2.png)

![Sequence Diagram](assets/CICD 시퀀스 다이어그램3.png)

![Sequence Diagram](assets/CICD 시퀀스 다이어그램4.png)

-----

## 6\. 기술 스택

#### Backend (Orchestrator - `user-service`)

* **Language & Framework**: Java 21, Spring Boot 3.5.4
* **Data Access**: MyBatis 3.0.5, MariaDB Java Client 3.3.3
* **Scheduling**: Spring Quartz
* **Resilience**: Spring Retry
* **Security**: Spring Security
* **Build Tool**: Gradle

#### Backend (Worker - `pre-processing-service`)

* **Language & Framework**: Python 3.11, FastAPI 0.116.2
* **Web Server**: Uvicorn, Gunicorn
* **AI & Machine Learning**:
    * Transformers 4.56.2
    * Scikit-learn 1.7.2
    * OpenAI 1.109.1
* **Web Scraping & Parsing**: BeautifulSoup4 4.13.5, Selenium 4.35.0
* **Data Processing**: SQLAlchemy 2.0.43
* **Translation**: Deep-Translator 1.11.4
* **OCR**: Google Cloud Vision 3.10.2
* **Package Manager**: Poetry

#### Database

* **Primary Database**: MariaDB 11.4

#### Frontend

* **Framework**: React 18.0.0

#### DevOps & Monitoring

* **Containerization**: Docker, Docker Compose
* **CI/CD**: GitHub Actions
* **Database Migration**: Flyway
* **Monitoring**: Spring Boot Actuator, Micrometer, Prometheus, Grafana
* **Logging**: Log4j2

-----

## 7\. 주요 구성 요소 및 역할

* **`WorkflowExecutionService`**: 워크플로우의 전체 실행 흐름을 제어하는 메인 오케스트레이터.
* **`TaskExecutionService`**: 개별 Task 실행과 재시도 정책(`RetryTemplate`)을 전담하는 서비스.
* **`TaskBodyBuilder` (전략 패턴)**: 각 Task의 특성에 맞는 Request Body를 동적으로 생성하는 전략 구현체.
* **`FastApiAdapter`**: FastAPI 서버와의 모든 HTTP 통신을 캡슐화하는 어댑터.
* **`QuartzSchedulerInitializer`**: 애플리케이션 시작 시 DB의 스케줄 정보를 Quartz 엔진에 동기화.
* **`ExecutionMdcManager`**: MDC를 활용하여 비동기 환경에서도 실행 컨텍스트(`workflowRunId`, `traceId` 등) 기반의 분산 추적 로깅을 구현.

-----

## 8\. 프로젝트 디렉토리 구조

프로젝트는 Monorepo로 구성되어 있으며, `apps` 디렉토리 아래에 각 서비스가 독립적으로 위치합니다.

```bash
Final-4team-icebang/
├── apps/
│   ├── user-service/         # Java/Spring Boot 서비스
│   └── pre-processing-service/ # Python/FastAPI 서비스
├── docker/                   # Docker 설정 (docker-compose.yml)
├── .github/                  # CI/CD 설정 (GitHub Actions)
└── logs/                     # 로그 파일
```

### 8.1. `user-service` (Spring Boot)

도메인 중심 아키텍처를 채택하여, 각 비즈니스 기능(도메인)이 자신의 모든 구성요소를 포함하도록 설계했습니다.

* **`domain`**: `user`, `workflow` 등 핵심 비즈니스 로직과 데이터를 캡슐화합니다.
* **`external`**: 외부 시스템(FastAPI)과의 통신을 추상화하는 '방화벽' 역할을 합니다.
* **`schedule`**: '언제(When)' 작업을 실행할지에 대한 책임을 전담합니다.
* **`global`**: `SecurityConfig`, `WebConfig`, 예외 처리 등 애플리케이션 전반에 적용되는 설정을 담당합니다.
* **`common`**: `ApiResponse`와 같이 여러 도메인에서 재사용 가능한 공용 유틸리티를 관리합니다.

<!-- end list -->

```bash
user-service/
├── src/
│   ├── main/
│   │   ├── java/site/icebang/
│   │   │   ├── UserServiceApplication.java
│   │   │   ├── common/           # 공통 기능
│   │   │   ├── domain/           # 도메인별 기능
│   │   │   │   ├── auth/           # 인증/인가
│   │   │   │   ├── user/           # 사용자 관리
│   │   │   │   ├── workflow/       # 워크플로우 관리
│   │   │   │   ├── organization/   # 조직 관리
│   │   │   │   ├── email/          # 이메일 서비스
│   │   │   │   ├── schedule/       # 스케줄 관리
│   │   │   │   └── log/            # 실행 로그
│   │   │   ├── external/         # 외부 API 연동
│   │   │   └── global/           # 글로벌 설정
│   │   │       ├── aop/            # AOP (로깅)
│   │   │       ├── config/         # 설정
│   │   │       ├── filter/         # 필터
│   │   │       └── handler/        # 예외 핸들러
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
├── build.gradle              # Gradle 빌드 설정
└── Dockerfile
```

### 8.2. `pre-processing-service` (FastAPI)

* **`api`**: FastAPI의 엔드포인트와 라우터를 정의합니다.
* **`service`**: 실제 비즈니스 로직(크롤링, AI 연동 등)을 구현합니다.
* **`core`**: 데이터베이스 연결, 미들웨어 등 핵심 설정을 담당합니다.

<!-- end list -->

```bash
pre-processing-service/
├── app/
│   ├── main.py               # FastAPI 진입점
│   ├── api/                  # API 레이어
│   │   ├── router.py           # 라우터 통합
│   │   └── endpoints/        # API 엔드포인트
│   ├── service/              # 비즈니스 로직
│   │   ├── blog/               # 블로그 서비스
│   │   ├── crawlers/           # 크롤러
│   │   ├── ocr/                # OCR 서비스
│   │   ├── crawl_service.py      # 크롤링 서비스
│   │   ├── keyword_service.py    # 키워드 추출
│   │   ├── search_service.py     # 검색 서비스
│   │   ├── match_service.py      # 매칭 서비스
│   │   ├── similarity_service.py # 유사도 분석
│   │   ├── product_selection_service.py # 상품 선택
│   │   └── s3_upload_service.py  # S3 업로드
│   ├── core/                 # 핵심 설정
│   ├── db/                   # 데이터베이스
│   ├── model/                # 데이터 모델
│   ├── middleware/           # 미들웨어
│   ├── errors/               # 예외 처리
│   ├── utils/                # 유틸리티
│   └── test/                 # 테스트 코드
├── pyproject.toml            # Poetry 설정
├── Dockerfile
└── .env                    # 환경 변수
```

-----

## 9\. 환경 변수 관리 전략

추후 작성 예정

* **FastAPI (`pre-processing-service`)**:
* **Spring Boot (`user-service`)**: 