package com.platform.application.notice;

import com.platform.common.error.ApiException;
import com.platform.common.error.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class NoticeContentPolicy {
	private static final Pattern IMAGE = Pattern.compile("^/notice-media/([1-9][0-9]{0,17})$");
	private static final Pattern COLOR = Pattern.compile("(?i)^(#[0-9a-f]{3,8}|rgb\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*\\))$");
	private static final Safelist ALLOWED = Safelist.none()
		.addTags("p", "br", "h2", "h3", "strong", "b", "em", "i", "s", "u", "blockquote", "ul", "ol", "li", "a", "img", "span", "hr", "table", "thead", "tbody", "tr", "th", "td")
		.addAttributes("a", "href", "title").addProtocols("a", "href", "http", "https", "mailto")
		.addEnforcedAttribute("a", "rel", "noopener noreferrer").addEnforcedAttribute("a", "target", "_blank")
		.addAttributes("img", "src", "alt").addAttributes("p", "style").addAttributes("h2", "style")
		.addAttributes("h3", "style").addAttributes("span", "style")
		.addAttributes("td", "colspan", "rowspan").addAttributes("th", "colspan", "rowspan");

	public CleanContent clean(String raw) {
		if (raw == null || raw.length() > 100_000) throw invalid("본문은 100,000자 이내로 입력해 주세요.");
		Document document = new Cleaner(ALLOWED).clean(Jsoup.parseBodyFragment(raw));
		document.outputSettings().prettyPrint(false);
		var ids = new LinkedHashSet<Long>();
		for (var image : document.select("img")) {
			var matcher = IMAGE.matcher(image.attr("src"));
			if (!matcher.matches()) throw invalid("본문 이미지는 이미지 업로드로 등록해 주세요.");
			ids.add(Long.valueOf(matcher.group(1)));
		}
		if (ids.size() > 30 || document.select("img").size() > 30) throw invalid("본문 이미지는 최대 30개까지 등록할 수 있습니다.");
		for (var element : document.select("[style]")) {
			List<String> styles = new ArrayList<>();
			for (String declaration : element.attr("style").split(";")) {
				String[] pair = declaration.split(":", 2);
				if (pair.length != 2) continue;
				String key = pair[0].trim().toLowerCase(java.util.Locale.ROOT);
				String value = pair[1].trim();
				if ("color".equals(key) && COLOR.matcher(value).matches()) styles.add("color: " + value);
				if ("text-align".equals(key) && List.of("left", "center", "right", "justify").contains(value)) styles.add("text-align: " + value);
			}
			element.removeAttr("style");
			if (!styles.isEmpty()) element.attr("style", String.join("; ", styles));
		}
		for (var cell : document.select("[colspan], [rowspan]")) {
			for (String attr : List.of("colspan", "rowspan")) {
				if (cell.hasAttr(attr) && !cell.attr(attr).matches("[1-9]|[1-4][0-9]|50")) cell.removeAttr(attr);
			}
		}
		String plain = document.text().replace('\u00a0', ' ').trim();
		if (plain.isBlank() && ids.isEmpty()) throw invalid("본문을 입력해 주세요.");
		return new CleanContent(document.body().html(), plain, List.copyOf(ids));
	}

	private ApiException invalid(String message) { return new ApiException(ErrorCode.INVALID_REQUEST, message); }
	public record CleanContent(String html, String plainText, List<Long> imageIds) {}
}
