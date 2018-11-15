package com.android.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.LocaleList;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.app.LocalePicker;
import java.util.ArrayList;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Builder;

public class UpdateConfig {
    private static final String TAG = "UpdateConfig";

    public static void updateLocalesWhenOTA(Context context) {
        if (context != null) {
            PackageManager mPackageManager = context.getPackageManager();
            String prop = SystemProperties.get("ril.operator.numeric");
            if (mPackageManager != null && mPackageManager.isUpgrade() && prop.length() >= 3 && "414".equals(prop.substring(0, 3))) {
                updateLocale();
            }
        }
    }

    public static void updateLocalesWhenOTAEX(Context context, int preSdkVersion) {
        if (context != null && preSdkVersion != -1 && preSdkVersion < 28) {
            PackageManager mPackageManager = context.getPackageManager();
            String prop = SystemProperties.get("ril.operator.numeric");
            if (mPackageManager != null && mPackageManager.isUpgrade() && prop.length() >= 3 && "414".equals(prop.substring(0, 3))) {
                updateLocale();
            }
        }
    }

    private static void updateLocale() {
        LocaleList defaultsList = LocalePicker.getLocales();
        if (defaultsList != null) {
            List<Locale> nList = new ArrayList();
            int size = defaultsList.size();
            boolean isChange = false;
            for (int i = 0; i < size; i++) {
                Locale locale = defaultsList.get(i);
                if (i == 0 && (locale.getLanguage().equals("en") || locale.getLanguage().equals("zh"))) {
                    Builder localeBuilder = new Builder().setLanguageTag("en");
                    try {
                        localeBuilder = new Builder().setLocale(locale).setRegion("ZG");
                    } catch (IllformedLocaleException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error locale: ");
                        stringBuilder.append(locale.toLanguageTag());
                        Slog.e(str, stringBuilder.toString());
                    }
                    nList.add(localeBuilder.build());
                    isChange = true;
                } else {
                    nList.add(locale);
                }
            }
            if (isChange) {
                List<Locale> rList = new ArrayList();
                for (int i2 = 0; i2 < size; i2++) {
                    Locale tmpL = (Locale) nList.get(i2);
                    if (!rList.contains(tmpL)) {
                        rList.add(tmpL);
                    }
                }
                LocalePicker.updateLocales(new LocaleList((Locale[]) rList.toArray(new Locale[0])));
            }
        }
    }
}
