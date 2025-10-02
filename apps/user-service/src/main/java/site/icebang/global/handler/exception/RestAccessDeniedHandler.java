package site.icebang.global.handler.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponseDto;

/**
 * 접근 거부 처리기 (REST 전용 AccessDeniedHandler).
 *
 * <p>Spring Security에서 인가(Authorization) 실패 시 호출됩니다. 사용자가 필요한 권한 없이 보호된 리소스에 접근하려고 하면 이 핸들러가 실행되어
 * JSON 형식의 에러 응답을 반환합니다.
 *
 * <p>주요 특징:
 *
 * <ul>
 *   <li>HTTP 상태 코드: {@link HttpStatus#FORBIDDEN} (403)
 *   <li>응답 본문: {@link ApiResponseDto} 형식의 에러 메시지
 *   <li>응답 Content-Type: {@code application/json;charset=UTF-8}
 * </ul>
 *
 * <p>이 핸들러는 기본 HTML 오류 페이지 대신, REST API 클라이언트에 JSON 기반의 표준 에러 응답을 제공하기 위해 사용됩니다.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  /** JSON 직렬화를 위한 ObjectMapper */
  private final ObjectMapper objectMapper;

  /**
   * 인가되지 않은 요청이 들어왔을 때 실행됩니다.
   *
   * @param request 현재 요청
   * @param response 응답 객체
   * @param ex 발생한 {@link AccessDeniedException}
   * @throws IOException 응답 스트림 처리 중 오류 발생 시
   */
  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    ApiResponseDto<String> body = ApiResponseDto.error("Access denied", HttpStatus.FORBIDDEN);

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
