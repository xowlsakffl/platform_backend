package com.medi.application.doctor.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record DoctorDeletedResult(Long id, @JsonProperty("deleted_at") LocalDateTime deletedAt) {
}
