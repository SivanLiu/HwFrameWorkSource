package com.huawei.nb.coordinator.helper.http;

import java.util.ArrayList;
import java.util.List;

public class HttpRequestBody {
    /* access modifiers changed from: private */
    public List<Parameter> bodies = new ArrayList();
    /* access modifiers changed from: private */
    public boolean isUseJson = false;
    /* access modifiers changed from: private */
    public String jsonBody;

    public List<Parameter> getBodyList() {
        return this.bodies;
    }

    public String getJsonBody() {
        return this.jsonBody;
    }

    public boolean useJson() {
        return this.isUseJson;
    }

    public static class Builder {
        private HttpRequestBody requestBody = new HttpRequestBody();

        public Builder add(String key, String value) {
            Parameter parameter = new Parameter();
            parameter.setKey(key);
            parameter.setValue(value);
            this.requestBody.bodies.add(parameter);
            return this;
        }

        public Builder addJsonBody(String jsonString) {
            String unused = this.requestBody.jsonBody = jsonString;
            boolean unused2 = this.requestBody.isUseJson = true;
            return this;
        }

        public HttpRequestBody build() {
            return this.requestBody;
        }
    }

    public static class Parameter {
        private String key;
        private String value;

        public void setKey(String key2) {
            this.key = key2;
        }

        public void setValue(String value2) {
            this.value = value2;
        }

        public String getK() {
            return this.key;
        }

        public String getV() {
            return this.value;
        }
    }
}
