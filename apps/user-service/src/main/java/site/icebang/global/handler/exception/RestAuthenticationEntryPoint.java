package site.icebang.global.handler.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponseDto;

/**
 * 인증 진입점 처리기 (REST 전용 AuthenticationEntryPoint).
 *
 * <p>Spring Security에서 인증(Authentication) 실패 시 호출됩니다. 인증되지 않은 사용자가 보호된 리소스에 접근하려고 하면 이 핸들러가 실행되어
 * JSON 형식의 에러 응답을 반환합니다.
 *
 * <p>주요 특징:
 *
 * <ul>
 *   <li>HTTP 상태 코드: {@link HttpStatus#UNAUTHORIZED} (401)
 *   <li>응답 본문: {@link ApiResponseDto} 형식의 에러 메시지
 *   <li>응답 Content-Type: {@code application/json;charset=UTF-8}
 * </ul>
 *
 * <p>이 핸들러는 기본 로그인 페이지 리다이렉트 대신, REST API 클라이언트에 JSON 기반의 표준 에러 응답을 제공하기 위해 사용됩니다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  /** JSON 직렬화를 위한 ObjectMapper */
  private final ObjectMapper objectMapper;

  /**
   * 인증되지 않은 요청이 들어왔을 때 실행됩니다.
   *
   * @param request 현재 요청
   * @param response 응답 객체
   * @param ex 발생한 {@link AuthenticationException}
   * @throws IOException 응답 스트림 처리 중 오류 발생 시
   */
  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    ApiResponseDto<String> body =
        ApiResponseDto.error("Authentication required", HttpStatus.UNAUTHORIZED);

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
