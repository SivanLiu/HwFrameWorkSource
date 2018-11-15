package com.huawei.android.pushagent.utils.f;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class a {
    private SharedPreferences az;

    private SharedPreferences ek(String str, String str2) {
        File file = new File(str, str2 + ".xml");
        try {
            Constructor declaredConstructor = Class.forName("android.app.SharedPreferencesImpl").getDeclaredConstructor(new Class[]{File.class, Integer.TYPE});
            declaredConstructor.setAccessible(true);
            return (SharedPreferences) declaredConstructor.newInstance(new Object[]{file, Integer.valueOf(0)});
        } catch (ClassNotFoundException e) {
            c.eq("PushLog3413", e.toString());
            return null;
        } catch (NoSuchMethodException e2) {
            c.eq("PushLog3413", e2.toString());
            return null;
        } catch (InstantiationException e3) {
            c.eq("PushLog3413", e3.toString());
            return null;
        } catch (IllegalAccessException e4) {
            c.eq("PushLog3413", e4.toString());
            return null;
        } catch (IllegalArgumentException e5) {
            c.eq("PushLog3413", e5.toString());
            return null;
        } catch (InvocationTargetException e6) {
            c.eq("PushLog3413", e6.toString());
            return null;
        }
    }

    public boolean eb(String str, boolean z) {
        return this.az != null ? this.az.getBoolean(str, z) : z;
    }

    public String ec(String str) {
        return this.az != null ? this.az.getString(str, "") : "";
    }

    public int getInt(String str, int i) {
        return this.az != null ? this.az.getInt(str, i) : i;
    }

    public a(Context context, String str) {
        if (context == null) {
            throw new NullPointerException("context is null!");
        }
        this.az = ek("/data/misc/hwpush", str);
    }

    public boolean ea(String str, Object obj) {
        if (this.az == null) {
            return false;
        }
        Editor edit = this.az.edit();
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

    public boolean ee(String str, String str2) {
        if (this.az == null) {
            return false;
        }
        Editor edit = this.az.edit();
        if (edit != null) {
            return edit.putString(str, str2).commit();
        }
        return false;
    }

    public void ef(String str, Integer num) {
        if (this.az != null) {
            Editor edit = this.az.edit();
            if (edit != null) {
                edit.putInt(str, num.intValue()).commit();
            }
        }
    }

    public void ej(String str, boolean z) {
        if (this.az != null) {
            Editor edit = this.az.edit();
            if (edit != null) {
                edit.putBoolean(str, z).commit();
            }
        }
    }

    public void ei(Map<String, Object> map) {
        for (Entry entry : map.entrySet()) {
            ea((String) entry.getKey(), entry.getValue());
        }
    }

    public boolean ed(String str) {
        if (this.az == null || !this.az.contains(str)) {
            return false;
        }
        return this.az.edit().remove(str).commit();
    }

    public boolean eg() {
        if (this.az != null) {
            Map all = this.az.getAll();
            if (all != null && all.size() > 0) {
                return true;
            }
        }
        return false;
    }

    public Map<String, ?> getAll() {
        if (this.az != null) {
            return this.az.getAll();
        }
        return new HashMap();
    }

    public boolean dz() {
        if (this.az != null) {
            return this.az.edit().clear().commit();
        }
        return false;
    }

    public int eh() {
        if (this.az != null) {
            return this.az.getAll().size();
        }
        return 0;
    }
}
