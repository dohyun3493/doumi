package com.doumi.donation.member.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Gmail SMTP를 통해 인증/안내 메일을 발송한다.
 * 실제 발송은 JavaMailSender(spring-boot-starter-mail)가 담당하고,
 * 여기서는 인증코드 메일 같은 도메인용 본문을 구성한다.
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String from;

    public MailService(JavaMailSender mailSender,
                       @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /** 회원가입 이메일 인증코드 메일 */
    public void sendSignupCode(String to, String code) {
        send(to,
                "[도우미] 회원가입 이메일 인증코드",
                "안녕하세요, 도우미입니다.\n\n"
                        + "아래 인증코드를 회원가입 화면에 입력해 주세요.\n\n"
                        + "인증코드: " + code + "\n\n"
                        + "이 코드는 5분간 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
    }

    /** 비밀번호 재설정 인증코드 메일 */
    public void sendPasswordResetCode(String to, String code) {
        send(to,
                "[도우미] 비밀번호 재설정 인증코드",
                "안녕하세요, 도우미입니다.\n\n"
                        + "비밀번호 재설정을 위한 인증코드입니다.\n\n"
                        + "인증코드: " + code + "\n\n"
                        + "이 코드는 30분간 유효합니다. 본인이 요청하지 않았다면 비밀번호를 변경하지 말고 이 메일을 무시해 주세요.");
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        log.info("메일 발송 완료: to={}, subject={}", to, subject);
    }
}
