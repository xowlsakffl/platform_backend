package com.platform.domain.operationhistory;

import com.platform.domain.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "operation_histories")
public class OperationHistory extends BaseTimeEntity {

	public static final String TARGET_PARTNER = "PARTNER";
	public static final String TARGET_PARTNER_ACCOUNT = "PARTNER_ACCOUNT";
	public static final String TARGET_CATEGORY = "CATEGORY";
	public static final String TARGET_SPECIALIST = "SPECIALIST";
	public static final String TARGET_NOTICE = "NOTICE";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "target_type", nullable = false, length = 80)
	private String targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(name = "actor_type", nullable = false, length = 20)
	private String actorType = "STAFF";

	@Column(name = "actor_id")
	private Long actorId;

	@Column(name = "actor_name_snapshot", length = 100)
	private String actorNameSnapshot;

	@Column(name = "actor_login_id_snapshot", length = 100)
	private String actorLoginIdSnapshot;

	@Column(nullable = false, length = 60)
	private String action;

	@Column(length = 500)
	private String reason;

	@Column(length = 500)
	private String memo;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@OneToMany(mappedBy = "operationHistory", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OperationHistoryChange> changes = new ArrayList<>();

	protected OperationHistory() {
	}

	public OperationHistory(String targetType, Long targetId, String action, String reason, String memo) {
		this(targetType, targetId, "STAFF", null, action, reason, memo);
	}

	public OperationHistory(
		String targetType,
		Long targetId,
		String actorType,
		Long actorId,
		String action,
		String reason,
		String memo
	) {
		this.targetType = targetType;
		this.targetId = targetId;
		this.actorType = actorType;
		this.actorId = actorId;
		this.action = action;
		this.reason = reason;
		this.memo = memo;
	}

	public void addChange(String fieldKey, String beforeValue, String afterValue) {
		OperationHistoryChange change = new OperationHistoryChange(this, fieldKey, beforeValue, afterValue);
		this.changes.add(change);
	}

	public OperationHistory captureActor(String actorName, String actorLoginId) {
		this.actorNameSnapshot = actorName;
		this.actorLoginIdSnapshot = actorLoginId;
		return this;
	}

	public OperationHistory captureRequest(String ipAddress, String userAgent) {
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
		return this;
	}

	public Long id() {
		return id;
	}

	public String action() {
		return action;
	}

	public String actorType() {
		return actorType;
	}

	public Long actorId() {
		return actorId;
	}

	public String actorNameSnapshot() {
		return actorNameSnapshot;
	}

	public String actorLoginIdSnapshot() {
		return actorLoginIdSnapshot;
	}

	public String memo() {
		return memo;
	}

	public String ipAddress() {
		return ipAddress;
	}

	public String userAgent() {
		return userAgent;
	}

	public String reason() {
		return reason;
	}

	public List<OperationHistoryChange> changes() {
		return changes;
	}

	public Long targetId() {
		return targetId;
	}

	public String targetType() {
		return targetType;
	}
}
