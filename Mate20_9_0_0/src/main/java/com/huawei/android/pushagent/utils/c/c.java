package com.huawei.android.pushagent.utils.c;

import android.net.Uri;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class c {
    String hh;
    String hi;
    final List<String> hj = new ArrayList();
    int hk;
    List<String> hl = new ArrayList();
    String hm;

    public c ud(String str) {
        if (TextUtils.isEmpty(str)) {
            return this;
        }
        Uri parse = Uri.parse(str);
        this.hm = parse.getScheme();
        this.hi = parse.getHost();
        this.hk = parse.getPort();
        List pathSegments = parse.getPathSegments();
        if (pathSegments != null) {
            this.hj.addAll(pathSegments);
        }
        String query = parse.getQuery();
        if (!TextUtils.isEmpty(query)) {
            String[] split = query.split("&");
            for (Object add : split) {
                this.hl.add(add);
            }
        }
        this.hh = parse.getFragment();
        return this;
    }

    public c uf(int i) {
        if (i != 0) {
            this.hk = i;
        }
        return this;
    }

    public c ue(List<String> list) {
        if (list != null) {
            this.hl.addAll(list);
        }
        return this;
    }

    public b ug() {
        return new b(this);
    }
}
