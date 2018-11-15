package com.huawei.android.pushagent.model.token;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.metadata.TokenDelReqMeta;
import com.huawei.android.pushagent.datatype.http.metadata.TokenDelRspMeta;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.b.f;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.m;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class a {
    private Context appCtx;

    public a(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public static void execute(Context context) {
        if (k.rh(context).isValid()) {
            new a(context).ks();
            return;
        }
        c.ep("PushLog3413", "delete token: TRS is invalid, so need to query TRS");
        f.yc(context).yd();
    }

    public void ks() {
        com.huawei.android.pushagent.utils.threadpool.a.cf(new c(this));
    }

    private void ku(TokenDelRsp tokenDelRsp) {
        g.fr(this.appCtx, 100);
        Iterable<TokenDelRspMeta> rets = tokenDelRsp.getRets();
        if (rets != null && rets.size() != 0) {
            for (TokenDelRspMeta tokenDelRspMeta : rets) {
                if (tokenDelRspMeta != null) {
                    String token;
                    String vn;
                    if (tokenDelRspMeta.isValid()) {
                        token = tokenDelRspMeta.getToken();
                        vn = m.vg(this.appCtx).vn(token);
                        m.vg(this.appCtx).vk(token);
                        new com.huawei.android.pushagent.utils.f.a(this.appCtx, "push_notify_key").ed(vn);
                    } else if (tokenDelRspMeta.isRemoveAble()) {
                        c.ep("PushLog3413", "PKG name is " + tokenDelRspMeta.getPkgName() + ", ErrCode is" + tokenDelRspMeta.getRet());
                        c.eq("PushLog3413", "delete token error, errorCode is: 1, not reApply in future");
                        token = tokenDelRspMeta.getToken();
                        vn = m.vg(this.appCtx).vn(token);
                        m.vg(this.appCtx).vk(token);
                        new com.huawei.android.pushagent.utils.f.a(this.appCtx, "push_notify_key").ed(vn);
                    } else {
                        c.eq("PushLog3413", "del token error, errorCode is: " + tokenDelRspMeta.getRet());
                    }
                }
            }
        }
    }

    private List<TokenDelReqMeta> kt() {
        Iterable<Entry> vm = m.vg(this.appCtx).vm();
        List<TokenDelReqMeta> arrayList = new ArrayList();
        for (Entry entry : vm) {
            Object j = com.huawei.android.pushagent.utils.a.c.j((String) entry.getKey());
            String str = (String) entry.getValue();
            if (!(TextUtils.isEmpty(j) || TextUtils.isEmpty(str))) {
                arrayList.add(new TokenDelReqMeta(g.fu(str), j));
            }
        }
        return arrayList;
    }
}
