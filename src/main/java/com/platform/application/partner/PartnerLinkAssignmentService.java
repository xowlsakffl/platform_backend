package com.platform.application.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import com.platform.domain.partner.Partner;
import com.platform.domain.partner.PartnerLink;
import com.platform.domain.partner.PartnerLinkType;
import com.platform.infrastructure.persistence.partner.PartnerLinkRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartnerLinkAssignmentService {

	private final PartnerLinkRepository linkRepository;
	private final ObjectMapper objectMapper;

	public PartnerLinkAssignmentService(PartnerLinkRepository linkRepository, ObjectMapper objectMapper) {
		this.linkRepository = linkRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void replace(Partner partner, String linksJson) {
		List<LinkPayload> payloads = parse(linksJson);
		if (payloads.size() > PartnerLinkType.values().length) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "외부 링크 개수가 너무 많습니다.");
		}
		Set<PartnerLinkType> types = new LinkedHashSet<>();
		for (LinkPayload payload : payloads) {
			if (payload.type() == null || !types.add(payload.type())) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "같은 종류의 외부 링크는 한 번만 등록할 수 있습니다.");
			}
			validateUrl(payload.url());
		}

		linkRepository.deleteByPartner_Id(partner.id());
		linkRepository.flush();
		for (int index = 0; index < payloads.size(); index++) {
			LinkPayload payload = payloads.get(index);
			linkRepository.save(new PartnerLink(
				partner,
				payload.type(),
				payload.url().trim(),
				payload.sortOrder() == null ? index : payload.sortOrder()
			));
		}
	}

	private List<LinkPayload> parse(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readerForListOf(LinkPayload.class).readValue(json);
		} catch (JsonProcessingException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "외부 링크 형식이 올바르지 않습니다.");
		}
	}

	private void validateUrl(String value) {
		String normalized = value == null ? null : value.trim();
		if (!StringUtils.hasText(normalized) || normalized.length() > 1000) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "외부 링크 URL이 올바르지 않습니다.");
		}
		try {
			URI uri = new URI(normalized);
			if (uri.getHost() == null || uri.getScheme() == null
				|| !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
				throw new ApiException(ErrorCode.INVALID_REQUEST, "외부 링크는 HTTP 또는 HTTPS 주소만 사용할 수 있습니다.");
			}
		} catch (URISyntaxException exception) {
			throw new ApiException(ErrorCode.INVALID_REQUEST, "외부 링크 URL이 올바르지 않습니다.");
		}
	}

	private record LinkPayload(
		PartnerLinkType type,
		String url,
		@JsonProperty("sort_order") Integer sortOrder
	) {
	}
}
