package com.huawei.nb.coordinator.helper.http;

import com.huawei.nb.coordinator.helper.http.HttpRequestBody.Parameter;
import com.huawei.nb.utils.logger.DSLog;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    private static final String DELETE_TYPE = "DELETE";
    private static final String GET_TYPE = "GET";
    private static final String POST_TYPE = "POST";
    private static final String TAG = "HttpRequest";
    private int connectTimeout = 10000;
    private int readTimeout = 10000;
    private String requestBodyString;
    private Map<String, String> requestHeaders;
    private String requestMethod = "GET";
    private String url;

    public static class Builder {
        private HttpRequest httpRequest = new HttpRequest();

        public Builder() {
            this.httpRequest.requestHeaders = new LinkedHashMap();
        }

        public Builder url(String url) {
            this.httpRequest.url = url;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.httpRequest.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.httpRequest.readTimeout = readTimeout;
            return this;
        }

        public Builder addRequestHeader(String k, String v) {
            this.httpRequest.requestHeaders.put(k, v);
            return this;
        }

        public Builder get(HttpRequestBody requsetBody) {
            this.httpRequest.requestMethod = "GET";
            this.httpRequest.requestBodyString = bodyString(requsetBody);
            return this;
        }

        public Builder post() {
            this.httpRequest.requestMethod = "POST";
            return this;
        }

        public Builder post(HttpRequestBody requestBody) {
            this.httpRequest.requestMethod = "POST";
            if (requestBody.useJson()) {
                this.httpRequest.requestBodyString = requestBody.getJsonBody();
            } else {
                this.httpRequest.requestBodyString = bodyString(requestBody);
            }
            return this;
        }

        public Builder delete(HttpRequestBody requestBody) {
            this.httpRequest.requestMethod = "DELETE";
            if (requestBody.useJson()) {
                this.httpRequest.requestBodyString = requestBody.getJsonBody();
            } else {
                this.httpRequest.requestBodyString = bodyString(requestBody);
            }
            return this;
        }

        private String bodyString(HttpRequestBody requsetBody) {
            List<Parameter> bodyList = requsetBody.getBodyList();
            StringBuilder builder = new StringBuilder();
            int size = bodyList.size();
            for (int i = 0; i < size; i++) {
                try {
                    String encode;
                    builder.append("&");
                    builder.append(((Parameter) bodyList.get(i)).getK());
                    builder.append("=");
                    if (this.httpRequest.requestMethod.equals("POST")) {
                        encode = URLEncoder.encode(((Parameter) bodyList.get(i)).getV(), "UTF-8");
                    } else {
                        encode = ((Parameter) bodyList.get(i)).getV();
                    }
                    builder.append(encode);
                } catch (UnsupportedEncodingException e) {
                    DSLog.e("HttpRequestHttpRequest post UnsupportedEncodingException! requestBodyString:" + this.httpRequest.requestBodyString, new Object[0]);
                }
            }
            if (size > 0) {
                builder.deleteCharAt(0);
            }
            return builder.toString();
        }

        public HttpRequest build() {
            return this.httpRequest;
        }
    }

    public String getUrl() {
        return this.url;
    }

    public String getRequestBodyString() {
        return this.requestBodyString;
    }

    public String getRequestMethod() {
        return this.requestMethod;
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public Map<String, String> getRequestHeaders() {
        return this.requestHeaders;
    }
}
