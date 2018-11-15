package com.huawei.android.pushagent.utils.f;

class j implements c<Boolean, Object> {
    private j() {
    }

    public Boolean eu(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return obj instanceof String ? Boolean.valueOf(Boolean.parseBoolean((String) obj)) : null;
    }
}
