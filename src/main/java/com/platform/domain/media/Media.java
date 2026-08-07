package com.platform.domain.media;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "media")
public class Media extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "owner_type", nullable = false, length = 50)
	private MediaOwnerType ownerType;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@Column(nullable = false, length = 50)
	private String collection;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MediaDisk disk;

	@Column(nullable = false, unique = true, length = 512)
	private String path;

	@Column(name = "original_name", nullable = false)
	private String originalName;

	@Column(name = "mime_type", nullable = false, length = 127)
	private String mimeType;

	@Column(nullable = false)
	private long size;

	private Integer width;

	private Integer height;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

	@Column(columnDefinition = "json")
	private String metadata;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected Media() {
	}

	public Media(
		MediaOwnerType ownerType,
		Long ownerId,
		String collection,
		MediaDisk disk,
		String path,
		String originalName,
		String mimeType,
		long size,
		Integer width,
		Integer height,
		int sortOrder,
		boolean primary,
		String metadata
	) {
		this.ownerType = ownerType;
		this.ownerId = ownerId;
		this.collection = collection;
		this.disk = disk;
		this.path = path;
		this.originalName = originalName;
		this.mimeType = mimeType;
		this.size = size;
		this.width = width;
		this.height = height;
		this.sortOrder = sortOrder;
		this.primary = primary;
		this.metadata = metadata;
	}

	public void changeSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void changePrimary(boolean primary) {
		this.primary = primary;
	}

	public void changeCollection(String collection) {
		this.collection = collection;
	}

	public void changeMetadata(String metadata) {
		this.metadata = metadata;
	}

	public void softDelete() {
		if (deletedAt == null) {
			deletedAt = LocalDateTime.now();
			primary = false;
		}
	}

	public Long id() {
		return id;
	}

	public MediaOwnerType ownerType() {
		return ownerType;
	}

	public Long ownerId() {
		return ownerId;
	}

	public String collection() {
		return collection;
	}

	public MediaDisk disk() {
		return disk;
	}

	public String path() {
		return path;
	}

	public String originalName() {
		return originalName;
	}

	public String mimeType() {
		return mimeType;
	}

	public long size() {
		return size;
	}

	public Integer width() {
		return width;
	}

	public Integer height() {
		return height;
	}

	public int sortOrder() {
		return sortOrder;
	}

	public boolean primary() {
		return primary;
	}

	public String metadata() {
		return metadata;
	}

	public LocalDateTime deletedAt() {
		return deletedAt;
	}
}
