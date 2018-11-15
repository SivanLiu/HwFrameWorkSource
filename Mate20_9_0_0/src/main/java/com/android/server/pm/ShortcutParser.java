package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;

public class ShortcutParser {
    private static final boolean DEBUG = false;
    @VisibleForTesting
    static final String METADATA_KEY = "android.app.shortcuts";
    private static final String TAG = "ShortcutService";
    private static final String TAG_CATEGORIES = "categories";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_SHORTCUTS = "shortcuts";

    public static List<ShortcutInfo> parseShortcuts(ShortcutService service, String packageName, int userId) throws IOException, XmlPullParserException {
        List<ResolveInfo> activities = service.injectGetMainActivities(packageName, userId);
        if (activities == null || activities.size() == 0) {
            return null;
        }
        List<ShortcutInfo> result = null;
        try {
            int size = activities.size();
            for (int i = 0; i < size; i++) {
                ActivityInfo activityInfoNoMetadata = ((ResolveInfo) activities.get(i)).activityInfo;
                if (activityInfoNoMetadata != null) {
                    ActivityInfo activityInfoWithMetadata = service.getActivityInfoWithMetadata(activityInfoNoMetadata.getComponentName(), userId);
                    if (activityInfoWithMetadata != null) {
                        result = parseShortcutsOneFile(service, activityInfoWithMetadata, packageName, userId, result);
                    }
                }
            }
            return result;
        } catch (RuntimeException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception caught while parsing shortcut XML for package=");
            stringBuilder.append(packageName);
            service.wtf(stringBuilder.toString(), e);
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:131:0x02b7  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x02b7  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static List<ShortcutInfo> parseShortcutsOneFile(ShortcutService service, ActivityInfo activityInfo, String packageName, int userId, List<ShortcutInfo> result) throws IOException, XmlPullParserException {
        Throwable th;
        ShortcutService shortcutService = service;
        ActivityInfo activityInfo2 = activityInfo;
        XmlResourceParser parser;
        List<ShortcutInfo> result2;
        try {
            parser = shortcutService.injectXmlMetaData(activityInfo2, METADATA_KEY);
            if (parser == null) {
                if (parser != null) {
                    parser.close();
                }
                return result;
            }
            try {
                ComponentName activity = new ComponentName(packageName, activityInfo2.name);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                int maxShortcuts = service.getMaxActivityShortcuts();
                ArrayList<Intent> intents = new ArrayList();
                int rank = 0;
                int numShortcuts = 0;
                ShortcutInfo currentShortcut = null;
                Set categories = null;
                List<ShortcutInfo> result3 = result;
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if (next == 1) {
                        result2 = result3;
                        break;
                    }
                    if (type == 3) {
                        try {
                            if (parser.getDepth() <= 0) {
                                result2 = result3;
                                break;
                            }
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Shortcut's extras contain un-persistable values. Skipping it.");
                        } catch (Throwable th2) {
                            th = th2;
                            result2 = result3;
                        }
                    }
                    try {
                        ComponentName activity2;
                        int depth;
                        String tag;
                        AttributeSet attrs2;
                        ArrayList<Intent> intents2;
                        AttributeSet attrs3;
                        String str;
                        ShortcutInfo si;
                        int depth2 = parser.getDepth();
                        String tag2 = parser.getName();
                        if (type == 3) {
                            activity2 = activity;
                            depth = depth2;
                            if (depth == 2) {
                                tag = tag2;
                                if (!TAG_SHORTCUT.equals(tag)) {
                                    attrs2 = attrs;
                                } else if (currentShortcut == null) {
                                    intents2 = intents;
                                    depth2 = numShortcuts;
                                    attrs3 = attrs;
                                    intents = activity2;
                                    result2 = result3;
                                    str = packageName;
                                    activity = intents;
                                    intents = intents2;
                                    result3 = result2;
                                    numShortcuts = depth2;
                                    activityInfo2 = activityInfo;
                                    attrs = attrs3;
                                } else {
                                    String str2;
                                    StringBuilder stringBuilder;
                                    si = currentShortcut;
                                    if (!si.isEnabled()) {
                                        attrs2 = attrs;
                                        intents.clear();
                                        intents.add(new Intent("android.intent.action.VIEW"));
                                    } else if (intents.size() == 0) {
                                        str2 = TAG;
                                        stringBuilder = new StringBuilder();
                                        attrs2 = attrs;
                                        stringBuilder.append("Shortcut ");
                                        stringBuilder.append(si.getId());
                                        stringBuilder.append(" has no intent. Skipping it.");
                                        Log.e(str2, stringBuilder.toString());
                                        currentShortcut = null;
                                        activity = activity2;
                                        attrs = attrs2;
                                        str = packageName;
                                    } else {
                                        attrs2 = attrs;
                                    }
                                    if (numShortcuts >= maxShortcuts) {
                                        str2 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("More than ");
                                        stringBuilder.append(maxShortcuts);
                                        stringBuilder.append(" shortcuts found for ");
                                        stringBuilder.append(activityInfo.getComponentName());
                                        stringBuilder.append(". Skipping the rest.");
                                        Log.e(str2, stringBuilder.toString());
                                        if (parser != null) {
                                            parser.close();
                                        }
                                        return result3;
                                    }
                                    ((Intent) intents.get(0)).addFlags(268484608);
                                    si.setIntents((Intent[]) intents.toArray(new Intent[intents.size()]));
                                    intents.clear();
                                    if (categories != null) {
                                        si.setCategories(categories);
                                        categories = null;
                                    }
                                    if (result3 == null) {
                                        result3 = new ArrayList();
                                    }
                                    result3.add(si);
                                    numShortcuts++;
                                    rank++;
                                    currentShortcut = null;
                                    activity = activity2;
                                    attrs = attrs2;
                                    str = packageName;
                                }
                            } else {
                                attrs2 = attrs;
                                tag = tag2;
                            }
                        } else {
                            activity2 = activity;
                            attrs2 = attrs;
                            depth = depth2;
                            tag = tag2;
                        }
                        if (type == 2 && !(depth == 1 && TAG_SHORTCUTS.equals(tag))) {
                            if (depth == 2) {
                                if (TAG_SHORTCUT.equals(tag)) {
                                    int i = 1;
                                    result2 = result3;
                                    intents2 = intents;
                                    depth2 = numShortcuts;
                                    try {
                                        si = parseShortcutAttributes(shortcutService, attrs2, packageName, activity2, userId, rank);
                                        if (si != null) {
                                            if (result2 != null) {
                                                tag = result2.size() - 1;
                                                while (tag >= null) {
                                                    if (si.getId().equals(((ShortcutInfo) result2.get(tag)).getId())) {
                                                        Log.e(TAG, "Duplicate shortcut ID detected. Skipping it.");
                                                    } else {
                                                        tag--;
                                                    }
                                                }
                                            }
                                            currentShortcut = si;
                                            categories = null;
                                            str = packageName;
                                            intents = intents2;
                                            result3 = result2;
                                            numShortcuts = depth2;
                                            activity = activity2;
                                            attrs = attrs2;
                                            activityInfo2 = activityInfo;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        if (parser != null) {
                                        }
                                        throw th;
                                    }
                                }
                            }
                            str = tag;
                            int i2 = type;
                            result2 = result3;
                            intents2 = intents;
                            depth2 = numShortcuts;
                            String str3;
                            StringBuilder stringBuilder2;
                            if (depth == 3 && "intent".equals(str)) {
                                if (currentShortcut == null) {
                                    intents = activity2;
                                    attrs3 = attrs2;
                                } else if (currentShortcut.isEnabled()) {
                                    attrs3 = attrs2;
                                    Intent intent = Intent.parseIntent(shortcutService.mContext.getResources(), parser, attrs3);
                                    if (TextUtils.isEmpty(intent.getAction())) {
                                        str3 = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Shortcut intent action must be provided. activity=");
                                        intents = activity2;
                                        stringBuilder2.append(intents);
                                        Log.e(str3, stringBuilder2.toString());
                                        currentShortcut = null;
                                    } else {
                                        intents = activity2;
                                        intents2.add(intent);
                                        str = packageName;
                                        activity = intents;
                                        intents = intents2;
                                        result3 = result2;
                                        numShortcuts = depth2;
                                        activityInfo2 = activityInfo;
                                        attrs = attrs3;
                                    }
                                } else {
                                    intents = activity2;
                                    attrs3 = attrs2;
                                }
                                Log.e(TAG, "Ignoring excessive intent tag.");
                                str = packageName;
                                activity = intents;
                                intents = intents2;
                                result3 = result2;
                                numShortcuts = depth2;
                                activityInfo2 = activityInfo;
                                attrs = attrs3;
                            } else {
                                intents = activity2;
                                attrs3 = attrs2;
                                if (depth == 3 && TAG_CATEGORIES.equals(str)) {
                                    if (currentShortcut != null && currentShortcut.getCategories() == null) {
                                        String name = parseCategories(shortcutService, attrs3);
                                        if (TextUtils.isEmpty(name)) {
                                            str3 = TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Empty category found. activity=");
                                            stringBuilder2.append(intents);
                                            Log.e(str3, stringBuilder2.toString());
                                        } else {
                                            if (categories == null) {
                                                categories = new ArraySet();
                                            }
                                            categories.add(name);
                                        }
                                    }
                                    str = packageName;
                                    activity = intents;
                                    intents = intents2;
                                    result3 = result2;
                                    numShortcuts = depth2;
                                    activityInfo2 = activityInfo;
                                    attrs = attrs3;
                                } else {
                                    Log.w(TAG, String.format("Invalid tag '%s' found at depth %d", new Object[]{str, Integer.valueOf(depth)}));
                                    str = packageName;
                                    activity = intents;
                                    intents = intents2;
                                    result3 = result2;
                                    numShortcuts = depth2;
                                    activityInfo2 = activityInfo;
                                    attrs = attrs3;
                                }
                            }
                            str = packageName;
                            activity = intents;
                            intents = intents2;
                            result3 = result2;
                            numShortcuts = depth2;
                            activityInfo2 = activityInfo;
                            attrs = attrs3;
                        } else {
                            result2 = result3;
                            intents2 = intents;
                            depth2 = numShortcuts;
                        }
                        intents = activity2;
                        attrs3 = attrs2;
                        str = packageName;
                        activity = intents;
                        intents = intents2;
                        result3 = result2;
                        numShortcuts = depth2;
                        activityInfo2 = activityInfo;
                        attrs = attrs3;
                    } catch (Throwable th4) {
                        th = th4;
                        result2 = result3;
                    }
                }
                if (parser != null) {
                    parser.close();
                }
                return result2;
            } catch (Throwable th5) {
                th = th5;
                result2 = result;
                if (parser != null) {
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            result2 = result;
            parser = null;
            if (parser != null) {
                parser.close();
            }
            throw th;
        }
    }

    private static String parseCategories(ShortcutService service, AttributeSet attrs) {
        TypedArray sa = service.mContext.getResources().obtainAttributes(attrs, R.styleable.ShortcutCategories);
        try {
            if (sa.getType(0) == 3) {
                String nonResourceString = sa.getNonResourceString(0);
                return nonResourceString;
            }
            Log.w(TAG, "android:name for shortcut category must be string literal.");
            sa.recycle();
            return null;
        } finally {
            sa.recycle();
        }
    }

    private static ShortcutInfo parseShortcutAttributes(ShortcutService service, AttributeSet attrs, String packageName, ComponentName activity, int userId, int rank) {
        ComponentName componentName = activity;
        ShortcutService shortcutService = service;
        TypedArray sa = shortcutService.mContext.getResources().obtainAttributes(attrs, R.styleable.Shortcut);
        try {
            ShortcutInfo shortcutInfo = null;
            String str;
            if (sa.getType(2) != 3) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("android:shortcutId must be string literal. activity=");
                stringBuilder.append(componentName);
                Log.w(str, stringBuilder.toString());
                return shortcutInfo;
            }
            str = sa.getNonResourceString(2);
            boolean enabled = sa.getBoolean(1, true);
            int iconResId = sa.getResourceId(0, 0);
            int titleResId = sa.getResourceId(3, 0);
            int textResId = sa.getResourceId(4, 0);
            int disabledMessageResId = sa.getResourceId(5, 0);
            String str2;
            StringBuilder stringBuilder2;
            if (TextUtils.isEmpty(str)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("android:shortcutId must be provided. activity=");
                stringBuilder2.append(componentName);
                Log.w(str2, stringBuilder2.toString());
                sa.recycle();
                return null;
            } else if (titleResId == 0) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("android:shortcutShortLabel must be provided. activity=");
                stringBuilder2.append(componentName);
                Log.w(str2, stringBuilder2.toString());
                sa.recycle();
                return null;
            } else {
                shortcutInfo = str;
                ShortcutInfo createShortcutFromManifest = createShortcutFromManifest(shortcutService, userId, shortcutInfo, packageName, componentName, titleResId, textResId, disabledMessageResId, rank, iconResId, enabled);
                sa.recycle();
                return createShortcutFromManifest;
            }
        } finally {
            sa.recycle();
        }
    }

    private static ShortcutInfo createShortcutFromManifest(ShortcutService service, int userId, String id, String packageName, ComponentName activityComponent, int titleResId, int textResId, int disabledMessageResId, int rank, int iconResId, boolean enabled) {
        int i = 0;
        int flags = ((enabled ? 32 : 64) | 256) | (iconResId != 0 ? 4 : 0);
        if (!enabled) {
            i = 1;
        }
        return new ShortcutInfo(userId, id, packageName, activityComponent, null, null, titleResId, null, null, textResId, null, null, disabledMessageResId, null, null, null, rank, null, service.injectCurrentTimeMillis(), flags, iconResId, null, null, i);
    }
}
