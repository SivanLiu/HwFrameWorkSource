package com.huawei.android.pushagent.b.a;

import android.content.Context;
import com.huawei.android.pushagent.utils.a.b;

public abstract class c implements a {
    protected Context appCtx;
    protected a il;

    protected abstract void aac(Context context);

    protected abstract void aaf(String str, String str2);

    protected abstract void aag(long j, String str, String str2, String str3);

    public void zw(Context context) {
        this.appCtx = context.getApplicationContext();
        aac(this.appCtx);
        if (this.il != null) {
            this.il.zw(this.appCtx);
        }
    }

    public void zx(String str, String str2) {
        if (this.appCtx == null) {
            b.ab("PushLog2976", "Please init log first");
            return;
        }
        aaf(str, str2);
        if (this.il != null) {
            this.il.zx(str, str2);
        }
    }

    public void zy(long j, String str, String str2, String str3) {
        if (this.appCtx == null) {
            b.ab("PushLog2976", "Please init log first");
            return;
        }
        aag(j, str, str2, str3);
        if (this.il != null) {
            this.il.zy(j, str, str2, str3);
        }
    }

    protected void aae(boolean z) {
    }

    public void zz(boolean z) {
        if (this.appCtx == null) {
            b.ab("PushLog2976", "Please init log first");
            return;
        }
        aae(z);
        if (this.il != null) {
            this.il.zz(z);
        }
    }
}
