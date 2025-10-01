package site.icebang.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponseDto;
import site.icebang.domain.auth.dto.LoginRequestDto;
import site.icebang.domain.auth.dto.RegisterDto;
import site.icebang.domain.auth.model.AuthCredential;
import site.icebang.domain.auth.service.AuthService;

@RestController
@RequestMapping("/v0/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  private final AuthenticationManager authenticationManager;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponseDto<Void> register(@Valid @RequestBody RegisterDto registerDto) {
    authService.registerUser(registerDto);
    return ApiResponseDto.success(null);
  }

  @PostMapping("/login")
  public ApiResponseDto<?> login(
      @RequestBody LoginRequestDto request, HttpServletRequest httpRequest) {
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

    Authentication auth = authenticationManager.authenticate(token);

    SecurityContextHolder.getContext().setAuthentication(auth);

    HttpSession session = httpRequest.getSession(true);
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        SecurityContextHolder.getContext());

    return ApiResponseDto.success(null);
  }

  @GetMapping("/check-session")
  public ApiResponseDto<Boolean> checkSession(@AuthenticationPrincipal AuthCredential user) {
    return ApiResponseDto.success(user != null);
  }

  @GetMapping("/permissions")
  public ApiResponseDto<AuthCredential> getPermissions(@AuthenticationPrincipal AuthCredential user) {
    return ApiResponseDto.success(user);
  }

  @PostMapping("/logout")
  public ApiResponseDto<Void> logout(HttpServletRequest request) {
    // SecurityContext 정리
    SecurityContextHolder.clearContext();

    // 세션 무효화
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    return ApiResponseDto.success(null);
  }
}
