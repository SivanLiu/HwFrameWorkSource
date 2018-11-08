package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.a.c;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class g {
    private HashMap<String, Object> dg = new HashMap();
    protected Context dh = null;
    private String di = "";

    public g(Context context, String str) {
        this.dh = context.getApplicationContext();
        this.di = str;
    }

    public boolean setValue(String str, Object obj) {
        this.dg.put(str, obj);
        new c(this.dh, this.di).am(str, obj);
        return true;
    }

    public boolean lx(Map<String, Object> map) {
        this.dg.putAll(map);
        mc();
        return true;
    }

    private Object ma(String str) {
        return this.dg.get(str);
    }

    private Object getValue(String str, Object obj) {
        Object ma = ma(str);
        if (ma == null) {
            return obj;
        }
        return ma;
    }

    public HashMap<String, Object> ly() {
        return this.dg;
    }

    public int getInt(String str, int i) {
        Object value = getValue(str, Integer.valueOf(i));
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        if (value instanceof Long) {
            return (int) ((Long) value).longValue();
        }
        return i;
    }

    public long getLong(String str, long j) {
        Object value = getValue(str, Long.valueOf(j));
        if (value instanceof Integer) {
            return (long) ((Integer) value).intValue();
        }
        if (value instanceof Long) {
            return ((Long) value).longValue();
        }
        return j;
    }

    public boolean lz(String str, boolean z) {
        Object value = getValue(str, Boolean.valueOf(z));
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return z;
    }

    public String getString(String str, String str2) {
        return String.valueOf(getValue(str, str2));
    }

    protected HashMap<String, Object> mb() {
        this.dg.clear();
        HashMap<String, Object> hashMap = new HashMap();
        for (Entry entry : new c(this.dh, this.di).getAll().entrySet()) {
            hashMap.put((String) entry.getKey(), entry.getValue());
        }
        if (hashMap.size() != 0) {
            this.dg = hashMap;
        }
        return hashMap;
    }

    private boolean mc() {
        new c(this.dh, this.di).ar(this.dg);
        return true;
    }

    public String toString() {
        String str = " ";
        String str2 = ":";
        StringBuffer stringBuffer = new StringBuffer();
        for (Entry entry : this.dg.entrySet()) {
            stringBuffer.append((String) entry.getKey()).append(str2).append(entry.getValue()).append(str);
        }
        return stringBuffer.toString();
    }
}
