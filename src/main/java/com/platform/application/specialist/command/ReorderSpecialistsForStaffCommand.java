package com.platform.application.specialist.command;

import java.util.List;

public record ReorderSpecialistsForStaffCommand(List<Long> specialistIds) {
}
