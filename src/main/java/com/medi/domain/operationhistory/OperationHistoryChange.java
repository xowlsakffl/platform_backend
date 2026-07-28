package com.medi.domain.operationhistory;

import com.medi.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "operation_history_changes")
public class OperationHistoryChange extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "operation_history_id", nullable = false)
	private OperationHistory operationHistory;

	@Column(name = "field_key", nullable = false, length = 80)
	private String fieldKey;

	@Column(name = "before_value", columnDefinition = "text")
	private String beforeValue;

	@Column(name = "after_value", columnDefinition = "text")
	private String afterValue;

	protected OperationHistoryChange() {
	}

	OperationHistoryChange(OperationHistory operationHistory, String fieldKey, String beforeValue, String afterValue) {
		this.operationHistory = operationHistory;
		this.fieldKey = fieldKey;
		this.beforeValue = beforeValue;
		this.afterValue = afterValue;
	}

	public String fieldKey() {
		return fieldKey;
	}

	public String beforeValue() {
		return beforeValue;
	}

	public String afterValue() {
		return afterValue;
	}
}
