package site.icebang.domain.email.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import site.icebang.domain.email.dto.EmailRequestDto;

@Slf4j
@Service
@Profile({"production"})
@RequiredArgsConstructor
// @ConditionalOnMissingBean(EmailService.class)
public class EmailServiceImpl implements EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String defaultSender;

  @Override
  public void send(EmailRequestDto request) {
    try {
      if (request.isHtml()) {
        sendHtmlEmail(request);
      } else {
        sendSimpleEmail(request);
      }
    } catch (Exception e) {
      log.error("❌ 이메일 전송 실패 - To: {}", request.getTo(), e);
      throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
    }
  }

  private void sendSimpleEmail(EmailRequestDto request) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(request.getTo());
      message.setSubject(request.getSubject());
      message.setText(request.getBody());
      message.setFrom(defaultSender);

      // CC 설정
      if (request.getCc() != null && !request.getCc().isEmpty()) {
        message.setCc(request.getCc().toArray(new String[0]));
      }

      // BCC 설정
      if (request.getBcc() != null && !request.getBcc().isEmpty()) {
        message.setBcc(request.getBcc().toArray(new String[0]));
      }

      mailSender.send(message);
      log.info("✅ 실제 Gmail 전송 성공! To: {}, Subject: {}", request.getTo(), request.getSubject());

    } catch (Exception e) {
      log.error("❌ Gmail 텍스트 전송 실패", e);
      throw e;
    }
  }

  private void sendHtmlEmail(EmailRequestDto request) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    helper.setTo(request.getTo());
    helper.setSubject(request.getSubject());
    helper.setText(request.getBody(), true); // HTML 모드
    helper.setFrom(defaultSender);

    // CC 설정
    if (request.getCc() != null && !request.getCc().isEmpty()) {
      helper.setCc(request.getCc().toArray(new String[0]));
    }

    // BCC 설정
    if (request.getBcc() != null && !request.getBcc().isEmpty()) {
      helper.setBcc(request.getBcc().toArray(new String[0]));
    }

    mailSender.send(message);
    log.info("✅ 실제 Gmail HTML 전송 성공! To: {}, Subject: {}", request.getTo(), request.getSubject());
  }
}
