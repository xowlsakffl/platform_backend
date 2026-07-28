package com.medi.application.category.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryDeletedResult(@JsonProperty("deleted_id") Long deletedId) {
}
