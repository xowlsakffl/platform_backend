package com.platform.domain.notice;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
public class Notice extends BaseTimeEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Version
	private long version;
	@Column(nullable = false, length = 100)
	private String title;
	@Column(nullable = false, columnDefinition = "mediumtext")
	private String content;
	@Column(name = "plain_content", nullable = false, columnDefinition = "mediumtext")
	private String plainContent;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
	private NoticeStatus status;
	@Column(name = "publish_start_at")
	private LocalDateTime publishStartAt;
	@Column(name = "publish_end_at")
	private LocalDateTime publishEndAt;
	@Column(name = "is_pinned", nullable = false)
	private boolean pinned;
	@Column(name = "is_popup", nullable = false)
	private boolean popup;
	@Column(name = "author_staff_id", nullable = false)
	private Long authorStaffId;
	@Column(name = "author_name", nullable = false, length = 100)
	private String authorName;
	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;
	@Column(name = "revision_at", nullable = false)
	private LocalDateTime revisionAt;

	protected Notice() {}

	public Notice(Long authorStaffId, String authorName) {
		this.authorStaffId = authorStaffId;
		this.authorName = authorName;
	}

	public void update(String title, String content, String plainContent, NoticeStatus status,
		LocalDateTime start, LocalDateTime end, boolean pinned, boolean popup, LocalDateTime now) {
		this.title = title;
		this.content = content;
		this.plainContent = plainContent;
		this.status = status;
		this.publishStartAt = start;
		this.publishEndAt = end;
		this.pinned = pinned;
		this.popup = popup;
		this.revisionAt = now;
	}

	public NoticePublicationStatus publicationStatus(LocalDateTime now) {
		if (deletedAt != null || status == NoticeStatus.PRIVATE) return NoticePublicationStatus.HIDDEN;
		if (publishStartAt != null && now.isBefore(publishStartAt)) return NoticePublicationStatus.SCHEDULED;
		if (publishEndAt != null && !now.isBefore(publishEndAt)) return NoticePublicationStatus.ENDED;
		return NoticePublicationStatus.PUBLISHED;
	}

	public void softDelete(LocalDateTime now) { this.deletedAt = now; }
	public Long id() { return id; }
	public long version() { return version; }
	public String title() { return title; }
	public String content() { return content; }
	public String plainContent() { return plainContent; }
	public NoticeStatus status() { return status; }
	public LocalDateTime publishStartAt() { return publishStartAt; }
	public LocalDateTime publishEndAt() { return publishEndAt; }
	public boolean pinned() { return pinned; }
	public boolean popup() { return popup; }
	public Long authorStaffId() { return authorStaffId; }
	public String authorName() { return authorName; }
	public LocalDateTime deletedAt() { return deletedAt; }
}
