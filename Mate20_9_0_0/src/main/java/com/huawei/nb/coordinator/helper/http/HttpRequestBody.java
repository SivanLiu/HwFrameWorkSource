package com.huawei.nb.coordinator.helper.http;

import java.util.ArrayList;
import java.util.List;

public class HttpRequestBody {
    private List<Parameter> bodyMap = new ArrayList();
    private String jsonBody;
    private boolean useJson = false;

    public static class Builder {
        private HttpRequestBody requestBody = new HttpRequestBody();

        public Builder add(String k, String v) {
            Parameter parameter = new Parameter();
            parameter.setKey(k);
            parameter.setValue(v);
            this.requestBody.bodyMap.add(parameter);
            return this;
        }

        public Builder addJsonBody(String jsonString) {
            this.requestBody.jsonBody = jsonString;
            this.requestBody.useJson = true;
            return this;
        }

        public HttpRequestBody build() {
            return this.requestBody;
        }
    }

    public static class Parameter {
        private String key;
        private String value;

        public void setKey(String key) {
            this.key = key;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getK() {
            return this.key;
        }

        public String getV() {
            return this.value;
        }
    }

    public List<Parameter> getBodyList() {
        return this.bodyMap;
    }

    public String getJsonBody() {
        return this.jsonBody;
    }

    public boolean useJson() {
        return this.useJson;
    }
}
