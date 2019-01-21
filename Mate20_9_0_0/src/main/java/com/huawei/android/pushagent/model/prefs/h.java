package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.b;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class h {
    private HashMap<String, Object> cz = new HashMap();
    protected Context da = null;
    private String db = "";

    public h(Context context, String str) {
        this.da = context.getApplicationContext();
        this.db = str;
    }

    public boolean setValue(String str, Object obj) {
        this.cz.put(str, obj);
        new b(this.da, this.db).th(str, obj);
        return true;
    }

    public boolean lf(Map<String, Object> map) {
        this.cz.putAll(map);
        le();
        return true;
    }

    private Object ld(String str) {
        return this.cz.get(str);
    }

    private Object getValue(String str, Object obj) {
        Object ld = ld(str);
        if (ld == null) {
            return obj;
        }
        return ld;
    }

    public HashMap<String, Object> lb() {
        return this.cz;
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

    public boolean lc(String str, boolean z) {
        Object value = getValue(str, Boolean.valueOf(z));
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return z;
    }

    public String getString(String str, String str2) {
        return String.valueOf(getValue(str, str2));
    }

    protected HashMap<String, Object> la() {
        this.cz.clear();
        HashMap hashMap = new HashMap();
        for (Entry entry : new b(this.da, this.db).getAll().entrySet()) {
            hashMap.put((String) entry.getKey(), entry.getValue());
        }
        if (hashMap.size() != 0) {
            this.cz = hashMap;
        }
        return hashMap;
    }

    private boolean le() {
        new b(this.da, this.db).tn(this.cz);
        return true;
    }

    public String toString() {
        String str = " ";
        String str2 = ":";
        StringBuffer stringBuffer = new StringBuffer();
        for (Entry entry : this.cz.entrySet()) {
            stringBuffer.append((String) entry.getKey()).append(str2).append(entry.getValue()).append(str);
        }
        return stringBuffer.toString();
    }
}
