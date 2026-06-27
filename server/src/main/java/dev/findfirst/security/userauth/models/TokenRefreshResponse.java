package dev.findfirst.security.userauth.models;

public record TokenRefreshResponse(String tokenType, String accessToken, String refreshToken, String error) {
	public TokenRefreshResponse(String accessToken, String refreshToken) {
		this("Bearer", accessToken, refreshToken, null);
	}
}
