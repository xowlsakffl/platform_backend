package com.platform.infrastructure.persistence.operationhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.platform.domain.operationhistory.OperationHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
class OperationHistoryRepositoryTests {

	@Autowired
	private OperationHistoryRepository repository;

	@Test
	void findsLatestMatchingHistoryWithoutDistinctOrderByConflict() {
		OperationHistory first = history("DRAFT", "APPROVED");
		OperationHistory latest = history("APPROVED", "REJECTED");
		repository.saveAndFlush(first);
		repository.saveAndFlush(latest);

		assertThat(repository.findLatestIdsByChange(
			OperationHistory.TARGET_PARTNER,
			1L,
			"allowStatus",
			"REJECTED",
			PageRequest.of(0, 1)
		)).containsExactly(latest.id());
	}

	private OperationHistory history(String beforeValue, String afterValue) {
		OperationHistory history = new OperationHistory(
			OperationHistory.TARGET_PARTNER,
			1L,
			"STATUS_CHANGED",
			null,
			null
		);
		history.addChange("allowStatus", beforeValue, afterValue);
		return history;
	}
}
