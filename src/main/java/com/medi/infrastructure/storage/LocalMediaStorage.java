package com.medi.infrastructure.storage;

import com.medi.application.media.storage.MediaContent;
import com.medi.application.media.storage.MediaFileSource;
import com.medi.application.media.storage.MediaStorage;
import com.medi.application.media.storage.MediaStorageException;
import com.medi.application.media.storage.StoredMediaFile;
import com.medi.common.config.MediaStorageProperties;
import com.medi.domain.media.MediaDisk;
import jakarta.annotation.PostConstruct;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LocalMediaStorage implements MediaStorage {

	private static final String BINARY_CONTENT_TYPE = "application/octet-stream";
	private static final int HEADER_SIZE = 16;
	private static final int COPY_BUFFER_SIZE = 8192;
	private static final int MAX_IMAGE_DIMENSION = 20_000;
	private static final Map<String, String> EXTENSIONS = Map.of(
		"image/jpeg", "jpg",
		"image/png", "png",
		"image/webp", "webp",
		"image/gif", "gif",
		"application/pdf", "pdf"
	);

	private final Path root;
	private final long maxFileSize;
	private final Set<String> allowedContentTypes;

	public LocalMediaStorage(MediaStorageProperties properties) {
		this.root = Path.of(properties.root()).toAbsolutePath().normalize();
		this.maxFileSize = properties.maxFileSize().toBytes();
		this.allowedContentTypes = properties.allowedContentTypes().stream()
			.map(value -> value.toLowerCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());
	}

	@PostConstruct
	void initialize() {
		try {
			Files.createDirectories(root);
			Files.createDirectories(root.resolve(".tmp"));
		} catch (IOException exception) {
			throw new MediaStorageException(
				MediaStorageException.Reason.IO_ERROR,
				"미디어 저장 디렉토리를 준비할 수 없습니다.",
				exception
			);
		}
	}

	@Override
	public StoredMediaFile store(MediaFileSource source) {
		validateSource(source);
		String originalName = sanitizeOriginalName(source.originalFilename());
		Path temporaryPath = root.resolve(".tmp").resolve(UUID.randomUUID() + ".upload");

		try {
			long size = copy(source, temporaryPath);
			String detectedContentType = detectContentType(temporaryPath);
			validateContentType(source.contentType(), detectedContentType);
			Dimension dimension = readImageDimension(temporaryPath, detectedContentType);
			String relativePath = buildRelativePath(detectedContentType);
			Path destination = safeResolve(relativePath);
			Files.createDirectories(destination.getParent());
			move(temporaryPath, destination);

			return new StoredMediaFile(
				MediaDisk.LOCAL,
				relativePath,
				originalName,
				detectedContentType,
				size,
				dimension == null ? null : dimension.width,
				dimension == null ? null : dimension.height
			);
		} catch (MediaStorageException exception) {
			deleteQuietly(temporaryPath);
			throw exception;
		} catch (IOException exception) {
			deleteQuietly(temporaryPath);
			throw new MediaStorageException(
				MediaStorageException.Reason.IO_ERROR,
				"미디어 파일을 저장할 수 없습니다.",
				exception
			);
		}
	}

	@Override
	public MediaContent load(String path) {
		Path resolved = safeResolve(path);
		if (!Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS)) {
			throw new MediaStorageException(
				MediaStorageException.Reason.FILE_NOT_FOUND,
				"저장된 미디어 파일을 찾을 수 없습니다."
			);
		}

		try {
			return new MediaContent(Files.newInputStream(resolved), Files.size(resolved));
		} catch (IOException exception) {
			throw new MediaStorageException(
				MediaStorageException.Reason.IO_ERROR,
				"미디어 파일을 읽을 수 없습니다.",
				exception
			);
		}
	}

	@Override
	public void delete(String path) {
		try {
			Files.deleteIfExists(safeResolve(path));
		} catch (IOException exception) {
			throw new MediaStorageException(
				MediaStorageException.Reason.IO_ERROR,
				"미디어 파일을 삭제할 수 없습니다.",
				exception
			);
		}
	}

	private void validateSource(MediaFileSource source) {
		if (source == null || source.size() <= 0) {
			throw invalid("업로드할 파일이 비어 있습니다.");
		}
		if (source.size() > maxFileSize) {
			throw new MediaStorageException(
				MediaStorageException.Reason.FILE_TOO_LARGE,
				"업로드 가능한 파일 크기를 초과했습니다."
			);
		}
	}

	private long copy(MediaFileSource source, Path temporaryPath) throws IOException {
		long total = 0;
		byte[] buffer = new byte[COPY_BUFFER_SIZE];
		try (
			InputStream input = source.openStream();
			OutputStream output = Files.newOutputStream(temporaryPath, StandardOpenOption.CREATE_NEW)
		) {
			int read;
			while ((read = input.read(buffer)) != -1) {
				total += read;
				if (total > maxFileSize) {
					throw new MediaStorageException(
						MediaStorageException.Reason.FILE_TOO_LARGE,
						"업로드 가능한 파일 크기를 초과했습니다."
					);
				}
				output.write(buffer, 0, read);
			}
		}
		if (total == 0) {
			throw invalid("업로드할 파일이 비어 있습니다.");
		}
		return total;
	}

	private String detectContentType(Path path) throws IOException {
		byte[] header;
		try (InputStream input = Files.newInputStream(path)) {
			header = input.readNBytes(HEADER_SIZE);
		}
		if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
			return "image/jpeg";
		}
		if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
			return "image/png";
		}
		if (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a")) {
			return "image/gif";
		}
		if (startsWithAscii(header, "RIFF") && asciiAt(header, 8, "WEBP")) {
			return "image/webp";
		}
		if (startsWithAscii(header, "%PDF-")) {
			return "application/pdf";
		}
		throw invalid("허용되지 않거나 파일 형식을 확인할 수 없습니다.");
	}

	private void validateContentType(String claimedContentType, String detectedContentType) {
		if (!allowedContentTypes.contains(detectedContentType)) {
			throw invalid("허용되지 않는 파일 형식입니다.");
		}
		String claimed = normalizeContentType(claimedContentType);
		if (claimed != null && !BINARY_CONTENT_TYPE.equals(claimed) && !claimed.equals(detectedContentType)) {
			throw invalid("파일 내용과 Content-Type이 일치하지 않습니다.");
		}
	}

	private String normalizeContentType(String contentType) {
		if (!StringUtils.hasText(contentType)) {
			return null;
		}
		String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
	}

	private Dimension readImageDimension(Path path, String contentType) {
		if (!contentType.startsWith("image/") || "image/webp".equals(contentType)) {
			return null;
		}
		try (ImageInputStream imageInput = ImageIO.createImageInputStream(path.toFile())) {
			if (imageInput == null) {
				throw invalid("이미지 정보를 확인할 수 없습니다.");
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
			if (!readers.hasNext()) {
				throw invalid("이미지 정보를 확인할 수 없습니다.");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(imageInput, true, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
					throw invalid("허용되지 않는 이미지 크기입니다.");
				}
				return new Dimension(width, height);
			} finally {
				reader.dispose();
			}
		} catch (IOException exception) {
			throw invalid("이미지 정보를 확인할 수 없습니다.");
		}
	}

	private String buildRelativePath(String contentType) {
		LocalDate today = LocalDate.now();
		return "%04d/%02d/%s.%s".formatted(
			today.getYear(),
			today.getMonthValue(),
			UUID.randomUUID(),
			EXTENSIONS.get(contentType)
		);
	}

	private Path safeResolve(String relativePath) {
		if (!StringUtils.hasText(relativePath)) {
			throw new MediaStorageException(MediaStorageException.Reason.FILE_NOT_FOUND, "미디어 경로가 비어 있습니다.");
		}
		Path resolved = root.resolve(relativePath).normalize();
		if (!resolved.startsWith(root)) {
			throw invalid("미디어 경로가 올바르지 않습니다.");
		}
		return resolved;
	}

	private String sanitizeOriginalName(String originalName) {
		String normalized = StringUtils.hasText(originalName) ? originalName.replace('\\', '/') : "file";
		normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
		normalized = normalized.replaceAll("[\\p{Cntrl}]", "");
		if (normalized.isBlank()) {
			normalized = "file";
		}
		return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
	}

	private void move(Path source, Path destination) throws IOException {
		try {
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, destination);
		}
	}

	private boolean startsWith(byte[] bytes, int... expected) {
		if (bytes.length < expected.length) {
			return false;
		}
		for (int index = 0; index < expected.length; index++) {
			if (Byte.toUnsignedInt(bytes[index]) != expected[index]) {
				return false;
			}
		}
		return true;
	}

	private boolean startsWithAscii(byte[] bytes, String expected) {
		return asciiAt(bytes, 0, expected);
	}

	private boolean asciiAt(byte[] bytes, int offset, String expected) {
		if (bytes.length < offset + expected.length()) {
			return false;
		}
		for (int index = 0; index < expected.length(); index++) {
			if (bytes[offset + index] != (byte) expected.charAt(index)) {
				return false;
			}
		}
		return true;
	}

	private MediaStorageException invalid(String message) {
		return new MediaStorageException(MediaStorageException.Reason.INVALID_FILE, message);
	}

	private void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// 실패한 업로드 임시 파일은 운영 정리 작업에서 제거한다.
		}
	}
}
