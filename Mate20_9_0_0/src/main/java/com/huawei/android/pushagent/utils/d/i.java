package com.huawei.android.pushagent.utils.d;

class i implements b<Boolean, Object> {
    /* synthetic */ i(i iVar) {
        this();
    }

    private i() {
    }

    public Boolean vi(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return obj instanceof String ? Boolean.valueOf(Boolean.parseBoolean((String) obj)) : null;
    }
}
