package com.deliveranything.global.infra;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${custom.email.from}")
  private String fromEmail;

  /**
   * 이메일 인증 코드 발송 (비동기)
   */
  @Async
  public void sendVerificationEmail(String toEmail, String verificationCode) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject("[뭐든배달] 이메일 인증 코드");
      helper.setText(buildVerificationEmailContent(verificationCode), true);

      mailSender.send(message);
      log.info("인증 이메일 발송 완료: {}", toEmail);

    } catch (MessagingException e) {
      log.error("이메일 발송 실패: {}", toEmail, e);
      throw new RuntimeException("이메일 발송에 실패했습니다.", e);
    }
  }

  /**
   * 이메일 인증 코드 HTML 템플릿
   */
  private String buildVerificationEmailContent(String verificationCode) {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                    font-family: Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                }
                .container {
                    background-color: #f9f9f9;
                    border-radius: 10px;
                    padding: 30px;
                    border: 1px solid #ddd;
                }
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                .header h1 {
                    color: #4CAF50;
                    margin: 0;
                }
                .code-container {
                    background-color: #fff;
                    border: 2px dashed #4CAF50;
                    border-radius: 5px;
                    padding: 20px;
                    text-align: center;
                    margin: 20px 0;
                }
                .code {
                    font-size: 32px;
                    font-weight: bold;
                    color: #4CAF50;
                    letter-spacing: 5px;
                }
                .info {
                    background-color: #fff3cd;
                    border-left: 4px solid #ffc107;
                    padding: 15px;
                    margin: 20px 0;
                }
                .footer {
                    text-align: center;
                    margin-top: 30px;
                    color: #666;
                    font-size: 14px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🚀 뭐든배달</h1>
                    <p>이메일 인증 코드</p>
                </div>

                <p>안녕하세요,</p>
                <p>뭐든배달 서비스 이용을 위한 이메일 인증 코드입니다.</p>

                <div class="code-container">
                    <div class="code">%s</div>
                </div>

                <div class="info">
                    <strong>⚠️ 주의사항</strong><br>
                    • 이 코드는 <strong>10분 동안만</strong> 유효합니다.<br>
                    • 본인이 요청하지 않았다면 이 이메일을 무시하세요.<br>
                    • 인증 코드를 타인과 공유하지 마세요.
                </div>

                <p>감사합니다.</p>

                <div class="footer">
                    <p>본 메일은 발신 전용입니다.</p>
                    <p>&copy; 2025 뭐든배달. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(verificationCode);
  }

  /**
   * 비밀번호 재설정 이메일 발송 (선택사항)
   */
  @Async
  public void sendPasswordResetEmail(String toEmail, String resetToken) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject("[뭐든배달] 비밀번호 재설정");
      helper.setText(buildPasswordResetEmailContent(resetToken), true);

      mailSender.send(message);
      log.info("비밀번호 재설정 이메일 발송 완료: {}", toEmail);

    } catch (MessagingException e) {
      log.error("비밀번호 재설정 이메일 발송 실패: {}", toEmail, e);
      throw new RuntimeException("이메일 발송에 실패했습니다.", e);
    }
  }

  private String buildPasswordResetEmailContent(String resetToken) {
    String resetUrl = "https://www.deliver-anything.shop/reset-password?token=" + resetToken;

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                    font-family: Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                }
                .container {
                    background-color: #f9f9f9;
                    border-radius: 10px;
                    padding: 30px;
                    border: 1px solid #ddd;
                }
                .button {
                    display: inline-block;
                    padding: 15px 30px;
                    background-color: #4CAF50;
                    color: white;
                    text-decoration: none;
                    border-radius: 5px;
                    margin: 20px 0;
                }
                .info {
                    background-color: #fff3cd;
                    border-left: 4px solid #ffc107;
                    padding: 15px;
                    margin: 20px 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🔒 비밀번호 재설정</h1>
                <p>안녕하세요,</p>
                <p>비밀번호 재설정 요청을 받았습니다.</p>
                <p>아래 버튼을 클릭하여 새로운 비밀번호를 설정하세요.</p>

                <a href="%s" class="button">비밀번호 재설정하기</a>

                <div class="info">
                    <strong>⚠️ 주의사항</strong><br>
                    • 이 링크는 <strong>10분 동안만</strong> 유효합니다.<br>
                    • 본인이 요청하지 않았다면 이 이메일을 무시하세요.
                </div>

                <p>감사합니다.</p>
            </div>
        </body>
        </html>
        """.formatted(resetUrl);
  }
}