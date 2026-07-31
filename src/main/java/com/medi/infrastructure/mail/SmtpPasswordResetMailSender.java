package com.medi.infrastructure.mail;

import com.medi.application.auth.PasswordResetMailSender;
import com.medi.common.error.InternalApplicationException;
import com.medi.common.security.PasswordResetProperties;
import com.medi.domain.account.AccountActorType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.auth.password-reset", name = "mail-mode", havingValue = "smtp")
public class SmtpPasswordResetMailSender implements PasswordResetMailSender {

	private final JavaMailSender mailSender;
	private final PasswordResetProperties properties;

	public SmtpPasswordResetMailSender(JavaMailSender mailSender, PasswordResetProperties properties) {
		this.mailSender = mailSender;
		this.properties = properties;
	}

	@Override
	public void send(AccountActorType actorType, String recipient, String resetUrl, long expireMinutes) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(properties.mailFromAddress(), properties.mailFromName());
			helper.setTo(recipient);
			helper.setSubject("[" + properties.serviceName() + "] " + actorLabel(actorType) + " 비밀번호 재설정 안내");
			helper.setText(html(actorType, resetUrl, expireMinutes), true);
			mailSender.send(message);
		} catch (MessagingException | UnsupportedEncodingException | RuntimeException exception) {
			throw new InternalApplicationException("비밀번호 재설정 메일을 발송할 수 없습니다.", exception);
		}
	}

	private String html(AccountActorType actorType, String resetUrl, long expireMinutes) {
		String escapedUrl = escape(resetUrl);
		return """
			<!doctype html>
			<html lang="ko">
			<head><meta charset="utf-8"><title>비밀번호 재설정</title></head>
			<body style="margin:0;background:#f8fafc;font-family:Arial,'Malgun Gothic',sans-serif;color:#111827;">
			<table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:32px 16px;">
			<tr><td align="center"><table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
			style="max-width:560px;background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:32px;">
			<tr><td><h1 style="margin:0 0 16px;font-size:20px;">비밀번호 재설정 안내</h1>
			<p style="font-size:14px;line-height:1.7;">%s 비밀번호 재설정을 요청하셨습니다.</p>
			<p style="font-size:13px;color:#6b7280;">이 링크는 %d분 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해 주세요.</p>
			<p style="margin:24px 0;"><a href="%s" style="display:inline-block;border-radius:6px;background:#465fff;padding:12px 18px;font-weight:700;color:#fff;text-decoration:none;">비밀번호 재설정</a></p>
			<p style="font-size:12px;color:#9ca3af;word-break:break-all;">버튼이 동작하지 않으면 아래 주소를 브라우저에 입력해 주세요.<br>%s</p>
			</td></tr></table></td></tr></table></body></html>
			""".formatted(actorLabel(actorType), expireMinutes, escapedUrl, escapedUrl);
	}

	private String actorLabel(AccountActorType actorType) {
		return switch (actorType) {
			case STAFF -> "관리자 계정";
			case PARTNER -> "파트너 계정";
			case BEAUTY -> "뷰티 계정";
			case USER -> "일반 회원";
		};
	}

	private String escape(String value) {
		return value.replace("&", "&amp;")
			.replace("\"", "&quot;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}
