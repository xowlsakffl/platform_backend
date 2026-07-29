package com.medi.adapter.in.web.staff.media.request;

import com.medi.application.media.command.UploadMediaCommand;
import com.medi.application.media.storage.MediaFileSource;
import com.medi.domain.media.MediaOwnerType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record MediaUploadRequest(
	@BindParam("owner_type") @NotNull MediaOwnerType ownerType,
	@BindParam("owner_id") @NotNull @Positive Long ownerId,
	@NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{0,49}$") String collection,
	@BindParam("sort_order") @Min(0) @Max(100_000) Integer sortOrder,
	@BindParam("is_primary") Boolean primary,
	@Size(max = 10_000) String metadata,
	@NotNull MultipartFile file
) {

	public UploadMediaCommand toCommand() {
		return new UploadMediaCommand(
			ownerType,
			ownerId,
			collection,
			sortOrder,
			primary,
			metadata,
			new MultipartMediaFileSource(file)
		);
	}

	private record MultipartMediaFileSource(MultipartFile file) implements MediaFileSource {

		@Override
		public String originalFilename() {
			return file.getOriginalFilename();
		}

		@Override
		public String contentType() {
			return file.getContentType();
		}

		@Override
		public long size() {
			return file.getSize();
		}

		@Override
		public InputStream openStream() throws IOException {
			return file.getInputStream();
		}
	}
}
