package com.platform.application.cache;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class StaffSummaryCacheInvalidator {

	private final StaffSummaryCache cache;

	public StaffSummaryCacheInvalidator(StaffSummaryCache cache) {
		this.cache = cache;
	}

	public void forgetAfterCommit(String domain) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()
			|| !TransactionSynchronizationManager.isSynchronizationActive()) {
			cache.forget(domain);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				cache.forget(domain);
			}
		});
	}
}
