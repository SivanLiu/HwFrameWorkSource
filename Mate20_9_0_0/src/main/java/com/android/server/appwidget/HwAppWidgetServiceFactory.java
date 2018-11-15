package com.android.server.appwidget;

import android.content.Context;
import android.util.Log;

public class HwAppWidgetServiceFactory {
    private static final String TAG = "HwAppWidgetServiceFactory";
    private static volatile Factory obj = null;

    public interface Factory {
        IHwAppWidgetService getHwAppWidgetService();
    }

    public interface IHwAppWidgetService {
        AppWidgetServiceImpl getAppWidgetImpl(Context context);
    }

    private static synchronized Factory getImplObject() {
        synchronized (HwAppWidgetServiceFactory.class) {
            Factory factory;
            if (obj != null) {
                factory = obj;
                return factory;
            }
            try {
                obj = (Factory) Class.forName("com.android.server.appwidget.HwAppWidgetServiceImplFactory").newInstance();
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(": reflection exception is ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
            factory = obj;
            return factory;
        }
    }

    public static IHwAppWidgetService getHwAppWidgetService() {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwAppWidgetService();
        }
        return null;
    }
}
