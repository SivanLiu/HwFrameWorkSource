package android.icu.impl;

import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;

public class ICUResourceTableAccess {
    public static String getTableString(String path, ULocale locale, String tableName, String itemName, String defaultValue) {
        return getTableString((ICUResourceBundle) UResourceBundle.getBundleInstance(path, locale.getBaseName()), tableName, null, itemName, defaultValue);
    }

    public static String getTableString(ICUResourceBundle bundle, String tableName, String subtableName, String item, String defaultValue) {
        String result = null;
        while (true) {
            try {
                ICUResourceBundle table = bundle.findWithFallback(tableName);
                if (table == null) {
                    return defaultValue;
                }
                String currentName;
                ICUResourceBundle stable = table;
                if (subtableName != null) {
                    stable = table.findWithFallback(subtableName);
                }
                if (stable != null) {
                    result = stable.findStringWithFallback(item);
                    if (result != null) {
                        break;
                    }
                }
                if (subtableName == null) {
                    currentName = null;
                    if (tableName.equals("Countries")) {
                        currentName = LocaleIDs.getCurrentCountryID(item);
                    } else if (tableName.equals("Languages")) {
                        currentName = LocaleIDs.getCurrentLanguageID(item);
                    }
                    if (currentName != null) {
                        result = table.findStringWithFallback(currentName);
                        if (result != null) {
                            break;
                        }
                    }
                }
                currentName = table.findStringWithFallback("Fallback");
                if (currentName == null) {
                    return defaultValue;
                }
                if (currentName.length() == 0) {
                    currentName = "root";
                }
                if (currentName.equals(table.getULocale().getName())) {
                    return defaultValue;
                }
                bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(bundle.getBaseName(), currentName);
            } catch (Exception e) {
            }
        }
        String str = (result == null || result.length() <= 0) ? defaultValue : result;
        return str;
    }
}
