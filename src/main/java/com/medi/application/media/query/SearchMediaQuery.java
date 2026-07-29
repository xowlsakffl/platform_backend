package com.medi.application.media.query;

import com.medi.domain.media.MediaOwnerType;

public record SearchMediaQuery(
	MediaOwnerType ownerType,
	Long ownerId,
	String collection
) {
}
