# app/api/endpoints/embedding.py
from time import sleep

import loguru
from fastapi import APIRouter
from sqlalchemy import text

from ...errors.CustomException import *
from fastapi import APIRouter
from typing import Mapping, Any, Dict
from ...model.schemas import *
from ...service.blog.blog_create_service import BlogContentService
from ...service.blog.naver_blog_post_service import NaverBlogPostService
from ...service.crawl_service import CrawlService
from ...service.keyword_service import keyword_search
from ...service.match_service import MatchService
from ...service.search_service import SearchService

# from ...service.similarity_service import SimilarityService
from ...db.db_connecter import engine  # ✅ 우리가 만든 DB 유틸 임포트
from ...service.similarity_service import SimilarityService

# 이 파일만의 독립적인 라우터를 생성합니다.
router = APIRouter()


@router.get("/hello/{name}", tags=["hello"])
async def say_hello(name: str):
    return {"message": f"Hello {name}"}


# 특정 경로에서 의도적으로 에러 발생
# 커스텀에러 테스터 url
@router.get("/error/{item_id}")
async def trigger_error(item_id: int):
    if item_id == 0:
        raise InvalidItemDataException()

    if item_id == 404:
        raise ItemNotFoundException(item_id=item_id)

    if item_id == 500:
        raise ValueError("이것은 테스트용 값 오류입니다.")

    return {"result": item_id}


@router.get("/db-test", tags=["db"])
async def db_test():
    """간단한 DB 연결 및 쿼리 테스트"""
    try:
        with engine.connect() as conn:
            result = conn.execute(text("SELECT NOW() as now"))
            row = result.fetchone()
            return {"status": "ok", "db_time": str(row.now)}
    except Exception as e:
        return {"status": "error", "detail": str(e)}


def with_meta(data: Mapping[str, Any], meta: Mapping[str, Any]) -> Dict[str, Any]:
    """요청 payload + 공통 meta 머지"""
    return {**meta, **data}


@router.get("/tester", response_model=None)
async def processing_tester():
    # 네이버 키워드 검색
    naver_request = RequestNaverSearch(tag="naver")
    response_data = await keyword_search(naver_request)
    keyword = response_data["data"].get("keyword")
    loguru.logger.info(keyword)

    # 싸다구 상품 검색
    sadagu_request = RequestSadaguSearch(keyword=keyword)
    searchservice = SearchService()
    keyword_result = await searchservice.search_products(sadagu_request)
    loguru.logger.info(keyword_result)

    # 싸다구 상품 매치

    data = keyword_result["data"]
    keyword_match_request = RequestSadaguMatch(
        keyword=data.get("keyword"), search_results=data.get("search_results")
    )
    match_service = MatchService()
    keyword_match_response = match_service.match_products(keyword_match_request)
    loguru.logger.info(keyword_match_response)

    # 싸다구 상품 유사도 분석
    data = keyword_match_response["data"]
    keyword_similarity_request = RequestSadaguSimilarity(
        keyword=data.get("keyword"), matched_products=data.get("matched_products")
    )
    similarity_service = SimilarityService()
    keyword_similarity_response = similarity_service.select_product_by_similarity(
        keyword_similarity_request
    )
    loguru.logger.info(keyword_similarity_response)
    sleep(5)
    # 싸다구 상품 크롤링
    a = RequestSadaguCrawl(
        product_url=keyword_similarity_response["data"]["selected_product"].get("url")
    )
    crawl = CrawlService()
    crawl_response = await crawl.crawl_product_detail(a)
    loguru.logger.info(crawl_response)

    sleep(5)
    # 블로그 생성
    data = crawl_response
    rag = RequestBlogCreate(product_info=data.get("product_detail"), target_length=500)
    blog_service = BlogContentService()
    rag_data = blog_service.generate_blog_content(rag)
    loguru.logger.info(rag_data)

    sleep(15)
    # 블로그 배포
    data = rag_data
    # tistory_service = TistoryBlogPostService()
    naverblogPostService = NaverBlogPostService()
    result = naverblogPostService.post_content(
        title=data.get("title"),
        content=data.get("content"),
        tags=data.get("tags"),
    )
    loguru.logger.info(result)

    return "구웃"
