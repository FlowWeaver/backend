package site.icebang.domain.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckEmailResponseDto {
  private Boolean available;
}
