package site.icebang.global.handler.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

import site.icebang.common.dto.ApiResponseDto;
import site.icebang.common.exception.DuplicateDataException;

/**
 * 전역 예외 처리기 (Global Exception Handler).
 *
 * <p>이 클래스는 애플리케이션 전역에서 발생하는 예외를 {@link ApiResponseDto} 형태로 변환하여 클라이언트에게 반환합니다. 예외 유형에 따라 적절한 {@link
 * HttpStatus} 코드를 설정하며, 공통적인 예외 처리 로직을 중앙화합니다.
 *
 * <p>처리되는 주요 예외는 다음과 같습니다:
 *
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} - 요청 데이터 유효성 검증 실패
 *   <li>{@link NoResourceFoundException} - 존재하지 않는 리소스 접근
 *   <li>{@link AuthenticationException} - 인증 실패
 *   <li>{@link AccessDeniedException} - 인가 실패
 *   <li>{@link DuplicateDataException} - 중복 데이터 발생
 *   <li>{@link Exception} - 그 외 처리되지 않은 일반 예외
 * </ul>
 *
 * <p>모든 응답은 {@code ApiResponse.error(...)} 메서드를 통해 생성되며, 에러 메시지와 HTTP 상태 코드가 포함됩니다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * 요청 데이터 유효성 검증 실패 시 발생하는 예외를 처리합니다.
   *
   * @param ex 발생한 {@link MethodArgumentNotValidException}
   * @return {@link ApiResponseDto} - 검증 실패 메시지와 {@link HttpStatus#BAD_REQUEST}
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponseDto<Void> handleValidation(MethodArgumentNotValidException ex) {
    String errorMessage =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

    return ApiResponseDto.error("입력 값 검증 실패: " + errorMessage, HttpStatus.BAD_REQUEST);
  }

  /**
   * 처리되지 않은 모든 일반 예외를 처리합니다. 서버 내부 오류로 간주되며, 에러 로그를 남깁니다.
   *
   * @param ex 발생한 {@link Exception}
   * @return {@link ApiResponseDto} - 내부 오류 메시지와 {@link HttpStatus#INTERNAL_SERVER_ERROR}
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponseDto<String> handleGeneric(Exception ex) {
    log.error(ex.getMessage(), ex);
    return ApiResponseDto.error("Internal error: ", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * 존재하지 않는 리소스 접근 시 발생하는 예외를 처리합니다.
   *
   * @param ex 발생한 {@link NoResourceFoundException}
   * @return {@link ApiResponseDto} - 리소스 없음 메시지와 {@link HttpStatus#NOT_FOUND}
   */
  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiResponseDto<String> handleNotFound(NoResourceFoundException ex) {
    return ApiResponseDto.error("Notfound: " + ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  /**
   * 인증 실패 시 발생하는 예외를 처리합니다.
   *
   * @param ex 발생한 {@link AuthenticationException}
   * @return {@link ApiResponseDto} - 인증 실패 메시지와 {@link HttpStatus#UNAUTHORIZED}
   */
  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiResponseDto<String> handleAuthentication(AuthenticationException ex) {
    return ApiResponseDto.error("Authentication failed: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
  }

  /**
   * 인가(권한) 실패 시 발생하는 예외를 처리합니다.
   *
   * @param ex 발생한 {@link AccessDeniedException}
   * @return {@link ApiResponseDto} - 접근 거부 메시지와 {@link HttpStatus#FORBIDDEN}
   */
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponseDto<String> handleAccessDenied(AccessDeniedException ex) {
    return ApiResponseDto.error("Access denied: " + ex.getMessage(), HttpStatus.FORBIDDEN);
  }

  /**
   * 중복 데이터 발생 시 발생하는 예외를 처리합니다.
   *
   * @param ex 발생한 {@link DuplicateDataException}
   * @return {@link ApiResponseDto} - 중복 데이터 메시지와 {@link HttpStatus#CONFLICT}
   */
  @ExceptionHandler(DuplicateDataException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiResponseDto<String> handleDuplicateData(DuplicateDataException ex) {
    log.warn(ex.getMessage(), ex);
    return ApiResponseDto.error("Duplicate: " + ex.getMessage(), HttpStatus.CONFLICT);
  }

  /**
   * IllegalArgumentException을 400 Bad Request로 처리합니다. WorkflowService에서 던지는 검증 오류를 처리하기 위해 추가되었습니다.
   *
   * @param ex 발생한 {@link IllegalArgumentException}
   * @return {@link ApiResponseDto} - 검증 실패 메시지와 {@link HttpStatus#BAD_REQUEST}
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponseDto<String> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    return ApiResponseDto.error("입력값 검증 실패: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
  }
}
