package site.icebang.global.filter.logging;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

/** 클라이언트 요청 정보를 MDC에 설정하는 필터 모든 HTTP 요청에 대해 클라이언트 IP와 User-Agent 정보를 추출하여 로깅 컨텍스트에 저장합니다. */
@Component
@Order(1) // 필터 체인에서 첫 번째로 실행되도록 우선순위 설정
public class ClientLoggingFilter implements Filter {

  /**
   * HTTP 요청을 필터링하여 클라이언트 정보를 MDC에 설정합니다.
   *
   * @param request 서블릿 요청 객체
   * @param response 서블릿 응답 객체
   * @param chain 필터 체인
   * @throws IOException 입출력 예외
   * @throws ServletException 서블릿 예외
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String ip = getClientIp(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    try {
      MDC.put("clientIp", ip);
      MDC.put("userAgent", userAgent);

      chain.doFilter(request, response);
    } finally {
      MDC.remove("clientIp");
      MDC.remove("userAgent");
    }
  }

  /**
   * 프록시 환경을 고려하여 클라이언트의 실제 IP 주소를 추출합니다. 로드 밸런서나 프록시 서버를 통해 들어오는 요청의 원본 IP를 찾습니다.
   *
   * @param request HTTP 요청 객체
   * @return 클라이언트의 실제 IP 주소
   */
  private String getClientIp(HttpServletRequest request) {

    String[] headers = {
      "X-Forwarded-For", // 표준 프록시 헤더
      "Proxy-Client-IP", // Apache 프록시
      "WL-Proxy-Client-IP", // WebLogic 프록시
      "HTTP_X_FORWARDED_FOR", // HTTP 프록시
      "HTTP_CLIENT_IP", // HTTP 클라이언트 IP
      "REMOTE_ADDR" // 원격 주소
    };

    for (String header : headers) {
      String ip = request.getHeader(header);
      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
        return ip.split(",")[0].trim();
      }
    }

    return request.getRemoteAddr();
  }
}
