package com.huawei.android.pushagent.model.token;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.metadata.TokenDelReqMeta;
import com.huawei.android.pushagent.datatype.http.metadata.TokenDelRspMeta;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.c.e;
import com.huawei.android.pushagent.utils.b.b;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.threadpool.c;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class a {
    private Context appCtx;

    public a(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public static void execute(Context context) {
        if (com.huawei.android.pushagent.model.prefs.a.ff(context).isValid()) {
            new a(context).rr();
            return;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "delete token: TRS is invalid, so need to query TRS");
        e.pw(context).py();
    }

    public void rr() {
        c.sn(new b(this));
    }

    private void rt(TokenDelRsp tokenDelRsp) {
        d.yr(this.appCtx, 100);
        List<TokenDelRspMeta> rets = tokenDelRsp.getRets();
        if (rets != null && rets.size() != 0) {
            for (TokenDelRspMeta tokenDelRspMeta : rets) {
                if (tokenDelRspMeta != null) {
                    String token;
                    String jf;
                    if (tokenDelRspMeta.isValid()) {
                        token = tokenDelRspMeta.getToken();
                        jf = com.huawei.android.pushagent.model.prefs.d.ja(this.appCtx).jf(token);
                        com.huawei.android.pushagent.model.prefs.d.ja(this.appCtx).jc(token);
                        new b(this.appCtx, "push_notify_key").ti(jf);
                    } else if (tokenDelRspMeta.isRemoveAble()) {
                        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "PKG name is " + tokenDelRspMeta.getPkgName() + ", ErrCode is" + tokenDelRspMeta.getRet());
                        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "delete token error, errorCode is: 1, not reApply in future");
                        token = tokenDelRspMeta.getToken();
                        jf = com.huawei.android.pushagent.model.prefs.d.ja(this.appCtx).jf(token);
                        com.huawei.android.pushagent.model.prefs.d.ja(this.appCtx).jc(token);
                        new b(this.appCtx, "push_notify_key").ti(jf);
                    } else {
                        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "del token error, errorCode is: " + tokenDelRspMeta.getRet());
                    }
                }
            }
        }
    }

    private List<TokenDelReqMeta> rs() {
        Set<Entry> je = com.huawei.android.pushagent.model.prefs.d.ja(this.appCtx).je();
        ArrayList arrayList = new ArrayList();
        for (Entry entry : je) {
            String vt = com.huawei.android.pushagent.utils.e.a.vt((String) entry.getKey());
            String str = (String) entry.getValue();
            if (!(TextUtils.isEmpty(vt) || TextUtils.isEmpty(str))) {
                arrayList.add(new TokenDelReqMeta(d.yw(str), vt));
            }
        }
        return arrayList;
    }
}
