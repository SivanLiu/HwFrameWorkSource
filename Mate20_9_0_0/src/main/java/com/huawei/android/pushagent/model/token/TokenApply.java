package com.huawei.android.pushagent.model.token;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.metadata.TokenApplyReqMeta;
import com.huawei.android.pushagent.datatype.http.metadata.TokenApplyRspMeta;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.model.c.e;
import com.huawei.android.pushagent.model.prefs.a;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.threadpool.c;
import com.huawei.android.pushagent.utils.tools.b;
import java.util.ArrayList;
import java.util.List;

public class TokenApply {
    private static final String TAG = "PushLog3414";
    private Context appCtx;

    public TokenApply(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public static void execute(Context context) {
        if (a.ff(context).isValid()) {
            new TokenApply(context).apply();
            return;
        }
        com.huawei.android.pushagent.utils.b.a.sv(TAG, "apply token: TRS is invalid, so need to query TRS");
        e.pw(context).py();
    }

    public void apply() {
        c.sn(new c(this));
    }

    public void responseToken(TokenApplyRsp tokenApplyRsp) {
        d.yr(this.appCtx, 100);
        List<TokenApplyRspMeta> rets = tokenApplyRsp.getRets();
        if (rets != null && rets.size() != 0) {
            for (TokenApplyRspMeta tokenApplyRspMeta : rets) {
                if (tokenApplyRspMeta != null) {
                    if (tokenApplyRspMeta.isValid()) {
                        String pkgName = tokenApplyRspMeta.getPkgName();
                        String token = tokenApplyRspMeta.getToken();
                        String userId = tokenApplyRspMeta.getUserId();
                        com.huawei.android.pushagent.b.a.abc(65, pkgName);
                        delToRegApp(pkgName, userId);
                        com.huawei.android.pushagent.utils.b.a.st(TAG, "pushSrv response register token to " + pkgName);
                        if (b.sf()) {
                            b.sj(pkgName);
                        }
                        com.huawei.android.pushagent.model.prefs.b.il(this.appCtx).it(d.ys(pkgName, userId), token);
                        d.yt(this.appCtx, pkgName, userId, token);
                    } else if (tokenApplyRspMeta.isRemoveAble()) {
                        com.huawei.android.pushagent.b.a.abc(66, tokenApplyRspMeta.getPkgName());
                        com.huawei.android.pushagent.utils.b.a.sv(TAG, "PKG name is " + tokenApplyRspMeta.getPkgName() + ", ErrCode is" + tokenApplyRspMeta.getRet());
                        com.huawei.android.pushagent.utils.b.a.su(TAG, "apply token error, remove info is complete");
                        delToRegApp(tokenApplyRspMeta.getPkgName(), tokenApplyRspMeta.getUserId());
                    } else {
                        com.huawei.android.pushagent.b.a.abc(66, tokenApplyRspMeta.getPkgName());
                        com.huawei.android.pushagent.utils.b.a.su(TAG, "apply token error, errorCode is: " + tokenApplyRspMeta.getRet());
                    }
                }
            }
        }
    }

    private void delToRegApp(String str, String str2) {
        if (!TextUtils.isEmpty(str)) {
            String ys = d.ys(str, str2);
            if (TextUtils.isEmpty(j.lm(this.appCtx).lo(ys))) {
                com.huawei.android.pushagent.utils.b.a.sv(TAG, "not found record in pclient_request_info after token response, so remove all similar packagenames.");
                for (String ys2 : j.lm(this.appCtx).lp()) {
                    if (ys2.startsWith(str)) {
                        j.lm(this.appCtx).remove(ys2);
                    }
                }
            } else {
                j.lm(this.appCtx).remove(ys2);
            }
        }
    }

    private List<TokenApplyReqMeta> getTokenReqs() {
        ArrayList arrayList = new ArrayList();
        for (String str : j.lm(this.appCtx).lp()) {
            String yw = d.yw(str);
            int zh = d.zh(str);
            TokenApplyReqMeta tokenApplyReqMeta = new TokenApplyReqMeta();
            tokenApplyReqMeta.setPkgName(yw);
            tokenApplyReqMeta.setUserId(com.huawei.android.pushagent.utils.a.ya(zh));
            arrayList.add(tokenApplyReqMeta);
            com.huawei.android.pushagent.b.a.abc(62, yw);
        }
        TokenApplyReqMeta ncTokenReq = getNcTokenReq();
        if (ncTokenReq != null) {
            arrayList.add(ncTokenReq);
        }
        return arrayList;
    }

    private TokenApplyReqMeta getNcTokenReq() {
        String ya = com.huawei.android.pushagent.utils.a.ya(com.huawei.android.pushagent.utils.a.xy());
        if (!TextUtils.isEmpty(com.huawei.android.pushagent.model.prefs.b.il(this.appCtx).ik(d.ys("com.huawei.android.pushagent", ya)))) {
            return null;
        }
        TokenApplyReqMeta tokenApplyReqMeta = new TokenApplyReqMeta();
        tokenApplyReqMeta.setPkgName("com.huawei.android.pushagent");
        tokenApplyReqMeta.setUserId(ya);
        com.huawei.android.pushagent.b.a.abc(62, "com.huawei.android.pushagent");
        return tokenApplyReqMeta;
    }
}
