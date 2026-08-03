package com.platform.common.error;

public class InternalApplicationException extends RuntimeException {

	public InternalApplicationException(String message) {
		super(message);
	}

	public InternalApplicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
