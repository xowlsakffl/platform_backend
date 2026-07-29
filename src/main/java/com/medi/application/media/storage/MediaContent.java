package com.medi.application.media.storage;

import java.io.InputStream;

public record MediaContent(InputStream inputStream, long contentLength) {
}
