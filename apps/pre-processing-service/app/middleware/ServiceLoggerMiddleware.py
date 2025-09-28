from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response
from loguru import logger
from contextvars import ContextVar
from fastapi.responses import JSONResponse
from typing import Dict, Optional, List, Any

import json
import time
import asyncio

from app.middleware.rds_logger import RDSLogger
from app.middleware.loki_logger import LokiLogger

trace_id_context: ContextVar[str] = ContextVar("trace_id", default="")


class ServiceLoggerMiddleware(BaseHTTPMiddleware):
    """
    완전 자동 서비스 로깅 미들웨어 - 의존성 주입 불필요
    URL 패턴을 기반으로 자동으로 서비스 타입 식별 및 로깅
    """

    def __init__(
        self,
        app,
        service_mappings: Dict[str, Dict] = None,
        enable_rds: bool = True,
        enable_loki: bool = True,
    ):
        """
        :param service_mappings: URL 패턴별 서비스 설정
        :param enable_rds: RDS 로깅 활성화 여부
        :param enable_loki: Loki 로깅 활성화 여부
        예: {
            "/keywords/search": {
                "service_type": "NAVER_CRAWLING",
                "track_params": ["keyword", "category"],
                "response_trackers": ["total_keywords", "results_count"]
            }
        }
        """
        super().__init__(app)
        self.service_mappings = service_mappings or self._default_mappings()
        self.enable_rds = enable_rds
        self.enable_loki = enable_loki

        # 로거 인스턴스 초기화
        self.rds_logger = RDSLogger() if enable_rds else None
        # Loki 직접 로깅 비활성화 - PromTail을 통해 파일로 로깅
        # self.loki_logger = LokiLogger() if enable_loki else None
        self.loki_logger = None

    def _default_mappings(self) -> Dict[str, Dict]:
        """기본 서비스 매핑 설정"""
        service_type = "TASK"
        return {
            # 네이버 키워드 검색
            "/keywords/search": {
                "service_type": service_type,
                "track_params": [
                    "tag",
                    "keyword",
                    "category",
                    "startDate",
                    "endDate",
                    "job_id",
                    "schedule_id",
                ],
                "response_trackers": ["keyword", "total_keyword", "success", "status"],
            },
            # 블로그 RAG 콘텐츠 생성
            "/blogs/rag/create": {
                "service_type": service_type,
                "track_params": [
                    "keyword",
                    "product_info",
                    "content_type",
                    "target_length",
                    "job_id",
                    "schedule_id",
                    "schedule_his_id",
                ],
                "response_trackers": [
                    "title",
                    "content_length",
                    "tags_count",
                    "success",
                    "status",
                ],
            },
            # 블로그 배포
            "/blogs/publish": {
                "service_type": service_type,
                "track_params": [
                    "tag",
                    "blog_id",
                    "post_title",
                    "post_content",
                    "post_tags",
                    "job_id",
                    "schedule_id",
                    "schedule_his_id",
                ],
                "response_trackers": [
                    "tag",
                    "post_title",
                    "post_url",
                    "published_at",
                    "publish_success",
                    "metadata",
                    "success",
                    "status",
                ],
            },
            # 상품 검색
            "/products/search": {
                "service_type": service_type,
                "track_params": [
                    "keyword",
                    "job_id",
                    "schedule_id",
                    "schedule_his_id",
                ],
                "response_trackers": [
                    "keyword",
                    "search_results_count",
                    "success",
                    "status",
                ],
            },
            # 상품 매칭
            "/products/match": {
                "service_type": service_type,
                "track_params": [
                    "keyword",
                    "search_results",
                    "job_id",
                    "schedule_id",
                    "schedule_his_id",
                ],
                "response_trackers": [
                    "keyword",
                    "matched_products_count",
                    "success",
                    "status",
                ],
            },
            # 상품 유사도 분석
            "/products/similarity": {
                "service_type": service_type,
                "track_params": [
                    "keyword",
                    "matched_products",
                    "search_results",
                    "job_id",
                    "schedule_id",
                    "schedule_his_id",
                ],
                "response_trackers": [
                    "keyword",
                    "selected_product",
                    "reason",
                    "success",
                    "status",
                ],
            },
            # 상품 크롤링
            "/products/crawl": {
                "service_type": service_type,
                "track_params": [
                    "tag",
                    "product_url",
                    "job_id",
                    "schedule_id",
                    "schedule_his_id",
                ],
                "response_trackers": [
                    "tag",
                    "product_url",
                    "product_detail",
                    "crawled_at",
                    "success",
                    "status",
                ],
            },
            # 상품 이미지 번역
            "blogs/ocr/extract": {
                "service_type": service_type,
                "track_params": [],
                "response_trackers": [],
            },
            # 상품 이미지 S3업로드
            "products/s3-upload": {
                "service_type": service_type,
                "track_params": [],
                "response_trackers": [],
            },
        }

    async def dispatch(self, request: Request, call_next):
        """요청-응답 사이클을 가로채서 자동 로깅 처리"""

        # 1. 서비스 설정 확인
        service_config = self._get_service_config(request.url.path)
        if not service_config:
            # 로깅 대상이 아닌 경우 그냥 통과
            return await call_next(request)

        # 2. 시작 로깅
        trace_id = request.headers.get("X-Request-ID", "")
        trace_id_context.set(trace_id)
        start_time = time.time()

        # 파라미터 추출 및 시작 로그
        params = await self._extract_params(request, service_config["track_params"])
        param_str = ""
        if params:
            param_strs = [f"{k}={v}" for k, v in params.items()]
            param_str = " " + " ".join(param_strs)

        service_type = service_config["service_type"]
        logger.info(f"[{service_type}_START] trace_id={trace_id}{param_str}")

        # source_id 추출 (job_id, schedule_id 등에서)
        source_id = self._extract_source_id(params)
        run_id = params.get("run_id")

        # RDS 및 Loki에 시작 로그 전송
        start_message = f"[{service_type}_START]{param_str}"
        await self._log_to_external_systems(
            "start", service_type, source_id, trace_id, start_message, run_id, params
        )

        # 3. 요청 처리
        try:
            response = await call_next(request)

            # 4. 성공 로깅
            duration_ms = int((time.time() - start_time) * 1000)

            if 200 <= response.status_code < 300:
                await self._log_success_response(
                    service_type,
                    trace_id,
                    start_time,
                    param_str,
                    response,
                    service_config["response_trackers"],
                )

                # 외부 로깅 시스템에 성공 로그 전송
                success_message = f"[{service_type}_SUCCESS]{param_str} status_code={response.status_code}"
                await self._log_to_external_systems(
                    "success",
                    service_type,
                    source_id,
                    trace_id,
                    success_message,
                    run_id,
                    params,
                    duration_ms=duration_ms,
                )
            else:
                await self._log_error_response(
                    service_type, trace_id, start_time, param_str, response
                )

                # 외부 로깅 시스템에 에러 로그 전송
                error_message = f"[{service_type}_ERROR]{param_str} status_code={response.status_code}"
                await self._log_to_external_systems(
                    "error",
                    service_type,
                    source_id,
                    trace_id,
                    error_message,
                    run_id,
                    params,
                    duration_ms=duration_ms,
                    error_code=f"HTTP_{response.status_code}",
                )

            return response

        except Exception as e:
            # 5. 예외 로깅
            duration_ms = int((time.time() - start_time) * 1000)
            await self._log_exception(service_type, trace_id, start_time, param_str, e)

            # 외부 로깅 시스템에 예외 로그 전송
            exception_message = (
                f"[{service_type}_EXCEPTION]{param_str} exception={str(e)}"
            )
            await self._log_to_external_systems(
                "error",
                service_type,
                source_id,
                trace_id,
                exception_message,
                run_id,
                params,
                duration_ms=duration_ms,
                error_code="EXCEPTION",
            )
            raise

    def _get_service_config(self, url_path: str) -> Optional[Dict]:
        """URL 경로를 기반으로 서비스 설정 반환"""
        for pattern, config in self.service_mappings.items():
            if self._match_pattern(url_path, pattern):
                return config
        return None

    def _match_pattern(self, url_path: str, pattern: str) -> bool:
        """URL 패턴 매칭 (간단한 구현, 필요시 정규식으로 확장 가능)"""
        # 정확히 일치하거나 패턴이 접두사인 경우
        return url_path == pattern or url_path.startswith(pattern.rstrip("*"))

    async def _extract_params(
        self, request: Request, track_params: List[str]
    ) -> Dict[str, Any]:
        """요청에서 추적 파라미터 추출"""
        params = {}

        try:
            # Query Parameters 추출
            for key, value in request.query_params.items():
                if key in track_params:
                    params[key] = value

            # JSON Body 추출
            try:
                # request body를 읽기 위한 안전한 방법
                body = await request.body()
                if body:
                    json_body = json.loads(body.decode())
                    if isinstance(json_body, dict):
                        for key, value in json_body.items():
                            if key in track_params:
                                if isinstance(value, str) and len(value) > 50:
                                    params[f"{key}_length"] = len(value)
                                elif isinstance(value, list):
                                    params[f"{key}_count"] = len(value)
                                else:
                                    params[key] = value
            except:
                pass

        except Exception as e:
            logger.debug(f"파라미터 추출 실패: {e}")

        return params

    async def _log_success_response(
        self,
        service_type: str,
        trace_id: str,
        start_time: float,
        param_str: str,
        response: Response,
        response_trackers: List[str],
    ):
        """성공 응답 로깅"""
        duration = time.time() - start_time

        log_parts = [
            f"[{service_type}_SUCCESS]",
            f"trace_id={trace_id}",
            f"execution_time={duration:.4f}s{param_str}",
            f"status_code={response.status_code}",
        ]

        # 응답 데이터에서 추적 정보 추출
        if isinstance(response, JSONResponse) and response_trackers:
            try:
                # JSONResponse body 읽기
                if hasattr(response, "body"):
                    response_data = json.loads(response.body.decode())
                elif hasattr(response, "content"):
                    response_data = response.content
                else:
                    response_data = None

                if response_data and isinstance(response_data, dict):
                    response_params = []
                    for tracker in response_trackers:
                        if tracker in response_data:
                            value = response_data[tracker]
                            if isinstance(value, dict):
                                response_params.append(
                                    f"{tracker}_keys={list(value.keys())}"
                                )
                                response_params.append(f"{tracker}_count={len(value)}")
                            elif isinstance(value, list):
                                response_params.append(f"{tracker}_count={len(value)}")
                            else:
                                response_params.append(f"{tracker}={value}")

                    if response_params:
                        log_parts.append(" ".join(response_params))

            except Exception as e:
                logger.debug(f"응답 추적 정보 추출 실패: {e}")

        logger.info(" ".join(log_parts))

    async def _log_error_response(
        self,
        service_type: str,
        trace_id: str,
        start_time: float,
        param_str: str,
        response: Response,
    ):
        """에러 응답 로깅"""
        duration = time.time() - start_time
        logger.error(
            f"[{service_type}_ERROR] trace_id={trace_id} "
            f"execution_time={duration:.4f}s{param_str} "
            f"status_code={response.status_code}"
        )

    async def _log_exception(
        self,
        service_type: str,
        trace_id: str,
        start_time: float,
        param_str: str,
        exception: Exception,
    ):
        """예외 로깅"""
        duration = time.time() - start_time
        logger.error(
            f"[{service_type}_EXCEPTION] trace_id={trace_id} "
            f"execution_time={duration:.4f}s{param_str} "
            f"exception={str(exception)}"
        )

    def _extract_source_id(self, params: Dict[str, Any]) -> int:
        """파라미터에서 source_id 추출 (job_id, schedule_id 등 우선순위)"""
        for key in ["job_id", "schedule_id", "task_id", "workflow_id"]:
            if key in params and params[key]:
                try:
                    return int(params[key])
                except (ValueError, TypeError):
                    continue
        return 0  # 기본값

    async def _log_to_external_systems(
        self,
        log_type: str,  # start, success, error
        service_type: str,
        source_id: int,
        trace_id: str,
        message: str,
        run_id: Optional[int] = None,
        params: Optional[Dict[str, Any]] = None,
        duration_ms: Optional[int] = None,
        error_code: Optional[str] = None,
    ):
        """RDS와 Loki에 로그 전송"""
        tasks = []

        # 로깅할 추가 데이터 준비
        additional_data = params.copy() if params else {}

        if self.rds_logger:
            if log_type == "start":
                task = self.rds_logger.log_start(
                    service_type, source_id, trace_id, message, run_id, additional_data
                )
            elif log_type == "success":
                task = self.rds_logger.log_success(
                    service_type,
                    source_id,
                    trace_id,
                    message,
                    duration_ms,
                    run_id,
                    additional_data,
                )
            elif log_type == "error":
                task = self.rds_logger.log_error(
                    service_type,
                    source_id,
                    trace_id,
                    message,
                    error_code,
                    duration_ms,
                    run_id,
                    additional_data,
                )
            tasks.append(task)

        if self.loki_logger:
            if log_type == "start":
                task = self.loki_logger.log_start(
                    service_type, source_id, trace_id, message, run_id, additional_data
                )
            elif log_type == "success":
                task = self.loki_logger.log_success(
                    service_type,
                    source_id,
                    trace_id,
                    message,
                    duration_ms,
                    run_id,
                    additional_data,
                )
            elif log_type == "error":
                task = self.loki_logger.log_error(
                    service_type,
                    source_id,
                    trace_id,
                    message,
                    error_code,
                    duration_ms,
                    run_id,
                    additional_data,
                )
            tasks.append(task)

        # 비동기로 병렬 실행 (로깅 실패가 메인 로직에 영향을 주지 않도록)
        if tasks:
            try:
                await asyncio.gather(*tasks, return_exceptions=True)
            except Exception as e:
                logger.debug(f"외부 로깅 시스템 전송 중 일부 실패: {e}")
