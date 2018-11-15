package com.huawei.android.pushagent.model.token;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.metadata.TokenApplyReqMeta;
import com.huawei.android.pushagent.datatype.http.metadata.TokenApplyRspMeta;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.model.b.f;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.model.prefs.h;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.threadpool.a;
import java.util.ArrayList;
import java.util.List;

public class TokenApply {
    private static final String TAG = "PushLog3413";
    private Context appCtx;

    public TokenApply(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public static void execute(Context context) {
        if (k.rh(context).isValid()) {
            new TokenApply(context).apply();
            return;
        }
        c.ep(TAG, "apply token: TRS is invalid, so need to query TRS");
        f.yc(context).yd();
    }

    public void apply() {
        a.cf(new b(this));
    }

    public void responseToken(TokenApplyRsp tokenApplyRsp) {
        g.fr(this.appCtx, 100);
        Iterable<TokenApplyRspMeta> rets = tokenApplyRsp.getRets();
        if (rets != null && rets.size() != 0) {
            for (TokenApplyRspMeta tokenApplyRspMeta : rets) {
                if (tokenApplyRspMeta != null) {
                    if (tokenApplyRspMeta.isValid()) {
                        String pkgName = tokenApplyRspMeta.getPkgName();
                        String token = tokenApplyRspMeta.getToken();
                        String userId = tokenApplyRspMeta.getUserId();
                        com.huawei.android.pushagent.a.a.hq(65, pkgName);
                        delToRegApp(pkgName, userId);
                        c.er(TAG, "pushSrv response register token to " + pkgName);
                        if (com.huawei.android.pushagent.utils.tools.c.cr()) {
                            com.huawei.android.pushagent.utils.tools.c.cs(pkgName);
                        }
                        e.pn(this.appCtx).po(g.fs(pkgName, userId), token);
                        g.ft(this.appCtx, pkgName, userId, token);
                    } else if (tokenApplyRspMeta.isRemoveAble()) {
                        com.huawei.android.pushagent.a.a.hq(66, tokenApplyRspMeta.getPkgName());
                        c.ep(TAG, "PKG name is " + tokenApplyRspMeta.getPkgName() + ", ErrCode is" + tokenApplyRspMeta.getRet());
                        c.eq(TAG, "apply token error, remove info is complete");
                        delToRegApp(tokenApplyRspMeta.getPkgName(), tokenApplyRspMeta.getUserId());
                    } else {
                        com.huawei.android.pushagent.a.a.hq(66, tokenApplyRspMeta.getPkgName());
                        c.eq(TAG, "apply token error, errorCode is: " + tokenApplyRspMeta.getRet());
                    }
                }
            }
        }
    }

    private void delToRegApp(String str, String str2) {
        if (!TextUtils.isEmpty(str)) {
            String fs = g.fs(str, str2);
            if (TextUtils.isEmpty(h.qr(this.appCtx).qs(fs))) {
                c.ep(TAG, "not found record in pclient_request_info after token response, so remove all similar packagenames.");
                for (String fs2 : h.qr(this.appCtx).qt()) {
                    if (fs2.startsWith(str)) {
                        h.qr(this.appCtx).remove(fs2);
                    }
                }
            } else {
                h.qr(this.appCtx).remove(fs2);
            }
        }
    }

    private List<TokenApplyReqMeta> getTokenReqs() {
        List<TokenApplyReqMeta> arrayList = new ArrayList();
        for (String str : h.qr(this.appCtx).qt()) {
            String fu = g.fu(str);
            int fv = g.fv(str);
            TokenApplyReqMeta tokenApplyReqMeta = new TokenApplyReqMeta();
            tokenApplyReqMeta.setPkgName(fu);
            tokenApplyReqMeta.setUserId(com.huawei.android.pushagent.utils.a.fa(fv));
            arrayList.add(tokenApplyReqMeta);
            com.huawei.android.pushagent.a.a.hq(62, fu);
        }
        TokenApplyReqMeta ncTokenReq = getNcTokenReq();
        if (ncTokenReq != null) {
            arrayList.add(ncTokenReq);
        }
        return arrayList;
    }

    private TokenApplyReqMeta getNcTokenReq() {
        String fa = com.huawei.android.pushagent.utils.a.fa(com.huawei.android.pushagent.utils.a.fb());
        if (!TextUtils.isEmpty(e.pn(this.appCtx).pp(g.fs("com.huawei.android.pushagent", fa)))) {
            return null;
        }
        TokenApplyReqMeta tokenApplyReqMeta = new TokenApplyReqMeta();
        tokenApplyReqMeta.setPkgName("com.huawei.android.pushagent");
        tokenApplyReqMeta.setUserId(fa);
        com.huawei.android.pushagent.a.a.hq(62, "com.huawei.android.pushagent");
        return tokenApplyReqMeta;
    }
}
