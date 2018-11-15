package com.hwawei.application.hwpay;

public class TokenInfo {
    public static final String TOKEN_TYPE_ACCESS_TOKEN = "AccessToken";
    private String mCredential;
    private String mType;

    public TokenInfo(String type, String hash) {
        this.mType = type;
        this.mCredential = hash;
    }

    public String getType() {
        return this.mType;
    }

    public String getCredential() {
        return this.mCredential;
    }

    public String getToken() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mType);
        stringBuilder.append(" ");
        stringBuilder.append(this.mCredential);
        return stringBuilder.toString();
    }
}
