package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyReq;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.c.a;
import org.json.JSONException;

public class c extends a<TokenApplyRsp> {
    private TokenApplyReq hj;

    public c(Context context, TokenApplyReq tokenApplyReq) {
        super(context);
        this.hj = tokenApplyReq;
    }

    protected String yt() {
        String yw = a.yw("pushtrs.push.hicloud.com", k.rh(yq()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(yw).append("/PushTokenServer/v4/pushtoken/apply");
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int yr() {
        return 5222;
    }

    protected String ys() {
        try {
            return a.bo(this.hj);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "fail to get reqContent");
            return null;
        }
    }

    protected TokenApplyRsp yu(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return (TokenApplyRsp) a.br(str, TokenApplyRsp.class, new Class[0]);
    }
}
