package com.huawei.android.pushagent.constant;

public enum HttpMethod {
    GET("GET"),
    POST("POST");
    
    private String method;

    private HttpMethod(String str) {
        this.method = str;
    }

    public String abk() {
        return this.method;
    }
}
