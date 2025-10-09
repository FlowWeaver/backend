package site.icebang.domain.email.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import site.icebang.domain.email.dto.EmailRequestDto;

@Service
@Profile({"develop", "test-e2e", "test-integration", "test-unit"})
@Slf4j
public class MockEmailService implements EmailService {

  @Override
  public void send(EmailRequestDto emailRequestDto) {
    log.info("Mock send mail to: {}", emailRequestDto.getTo());
    log.info("Subject: {}", emailRequestDto.getSubject());
    log.info("Body: {}", emailRequestDto.getBody());
  }
}
