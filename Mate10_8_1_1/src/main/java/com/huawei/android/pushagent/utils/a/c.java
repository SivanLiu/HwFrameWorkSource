package com.huawei.android.pushagent.utils.a;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class c {
    private SharedPreferences t;

    private SharedPreferences at(String str, String str2) {
        File file = new File(str, str2 + ".xml");
        try {
            Constructor declaredConstructor = Class.forName("android.app.SharedPreferencesImpl").getDeclaredConstructor(new Class[]{File.class, Integer.TYPE});
            declaredConstructor.setAccessible(true);
            return (SharedPreferences) declaredConstructor.newInstance(new Object[]{file, Integer.valueOf(0)});
        } catch (ClassNotFoundException e) {
            b.y("PushLog2976", e.toString());
            return null;
        } catch (NoSuchMethodException e2) {
            b.y("PushLog2976", e2.toString());
            return null;
        } catch (InstantiationException e3) {
            b.y("PushLog2976", e3.toString());
            return null;
        } catch (IllegalAccessException e4) {
            b.y("PushLog2976", e4.toString());
            return null;
        } catch (IllegalArgumentException e5) {
            b.y("PushLog2976", e5.toString());
            return null;
        } catch (InvocationTargetException e6) {
            b.y("PushLog2976", e6.toString());
            return null;
        }
    }

    public boolean ap(String str, boolean z) {
        return this.t != null ? this.t.getBoolean(str, z) : z;
    }

    public String aj(String str) {
        return this.t != null ? this.t.getString(str, "") : "";
    }

    public int getInt(String str, int i) {
        return this.t != null ? this.t.getInt(str, i) : i;
    }

    public c(Context context, String str) {
        if (context == null) {
            throw new NullPointerException("context is null!");
        }
        this.t = at("/data/misc/hwpush", str);
    }

    public boolean am(String str, Object obj) {
        if (this.t == null) {
            return false;
        }
        Editor edit = this.t.edit();
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

    public boolean ak(String str, String str2) {
        if (this.t == null) {
            return false;
        }
        Editor edit = this.t.edit();
        if (edit != null) {
            return edit.putString(str, str2).commit();
        }
        return false;
    }

    public void au(String str, Integer num) {
        if (this.t != null) {
            Editor edit = this.t.edit();
            if (edit != null) {
                edit.putInt(str, num.intValue()).commit();
            }
        }
    }

    public void as(String str, boolean z) {
        if (this.t != null) {
            Editor edit = this.t.edit();
            if (edit != null) {
                edit.putBoolean(str, z).commit();
            }
        }
    }

    public void ar(Map<String, Object> map) {
        for (Entry entry : map.entrySet()) {
            am((String) entry.getKey(), entry.getValue());
        }
    }

    public boolean an(String str) {
        if (this.t == null || !this.t.contains(str)) {
            return false;
        }
        return this.t.edit().remove(str).commit();
    }

    public boolean al() {
        if (this.t != null) {
            Map all = this.t.getAll();
            if (all != null && all.size() > 0) {
                return true;
            }
        }
        return false;
    }

    public Map<String, ?> getAll() {
        if (this.t != null) {
            return this.t.getAll();
        }
        return new HashMap();
    }

    public boolean ao() {
        if (this.t != null) {
            return this.t.edit().clear().commit();
        }
        return false;
    }

    public int aq() {
        if (this.t != null) {
            return this.t.getAll().size();
        }
        return 0;
    }
}
