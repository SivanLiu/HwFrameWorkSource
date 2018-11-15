package com.huawei.nb.coordinator.helper.http;

import com.huawei.nb.utils.logger.DSLog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse {
    private static final int DEFAULT_STATUS_CODE = -1;
    private static final String TAG = "HttpResponse";
    private Map<String, List<String>> headerFields = new LinkedHashMap();
    private String httpExceptionMsg = "";
    private boolean isDownloadStart = false;
    private String responseMsg = "";
    private long responseSize = 0;
    private String responseString = "";
    private int statusCode = -1;
    private String url = "";

    public long getResponseSize() {
        return this.responseSize;
    }

    public void setResponseSize(long responseSize) {
        this.responseSize = responseSize;
    }

    public String getHttpExceptionMsg() {
        return this.httpExceptionMsg;
    }

    public void setHttpExceptionMsg(String httpExceptionMsg) {
        this.httpExceptionMsg = httpExceptionMsg;
    }

    public String getResponseString() {
        return this.responseString;
    }

    public void setResponseString(String responseString) {
        this.responseString = responseString;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseMsg() {
        return this.responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }

    public String getHeaderValue(String headerKey) {
        List<String> tokenList = (List) this.headerFields.get(headerKey);
        StringBuilder builder = new StringBuilder();
        if (tokenList == null) {
            return builder.toString();
        }
        for (String s : tokenList) {
            builder.append(s);
        }
        return builder.toString();
    }

    public void setHeaderFields(Map<String, List<String>> headerFields) {
        if (headerFields == null) {
            DSLog.e("HttpResponse HeaderFields is empty.", new Object[0]);
        } else {
            this.headerFields.putAll(headerFields);
        }
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isDownloadStart() {
        return this.isDownloadStart;
    }

    public void setDownloadStart(boolean downloadStart) {
        this.isDownloadStart = downloadStart;
    }
}
