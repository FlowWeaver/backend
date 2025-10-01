package site.icebang.common.dto;

import org.springframework.http.HttpStatus;

import lombok.Data;

/**
 * 공통 APi 응답 DTO 클래스입니다.
 *
 * <p>REST API의 응답 형식을 표준화하기 위해 사용됩니다. 모든 응답은 성공 여부({@link #success}), 응답 데이터({@link #data}), 응답
 * 메시지({@link #message}), 그리고 HTTP 상태 코드({@link #status})를 포함합니다.
 *
 * <p><b>사용 예시:</b>
 *
 * <pre>{@code
 * // 성공 응답 생성
 * ApiResponse<UserDto> response = ApiResponse.success(userDto);
 *
 * // 메시지를 포함한 성공 응답
 * ApiResponse<UserDto> response = ApiResponse.success(userDto, "회원 조회 성공");
 *
 * // 오류 응답 생성
 * ApiResponse<Void> errorResponse = ApiResponse.error("잘못된 요청입니다.", HttpStatus.BAD_REQUEST);
 * }</pre>
 *
 * @param <T> 응답 데이터의 타입
 * @author jys01012@gmail.com
 * @since v0.0.1-alpha
 * @see HttpStatus
 * @see lombok.Data
 */
@Data
public class ApiResponseDto<T> {
  /**
   * 요청 처리 성공 여부.
   *
   * <p>true: 요청이 정상적으로 처리됨 false: 요청 처리 중 오류 발생
   */
  private boolean success;

  /**
   * 실제 응답 데이터(payload).
   *
   * <p>요청이 성공적으로 처리되었을 경우 반환되는 데이터이며, 실패 시에는 {@code null}일 수 있습니다.
   */
  private T data;

  /**
   * 응답 메세지.
   *
   * <p>성공 또는 오류 상황을 설명하는 메시지를 담습니다. 클라이언트에서 사용자에게 직접 표시할 수도 있습니다.
   */
  private String message;

  /**
   * HTTP 상태 코드.
   *
   * <p>Spring의 {@link HttpStatus} 열거형을 사용합니다.
   */
  private HttpStatus status; // HttpStatus로 변경

  /** 기본 생성자입니다. 모든 필드가 기본값으로 초기화됩니다. */
  public ApiResponseDto() {}

  /**
   * 모든 필드를 초기화하는 생성자.
   *
   * @param success 요청 성공 여부
   * @param data 응답 데이터
   * @param message 응답 메시지
   * @param status HTTP 상태 코드
   */
  public ApiResponseDto(boolean success, T data, String message, HttpStatus status) {
    this.success = success;
    this.data = data;
    this.message = message;
    this.status = status;
  }

  /**
   * 성공 응답을 생성합니다. (기본 메시지: "OK", 상태: 200 OK)
   *
   * @param data 응답 데이터
   * @param <T> 데이터 타입
   * @return 성공 응답 객체
   */
  public static <T> ApiResponseDto<T> success(T data) {
    return new ApiResponseDto<>(true, data, "OK", HttpStatus.OK);
  }

  /**
   * 성공 응답을 생성합니다. (상태: 200 OK)
   *
   * @param data 응답 데이터
   * @param message 사용자 정의 메시지
   * @param <T> 데이터 타입
   * @return 성공 응답 객체
   */
  public static <T> ApiResponseDto<T> success(T data, String message) {
    return new ApiResponseDto<>(true, data, message, HttpStatus.OK);
  }

  /**
   * 성공 응답을 생성합니다.
   *
   * @param data 응답 데이터
   * @param message 사용자 정의 메시지
   * @param status 사용자 정의 상태 코드
   * @param <T> 데이터 타입
   * @return 성공 응답 객체
   */
  public static <T> ApiResponseDto<T> success(T data, String message, HttpStatus status) {
    return new ApiResponseDto<>(true, data, message, status);
  }

  /**
   * 오류 응답을 생성합니다.
   *
   * @param message 오류 메시지
   * @param status HTTP 상태 코드
   * @param <T> 데이터 타입
   * @return 오류 응답 객체
   */
  public static <T> ApiResponseDto<T> error(String message, HttpStatus status) {
    return new ApiResponseDto<>(false, null, message, status);
  }
}
