package site.icebang.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import site.icebang.common.exception.DuplicateDataException;
import site.icebang.common.utils.RandomPasswordGenerator;
import site.icebang.domain.auth.dto.ChangePasswordRequestDto;
import site.icebang.domain.auth.dto.RegisterDto;
import site.icebang.domain.auth.mapper.AuthMapper;
import site.icebang.domain.email.dto.EmailRequest;
import site.icebang.domain.email.service.EmailService;
import site.icebang.global.handler.exception.InvalidPasswordException;
import site.icebang.global.handler.exception.PasswordMismatchException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
  private final AuthMapper authMapper;
  private final RandomPasswordGenerator passwordGenerator;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  public void registerUser(RegisterDto registerDto) {
    if (authMapper.existsByEmail(registerDto.getEmail())) {
      throw new DuplicateDataException("이미 가입된 이메일입니다.");
    }
    String randomPassword = passwordGenerator.generate();
    String hashedPassword = passwordEncoder.encode(randomPassword);

    registerDto.setPassword(hashedPassword);
    registerDto.setStatus("PENDING");

    authMapper.insertUser(registerDto);

    // 2. user_organizations insert → userOrgId 반환
    authMapper.insertUserOrganization(registerDto);

    // 3. user_roles insert (foreach)
    if (registerDto.getRoleIds() != null && !registerDto.getRoleIds().isEmpty()) {
      authMapper.insertUserRoles(registerDto);
    }

    EmailRequest emailRequest =
        EmailRequest.builder()
            .to(registerDto.getEmail())
            .subject("[ice-bang] 비밀번호")
            .body(randomPassword)
            .build();

    emailService.send(emailRequest);
  }

  public void changePassword(String email, ChangePasswordRequestDto request) {
    // 1. 새 비밀번호와 확인 비밀번호 일치 검증
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new PasswordMismatchException("새 비밀번호가 일치하지 않습니다");
    }

    // 2. 사용자 조회
    String currentHashedPassword = authMapper.findPasswordByEmail(email);
    if (currentHashedPassword == null) {
      throw new IllegalArgumentException("사용자를 찾을 수 없습니다"); // 이건 그대로
    }

    // 3. 현재 비밀번호 검증
    if (!passwordEncoder.matches(request.getCurrentPassword(), currentHashedPassword)) {
      throw new InvalidPasswordException("현재 비밀번호가 올바르지 않습니다");
    }

    // 4. 새 비밀번호 해싱 및 업데이트
    String hashedNewPassword = passwordEncoder.encode(request.getNewPassword());
    authMapper.updatePassword(email, hashedNewPassword);
  }
}
