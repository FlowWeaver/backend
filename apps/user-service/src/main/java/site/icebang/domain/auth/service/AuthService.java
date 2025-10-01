package site.icebang.domain.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import site.icebang.common.exception.DuplicateDataException;
import site.icebang.common.utils.RandomPasswordGenerator;
import site.icebang.domain.auth.dto.RegisterDto;
import site.icebang.domain.auth.mapper.AuthMapper;
import site.icebang.domain.email.dto.EmailRequestDto;
import site.icebang.domain.email.service.EmailService;

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

    EmailRequestDto emailRequestDto =
        EmailRequestDto.builder()
            .to(registerDto.getEmail())
            .subject("[ice-bang] 비밀번호")
            .body(randomPassword)
            .build();

    emailService.send(emailRequestDto);
  }
}
