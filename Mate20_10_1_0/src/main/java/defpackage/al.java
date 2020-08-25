package defpackage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArrayMap;

/* renamed from: al  reason: default package */
public class al {
    protected Context M;
    private String ab;
    public ArrayMap<String, Object> ac = new ArrayMap<>();

    public al(Context context, String str) {
        this.M = context;
        this.ab = str;
    }

    public final boolean a(String str, Object obj) {
        this.ac.put(str, obj);
        ak akVar = new ak(this.M, this.ab);
        if (akVar.aa == null) {
            return true;
        }
        SharedPreferences.Editor edit = akVar.aa.edit();
        if (obj instanceof String) {
            edit.putString(str, String.valueOf(obj));
        } else if ((obj instanceof Integer) || (obj instanceof Short) || (obj instanceof Byte)) {
            edit.putInt(str, ((Integer) obj).intValue());
        } else if (obj instanceof Long) {
            edit.putLong(str, ((Long) obj).longValue());
        } else if (obj instanceof Float) {
            edit.putFloat(str, ((Float) obj).floatValue());
        } else if (obj instanceof Double) {
            edit.putFloat(str, (float) ((Double) obj).doubleValue());
        } else if (obj instanceof Boolean) {
            edit.putBoolean(str, ((Boolean) obj).booleanValue());
        }
        edit.commit();
        return true;
    }

    /* access modifiers changed from: protected */
    public final void g() {
        this.ac.clear();
        ak akVar = new ak(this.M, this.ab);
        this.ac.putAll(akVar.aa != null ? akVar.aa.getAll() : new ArrayMap<>());
    }
}
