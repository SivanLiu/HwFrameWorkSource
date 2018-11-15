package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TokenDelReq;
import com.huawei.android.pushagent.datatype.http.server.TokenDelRsp;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.c.a;
import com.huawei.android.pushagent.utils.f.c;
import org.json.JSONException;

public class b extends a<TokenDelRsp> {
    private TokenDelReq hi;

    public b(Context context, TokenDelReq tokenDelReq) {
        super(context);
        this.hi = tokenDelReq;
    }

    protected String yt() {
        String yw = a.yw("pushtrs.push.hicloud.com", k.rh(yq()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(yw).append("/PushTokenServer/v4/pushtoken/cancel");
        c.er("PushLog3413", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int yr() {
        return 5222;
    }

    protected String ys() {
        try {
            return a.bo(this.hi);
        } catch (JSONException e) {
            c.eq("PushLog3413", "fail to get reqContent");
            return null;
        }
    }

    protected TokenDelRsp yu(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return (TokenDelRsp) a.br(str, TokenDelRsp.class, new Class[0]);
    }
}
