package com.huawei.android.pushagent.utils.b;

import android.net.Uri;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class b {
    String i;
    String j;
    final List<String> k = new ArrayList();
    int l;
    List<String> m = new ArrayList();
    String n;

    public b ak(String str) {
        if (TextUtils.isEmpty(str)) {
            return this;
        }
        Uri parse = Uri.parse(str);
        this.n = parse.getScheme();
        this.j = parse.getHost();
        this.l = parse.getPort();
        Collection pathSegments = parse.getPathSegments();
        if (pathSegments != null) {
            this.k.addAll(pathSegments);
        }
        Object query = parse.getQuery();
        if (!TextUtils.isEmpty(query)) {
            String[] split = query.split("&");
            for (Object add : split) {
                this.m.add(add);
            }
        }
        this.i = parse.getFragment();
        return this;
    }

    public b al(int i) {
        if (i != 0) {
            this.l = i;
        }
        return this;
    }

    public b ai(List<String> list) {
        if (list != null) {
            this.m.addAll(list);
        }
        return this;
    }

    public a aj() {
        return new a(this);
    }
}
