package com.medi.adapter.in.web.staff.media.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.medi.application.media.command.UpdateMediaCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;

public final class MediaUpdateRequest {

	@Min(0)
	@Max(100_000)
	private Integer sortOrder;

	private Boolean primary;
	private Map<String, Object> metadata;
	private boolean metadataSpecified;

	@JsonSetter("sort_order")
	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	@JsonSetter("is_primary")
	public void setPrimary(Boolean primary) {
		this.primary = primary;
	}

	@JsonSetter("metadata")
	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
		this.metadataSpecified = true;
	}

	public UpdateMediaCommand toCommand() {
		return new UpdateMediaCommand(
			sortOrder,
			primary,
			metadata,
			metadataSpecified
		);
	}
}
