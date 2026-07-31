package com.medi.application.cache;

import java.util.function.Supplier;

public interface StaffSummaryCache {

	String PARTNER = "partner";

	<T> T remember(String domain, Class<T> resultType, Supplier<T> resolver);

	void forget(String domain);
}
