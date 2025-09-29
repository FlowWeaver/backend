from typing import Dict, List, Optional
from app.service.blog.base_blog_post_service import BaseBlogPostService
from app.service.blog.blogger_blog_post_service import BloggerApiService
from app.errors.BlogPostingException import *


class BloggerBlogPostAdapter(BaseBlogPostService):
    """
    BaseBlogPostService와 호환되도록 BloggerApiService를 감싼 어댑터
    현재 BaseBlogPostService 인터페이스와 호환
    """

    def __init__(self, config_file="blogger/blog_config.json"):
        # API 전용 서비스 (Adaptee) 먼저 초기화
        self.api_service = BloggerApiService(config_file=config_file)

        try:
            # 부모 클래스의 웹드라이버 초기화를 시도하지만, 실패해도 무시
            # 이렇게 하면 부모의 다른 초기화 로직은 실행됨
            super().__init__()
        except Exception:
            # 웹드라이버 초기화 실패 시 API 서비스용으로 속성 설정
            self.crawling_service = None
            self.web_driver = None
            self.wait_driver = None
            # 설정 로드는 직접 호출
            self._load_config()

    def _load_config(self) -> None:
        """
        BloggerApiService 내부에서 이미 처리되므로 별도 구현 불필요
        """
        # API 서비스의 설정이 이미 로드되었으므로 추가 작업 없음
        pass

    def _login(self) -> None:
        """
        Selenium 로그인과 달리, OAuth 인증으로 대체
        """
        try:
            self.api_service.authenticate_with_google_oauth()
        except Exception as e:
            raise BlogLoginException("Blogger", f"OAuth 인증 실패: {str(e)}")

    def _write_content(self, title: str, content: str, tags: List[str] = None) -> None:
        """
        API를 통한 포스트 작성
        """
        try:
            result = self.api_service.create_post_via_api(title, content, labels=tags)
            # 결과 로깅
            print(f"포스트 생성 완료: {result.get('published_url', 'URL 없음')}")
            return result.get("published_url")
        except Exception as e:
            raise BlogPostPublishException("Blogger", f"포스트 작성 실패: {str(e)}")

    def _get_platform_name(self) -> str:
        """플랫폼 이름 반환"""
        return "BLOGGER"

    def _validate_content(
        self, title: str, content: str, tags: Optional[List[str]] = None
    ) -> None:
        """
        API 전용 유효성 검사 호출
        """
        try:
            # Optional을 List로 변환 (None인 경우 빈 리스트)
            tags_list = tags if tags is not None else []
            self.api_service.validate_api_content(title, content, labels=tags_list)
        except Exception as e:
            # BloggerApiService의 예외를 BaseBlogPostService 호환 예외로 변환
            if "title" in str(e).lower():
                raise BlogContentValidationException("title", str(e))
            elif "content" in str(e).lower():
                raise BlogContentValidationException("content", str(e))
            else:
                raise BlogContentValidationException("general", str(e))

    def __del__(self):
        """
        API 서비스이므로 웹드라이버 정리가 불필요
        """
        # 웹드라이버가 없으므로 정리할 것이 없음
        pass
