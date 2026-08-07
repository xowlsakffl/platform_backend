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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
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
			[{"category_id":2,"name":"Hair cut","description":"Basic cut","regular_price":50000,"sale_price":40000,"duration_minutes":60,"is_visible":true,"sort_order":0,"specialists":[]}]
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
			.param("name", "Platform Salon")
			.param("description", "Partner registration request")
			.param("category_id", "1")
			.param("road_address", "1 Teheran-ro, Gangnam-gu, Seoul")
			.param("latitude", "37.5001")
			.param("longitude", "127.0364")
			.param("operation_hours", operationHours)
			.param("holiday_policy", "{\"enabled\":false}")
			.param("representative_phone", "0212345678")
			.param("representative_email", "partner@platform.local")
			.param("business_number", "123-45-67890")
			.param("company_name", "Platform Salon")
			.param("ceo_name", "Test Owner")
			.param("business_type", "Service")
			.param("business_item", "Hair salon")
			.param("feature_ids[]", "1"))
			.andExpect(status().isOk());

		verify(service).create(
			org.mockito.ArgumentMatchers.same(actor),
			argThat(command ->
				command != null
					&& command.options().size() == 1
					&& "Hair cut".equals(command.options().getFirst().name())
					&& command.logo() != null
					&& command.mainImage() != null
					&& command.businessRegistrationFile() != null
			)
		);
	}

	@Test
	void acceptsRegistrationWithoutOptions() throws Exception {
		mockMvc.perform(multipart("/api/v1/staff/partners")
			.file(file("logo", "logo.png", MediaType.IMAGE_PNG_VALUE))
			.file(file("images[]", "shop.png", MediaType.IMAGE_PNG_VALUE))
			.file(file("business_registration_file", "registration.pdf", MediaType.APPLICATION_PDF_VALUE))
			.param("name", "Platform Salon")
			.param("description", "Partner registration request")
			.param("category_id", "1")
			.param("road_address", "1 Teheran-ro, Gangnam-gu, Seoul")
			.param("latitude", "37.5001")
			.param("longitude", "127.0364")
			.param("operation_hours", "{}")
			.param("holiday_policy", "{\"enabled\":false}")
			.param("representative_phone", "0212345678")
			.param("representative_email", "partner@platform.local")
			.param("business_number", "123-45-67890")
			.param("company_name", "Platform Salon")
			.param("ceo_name", "Test Owner")
			.param("business_type", "Service")
			.param("business_item", "Hair salon")
			.param("feature_ids[]", "1"))
			.andExpect(status().isOk());

		verify(service).create(
			org.mockito.ArgumentMatchers.same(actor),
			argThat(command -> command != null && command.options().isEmpty())
		);
	}

	@Test
	void bindsBasicCardUpdateWithoutTouchingOtherCards() throws Exception {
		mockMvc.perform(patchMultipart("/api/v1/staff/partners/12/fields")
			.param("name", "Updated Salon")
			.param("description", "Updated partner description")
			.param("category_id", "1"))
			.andExpect(status().isOk());

		verify(service).update(
			org.mockito.ArgumentMatchers.same(actor),
			org.mockito.ArgumentMatchers.eq(12L),
			argThat(command ->
				command != null
					&& command.specified("name")
					&& command.specified("category_id")
					&& !command.specified("road_address")
					&& !command.specified("interior_images")
					&& !command.specified("operation_hours")
					&& command.businessRegistration() == null
			)
		);
	}

	@Test
	void bindsHoursCardUpdateWithoutTouchingOtherCards() throws Exception {
		mockMvc.perform(patchMultipart("/api/v1/staff/partners/12/fields")
			.param("operation_hours", "{\"timezone\":\"Asia/Seoul\"}")
			.param("operating_hours_notice", "Appointment only"))
			.andExpect(status().isOk());

		verify(service).update(
			org.mockito.ArgumentMatchers.same(actor),
			org.mockito.ArgumentMatchers.eq(12L),
			argThat(command ->
				command != null
					&& command.specified("operation_hours")
					&& command.specified("operating_hours_notice")
					&& !command.specified("holiday_policy")
					&& command.contacts() == null
					&& !command.specified("name")
					&& command.businessRegistration() == null
			)
		);
	}

	@Test
	void bindsBusinessRegistrationUpdateWithoutTouchingOtherSections() throws Exception {
		mockMvc.perform(patchMultipart("/api/v1/staff/partners/12/fields")
			.param("business_number", "123-45-67890")
			.param("company_name", "Updated Salon")
			.param("ceo_name", "Test Owner")
			.param("business_type", "Service")
			.param("business_item", "Hair salon"))
			.andExpect(status().isOk());

		verify(service).update(
			org.mockito.ArgumentMatchers.same(actor),
			org.mockito.ArgumentMatchers.eq(12L),
			argThat(command ->
				command != null
					&& command.specified("business_number")
					&& !command.specified("business_address")
					&& command.businessRegistration() != null
					&& !command.specified("name")
					&& !command.specified("operation_hours")
			)
		);
	}

	private MockMultipartFile file(String field, String name, String contentType) {
		return new MockMultipartFile(field, name, contentType, new byte[]{1, 2, 3});
	}

	private MockMultipartHttpServletRequestBuilder patchMultipart(String path) {
		return multipart(path).with(request -> {
			request.setMethod("PATCH");
			return request;
		});
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
