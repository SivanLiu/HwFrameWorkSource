package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TokenDelReq;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import org.json.JSONException;

public class a extends d<TokenDelRsp> {
    private TokenDelReq eu;

    public a(Context context, TokenDelReq tokenDelReq) {
        super(context);
        this.eu = tokenDelReq;
    }

    protected String tp() {
        String ts = d.ts("pushtrs.push.hicloud.com", i.mj(tr()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(ts).append("/PushTokenServer/v4/pushtoken/cancel");
        b.x("PushLog2976", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int tn() {
        return 5222;
    }

    protected String to() {
        try {
            return com.huawei.android.pushagent.utils.f.b.dt(this.eu);
        } catch (JSONException e) {
            b.y("PushLog2976", "fail to get reqContent");
            return null;
        }
    }

    protected TokenDelRsp tq(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return (TokenDelRsp) com.huawei.android.pushagent.utils.f.b.du(str, TokenDelRsp.class, new Class[0]);
    }
}
