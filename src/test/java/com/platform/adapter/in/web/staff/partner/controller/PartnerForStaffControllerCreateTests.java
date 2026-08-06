package com.platform.adapter.in.web.staff.partner.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.platform.application.partner.PartnerForStaffService;
import com.platform.common.security.AuthenticatedActor;
import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class PartnerForStaffControllerCreateTests {

	private PartnerForStaffService service;
	private AuthenticatedActor actor;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(PartnerForStaffService.class);
		actor = mock(AuthenticatedActor.class);
		when(service.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
			.thenReturn(null);
		var validator = Validation.buildDefaultValidatorFactory().getValidator();
		mockMvc = MockMvcBuilders
			.standaloneSetup(new PartnerForStaffController(service))
			.setValidator(new org.springframework.validation.beanvalidation.SpringValidatorAdapter(validator))
			.setCustomArgumentResolvers(new AuthenticatedActorArgumentResolver(actor))
			.build();
	}

	@Test
	void bindsCompleteMultipartRegistrationRequest() throws Exception {
		String operationHours = """
			{"timezone":"Asia/Seoul","mon":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00","ends_next_day":false}],"breaks":[]},"tue":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00","ends_next_day":false}],"breaks":[]},"wed":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00","ends_next_day":false}],"breaks":[]},"thu":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00","ends_next_day":false}],"breaks":[]},"fri":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00","ends_next_day":false}],"breaks":[]},"sat":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00","ends_next_day":false}],"breaks":[]},"sun":{"is_closed":false,"is_24_hours":false,"periods":[{"start":"10:00","end":"19:00","ends_next_day":false}],"breaks":[]}}
			""";
		String options = """
			[{"category_id":2,"name":"여성 커트","description":"기본 커트","regular_price":50000,"sale_price":40000,"duration_minutes":60,"is_visible":true,"sort_order":0,"specialists":[]}]
			""";

		mockMvc.perform(multipart("/api/v1/staff/partners")
			.file(file("logo", "logo.png", MediaType.IMAGE_PNG_VALUE))
			.file(file("images[]", "shop.png", MediaType.IMAGE_PNG_VALUE))
			.file(file("business_registration_file", "registration.pdf", MediaType.APPLICATION_PDF_VALUE))
			.file(new MockMultipartFile(
				"options",
				"options.json",
				MediaType.APPLICATION_JSON_VALUE,
				options.getBytes(StandardCharsets.UTF_8)
			))
			.param("name", "플랫폼 헤어")
			.param("description", "업체 상세설명")
			.param("category_id", "1")
			.param("road_address", "서울 강남구 테헤란로 1")
			.param("latitude", "37.5001")
			.param("longitude", "127.0364")
			.param("operation_hours", operationHours)
			.param("holiday_policy", "{\"enabled\":false}")
			.param("representative_phone", "0212345678")
			.param("representative_email", "partner@platform.local")
			.param("business_number", "123-45-67890")
			.param("company_name", "플랫폼 헤어")
			.param("ceo_name", "홍길동")
			.param("business_type", "서비스업")
			.param("business_item", "미용업(일반)")
			.param("feature_ids[]", "1"))
			.andExpect(status().isOk());

		verify(service).create(
			org.mockito.ArgumentMatchers.same(actor),
			argThat(command ->
				command != null
					&& command.options().size() == 1
					&& "여성 커트".equals(command.options().getFirst().name())
					&& command.logo() != null
					&& command.mainImage() != null
					&& command.businessRegistrationFile() != null
			)
		);
	}

	private MockMultipartFile file(String field, String name, String contentType) {
		return new MockMultipartFile(field, name, contentType, new byte[]{1, 2, 3});
	}

	private record AuthenticatedActorArgumentResolver(AuthenticatedActor actor)
		implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType() == AuthenticatedActor.class;
		}

		@Override
		public Object resolveArgument(
			MethodParameter parameter,
			ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory
		) {
			return actor;
		}
	}
}
