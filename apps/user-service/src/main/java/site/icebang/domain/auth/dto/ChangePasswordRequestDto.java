package site.icebang.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequestDto {
  @NotBlank(message = "현재 비밀번호는 필수입니다")
  private String currentPassword;

  @NotBlank(message = "새 비밀번호는 필수입니다")
  @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
  private String newPassword;

  @NotBlank(message = "새 비밀번호 확인은 필수입니다")
  private String confirmPassword;
}
