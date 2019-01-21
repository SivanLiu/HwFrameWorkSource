package com.android.internal.app;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.ListFragment;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.icu.util.ULocale;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.RemoteException;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LocalePicker extends ListFragment {
    private static final boolean DEBUG = false;
    private static final String TAG = "LocalePicker";
    private static final String[] pseudoLocales = new String[]{"en-XA", "ar-XB"};
    LocaleSelectionListener mListener;

    public static class LocaleInfo implements Comparable<LocaleInfo> {
        static final Collator sCollator = Collator.getInstance();
        String label;
        final Locale locale;

        public LocaleInfo(String label, Locale locale) {
            this.label = label;
            this.locale = locale;
        }

        public String getLabel() {
            return this.label;
        }

        public Locale getLocale() {
            return this.locale;
        }

        public String toString() {
            return this.label;
        }

        public int compareTo(LocaleInfo another) {
            return sCollator.compare(this.label, another.label);
        }
    }

    public interface LocaleSelectionListener {
        void onLocaleSelected(Locale locale);
    }

    public static String[] getSystemAssetLocales() {
        String[] internal = Resources.getSystem().getAssets().getLocales();
        Resources.getSystem().getAssets();
        String[] shared = AssetManager.getSharedResList();
        if (shared == null) {
            shared = new String[0];
        }
        String[] result = (String[]) Arrays.copyOf(internal, internal.length + shared.length);
        System.arraycopy(shared, 0, result, internal.length, shared.length);
        return result;
    }

    public static String[] getSupportedLocales(Context context) {
        Set<String> realList = new HashSet();
        realList.addAll(getRealLocaleList(context, context.getResources().getStringArray(17236083)));
        List<String> supportedLocales = new ArrayList(realList);
        return (String[]) supportedLocales.toArray(new String[supportedLocales.size()]);
    }

    public static List<LocaleInfo> getAllAssetLocales(Context context, boolean isInDeveloperMode) {
        Resources resources = context.getResources();
        String[] locales = getSystemAssetLocales();
        List<String> localeList = new ArrayList(locales.length);
        Collections.addAll(localeList, locales);
        Collections.sort(localeList);
        String[] specialLocaleCodes = resources.getStringArray(17236081);
        String[] specialLocaleNames = resources.getStringArray(17236082);
        ArrayList<LocaleInfo> localeInfos = new ArrayList(localeList.size());
        for (String locale : localeList) {
            Locale l = Locale.forLanguageTag(locale.replace('_', '-'));
            if (!(l == null || "und".equals(l.getLanguage()) || l.getLanguage().isEmpty())) {
                if (!l.getCountry().isEmpty()) {
                    if (isInDeveloperMode || !LocaleList.isPseudoLocale(l)) {
                        if (localeInfos.isEmpty()) {
                            localeInfos.add(new LocaleInfo(toTitleCase(l.getDisplayLanguage(l)), l));
                        } else {
                            LocaleInfo previous = (LocaleInfo) localeInfos.get(localeInfos.size() - 1);
                            if (!previous.locale.getLanguage().equals(l.getLanguage()) || previous.locale.getLanguage().equals("zz")) {
                                localeInfos.add(new LocaleInfo(toTitleCase(l.getDisplayLanguage(l)), l));
                            } else {
                                previous.label = toTitleCase(getDisplayName(previous.locale, specialLocaleCodes, specialLocaleNames));
                                localeInfos.add(new LocaleInfo(toTitleCase(getDisplayName(l, specialLocaleCodes, specialLocaleNames)), l));
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(localeInfos);
        return localeInfos;
    }

    public static ArrayAdapter<LocaleInfo> constructAdapter(Context context) {
        return constructAdapter(context, 17367174, 16909051);
    }

    public static ArrayAdapter<LocaleInfo> constructAdapter(Context context, int layoutId, int fieldId) {
        boolean z = false;
        if (Global.getInt(context.getContentResolver(), "development_settings_enabled", 0) != 0) {
            z = true;
        }
        final LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        final int i = layoutId;
        final int i2 = fieldId;
        return new ArrayAdapter<LocaleInfo>(context, layoutId, fieldId, getAllAssetLocales(context, z)) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view;
                TextView text;
                if (convertView == null) {
                    view = layoutInflater.inflate(i, parent, false);
                    text = (TextView) view.findViewById(i2);
                    view.setTag(text);
                } else {
                    view = convertView;
                    text = (TextView) view.getTag();
                }
                LocaleInfo item = (LocaleInfo) getItem(position);
                text.setText(item.toString());
                text.setTextLocale(item.getLocale());
                return view;
            }
        };
    }

    private static String toTitleCase(String s) {
        if (s.length() == 0) {
            return s;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Character.toUpperCase(s.charAt(0)));
        stringBuilder.append(s.substring(1));
        return stringBuilder.toString();
    }

    private static String getDisplayName(Locale l, String[] specialLocaleCodes, String[] specialLocaleNames) {
        String code = l.toString();
        for (int i = 0; i < specialLocaleCodes.length; i++) {
            if (specialLocaleCodes[i].equals(code)) {
                return specialLocaleNames[i];
            }
        }
        return l.getDisplayName(l);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(constructAdapter(getActivity()));
    }

    public void setLocaleSelectionListener(LocaleSelectionListener listener) {
        this.mListener = listener;
    }

    public void onResume() {
        super.onResume();
        getListView().requestFocus();
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        if (this.mListener != null) {
            this.mListener.onLocaleSelected(((LocaleInfo) getListAdapter().getItem(position)).locale);
        }
    }

    public static void updateLocale(Locale locale) {
        updateLocales(new LocaleList(new Locale[]{locale}));
    }

    public static void updateLocales(LocaleList locales) {
        try {
            IActivityManager am = ActivityManager.getService();
            Configuration config = am.getConfiguration();
            config.setLocales(checkLocaleList(locales));
            config.userSetLocale = true;
            am.updatePersistentConfiguration(config);
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (RemoteException e) {
        }
    }

    private static LocaleList checkLocaleList(LocaleList locales) {
        List<Locale> nList = new ArrayList();
        int length = locales.size();
        for (int i = 0; i < length; i++) {
            Locale locale = locales.get(i);
            if (locale.getLanguage().equals("my") && locale.getUnicodeLocaleType("nu") == null) {
                String languageTag = locale.toLanguageTag();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(languageTag);
                stringBuilder.append("-u-nu-latn");
                nList.add(Locale.forLanguageTag(stringBuilder.toString()));
            } else {
                nList.add(locale);
            }
        }
        return new LocaleList((Locale[]) nList.toArray(new Locale[0]));
    }

    public static LocaleList getLocales() {
        try {
            return ActivityManager.getService().getConfiguration().getLocales();
        } catch (RemoteException e) {
            return LocaleList.getDefault();
        }
    }

    private static Set<String> getRealLocaleList(Context context, String[] locales) {
        Set<String> realLocales = new HashSet();
        List<String> whiteLanguage = getWhiteLanguage(context);
        List<String> blackLanguage = getBlackLanguage(context);
        int i = 0;
        for (String localeStr : locales) {
            ULocale locale = ULocale.forLanguageTag(localeStr.replace('_', '-'));
            ULocale compareLocale = ULocale.addLikelySubtags(locale);
            if ((blackLanguage.isEmpty() || !blackLanguage.contains(compareLocale.toLanguageTag())) && (whiteLanguage.isEmpty() || whiteLanguage.contains(compareLocale.getFallback().toLanguageTag()))) {
                realLocales.add(locale.toLanguageTag());
            }
        }
        String[] strArr = pseudoLocales;
        int length = strArr.length;
        while (i < length) {
            ULocale locale2 = ULocale.forLanguageTag(strArr[i].replace('_', '-'));
            if (!blackLanguage.contains(ULocale.addLikelySubtags(locale2).toLanguageTag())) {
                realLocales.add(locale2.toLanguageTag());
            }
            i++;
        }
        return realLocales;
    }

    private static ArrayList<String> getWhiteLanguage(Context context) {
        String[] assetLocales;
        String localeStr;
        ArrayList<String> whiteLanguages = new ArrayList();
        String whiteStrings = null;
        String whiteLanguagesAmendForCust = null;
        try {
            whiteStrings = System.getString(context.getContentResolver(), "white_languages");
            whiteLanguagesAmendForCust = System.getString(context.getContentResolver(), "white_languages_amend_for_cust");
        } catch (Exception e) {
            Log.e(TAG, "Could not load default locales", e);
        }
        int i = 0;
        if (whiteStrings != null) {
            if (whiteLanguagesAmendForCust != null) {
                whiteStrings = parseWhiteLanguageAmend(whiteLanguagesAmendForCust, whiteStrings);
            }
            whiteStrings = whiteStrings.replace("tl", "fil");
            assetLocales = getSystemAssetLocales();
            for (String localeStr2 : whiteStrings.split(",")) {
                String locale_tag = ULocale.addLikelySubtags(ULocale.forLanguageTag(localeStr2.replace('_', '-'))).getFallback().toLanguageTag();
                for (String assetLocal : assetLocales) {
                    if (ULocale.addLikelySubtags(ULocale.forLanguageTag(assetLocal)).getFallback().toLanguageTag().equals(locale_tag)) {
                        whiteLanguages.add(locale_tag);
                        break;
                    }
                }
            }
        }
        context.getResources().getAssets();
        assetLocales = AssetManager.getSharedResList();
        if (assetLocales != null) {
            int length = assetLocales.length;
            while (i < length) {
                boolean repeat = false;
                localeStr2 = ULocale.addLikelySubtags(ULocale.forLanguageTag(assetLocales[i])).getFallback().toLanguageTag();
                Iterator it = whiteLanguages.iterator();
                while (it.hasNext()) {
                    if (localeStr2.equals((String) it.next())) {
                        repeat = true;
                        break;
                    }
                }
                if (!repeat) {
                    whiteLanguages.add(localeStr2);
                }
                i++;
            }
        }
        return whiteLanguages;
    }

    private static String parseWhiteLanguageAmend(String whiteLanguagesAmendForCust, String whiteStrings) {
        int length;
        String operLanguagesStr;
        int i = 0;
        String amendDelStrings = null;
        String amendAddString = null;
        for (String operLanguagesStr2 : whiteLanguagesAmendForCust.split(";")) {
            String[] operLanguagesArr = operLanguagesStr2.split(":");
            if (2 == operLanguagesArr.length) {
                if ("add".equalsIgnoreCase(operLanguagesArr[0])) {
                    amendAddString = operLanguagesArr[1];
                } else {
                    amendDelStrings = operLanguagesArr[1];
                }
            }
        }
        if (amendAddString != null) {
            operLanguagesStr2 = whiteStrings;
            for (String amendAddLanguage : amendAddString.split(",")) {
                if (!operLanguagesStr2.contains(amendAddLanguage)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(operLanguagesStr2);
                    stringBuilder.append(",");
                    stringBuilder.append(amendAddLanguage);
                    operLanguagesStr2 = stringBuilder.toString();
                }
            }
            whiteStrings = operLanguagesStr2;
        }
        if (amendDelStrings != null) {
            String[] split = amendDelStrings.split(",");
            length = split.length;
            while (i < length) {
                operLanguagesStr2 = split[i];
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(operLanguagesStr2);
                stringBuilder2.append(",");
                if (whiteStrings.contains(stringBuilder2.toString())) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(operLanguagesStr2);
                    stringBuilder2.append(",");
                    whiteStrings = whiteStrings.replace(stringBuilder2.toString(), "");
                } else if (whiteStrings.contains(operLanguagesStr2)) {
                    whiteStrings = whiteStrings.replace(operLanguagesStr2, "");
                }
                i++;
            }
        }
        return whiteStrings;
    }

    public static ArrayList<String> getBlackLanguage(Context context) {
        ArrayList<String> blackLanguages = new ArrayList();
        String blackStrings = null;
        try {
            blackStrings = System.getString(context.getContentResolver(), "black_languages");
        } catch (Exception e) {
            Log.e(TAG, "Could not load default locales", e);
        }
        if (blackStrings != null) {
            for (String localeStr : blackStrings.replace("tl", "fil").split(",")) {
                ULocale locale = ULocale.forLanguageTag(localeStr.replace('_', '-'));
                blackLanguages.add(locale.toLanguageTag());
                blackLanguages.add(ULocale.addLikelySubtags(locale).toLanguageTag());
            }
        }
        return blackLanguages;
    }
}
