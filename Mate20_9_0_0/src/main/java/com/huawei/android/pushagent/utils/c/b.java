package com.huawei.android.pushagent.utils.c;

import android.text.TextUtils;
import java.util.List;

public class b {
    final String hb;
    final String hc;
    final List<String> hd;
    final int he;
    final List<String> hf;
    final String hg;

    b(c cVar) {
        this.hg = cVar.hm;
        this.hc = cVar.hi;
        this.he = cVar.hk;
        this.hd = cVar.hj;
        this.hf = cVar.hl;
        this.hb = cVar.hh;
    }

    public String ub() {
        int i;
        int i2 = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.hg).append("://").append(this.hc);
        if (this.he > 0) {
            stringBuilder.append(':').append(this.he);
        }
        stringBuilder.append('/');
        if (this.hd != null) {
            int size = this.hd.size();
            for (i = 0; i < size; i++) {
                stringBuilder.append((String) this.hd.get(i)).append('/');
            }
        }
        uc(stringBuilder, '/');
        if (this.hf != null) {
            i = this.hf.size();
            if (i > 0) {
                stringBuilder.append('?');
                while (i2 < i) {
                    stringBuilder.append((String) this.hf.get(i2)).append('&');
                    i2++;
                }
                uc(stringBuilder, '&');
            }
        }
        if (!TextUtils.isEmpty(this.hb)) {
            stringBuilder.append('#').append(this.hb);
        }
        return stringBuilder.toString();
    }

    private static void uc(StringBuilder stringBuilder, char c) {
        if (stringBuilder != null && stringBuilder.lastIndexOf(String.valueOf(c)) == stringBuilder.length() - 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
    }
}
