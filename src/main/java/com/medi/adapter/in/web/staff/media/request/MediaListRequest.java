package com.medi.adapter.in.web.staff.media.request;

import com.medi.application.media.query.SearchMediaQuery;
import com.medi.domain.media.MediaOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.BindParam;

public record MediaListRequest(
	@BindParam("owner_type") @NotNull MediaOwnerType ownerType,
	@BindParam("owner_id") @NotNull @Positive Long ownerId,
	@NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{0,49}$") String collection
) {

	public SearchMediaQuery toQuery() {
		return new SearchMediaQuery(ownerType, ownerId, collection);
	}
}
