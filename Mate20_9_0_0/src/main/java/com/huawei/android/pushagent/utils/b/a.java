package com.huawei.android.pushagent.utils.b;

import android.text.TextUtils;
import java.util.List;

public class a {
    final String c;
    final String d;
    final List<String> e;
    final int f;
    final List<String> g;
    final String h;

    a(b bVar) {
        this.h = bVar.n;
        this.d = bVar.j;
        this.f = bVar.l;
        this.e = bVar.k;
        this.g = bVar.m;
        this.c = bVar.i;
    }

    public String ah() {
        int i;
        int i2 = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.h).append("://").append(this.d);
        if (this.f > 0) {
            stringBuilder.append(':').append(this.f);
        }
        stringBuilder.append('/');
        if (this.e != null) {
            int size = this.e.size();
            for (i = 0; i < size; i++) {
                stringBuilder.append((String) this.e.get(i)).append('/');
            }
        }
        ag(stringBuilder, '/');
        if (this.g != null) {
            i = this.g.size();
            if (i > 0) {
                stringBuilder.append('?');
                while (i2 < i) {
                    stringBuilder.append((String) this.g.get(i2)).append('&');
                    i2++;
                }
                ag(stringBuilder, '&');
            }
        }
        if (!TextUtils.isEmpty(this.c)) {
            stringBuilder.append('#').append(this.c);
        }
        return stringBuilder.toString();
    }

    private static void ag(StringBuilder stringBuilder, char c) {
        if (stringBuilder != null && stringBuilder.lastIndexOf(String.valueOf(c)) == stringBuilder.length() - 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
    }
}
