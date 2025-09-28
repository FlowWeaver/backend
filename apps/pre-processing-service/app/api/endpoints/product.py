from fastapi import APIRouter, Request, HTTPException
from ...errors.CustomException import (
    InvalidItemDataException,
    ItemNotFoundException,
    CustomException,
)
from ...service.crawl_service import CrawlService
from ...service.s3_upload_service import S3UploadService
from ...service.search_service import SearchService
from ...service.match_service import MatchService
from ...service.similarity_service import SimilarityService
from ...service.product_selection_service import ProductSelectionService


# from ...service.similarity_service import SimilarityService
from ...model.schemas import *

router = APIRouter()


@router.post("/search", response_model=ResponseSadaguSearch, summary="상품 검색")
async def search(request: RequestSadaguSearch):
    """
    요청된 키워드로 사다구몰 상품을 검색합니다.
    """
    try:
        search_service = SearchService()
        response_data = await search_service.search_products(request)

        if not response_data:
            raise CustomException(500, "상품 검색에 실패했습니다.", "SEARCH_FAILED")

        return response_data
    except InvalidItemDataException as e:
        raise HTTPException(status_code=e.status_code, detail=e.detail)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/match", response_model=ResponseSadaguMatch, summary="상품 매칭")
async def match(request: RequestSadaguMatch):
    """
    검색 결과 상품과 키워드를 기반으로 매칭을 수행합니다.
    """
    try:
        match_service = MatchService()
        response_data = match_service.match_products(request)

        if not response_data:
            raise CustomException(500, "상품 매칭에 실패했습니다.", "MATCH_FAILED")

        return response_data
    except InvalidItemDataException as e:
        raise HTTPException(status_code=e.status_code, detail=e.detail)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/similarity", response_model=ResponseSadaguSimilarity, summary="상품 유사도 분석"
)
async def similarity(request: RequestSadaguSimilarity):
    """
    매칭된 상품들 중 키워드와의 유사도를 계산하여 상위 10개 상품을 선택합니다.
    """
    try:
        similarity_service = SimilarityService()
        response_data = similarity_service.select_top_products_by_similarity(request)

        if not response_data:
            raise CustomException(
                500, "유사도 분석에 실패했습니다.", "SIMILARITY_FAILED"
            )

        return response_data
    except InvalidItemDataException as e:
        raise HTTPException(status_code=e.status_code, detail=e.detail)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/crawl", response_model=ResponseSadaguCrawl, summary="상품 상세 정보 크롤링"
)
async def crawl(body: RequestSadaguCrawl):
    """
    상품 상세 페이지를 크롤링하여 상세 정보를 수집합니다.
    """
    try:
        crawl_service = CrawlService()
        response_data = await crawl_service.crawl_product_detail(body)

        if not response_data:
            raise CustomException(500, "상품 크롤링에 실패했습니다.", "CRAWL_FAILED")

        return response_data
    except InvalidItemDataException as e:
        raise HTTPException(status_code=e.status_code, detail=e.detail)
    except ItemNotFoundException as e:
        raise HTTPException(status_code=e.status_code, detail=e.detail)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/s3-upload", response_model=ResponseS3Upload, summary="S3 이미지 업로드")
async def s3_upload(request: RequestS3Upload):
    """
    크롤링 완료 후 별도로 호출하여 이미지들을 S3 저장소에 업로드합니다.
    """
    try:
        s3_upload_service = S3UploadService()
        response_data = await s3_upload_service.upload_crawled_products_to_s3(request)

        if not response_data:
            raise CustomException(
                500, "S3 이미지 업로드에 실패했습니다.", "S3_UPLOAD_FAILED"
            )

        return response_data
    except InvalidItemDataException as e:
        raise HTTPException(status_code=e.status_code, detail=e.detail)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/select", response_model=ResponseProductSelect, summary="콘텐츠용 상품 선택"
)
def select_product(request: RequestProductSelect):  # async 제거
    """
    S3 업로드 완료 후 콘텐츠 생성을 위한 최적 상품을 선택합니다.
    """
    try:
        selection_service = ProductSelectionService()
        response_data = selection_service.select_product_for_content(
            request
        )  # await 제거

        if not response_data:
            raise CustomException(
                500, "상품 선택에 실패했습니다.", "PRODUCT_SELECTION_FAILED"
            )

        return response_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
