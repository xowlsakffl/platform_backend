package com.platform.application.media.storage;

import java.io.InputStream;

public record MediaContent(InputStream inputStream, long contentLength) {
}
