# 블로그 콘텐츠 자동화 플랫폼

### AI 기반 워크플로우 오케스트레이터를 활용한 RAG 기반 블로그 콘텐츠 자동화 시스템

---

### 목차

1. [서비스 개요](#1-서비스-개요)
2. [시스템 개요](#2-시스템-개요)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [유스케이스 다이어그램](#4-유스케이스-다이어그램)
5. [시퀀스 다이어그램](#5-시퀀스-다이어그램)
6. [기술 스택](#6-기술-스택)
7. [주요 구성 요소 및 역할](#7-주요-구성-요소-및-역할)
8. [프로젝트 디렉토리 구조](#8-프로젝트-디렉토리-구조)
9. [환경 변수 관리 전략](#9-환경-변수-관리-전략)
10. [시연 영상](#10-시연-영상)

---

## 1. 서비스 개요

최근 커머스 업계의 AI 기반 콘텐츠 자동화 트렌드에도 불구하고, 여전히 트렌드 분석, 상품 조사, 콘텐츠 생성 및 발행 등 각 단계에서 시간 소모적인 수작업이 필요합니다.
본 프로젝트는 이러한 비효율을 해결하기 위해, **RAG(검색 증강 생성)** 기술을 기반으로 블로그 콘텐츠 생성의 전 과정을 자동화하는 워크플로우 플랫폼을 구축하는 것을 목표로 합니다.

---

## 2. 시스템 개요

본 시스템은 `Spring Boot` 기반의 오케스트레이터와 `Python(FastAPI)` 기반의 AI 워커로 구성된 **이중 레이어 아키텍처**를 채택하여, 각 서비스의 역할을 명확히 분리했습니다.

* **워크플로우 자동화**: 네이버 데이터 랩의 실시간 트렌드 키워드를 자동 수집하고, 싸다구몰(1688)에서 유사도 기반 검색을 통해 관련 상품을 매칭합니다.
* **AI 콘텐츠 생성**: 수집된 상품 정보와 이미지를 OCR 및 번역 기술로 분석한 후, RAG 기술을 통해 SEO에 최적화된 블로그 콘텐츠를 자동 생성합니다.
* **관리 및 모니터링**: 관리자 전용 대시보드에서 워크플로우 실행, 스케줄 제어, 실행 이력 및 결과를 실시간 모니터링합니다. Grafana로 서버 리소스와 API 상태를 시각적으로 확인할 수 있습니다.

---

## 3. 시스템 아키텍처

역할과 책임을 명확히 분리하기 위해 `Spring Boot`가 **Orchestrator**, `FastAPI`가 **Worker** 역할을 수행하는 이중 레이어 아키텍처를 채택했습니다.

* **Spring Boot (Orchestrator)**: `Workflow → Job → Task` 구조를 기반으로 전체 비즈니스 흐름을 제어합니다. 스케줄링(`Quartz`), 상태 관리, 데이터 영속성, 인증/인가 등 핵심 로직 담당.
* **FastAPI (Worker)**: 키워드 추출, 상품 검색, 웹 크롤링, AI 연동(RAG), OCR 등 Python 생태계 특화 작업을 담당.

![System Architecture](assets/시스템_아키텍처_1.png)
![System Architecture](assets/시스템_아키텍처_2.png)

---

## 4. 유스케이스 다이어그램

시스템의 주요 액터는 **관리자(Admin)** 와 **스케줄러(Scheduler)** 입니다.
관리자는 워크플로우와 스케줄을 관리하고 수동 실행이 가능하며, 스케줄러는 자동 실행을 담당합니다.

![Usecase Diagram](assets/유스케이스_다이어그램.png)

---

## 5. 시퀀스 다이어그램

### 5.1. 워크플로우 실행 흐름 (스케줄/수동)

1. **트리거**: Quartz 스케줄러 또는 사용자의 `POST /v0/workflows/{id}/run` 요청으로 워크플로우 실행 시작
2. **비동기 실행**: `WorkflowController`가 `WorkflowExecutionService`를 `@Async`로 호출하고 즉시 `202 Accepted` 응답
3. **오케스트레이션**: `WorkflowExecutionService`가 Job, Task 순차 실행 및 `TaskExecutionService`에 재시도 위임
4. **외부 API 호출**: `FastApiTaskRunner`와 `FastApiAdapter`를 통해 FastAPI와 통신
5. **결과 기록**: 모든 실행 결과는 DB에 기록되며, 실패 시에도 다음 작업은 계속 진행

#### 수동 실행

![Sequence Diagram](assets/시퀀스 다이어그램(수동 실행).png)

#### 스케줄 실행

![Sequence Diagram](assets/시퀀스 다이어그램(스케줄 실행).png)

### 5.2. CI/CD 파이프라인

GitHub Actions 기반으로 빌드 → 테스트 → Docker 빌드 및 푸시 → EC2 배포까지 자동화되어 있습니다.

![Sequence Diagram](assets/CICD_시퀀스_다이어그램_1.png)
![Sequence Diagram](assets/CICD_시퀀스_다이어그램_2.png)
![Sequence Diagram](assets/CICD_시퀀스_다이어그램_3.png)
![Sequence Diagram](assets/CICD_시퀀스_다이어그램_4.png)

---

## 6. 기술 스택

### Backend (Orchestrator - `user-service`)

* **Language & Framework**: Java 21, Spring Boot 3.5.4
* **Data Access**: MyBatis 3.0.5, MariaDB Java Client 3.3.3
* **Scheduling**: Spring Quartz
* **Resilience**: Spring Retry
* **Security**: Spring Security
* **Build Tool**: Gradle

### Backend (Worker - `pre-processing-service`)

* **Language & Framework**: Python 3.11, FastAPI 0.116.2
* **AI & ML**: Transformers, Scikit-learn, OpenAI API
* **Web Scraping & OCR**: BeautifulSoup4, Selenium, Google Cloud Vision
* **DB & Translation**: SQLAlchemy, Deep-Translator
* **Package Manager**: Poetry

### Database

* **MariaDB 11.4**

### DevOps & Monitoring

* **Containerization**: Docker, Docker Compose
* **CI/CD**: GitHub Actions
* **Monitoring**: Prometheus, Grafana
* **Migration**: Flyway
* **Logging**: Log4j2

---

## 7. 주요 구성 요소 및 역할

* **WorkflowExecutionService**: 워크플로우 전체 실행 흐름 제어
* **TaskExecutionService**: Task 실행 및 재시도 정책 관리
* **TaskBodyBuilder (전략 패턴)**: 각 Task별 동적 Request Body 생성
* **FastApiAdapter**: FastAPI 서버 통신 캡슐화
* **QuartzSchedulerInitializer**: DB 스케줄 정보 Quartz 엔진 동기화
* **ExecutionMdcManager**: 비동기 환경에서도 traceId 기반 분산 추적 로깅

---

## 8. 프로젝트 디렉토리 구조

Monorepo 형태로, `apps` 하위에 서비스별 디렉토리가 존재합니다.

```bash
Final-4team-icebang/
├── apps/
│   ├── user-service/          # Java/Spring Boot 서비스
│   └── pre-processing-service/# Python/FastAPI 서비스
├── docker/                    # Docker 설정
├── .github/                   # GitHub Actions (CI/CD)
└── logs/                      # 로그 파일
```

### user-service (Spring Boot)

도메인 중심 아키텍처 적용. 각 도메인이 자체 구성요소를 포함.

```bash
user-service/
├── src/main/java/site/icebang/
│   ├── domain/        # 도메인 로직
│   ├── external/      # 외부 API 연동
│   ├── global/        # 글로벌 설정
│   └── common/        # 공용 유틸리티
└── Dockerfile
```

### pre-processing-service (FastAPI)

```bash
pre-processing-service/
├── app/
│   ├── api/               # 엔드포인트
│   ├── service/           # 비즈니스 로직
│   ├── core/              # 핵심 설정
│   ├── db/                # DB 관련
│   └── utils/             # 유틸리티
└── pyproject.toml
```

---

## 9. 환경 변수 관리 전략

추후 작성 예정

* **FastAPI (`pre-processing-service`)**:
* **Spring Boot (`user-service`)**:

---

## 10. 시연 영상

[https://www.youtube.com/watch?v=1vApNttVxVg](https://www.youtube.com/watch?v=1vApNttVxVg)
[![Video Label](http://img.youtube.com/vi/1vApNttVxVg/0.jpg)](https://www.youtube.com/watch?v=1vApNttVxVg)
