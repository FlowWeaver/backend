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

import site.icebang.common.dto.ApiResponse;
import site.icebang.domain.auth.dto.ChangePasswordRequestDto;
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
  public ApiResponse<Void> register(@Valid @RequestBody RegisterDto registerDto) {
    authService.registerUser(registerDto);
    return ApiResponse.success(null);
  }

  @PostMapping("/login")
  public ApiResponse<?> login(
      @RequestBody LoginRequestDto request, HttpServletRequest httpRequest) {
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

    Authentication auth = authenticationManager.authenticate(token);

    SecurityContextHolder.getContext().setAuthentication(auth);

    HttpSession session = httpRequest.getSession(true);
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        SecurityContextHolder.getContext());

    return ApiResponse.success(null);
  }

  @GetMapping("/check-session")
  public ApiResponse<Boolean> checkSession(@AuthenticationPrincipal AuthCredential user) {
    return ApiResponse.success(user != null);
  }

  @GetMapping("/permissions")
  public ApiResponse<AuthCredential> getPermissions(@AuthenticationPrincipal AuthCredential user) {
    return ApiResponse.success(user);
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(HttpServletRequest request) {
    // SecurityContext 정리
    SecurityContextHolder.clearContext();

    // 세션 무효화
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    return ApiResponse.success(null);
  }

  @PatchMapping("/change-password")
  public ApiResponse<Void> changePassword(
      @Valid @RequestBody ChangePasswordRequestDto request,
      @AuthenticationPrincipal AuthCredential user) {

    authService.changePassword(user.getEmail(), request);
    return ApiResponse.success(null);
  }
}
