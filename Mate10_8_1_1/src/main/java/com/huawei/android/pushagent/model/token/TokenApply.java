package com.huawei.android.pushagent.model.token;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.b.a;
import com.huawei.android.pushagent.datatype.http.metadata.TokenApplyReqMeta;
import com.huawei.android.pushagent.datatype.http.metadata.TokenApplyRspMeta;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.model.c.c;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import java.util.ArrayList;
import java.util.List;

public class TokenApply {
    private static final String TAG = "PushLog2976";
    private Context appCtx;

    public TokenApply(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public static void execute(Context context) {
        if (i.mj(context).isValid()) {
            new TokenApply(context).apply();
            return;
        }
        b.z(TAG, "apply token: TRS is invalid, so need to query TRS");
        c.sp(context).sq(false);
    }

    public void apply() {
        com.huawei.android.pushagent.utils.threadpool.b.f(new b(this));
    }

    public void responseToken(TokenApplyRsp tokenApplyRsp) {
        f.gf(this.appCtx, 100);
        Iterable<TokenApplyRspMeta> rets = tokenApplyRsp.getRets();
        if (rets != null && rets.size() != 0) {
            for (TokenApplyRspMeta tokenApplyRspMeta : rets) {
                if (tokenApplyRspMeta != null) {
                    if (tokenApplyRspMeta.isValid()) {
                        String pkgName = tokenApplyRspMeta.getPkgName();
                        String token = tokenApplyRspMeta.getToken();
                        String userId = tokenApplyRspMeta.getUserId();
                        a.aaj(65, pkgName);
                        delToRegApp(pkgName, userId);
                        b.x(TAG, "pushSrv response register token to " + pkgName);
                        if (com.huawei.android.pushagent.utils.tools.a.j()) {
                            com.huawei.android.pushagent.utils.tools.a.k(pkgName);
                        }
                        com.huawei.android.pushagent.model.prefs.a.kc(this.appCtx).kk(f.fz(pkgName, userId), token);
                        f.gg(this.appCtx, pkgName, userId, token);
                    } else if (tokenApplyRspMeta.isRemoveAble()) {
                        a.aaj(66, tokenApplyRspMeta.getPkgName());
                        b.z(TAG, "PKG name is " + tokenApplyRspMeta.getPkgName() + ", ErrCode is" + tokenApplyRspMeta.getRet());
                        b.y(TAG, "apply token error, remove info is complete");
                        delToRegApp(tokenApplyRspMeta.getPkgName(), tokenApplyRspMeta.getUserId());
                    } else {
                        a.aaj(66, tokenApplyRspMeta.getPkgName());
                        b.y(TAG, "apply token error, errorCode is: " + tokenApplyRspMeta.getRet());
                    }
                }
            }
        }
    }

    private void delToRegApp(String str, String str2) {
        if (!TextUtils.isEmpty(str)) {
            String fz = f.fz(str, str2);
            if (TextUtils.isEmpty(j.pn(this.appCtx).po(fz))) {
                b.z(TAG, "not found record in pclient_request_info after token response, so remove all similar packagenames.");
                for (String fz2 : j.pn(this.appCtx).pp()) {
                    if (fz2.startsWith(str)) {
                        j.pn(this.appCtx).remove(fz2);
                    }
                }
            } else {
                j.pn(this.appCtx).remove(fz2);
            }
        }
    }

    private List<TokenApplyReqMeta> getTokenReqs() {
        List<TokenApplyReqMeta> arrayList = new ArrayList();
        for (String str : j.pn(this.appCtx).pp()) {
            String gh = f.gh(str);
            int gi = f.gi(str);
            TokenApplyReqMeta tokenApplyReqMeta = new TokenApplyReqMeta();
            tokenApplyReqMeta.setPkgName(gh);
            tokenApplyReqMeta.setUserId(com.huawei.android.pushagent.utils.a.fe(gi));
            arrayList.add(tokenApplyReqMeta);
            a.aaj(62, gh);
        }
        TokenApplyReqMeta ncTokenReq = getNcTokenReq();
        if (ncTokenReq != null) {
            arrayList.add(ncTokenReq);
        }
        return arrayList;
    }

    private TokenApplyReqMeta getNcTokenReq() {
        String fe = com.huawei.android.pushagent.utils.a.fe(com.huawei.android.pushagent.utils.a.fc());
        if (!TextUtils.isEmpty(com.huawei.android.pushagent.model.prefs.a.kc(this.appCtx).kb(f.fz("com.huawei.android.pushagent", fe)))) {
            return null;
        }
        TokenApplyReqMeta tokenApplyReqMeta = new TokenApplyReqMeta();
        tokenApplyReqMeta.setPkgName("com.huawei.android.pushagent");
        tokenApplyReqMeta.setUserId(fe);
        a.aaj(62, "com.huawei.android.pushagent");
        return tokenApplyReqMeta;
    }
}
