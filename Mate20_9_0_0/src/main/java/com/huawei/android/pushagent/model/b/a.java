package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyReq;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import org.json.JSONException;

public class a extends b<TokenApplyRsp> {
    private TokenApplyReq dy;

    public a(Context context, TokenApplyReq tokenApplyReq) {
        super(context);
        this.dy = tokenApplyReq;
    }

    protected String mx() {
        String na = b.na("pushtrs.push.hicloud.com", com.huawei.android.pushagent.model.prefs.a.ff(mz()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(na).append("/PushTokenServer/v4/pushtoken/apply");
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int mv() {
        return 5222;
    }

    protected String mw() {
        try {
            return com.huawei.android.pushagent.utils.d.a.uh(this.dy);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "fail to get reqContent");
            return null;
        }
    }

    protected TokenApplyRsp my(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return (TokenApplyRsp) com.huawei.android.pushagent.utils.d.a.ui(str, TokenApplyRsp.class, new Class[0]);
    }
}
