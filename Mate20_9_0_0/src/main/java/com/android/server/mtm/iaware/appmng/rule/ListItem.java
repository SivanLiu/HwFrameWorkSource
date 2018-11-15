package com.android.server.mtm.iaware.appmng.rule;

import android.util.ArrayMap;
import java.io.PrintWriter;
import java.util.Map.Entry;

public class ListItem {
    private static final int UNINIT_VALUE = -1;
    private ArrayMap<String, Integer> mComplicatePolicy = null;
    private String mIndex = null;
    private int mPolicy = -1;

    public int getPolicy() {
        return this.mPolicy;
    }

    public int getPolicy(String key) {
        if (this.mComplicatePolicy == null) {
            return -1;
        }
        Integer policy = (Integer) this.mComplicatePolicy.get(key);
        if (policy == null) {
            return -1;
        }
        return policy.intValue();
    }

    public void setPolicy(int policy) {
        this.mPolicy = policy;
    }

    public void setComplicatePolicy(ArrayMap<String, Integer> policy) {
        this.mComplicatePolicy = policy;
    }

    public void dump(PrintWriter pw) {
        if (this.mPolicy != -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    policy = ");
            stringBuilder.append(this.mPolicy);
            pw.println(stringBuilder.toString());
        }
        if (this.mComplicatePolicy != null) {
            for (Entry<String, Integer> entry : this.mComplicatePolicy.entrySet()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    policy = ");
                stringBuilder2.append((String) entry.getKey());
                stringBuilder2.append(":");
                stringBuilder2.append(entry.getValue());
                pw.println(stringBuilder2.toString());
            }
        }
    }

    public void setIndex(String index) {
        this.mIndex = index;
    }

    public String getIndex() {
        return this.mIndex;
    }
}
