package com.medi.application.media.storage;

import java.io.IOException;
import java.io.InputStream;

public interface MediaFileSource {

	String originalFilename();

	String contentType();

	long size();

	InputStream openStream() throws IOException;
}
