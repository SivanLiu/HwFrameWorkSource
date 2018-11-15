package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.a;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class n {
    private HashMap<String, Object> gm = new HashMap();
    protected Context gn = null;
    private String go = "";

    public n(Context context, String str) {
        this.gn = context.getApplicationContext();
        this.go = str;
    }

    public boolean setValue(String str, Object obj) {
        this.gm.put(str, obj);
        new a(this.gn, this.go).ea(str, obj);
        return true;
    }

    public boolean vu(Map<String, Object> map) {
        this.gm.putAll(map);
        vt();
        return true;
    }

    private Object vs(String str) {
        return this.gm.get(str);
    }

    private Object getValue(String str, Object obj) {
        Object vs = vs(str);
        if (vs == null) {
            return obj;
        }
        return vs;
    }

    public HashMap<String, Object> vq() {
        return this.gm;
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

    public boolean vr(String str, boolean z) {
        Object value = getValue(str, Boolean.valueOf(z));
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return z;
    }

    public String getString(String str, String str2) {
        return String.valueOf(getValue(str, str2));
    }

    protected HashMap<String, Object> vp() {
        this.gm.clear();
        HashMap<String, Object> hashMap = new HashMap();
        for (Entry entry : new a(this.gn, this.go).getAll().entrySet()) {
            hashMap.put((String) entry.getKey(), entry.getValue());
        }
        if (hashMap.size() != 0) {
            this.gm = hashMap;
        }
        return hashMap;
    }

    private boolean vt() {
        new a(this.gn, this.go).ei(this.gm);
        return true;
    }

    public String toString() {
        String str = " ";
        String str2 = ":";
        StringBuffer stringBuffer = new StringBuffer();
        for (Entry entry : this.gm.entrySet()) {
            stringBuffer.append((String) entry.getKey()).append(str2).append(entry.getValue()).append(str);
        }
        return stringBuffer.toString();
    }
}
