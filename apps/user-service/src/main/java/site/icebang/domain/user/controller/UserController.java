package site.icebang.domain.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponseDto;
import site.icebang.domain.auth.model.AuthCredential;
import site.icebang.domain.user.dto.CheckEmailRequestDto;
import site.icebang.domain.user.dto.CheckEmailResponseDto;
import site.icebang.domain.user.dto.UserProfileResponseDto;
import site.icebang.domain.user.service.UserService;

@RestController
@RequestMapping("/v0/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping("/check-email")
  public ApiResponseDto<CheckEmailResponseDto> checkEmailAvailable(
      @Valid @RequestBody CheckEmailRequestDto request) {
    Boolean available = !userService.isExistEmail(request);
    String message = available.equals(Boolean.TRUE) ? "사용 가능한 이메일입니다." : "이미 가입된 이메일입니다.";

    return ApiResponseDto.success(
        CheckEmailResponseDto.builder().available(available).build(), message);
  }

  @GetMapping("/me")
  public ApiResponseDto<UserProfileResponseDto> getUserProfile(
      @AuthenticationPrincipal AuthCredential user) {
    return ApiResponseDto.success(UserProfileResponseDto.from(user));
  }
}
