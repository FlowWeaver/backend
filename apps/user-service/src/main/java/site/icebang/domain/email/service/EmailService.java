package site.icebang.domain.email.service;

import site.icebang.domain.email.dto.EmailRequestDto;

public interface EmailService {
  void send(EmailRequestDto emailRequestDto);
}
