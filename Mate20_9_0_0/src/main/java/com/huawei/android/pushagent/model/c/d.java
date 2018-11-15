package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.http.server.TrsReq;
import com.huawei.android.pushagent.datatype.http.server.TrsRsp;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.c.a;
import com.huawei.android.pushagent.utils.f.c;
import org.json.JSONException;

public class d extends a<TrsRsp> {
    private TrsReq hk;

    public d(Context context, TrsReq trsReq) {
        super(context);
        this.hk = trsReq;
    }

    protected String yt() {
        String yw = a.yw("pushtrs.push.hicloud.com", k.rh(yq()).getBelongId());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("https://").append(yw).append("/TRSServer/v4/TRSRequest");
        c.er("PushLog3413", "url:" + stringBuffer.toString());
        return stringBuffer.toString();
    }

    protected int yr() {
        return 5222;
    }

    protected String ys() {
        try {
            return a.bo(this.hk);
        } catch (JSONException e) {
            c.eq("PushLog3413", "fail to get reqContent");
            return null;
        }
    }

    protected TrsRsp yu(String str) {
        if (TextUtils.isEmpty(str)) {
            return new TrsRsp(null);
        }
        return new TrsRsp(str);
    }
}
