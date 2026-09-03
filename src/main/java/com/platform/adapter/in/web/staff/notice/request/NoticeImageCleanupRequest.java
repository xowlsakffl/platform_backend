package com.platform.adapter.in.web.staff.notice.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NoticeImageCleanupRequest(@JsonProperty("media_ids") @NotNull @Size(max = 100) List<@NotNull @Positive Long> mediaIds) {}
