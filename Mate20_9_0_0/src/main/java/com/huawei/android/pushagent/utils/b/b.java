package com.huawei.android.pushagent.utils.b;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class b {
    private SharedPreferences gt;

    private SharedPreferences tp(String str, String str2) {
        File file = new File(str, str2 + ".xml");
        try {
            Constructor declaredConstructor = Class.forName("android.app.SharedPreferencesImpl").getDeclaredConstructor(new Class[]{File.class, Integer.TYPE});
            declaredConstructor.setAccessible(true);
            return (SharedPreferences) declaredConstructor.newInstance(new Object[]{file, Integer.valueOf(0)});
        } catch (ClassNotFoundException e) {
            a.su("PushLog3414", e.toString());
            return null;
        } catch (NoSuchMethodException e2) {
            a.su("PushLog3414", e2.toString());
            return null;
        } catch (InstantiationException e3) {
            a.su("PushLog3414", e3.toString());
            return null;
        } catch (IllegalAccessException e4) {
            a.su("PushLog3414", e4.toString());
            return null;
        } catch (IllegalArgumentException e5) {
            a.su("PushLog3414", e5.toString());
            return null;
        } catch (InvocationTargetException e6) {
            a.su("PushLog3414", e6.toString());
            return null;
        }
    }

    public boolean tk(String str, boolean z) {
        return this.gt != null ? this.gt.getBoolean(str, z) : z;
    }

    public String tg(String str) {
        return this.gt != null ? this.gt.getString(str, "") : "";
    }

    public int getInt(String str, int i) {
        return this.gt != null ? this.gt.getInt(str, i) : i;
    }

    public b(Context context, String str) {
        if (context == null) {
            throw new NullPointerException("context is null!");
        }
        this.gt = tp("/data/misc/hwpush", str);
    }

    public boolean th(String str, Object obj) {
        if (this.gt == null) {
            return false;
        }
        Editor edit = this.gt.edit();
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
        return edit.commit();
    }

    public boolean tm(String str, String str2) {
        if (this.gt == null) {
            return false;
        }
        Editor edit = this.gt.edit();
        if (edit != null) {
            return edit.putString(str, str2).commit();
        }
        return false;
    }

    public void to(String str, Integer num) {
        if (this.gt != null) {
            Editor edit = this.gt.edit();
            if (edit != null) {
                edit.putInt(str, num.intValue()).commit();
            }
        }
    }

    public void tl(String str, boolean z) {
        if (this.gt != null) {
            Editor edit = this.gt.edit();
            if (edit != null) {
                edit.putBoolean(str, z).commit();
            }
        }
    }

    public void tn(Map<String, Object> map) {
        for (Entry entry : map.entrySet()) {
            th((String) entry.getKey(), entry.getValue());
        }
    }

    public boolean ti(String str) {
        if (this.gt == null || !this.gt.contains(str)) {
            return false;
        }
        return this.gt.edit().remove(str).commit();
    }

    public boolean tf() {
        if (this.gt != null) {
            Map all = this.gt.getAll();
            if (all != null && all.size() > 0) {
                return true;
            }
        }
        return false;
    }

    public Map<String, ?> getAll() {
        if (this.gt != null) {
            return this.gt.getAll();
        }
        return new HashMap();
    }

    public boolean tj() {
        if (this.gt != null) {
            return this.gt.edit().clear().commit();
        }
        return false;
    }

    public int tq() {
        if (this.gt != null) {
            return this.gt.getAll().size();
        }
        return 0;
    }
}
