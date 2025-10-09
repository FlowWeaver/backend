package site.icebang.common.dto;

import lombok.Data;

/**
 * 페이징, 검색, 정렬, 필터링을 위한 공통 매개변수 클래스입니다.
 *
 * <p>목록 조회 API에서 공통적으로 사용되는 요청 파라미터를 정의합니다. 현재 페이지 번호({@link #current}), 페이지 크기({@link #pageSize}),
 * 검색어({@link #search}), 정렬 조건({@link #sorters}), 필터 조건({@link #filters})를 포함합니다.
 *
 * <p><b>사용 예시:</b>
 *
 * <pre>{@code
 * PageParams params = new PageParams();
 * params.setCurrent(2);
 * params.setPageSize(20);
 * params.setSearch("회원");
 *
 * int offset = params.getOffset(); // 20
 * boolean searchable = params.hasSearch(); // true
 * }</pre>
 *
 * @author jys01012@gmail.com
 * @since v0.0.1-alpha
 * @see lombok.Data
 */
@Data
public class PageParamsDto {
  /**
   * 현재 페이지 번호 (1부터 시작).
   *
   * <p>1부터 시작하며, 기본값은 1입니다. 0 이하의 값은 유효하지 않습니다.
   */
  private int current = 1;

  /**
   * 한 펭지에 표시할 데이터 개수.
   *
   * <p>한 페이지에 표시할 항목의 개수를 지정합니다. 기본값은 10개이며, 일반적으로 10, 20, 50, 100 등의 값을 사용합니다.
   */
  private int pageSize = 10;

  /**
   * 검색어.
   *
   * <p>목록에서 특정 조건으로 검색할 때 사용되는 키워드입니다. {@code null}이거나 빈 문자열인 경우 검색 조건이 적용되지 않습니다.
   */
  private String search;

  /**
   * 정렬 조건 배열.
   *
   * <p>예: {@code ["name:asc", "createdAt:desc"]} API 설계에 따라 "필드명:정렬방향" 형식을 권장합니다. {@code null}이거나 빈
   * 배열인 경우 기본 정렬이 적용됩니다.
   */
  private String[] sorters;

  /**
   * 필터링 조건 배열.
   *
   * <p>예: {@code ["status:active", "role:admin"]} 각 요소는 특정 필드에 대한 필터링 조건을 나타냅니다. 형태는 구현에 따라 다를 수
   * 있습니다.
   */
  private String[] filters;

  /**
   * 페이징 처리를 위한 offset(시작 위치)을 계산합니다.
   *
   * @return (current - 1) * pageSize
   */
  public int getOffset() {
    return (current - 1) * pageSize;
  }

  /**
   * 검색어가 유효하게 존재하는지 확인합니다.
   *
   * @return 검색어가 null이 아니고 공백이 아닌 경우 true
   */
  public boolean hasSearch() {
    return search != null && !search.trim().isEmpty();
  }

  /**
   * 정렬 조건이 존재하는지 확인합니다.
   *
   * @return 정렬 조건 배열이 null이 아니고, 1개 이상 있는 경우 true
   */
  public boolean hasSorters() {
    return sorters != null && sorters.length > 0;
  }
}
