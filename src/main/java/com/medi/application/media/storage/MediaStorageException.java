package com.medi.application.media.storage;

public class MediaStorageException extends RuntimeException {

	private final Reason reason;

	public MediaStorageException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public MediaStorageException(Reason reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	public Reason reason() {
		return reason;
	}

	public enum Reason {
		INVALID_FILE,
		FILE_TOO_LARGE,
		FILE_NOT_FOUND,
		IO_ERROR
	}
}
