package site.icebang.domain.email.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import site.icebang.domain.email.dto.EmailRequestDto;
import site.icebang.domain.email.service.EmailService;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailTestController {
  private final EmailService emailService;

  @GetMapping("/test")
  @PreAuthorize("permitAll()")
  public ResponseEntity<String> sendTestEmail(@RequestParam String to) {
    try {
      EmailRequestDto emailRequestDto =
          EmailRequestDto.builder()
              .to(to)
              .subject("IceBang 실제 테스트 이메일")
              .body("안녕하세요!\n\nIceBang에서 보내는 실제 Gmail 테스트 이메일입니다.\n\n성공적으로 연동되었습니다!")
              .isHtml(false)
              .build();

      emailService.send(emailRequestDto);
      return ResponseEntity.ok("실제 Gmail 테스트 이메일 전송 완료! 받은편지함을 확인하세요!");

    } catch (Exception e) {
      return ResponseEntity.badRequest().body("이메일 전송 실패: " + e.getMessage());
    }
  }
}
