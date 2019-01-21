package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TokenDelReq;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.prefs.a;
import org.json.JSONException;

public class d extends b<TokenDelRsp> {
    private TokenDelReq ea;

    public d(Context context, TokenDelReq tokenDelReq) {
        super(context);
        this.ea = tokenDelReq;
    }

    protected String mx() {
        String na = b.na("pushtrs.push.hicloud.com", a.ff(mz()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(na).append("/PushTokenServer/v4/pushtoken/cancel");
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int mv() {
        return 5222;
    }

    protected String mw() {
        try {
            return com.huawei.android.pushagent.utils.d.a.uh(this.ea);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "fail to get reqContent");
            return null;
        }
    }

    protected TokenDelRsp my(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return (TokenDelRsp) com.huawei.android.pushagent.utils.d.a.ui(str, TokenDelRsp.class, new Class[0]);
    }
}
