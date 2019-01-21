package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TrsReq;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.prefs.a;
import org.json.JSONException;

public class c extends b<TrsRsp> {
    private TrsReq dz;

    public c(Context context, TrsReq trsReq) {
        super(context);
        this.dz = trsReq;
    }

    protected String mx() {
        String na = b.na("pushtrs.push.hicloud.com", a.ff(mz()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(na).append("/TRSServer/v4/TRSRequest");
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int mv() {
        return 5222;
    }

    protected String mw() {
        try {
            return com.huawei.android.pushagent.utils.d.a.uh(this.dz);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "fail to get reqContent");
            return null;
        }
    }

    protected TrsRsp my(String str) {
        if (TextUtils.isEmpty(str)) {
            return new TrsRsp(null);
        }
        return new TrsRsp(str);
    }
}
