package com.huawei.android.pushagent.utils.e;

import android.net.Uri;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class c {
    String av;
    String aw;
    final List<String> ax = new ArrayList();
    int ay;
    List<String> az = new ArrayList();
    String ba;

    public c dl(String str) {
        if (TextUtils.isEmpty(str)) {
            return this;
        }
        Uri parse = Uri.parse(str);
        this.ba = parse.getScheme();
        this.aw = parse.getHost();
        this.ay = parse.getPort();
        Collection pathSegments = parse.getPathSegments();
        if (pathSegments != null) {
            this.ax.addAll(pathSegments);
        }
        Object query = parse.getQuery();
        if (!TextUtils.isEmpty(query)) {
            String[] split = query.split("&");
            for (Object add : split) {
                this.az.add(add);
            }
        }
        this.av = parse.getFragment();
        return this;
    }

    public c dn(int i) {
        if (i != 0) {
            this.ay = i;
        }
        return this;
    }

    public c dm(List<String> list) {
        if (list != null) {
            this.az.addAll(list);
        }
        return this;
    }

    public b do() {
        return new b(this);
    }
}
