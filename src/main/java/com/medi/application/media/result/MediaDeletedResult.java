package com.medi.application.media.result;

import java.time.LocalDateTime;

public record MediaDeletedResult(Long id, LocalDateTime deletedAt) {
}
