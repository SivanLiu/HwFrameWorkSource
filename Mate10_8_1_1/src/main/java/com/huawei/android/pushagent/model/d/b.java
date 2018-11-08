package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyReq;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.model.prefs.i;
import org.json.JSONException;

public class b extends d<TokenApplyRsp> {
    private TokenApplyReq ev;

    public b(Context context, TokenApplyReq tokenApplyReq) {
        super(context);
        this.ev = tokenApplyReq;
    }

    protected String tp() {
        String ts = d.ts("pushtrs.push.hicloud.com", i.mj(tr()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(ts).append("/PushTokenServer/v4/pushtoken/apply");
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int tn() {
        return 5222;
    }

    protected String to() {
        try {
            return com.huawei.android.pushagent.utils.f.b.dt(this.ev);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "fail to get reqContent");
            return null;
        }
    }

    protected TokenApplyRsp tq(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return (TokenApplyRsp) com.huawei.android.pushagent.utils.f.b.du(str, TokenApplyRsp.class, new Class[0]);
    }
}
