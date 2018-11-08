package com.huawei.android.pushagent.model.token;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.metadata.TokenDelReqMeta;
import com.huawei.android.pushagent.datatype.http.metadata.TokenDelRspMeta;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.c.c;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class a {
    private Context appCtx;

    public a(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public static void execute(Context context) {
        if (i.mj(context).isValid()) {
            new a(context).tv();
            return;
        }
        b.z("PushLog2976", "delete token: TRS is invalid, so need to query TRS");
        c.sp(context).sq(false);
    }

    public void tv() {
        com.huawei.android.pushagent.utils.threadpool.b.f(new c(this));
    }

    private void tx(TokenDelRsp tokenDelRsp) {
        f.gf(this.appCtx, 100);
        Iterable<TokenDelRspMeta> rets = tokenDelRsp.getRets();
        if (rets != null && rets.size() != 0) {
            for (TokenDelRspMeta tokenDelRspMeta : rets) {
                if (tokenDelRspMeta != null) {
                    String token;
                    String qv;
                    if (tokenDelRspMeta.isValid()) {
                        token = tokenDelRspMeta.getToken();
                        qv = l.qo(this.appCtx).qv(token);
                        l.qo(this.appCtx).qq(token);
                        new com.huawei.android.pushagent.utils.a.c(this.appCtx, "push_notify_key").an(qv);
                    } else if (tokenDelRspMeta.isRemoveAble()) {
                        b.z("PushLog2976", "PKG name is " + tokenDelRspMeta.getPkgName() + ", ErrCode is" + tokenDelRspMeta.getRet());
                        b.y("PushLog2976", "delete token error, errorCode is: 1, not reApply in future");
                        token = tokenDelRspMeta.getToken();
                        qv = l.qo(this.appCtx).qv(token);
                        l.qo(this.appCtx).qq(token);
                        new com.huawei.android.pushagent.utils.a.c(this.appCtx, "push_notify_key").an(qv);
                    } else {
                        b.y("PushLog2976", "del token error, errorCode is: " + tokenDelRspMeta.getRet());
                    }
                }
            }
        }
    }

    private List<TokenDelReqMeta> tw() {
        Iterable<Entry> qu = l.qo(this.appCtx).qu();
        List<TokenDelReqMeta> arrayList = new ArrayList();
        for (Entry entry : qu) {
            Object bb = com.huawei.android.pushagent.utils.c.c.bb((String) entry.getKey());
            String str = (String) entry.getValue();
            if (!(TextUtils.isEmpty(bb) || TextUtils.isEmpty(str))) {
                arrayList.add(new TokenDelReqMeta(f.gh(str), bb));
            }
        }
        return arrayList;
    }
}
