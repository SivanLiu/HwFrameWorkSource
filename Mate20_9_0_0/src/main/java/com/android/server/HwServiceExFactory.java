package com.android.server;

import android.content.Context;
import android.util.Log;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.IHwActivityManagerInner;
import com.android.server.am.IHwActivityManagerServiceEx;
import com.android.server.audio.IHwAudioServiceEx;
import com.android.server.audio.IHwAudioServiceInner;
import com.android.server.display.IHwDisplayManagerInner;
import com.android.server.display.IHwDisplayManagerServiceEx;
import com.android.server.imm.IHwInputMethodManagerInner;
import com.android.server.imm.IHwInputMethodManagerServiceEx;
import com.android.server.input.IHwInputManagerInner;
import com.android.server.input.IHwInputManagerServiceEx;
import com.android.server.pm.IHwBackgroundDexOptInner;
import com.android.server.pm.IHwBackgroundDexOptServiceEx;
import com.android.server.pm.IHwPackageManagerInner;
import com.android.server.pm.IHwPackageManagerServiceEx;
import com.android.server.power.IHwPowerManagerInner;
import com.android.server.power.IHwPowerManagerServiceEx;
import com.android.server.wm.IHwWindowManagerInner;
import com.android.server.wm.IHwWindowManagerServiceEx;
import com.huawei.server.am.IHwActivityStackSupervisorEx;
import com.huawei.server.am.IHwActivityStarterEx;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class HwServiceExFactory {
    private static final String TAG = "HwServiceExFactory";
    private static final Object mLock = new Object();
    private static Factory obj = null;

    public interface Factory {
        IHwActivityManagerServiceEx getHwActivityManagerServiceEx(IHwActivityManagerInner iHwActivityManagerInner, Context context);

        IHwActivityStackSupervisorEx getHwActivityStackSupervisorEx();

        IHwActivityStarterEx getHwActivityStarterEx(ActivityManagerService activityManagerService);

        IHwAudioServiceEx getHwAudioServiceEx(IHwAudioServiceInner iHwAudioServiceInner, Context context);

        IHwBackgroundDexOptServiceEx getHwBackgroundDexOptServiceEx(IHwBackgroundDexOptInner iHwBackgroundDexOptInner, Context context);

        IHwDisplayManagerServiceEx getHwDisplayManagerServiceEx(IHwDisplayManagerInner iHwDisplayManagerInner, Context context);

        IHwInputManagerServiceEx getHwInputManagerServiceEx(IHwInputManagerInner iHwInputManagerInner, Context context);

        IHwInputMethodManagerServiceEx getHwInputMethodManagerServiceEx(IHwInputMethodManagerInner iHwInputMethodManagerInner, Context context);

        IHwPackageManagerServiceEx getHwPackageManagerServiceEx(IHwPackageManagerInner iHwPackageManagerInner, Context context);

        IHwPowerManagerServiceEx getHwPowerManagerServiceEx(IHwPowerManagerInner iHwPowerManagerInner, Context context);

        IHwWindowManagerServiceEx getHwWindowManagerServiceEx(IHwWindowManagerInner iHwWindowManagerInner, Context context);
    }

    private static Factory getImplObject() {
        synchronized (mLock) {
            if (obj == null) {
                String str;
                StringBuilder stringBuilder;
                try {
                    obj = (Factory) Class.forName("com.android.server.HwServiceExFactoryImpl").newInstance();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("get AllImpl object = ");
                    stringBuilder.append(obj);
                    Log.v(str, stringBuilder.toString());
                } catch (ClassNotFoundException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ClassNotFoundException : ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                } catch (Exception e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(": reflection exception is ");
                    stringBuilder.append(e2);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }
        return obj;
    }

    private static Object getHwInterfaceExProxy(Class<?>[] interfaces) {
        return Proxy.newProxyInstance(interfaces[0].getClassLoader(), interfaces, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Class returnType = method.getReturnType();
                if (returnType == Integer.TYPE || returnType == Long.TYPE || returnType == Byte.TYPE || returnType == Character.TYPE || returnType == Short.TYPE || returnType == Long.TYPE || returnType == Double.TYPE) {
                    return Integer.valueOf(0);
                }
                if (returnType == Float.TYPE) {
                    return Float.valueOf(0.0f);
                }
                if (returnType == Boolean.TYPE) {
                    return Boolean.valueOf(false);
                }
                return null;
            }
        });
    }

    public static IHwActivityManagerServiceEx getHwActivityManagerServiceEx(IHwActivityManagerInner ams, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwActivityManagerServiceEx(ams, context);
        }
        return (IHwActivityManagerServiceEx) getHwInterfaceExProxy(new Class[]{IHwActivityManagerServiceEx.class});
    }

    public static IHwWindowManagerServiceEx getHwWindowManagerServiceEx(IHwWindowManagerInner wms, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwWindowManagerServiceEx(wms, context);
        }
        return (IHwWindowManagerServiceEx) getHwInterfaceExProxy(new Class[]{IHwWindowManagerServiceEx.class});
    }

    public static IHwPackageManagerServiceEx getHwPackageManagerServiceEx(IHwPackageManagerInner pms, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwPackageManagerServiceEx(pms, context);
        }
        return (IHwPackageManagerServiceEx) getHwInterfaceExProxy(new Class[]{IHwPackageManagerServiceEx.class});
    }

    public static IHwInputMethodManagerServiceEx getHwInputMethodManagerServiceEx(IHwInputMethodManagerInner ims, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwInputMethodManagerServiceEx(ims, context);
        }
        return (IHwInputMethodManagerServiceEx) getHwInterfaceExProxy(new Class[]{IHwInputMethodManagerServiceEx.class});
    }

    public static IHwBackgroundDexOptServiceEx getHwBackgroundDexOptServiceEx(IHwBackgroundDexOptInner bdox, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwBackgroundDexOptServiceEx(bdox, context);
        }
        return (IHwBackgroundDexOptServiceEx) getHwInterfaceExProxy(new Class[]{IHwBackgroundDexOptServiceEx.class});
    }

    public static IHwPowerManagerServiceEx getHwPowerManagerServiceEx(IHwPowerManagerInner pms, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwPowerManagerServiceEx(pms, context);
        }
        return (IHwPowerManagerServiceEx) getHwInterfaceExProxy(new Class[]{IHwPowerManagerServiceEx.class});
    }

    public static IHwActivityStarterEx getHwActivityStarterEx(ActivityManagerService ams) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwActivityStarterEx(ams);
        }
        return (IHwActivityStarterEx) getHwInterfaceExProxy(new Class[]{IHwActivityStarterEx.class});
    }

    public static IHwAudioServiceEx getHwAudioServiceEx(IHwAudioServiceInner ias, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwAudioServiceEx(ias, context);
        }
        return (IHwAudioServiceEx) getHwInterfaceExProxy(new Class[]{IHwAudioServiceEx.class});
    }

    public static IHwInputManagerServiceEx getHwInputManagerServiceEx(IHwInputManagerInner ims, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwInputManagerServiceEx(ims, context);
        }
        return (IHwInputManagerServiceEx) getHwInterfaceExProxy(new Class[]{IHwInputManagerServiceEx.class});
    }

    public static IHwActivityStackSupervisorEx getHwActivityStackSupervisorEx() {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwActivityStackSupervisorEx();
        }
        return (IHwActivityStackSupervisorEx) getHwInterfaceExProxy(new Class[]{IHwActivityStackSupervisorEx.class});
    }

    public static IHwDisplayManagerServiceEx getHwDisplayManagerServiceEx(IHwDisplayManagerInner dms, Context context) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwDisplayManagerServiceEx(dms, context);
        }
        return (IHwDisplayManagerServiceEx) getHwInterfaceExProxy(new Class[]{IHwDisplayManagerServiceEx.class});
    }
}
