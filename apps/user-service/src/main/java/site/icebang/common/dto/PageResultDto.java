package site.icebang.common.dto;

import java.util.List;
import java.util.function.Supplier;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 페이징 처리된 결과 DTO 클래스.
 *
 * <p>목록 조회 API에서 페이징된 데이터를 반환할 때 사용됩니다. 실제 데이터 목록({@link #data}), 전체 개수({@link #total}), 현재 페이지
 * 번호({@link #current}), 페이지 크기({@link #pageSize}), 전체 페이지 수({@link #totalPages}), 다음/이전 페이지 여부를
 * 포함합니다.
 *
 * <p><b>사용 예시:</b>
 *
 * <pre>{@code
 * PageParams params = new PageParams();
 * params.setCurrent(2);
 * params.setPageSize(10);
 *
 * // Repository나 Mapper에서 데이터를 가져와 PageResult 생성
 * PageResult<UserDto> pageResult = PageResult.from(
 *     params,
 *     () -> userRepository.findUsers(params.getOffset(), params.getPageSize()),
 *     () -> userRepository.countUsers()
 * );
 *
 * boolean hasNext = pageResult.isHasNext(); // true or false
 * }</pre>
 *
 * @param <T> 데이터 타입
 * @author jys01012@gmail.com
 * @since v0.0.1-alpha
 */
@Data
@NoArgsConstructor
public class PageResultDto<T> {

  /** 현재 페이지에 포함된 데이터 목록. */
  private List<T> data;

  /** 전체 데이터 개수. */
  private int total;

  /** 현재 페이지 번호 (1부터 시작). */
  private int current;

  /** 한 페이지에 포함되는 데이터 개수. */
  private int pageSize;

  /** 전체 페이지 수. */
  private int totalPages;

  /** 다음 페이지가 존재하는지 여부. */
  private boolean hasNext;

  /** 이전 페이지가 존재하는지 여부. */
  private boolean hasPrevious;

  /**
   * 생성자.
   *
   * @param data 현재 페이지 데이터
   * @param total 전체 데이터 개수
   * @param current 현재 페이지 번호
   * @param pageSize 페이지 크기
   */
  public PageResultDto(List<T> data, int total, int current, int pageSize) {
    this.data = data;
    this.total = total;
    this.current = current;
    this.pageSize = pageSize;
    calculatePagination();
  }

  /**
   * 페이징 관련 필드를 계산합니다.
   *
   * <p>totalPages, hasNext, hasPrevious 값을 설정합니다.
   */
  private void calculatePagination() {
    this.totalPages = total > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    this.hasNext = current < totalPages;
    this.hasPrevious = current > 1;
  }

  /**
   * PageResult 객체를 생성합니다.
   *
   * @param data 현재 페이지 데이터
   * @param total 전체 데이터 개수
   * @param current 현재 페이지 번호
   * @param pageSize 페이지 크기
   * @param <T> 데이터 타입
   * @return PageResult 객체
   */
  public static <T> PageResultDto<T> of(List<T> data, int total, int current, int pageSize) {
    return new PageResultDto<>(data, total, current, pageSize);
  }

  /**
   * PageParams를 기반으로 PageResult 객체를 생성합니다.
   *
   * @param data 현재 페이지 데이터
   * @param total 전체 데이터 개수
   * @param pageParamsDto 요청 파라미터 ({@link PageParamsDto})
   * @param <T> 데이터 타입
   * @return PageResult 객체
   */
  public static <T> PageResultDto<T> of(List<T> data, int total, PageParamsDto pageParamsDto) {
    return new PageResultDto<>(
        data, total, pageParamsDto.getCurrent(), pageParamsDto.getPageSize());
  }

  /**
   * 함수형 인터페이스를 활용해 PageResult를 생성합니다.
   *
   * <p>데이터 조회와 카운트 조회를 별도의 Supplier로 받아 트랜잭션 내에서 실행할 수 있습니다.
   *
   * @param pageParamsDto 요청 파라미터 ({@link PageParamsDto})
   * @param dataSupplier 데이터 조회 함수
   * @param countSupplier 전체 개수 조회 함수
   * @param <T> 데이터 타입
   * @return PageResult 객체
   */
  public static <T> PageResultDto<T> from(
      PageParamsDto pageParamsDto,
      Supplier<List<T>> dataSupplier,
      Supplier<Integer> countSupplier) {
    List<T> data = dataSupplier.get();
    int total = countSupplier.get();
    return new PageResultDto<>(
        data, total, pageParamsDto.getCurrent(), pageParamsDto.getPageSize());
  }

  /**
   * 비어 있는 페이지 결과를 생성합니다.
   *
   * @param pageParamsDto 요청 파라미터 ({@link PageParamsDto})
   * @param <T> 데이터 타입
   * @return 빈 PageResult 객체
   */
  public static <T> PageResultDto<T> empty(PageParamsDto pageParamsDto) {
    return new PageResultDto<>(
        List.of(), 0, pageParamsDto.getCurrent(), pageParamsDto.getPageSize());
  }

  /**
   * 기본값(1페이지, 10개)으로 비어 있는 페이지 결과를 생성합니다.
   *
   * @param <T> 데이터 타입
   * @return 빈 PageResult 객체
   */
  public static <T> PageResultDto<T> empty() {
    return new PageResultDto<>(List.of(), 0, 1, 10);
  }

  /**
   * 현재 페이지에 데이터가 있는지 확인합니다.
   *
   * @return 데이터가 존재하면 true
   */
  public boolean hasData() {
    return data != null && !data.isEmpty();
  }

  /**
   * 현재 페이지가 첫 번째 페이지인지 확인합니다.
   *
   * @return 첫 번째 페이지면 true
   */
  public boolean isFirstPage() {
    return current == 1;
  }

  /**
   * 현재 페이지가 마지막 페이지인지 확인합니다.
   *
   * @return 마지막 페이지면 true
   */
  public boolean isLastPage() {
    return current == totalPages;
  }
}
