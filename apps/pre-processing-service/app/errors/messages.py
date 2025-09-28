# app/errors/messages.py
from fastapi import status

ERROR_MESSAGES = {
    status.HTTP_400_BAD_REQUEST: "잘못된 요청입니다.",
    status.HTTP_401_UNAUTHORIZED: "인증이 필요합니다.",
    status.HTTP_403_FORBIDDEN: "접근 권한이 없습니다.",
    status.HTTP_404_NOT_FOUND: "요청하신 리소스를 찾을 수 없습니다.",
    status.HTTP_422_UNPROCESSABLE_CONTENT: "입력 데이터가 유효하지 않습니다.",
    status.HTTP_500_INTERNAL_SERVER_ERROR: "서버 내부 오류가 발생했습니다.",
}


def get_error_message(status_code: int, detail: str | None = None) -> str:
    """상태 코드에 맞는 기본 메시지를 가져오되, detail이 있으면 우선"""
    return detail or ERROR_MESSAGES.get(status_code, "알 수 없는 오류가 발생했습니다.")
