from fastapi import Request, status
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from starlette.exceptions import HTTPException as StarletteHTTPException
from fastapi.exceptions import RequestValidationError
from .messages import ERROR_MESSAGES, get_error_message
from ..errors.CustomException import CustomException


class ErrorBaseModel(BaseModel):
    """
    모든 에러 응답의 기반이 되는 Pydantic 모델.
    API의 에러 응답 형식을 통일하는 역할을 합니다.
    """

    status_code: int
    detail: str
    code: str


# CustomException 핸들러
async def custom_exception_handler(request: Request, exc: CustomException):
    """
    CustomException을 상속받는 모든 예외를 처리합니다.
    """
    # 변경점: ErrorBaseModel을 사용하여 응답 본문 생성
    error_content = ErrorBaseModel(
        status_code=exc.status_code, detail=exc.detail, code=exc.code
    )
    return JSONResponse(
        status_code=exc.status_code,
        content=error_content.model_dump(),
    )


# FastAPI의 HTTPException 핸들러 (예: 404 Not Found)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    """
    FastAPI에서 기본적으로 발생하는 HTTP 관련 예외를 처리합니다.
    """
    message = get_error_message(exc.status_code, exc.detail)

    # 변경점: ErrorBaseModel을 사용하여 응답 본문 생성
    error_content = ErrorBaseModel(
        status_code=exc.status_code, detail=message, code=f"HTTP_{exc.status_code}"
    )
    return JSONResponse(
        status_code=exc.status_code,
        content=error_content.model_dump(),
    )


# Pydantic Validation Error 핸들러 (422)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """
    Pydantic 모델 유효성 검사 실패 시 발생하는 예외를 처리합니다.
    """
    # 변경점: ErrorBaseModel을 기본 구조로 사용하고, 추가 정보를 더함
    base_error = ErrorBaseModel(
        status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
        detail=ERROR_MESSAGES[status.HTTP_422_UNPROCESSABLE_CONTENT],
        code="VALIDATION_ERROR",
    )

    # 모델의 내용과 추가적인 'details' 필드를 결합
    response_content = base_error.model_dump()
    response_content["details"] = exc.errors()

    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
        content=response_content,
    )


# 처리되지 않은 모든 예외 핸들러 (500)
async def unhandled_exception_handler(request: Request, exc: Exception):
    """
    처리되지 않은 모든 예외를 처리합니다.
    """
    # 변경점: ErrorBaseModel을 사용하여 응답 본문 생성
    error_content = ErrorBaseModel(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        detail=ERROR_MESSAGES[status.HTTP_500_INTERNAL_SERVER_ERROR],
        code="INTERNAL_SERVER_ERROR",
    )
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content=error_content.model_dump(),
    )
