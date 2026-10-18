package com.portfolio.drivecare.maintenance;

import java.net.URI;

/** Validate stored links only. The server never fetches the destination. */
final class PurchaseLink {
	private PurchaseLink() { }

	static String normalize(String value) {
		if (value == null || value.isBlank()) return null;
		String url = value.trim();
		try {
			URI uri = URI.create(url);
			if (url.length() > 2048 || uri.getHost() == null || uri.getHost().length() > 255
					|| uri.getUserInfo() != null
					|| !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
				throw new IllegalArgumentException();
			}
			return url;
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("구매 링크는 http:// 또는 https://로 시작하는 올바른 상품 주소를 입력해 주세요.");
		}
	}
}
