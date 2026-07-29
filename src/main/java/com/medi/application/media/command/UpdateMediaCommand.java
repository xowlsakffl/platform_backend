package com.medi.application.media.command;

import java.util.Map;

public record UpdateMediaCommand(
	Integer sortOrder,
	Boolean primary,
	Map<String, Object> metadata,
	boolean metadataSpecified
) {
}
