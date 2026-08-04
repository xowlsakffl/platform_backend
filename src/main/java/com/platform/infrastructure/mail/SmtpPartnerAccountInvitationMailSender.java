package com.platform.infrastructure.mail;

import com.platform.application.partner.PartnerAccountInvitationMailSender;
import com.platform.common.error.InternalApplicationException;
import com.platform.common.security.PartnerAccountInvitationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.partner-account-invitation", name = "mail-mode", havingValue = "smtp")
public class SmtpPartnerAccountInvitationMailSender implements PartnerAccountInvitationMailSender {

	private final JavaMailSender mailSender;
	private final PartnerAccountInvitationProperties properties;

	public SmtpPartnerAccountInvitationMailSender(
		JavaMailSender mailSender,
		PartnerAccountInvitationProperties properties
	) {
		this.mailSender = mailSender;
		this.properties = properties;
	}

	@Override
	public void send(
		String recipient,
		String partnerName,
		String setupUrl,
		long expireHours
	) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(properties.mailFromAddress(), properties.mailFromName());
			helper.setTo(recipient);
			helper.setSubject("[" + properties.serviceName() + "] 파트너 계정 생성 안내");
			helper.setText(html(partnerName, setupUrl, expireHours), true);
			mailSender.send(message);
		} catch (MessagingException | UnsupportedEncodingException | RuntimeException exception) {
			throw new InternalApplicationException("파트너 계정 초대 메일을 발송할 수 없습니다.", exception);
		}
	}

	private String html(String partnerName, String setupUrl, long expireHours) {
		String escapedPartner = escape(partnerName);
		String escapedUrl = escape(setupUrl);
		return """
			<!doctype html>
			<html lang="ko">
			<head><meta charset="utf-8"><title>파트너 계정 생성</title></head>
			<body style="margin:0;background:#f8fafc;font-family:Arial,'Malgun Gothic',sans-serif;color:#111827;">
			<table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:32px 16px;">
			<tr><td align="center"><table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
			style="max-width:560px;background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:32px;">
			<tr><td><h1 style="margin:0 0 16px;font-size:20px;">파트너 계정 생성 안내</h1>
			<p style="font-size:14px;line-height:1.7;">%s 업체의 관리 계정으로 초대되었습니다.</p>
			<p style="font-size:13px;color:#6b7280;">아래 링크는 %d시간 동안 유효하며 한 번만 사용할 수 있습니다.</p>
			<p style="margin:24px 0;"><a href="%s" style="display:inline-block;border-radius:6px;background:#465fff;padding:12px 18px;font-weight:700;color:#fff;text-decoration:none;">계정 만들기</a></p>
			<p style="font-size:12px;color:#9ca3af;word-break:break-all;">버튼이 동작하지 않으면 아래 주소를 브라우저에 입력해 주세요.<br>%s</p>
			</td></tr></table></td></tr></table></body></html>
			""".formatted(escapedPartner, expireHours, escapedUrl, escapedUrl);
	}

	private String escape(String value) {
		return value.replace("&", "&amp;")
			.replace("\"", "&quot;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}
