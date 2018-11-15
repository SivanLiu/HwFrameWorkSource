package com.huawei.android.pushagent.a.a;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.c;

public abstract class a implements b {
    protected Context appCtx;
    protected b bd;

    protected abstract void hh(Context context);

    protected abstract void hj(String str, String str2);

    protected abstract void hk(long j, String str, String str2, String str3);

    public void hd(Context context) {
        this.appCtx = context.getApplicationContext();
        hh(this.appCtx);
        if (this.bd != null) {
            this.bd.hd(this.appCtx);
        }
    }

    public void he(String str, String str2) {
        if (this.appCtx == null) {
            c.eo("PushLog3413", "Please init log first");
            return;
        }
        hj(str, str2);
        if (this.bd != null) {
            this.bd.he(str, str2);
        }
    }

    public void hf(long j, String str, String str2, String str3) {
        if (this.appCtx == null) {
            c.eo("PushLog3413", "Please init log first");
            return;
        }
        hk(j, str, str2, str3);
        if (this.bd != null) {
            this.bd.hf(j, str, str2, str3);
        }
    }

    protected void hi(boolean z) {
    }

    public void hg(boolean z) {
        if (this.appCtx == null) {
            c.eo("PushLog3413", "Please init log first");
            return;
        }
        hi(z);
        if (this.bd != null) {
            this.bd.hg(z);
        }
    }
}
