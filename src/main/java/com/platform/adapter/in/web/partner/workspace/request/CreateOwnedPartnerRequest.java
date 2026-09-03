package com.platform.adapter.in.web.partner.workspace.request;

import com.platform.adapter.in.web.staff.partner.request.PartnerRequestSupport;
import com.platform.application.partner.command.CreateOwnedPartnerCommand;
import com.platform.common.web.multipart.MultipartMediaFileSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record CreateOwnedPartnerRequest(
	@NotBlank @Size(max = 30) String name,
	@BindParam("english_name") @Size(max = 90) String englishName,
	@BindParam("category_id") @NotNull @Positive Long categoryId,
	@BindParam("representative_phone") @NotBlank
	@Pattern(regexp = PartnerRequestSupport.PHONE_PATTERN) String representativePhone,
	@BindParam("representative_email") @NotBlank @Email @Size(max = 255) String representativeEmail,
	@BindParam("business_number") @NotBlank
	@Pattern(regexp = PartnerRequestSupport.BUSINESS_NUMBER_PATTERN) String businessNumber,
	@BindParam("company_name") @NotBlank @Size(max = 255) String companyName,
	@BindParam("ceo_name") @NotBlank @Size(max = 100) String ceoName,
	@BindParam("opening_date") LocalDate openingDate,
	@BindParam("road_address") @NotBlank @Size(max = 255) String roadAddress,
	@BindParam("detail_address") @Size(max = 255) String detailAddress,
	@Size(max = 50) String latitude,
	@Size(max = 50) String longitude,
	@BindParam("business_registration_file") @NotNull MultipartFile businessRegistrationFile
) {

	public CreateOwnedPartnerCommand toCommand() {
		return new CreateOwnedPartnerCommand(
			name,
			englishName,
			categoryId,
			representativePhone,
			representativeEmail,
			businessNumber,
			companyName,
			ceoName,
			openingDate,
			roadAddress,
			detailAddress,
			latitude,
			longitude,
			MultipartMediaFileSource.from(businessRegistrationFile)
		);
	}
}
