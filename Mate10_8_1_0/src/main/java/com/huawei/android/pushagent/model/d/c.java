package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TrsReq;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import org.json.JSONException;

public class c extends d<TrsRsp> {
    private TrsReq ew;

    public c(Context context, TrsReq trsReq) {
        super(context);
        this.ew = trsReq;
    }

    protected String tp() {
        String ts = d.ts("pushtrs.push.hicloud.com", i.mj(tr()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(ts).append("/TRSServer/v4/TRSRequest");
        b.x("PushLog2976", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int tn() {
        return 5222;
    }

    protected String to() {
        try {
            return com.huawei.android.pushagent.utils.f.b.dt(this.ew);
        } catch (JSONException e) {
            b.y("PushLog2976", "fail to get reqContent");
            return null;
        }
    }

    protected TrsRsp tq(String str) {
        if (TextUtils.isEmpty(str)) {
            return new TrsRsp(null);
        }
        return new TrsRsp(str);
    }
}
