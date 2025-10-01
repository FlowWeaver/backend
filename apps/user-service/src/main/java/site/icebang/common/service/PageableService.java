package site.icebang.common.service;

import site.icebang.common.dto.PageParamsDto;
import site.icebang.common.dto.PageResultDto;

/**
 * 페이징 가능한 서비스 인터페이스.
 *
 * <p>엔티티나 DTO 목록을 페이징 처리하여 반환해야 하는 서비스에서 구현합니다. 공통적으로 {@link PageParamsDto} 요청 파라미터를 받아 {@link
 * PageResultDto} 응답을 제공합니다.
 *
 * <p><b>사용 예시:</b>
 *
 * <pre>{@code
 * @Service
 * public class UserService implements PageableService<UserDto> {
 *
 *     private final UserRepository userRepository;
 *
 *     @Override
 *     public PageResult<UserDto> getPagedResult(PageParams pageParams) {
 *         List<UserDto> users = userRepository.findUsers(
 *             pageParams.getOffset(), pageParams.getPageSize()
 *         );
 *         int total = userRepository.countUsers();
 *         return PageResult.of(users, total, pageParams);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 페이징 처리할 데이터 타입
 * @author jys01012@gmail.com
 * @since v0.0.1-alpha
 */
public interface PageableService<T> {

  /**
   * 페이징 처리된 결과를 반환합니다.
   *
   * @param pageParamsDto 페이징 및 검색/정렬 요청 파라미터
   * @return 페이징 처리된 결과 객체
   */
  PageResultDto<T> getPagedResult(PageParamsDto pageParamsDto);
}
