package com.huawei.android.pushagent.utils.e;

import android.text.TextUtils;
import java.util.List;

public class b {
    final String ap;
    final String aq;
    final List<String> ar;
    final int as;
    final List<String> at;
    final String au;

    b(c cVar) {
        this.au = cVar.ba;
        this.aq = cVar.aw;
        this.as = cVar.ay;
        this.ar = cVar.ax;
        this.at = cVar.az;
        this.ap = cVar.av;
    }

    public String dj() {
        int i;
        int i2 = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.au).append("://").append(this.aq);
        if (this.as > 0) {
            stringBuilder.append(':').append(this.as);
        }
        stringBuilder.append('/');
        if (this.ar != null) {
            int size = this.ar.size();
            for (i = 0; i < size; i++) {
                stringBuilder.append((String) this.ar.get(i)).append('/');
            }
        }
        dk(stringBuilder, '/');
        if (this.at != null) {
            i = this.at.size();
            if (i > 0) {
                stringBuilder.append('?');
                while (i2 < i) {
                    stringBuilder.append((String) this.at.get(i2)).append('&');
                    i2++;
                }
                dk(stringBuilder, '&');
            }
        }
        if (!TextUtils.isEmpty(this.ap)) {
            stringBuilder.append('#').append(this.ap);
        }
        return stringBuilder.toString();
    }

    private static void dk(StringBuilder stringBuilder, char c) {
        if (stringBuilder != null && stringBuilder.lastIndexOf(String.valueOf(c)) == stringBuilder.length() - 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
    }
}
