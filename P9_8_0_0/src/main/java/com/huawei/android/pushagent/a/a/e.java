package com.huawei.android.pushagent.a.a;

import android.content.Context;
import android.content.SharedPreferences;

public class e {
    protected SharedPreferences a;

    public e(Context context, String str) {
        if (context != null) {
            this.a = context.getSharedPreferences(str, 4);
            return;
        }
        throw new NullPointerException("context is null!");
    }

    public void a(String str, boolean z) {
        if (this.a != null) {
            Object -l_3_R = this.a.edit();
            if (-l_3_R != null) {
                -l_3_R.putBoolean(str, z).commit();
            }
        }
    }

    public boolean a(String str) {
        return this.a == null ? false : this.a.getBoolean(str, false);
    }

    public boolean a(String str, String str2) {
        if (this.a == null) {
            return false;
        }
        Object -l_3_R = this.a.edit();
        return -l_3_R == null ? false : -l_3_R.putString(str, str2).commit();
    }

    public String b(String str) {
        return this.a == null ? "" : this.a.getString(str, "");
    }

    public boolean c(String str) {
        return this.a != null && this.a.contains(str);
    }

    public boolean d(String str) {
        if (this.a == null || !this.a.contains(str)) {
            return false;
        }
        Object -l_2_R = this.a.edit().remove(str);
        -l_2_R.commit();
        return -l_2_R.commit();
    }
}
