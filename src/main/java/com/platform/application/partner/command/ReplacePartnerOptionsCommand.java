package com.platform.application.partner.command;

import java.util.List;

public record ReplacePartnerOptionsCommand(List<Item> options) {

	public record Item(Long id, SavePartnerOptionCommand value) {
	}
}
