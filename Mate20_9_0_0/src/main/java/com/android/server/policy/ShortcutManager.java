package com.android.server.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import com.android.internal.util.XmlUtils;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

class ShortcutManager {
    private static final String ATTRIBUTE_CATEGORY = "category";
    private static final String ATTRIBUTE_CLASS = "class";
    private static final String ATTRIBUTE_PACKAGE = "package";
    private static final String ATTRIBUTE_SHIFT = "shift";
    private static final String ATTRIBUTE_SHORTCUT = "shortcut";
    private static final String TAG = "ShortcutManager";
    private static final String TAG_BOOKMARK = "bookmark";
    private static final String TAG_BOOKMARKS = "bookmarks";
    private final Context mContext;
    private final SparseArray<ShortcutInfo> mShiftShortcuts = new SparseArray();
    private final SparseArray<ShortcutInfo> mShortcuts = new SparseArray();

    private static final class ShortcutInfo {
        public final Intent intent;
        public final String title;

        public ShortcutInfo(String title, Intent intent) {
            this.title = title;
            this.intent = intent;
        }
    }

    public ShortcutManager(Context context) {
        this.mContext = context;
        loadShortcuts();
    }

    public Intent getIntent(KeyCharacterMap kcm, int keyCode, int metaState) {
        ShortcutInfo shortcut = null;
        boolean z = true;
        if ((metaState & 1) != 1) {
            z = false;
        }
        SparseArray<ShortcutInfo> shortcutMap = z ? this.mShiftShortcuts : this.mShortcuts;
        int shortcutChar = kcm.get(keyCode, metaState);
        if (shortcutChar != 0) {
            shortcut = (ShortcutInfo) shortcutMap.get(shortcutChar);
        }
        if (shortcut == null) {
            shortcutChar = Character.toLowerCase(kcm.getDisplayLabel(keyCode));
            if (shortcutChar != 0) {
                shortcut = (ShortcutInfo) shortcutMap.get(shortcutChar);
            }
        }
        return shortcut != null ? shortcut.intent : null;
    }

    private void loadShortcuts() {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            XmlResourceParser parser = this.mContext.getResources().getXml(18284548);
            XmlUtils.beginDocument(parser, TAG_BOOKMARKS);
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() != 1 && TAG_BOOKMARK.equals(parser.getName())) {
                    String packageName = parser.getAttributeValue(null, "package");
                    String className = parser.getAttributeValue(null, "class");
                    String shortcutName = parser.getAttributeValue(null, ATTRIBUTE_SHORTCUT);
                    String categoryName = parser.getAttributeValue(null, ATTRIBUTE_CATEGORY);
                    String shiftName = parser.getAttributeValue(null, ATTRIBUTE_SHIFT);
                    String str;
                    StringBuilder stringBuilder;
                    if (TextUtils.isEmpty(shortcutName)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to get shortcut for: ");
                        stringBuilder.append(packageName);
                        stringBuilder.append(SliceAuthority.DELIMITER);
                        stringBuilder.append(className);
                        Log.w(str, stringBuilder.toString());
                    } else {
                        Intent intent;
                        char shortcutChar = shortcutName.charAt(0);
                        boolean z = shiftName != null && shiftName.equals("true");
                        boolean isShiftShortcut = z;
                        if (packageName != null && className != null) {
                            ActivityInfo info;
                            ComponentName componentName = new ComponentName(packageName, className);
                            try {
                                info = packageManager.getActivityInfo(componentName, 794624);
                            } catch (NameNotFoundException e) {
                                NameNotFoundException e2 = e;
                                componentName = new ComponentName(packageManager.canonicalToCurrentPackageNames(new String[]{packageName})[0], className);
                                try {
                                    info = packageManager.getActivityInfo(componentName, 794624);
                                } catch (NameNotFoundException e3) {
                                    NameNotFoundException nameNotFoundException = e3;
                                    String str2 = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Unable to add bookmark: ");
                                    stringBuilder2.append(packageName);
                                    stringBuilder2.append(SliceAuthority.DELIMITER);
                                    stringBuilder2.append(className);
                                    Log.w(str2, stringBuilder2.toString(), e2);
                                }
                            }
                            intent = new Intent("android.intent.action.MAIN");
                            intent.addCategory("android.intent.category.LAUNCHER");
                            intent.setComponent(componentName);
                            str = info.loadLabel(packageManager).toString();
                        } else if (categoryName != null) {
                            intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", categoryName);
                            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        } else {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unable to add bookmark for shortcut ");
                            stringBuilder.append(shortcutName);
                            stringBuilder.append(": missing package/class or category attributes");
                            Log.w(str, stringBuilder.toString());
                        }
                        ShortcutInfo shortcut = new ShortcutInfo(str, intent);
                        if (isShiftShortcut) {
                            this.mShiftShortcuts.put(shortcutChar, shortcut);
                        } else {
                            this.mShortcuts.put(shortcutChar, shortcut);
                        }
                    }
                } else {
                    return;
                }
            }
        } catch (XmlPullParserException e4) {
            Log.w(TAG, "Got exception parsing bookmarks.", e4);
        } catch (IOException e5) {
            Log.w(TAG, "Got exception parsing bookmarks.", e5);
        }
    }
}
