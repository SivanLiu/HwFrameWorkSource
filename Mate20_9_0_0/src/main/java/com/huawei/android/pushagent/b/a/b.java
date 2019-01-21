package com.huawei.android.pushagent.b.a;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.a;

public abstract class b implements c {
    protected Context appCtx;
    protected c iq;

    protected abstract void aaq(Context context);

    protected abstract void aat(String str, String str2);

    protected abstract void aau(long j, String str, String str2, String str3);

    public void aax(Context context) {
        this.appCtx = context.getApplicationContext();
        aaq(this.appCtx);
        if (this.iq != null) {
            this.iq.aax(this.appCtx);
        }
    }

    public void aay(String str, String str2) {
        if (this.appCtx == null) {
            a.sx("PushLog3414", "Please init log first");
            return;
        }
        aat(str, str2);
        if (this.iq != null) {
            this.iq.aay(str, str2);
        }
    }

    public void aaz(long j, String str, String str2, String str3) {
        if (this.appCtx == null) {
            a.sx("PushLog3414", "Please init log first");
            return;
        }
        aau(j, str, str2, str3);
        if (this.iq != null) {
            this.iq.aaz(j, str, str2, str3);
        }
    }

    protected void aas(boolean z) {
    }

    public void aba(boolean z) {
        if (this.appCtx == null) {
            a.sx("PushLog3414", "Please init log first");
            return;
        }
        aas(z);
        if (this.iq != null) {
            this.iq.aba(z);
        }
    }
}
