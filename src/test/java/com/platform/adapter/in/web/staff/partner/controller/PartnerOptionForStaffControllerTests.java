package com.platform.adapter.in.web.staff.partner.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.platform.application.partner.PartnerOptionForPartnerService;
import com.platform.common.security.AuthenticatedActor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class PartnerOptionForStaffControllerTests {

	private PartnerOptionForPartnerService service;
	private AuthenticatedActor actor;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(PartnerOptionForPartnerService.class);
		actor = mock(AuthenticatedActor.class);
		when(service.replaceForStaff(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.anyLong(),
			org.mockito.ArgumentMatchers.any()
		)).thenReturn(List.of());
		mockMvc = MockMvcBuilders
			.standaloneSetup(new PartnerOptionForStaffController(service))
			.setCustomArgumentResolvers(new AuthenticatedActorArgumentResolver(actor))
			.build();
	}

	@Test
	void replacesAllOptionsWithOneRequest() throws Exception {
		mockMvc.perform(put("/api/v1/staff/partners/12/options")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "options": [
				    {
				      "id": 31,
				      "category_id": 7,
				      "name": "Cut",
				      "description": "Basic cut",
				      "regular_price": 50000,
				      "sale_price": 40000,
				      "duration_minutes": 60,
				      "is_visible": true,
				      "sort_order": 0,
				      "specialists": []
				    }
				  ]
				}
				"""))
			.andExpect(status().isOk());

		verify(service).replaceForStaff(
			same(actor),
			eq(12L),
			argThat(command -> command.options().size() == 1
				&& command.options().getFirst().id() == 31L
				&& "Cut".equals(command.options().getFirst().value().name()))
		);
	}

	private record AuthenticatedActorArgumentResolver(AuthenticatedActor actor)
		implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(AuthenticatedActor.class);
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
