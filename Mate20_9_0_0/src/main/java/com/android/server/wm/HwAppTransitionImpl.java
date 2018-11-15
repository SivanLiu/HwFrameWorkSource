package com.android.server.wm;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import com.android.server.AttributeCache.Entry;

public class HwAppTransitionImpl implements IHwAppTransition {
    public static final boolean IS_EMUI_LITE = SystemProperties.getBoolean("ro.build.hw_emui_lite.enable", false);
    public static final boolean IS_NOVA_PERF = SystemProperties.getBoolean("ro.config.hw_nova_performance", false);
    private static final String TAG = "HwAppTransitionImpl";
    private Context mHwextContext = null;

    public Entry overrideAnimation(LayoutParams lp, int animAttr, Context mContext, Entry mEnt, AppTransition appTransition) {
        int i = animAttr;
        Entry entry = mEnt;
        AppTransition appTransition2 = appTransition;
        Entry ent = null;
        Context context;
        if (entry != null) {
            Context context2 = entry.context;
            if (entry.array.getResourceId(i, 0) != 0) {
                String packageName = "androidhwext";
                if (this.mHwextContext == null) {
                    try {
                        this.mHwextContext = mContext.createPackageContext(packageName, 0);
                    } catch (NameNotFoundException e) {
                        Log.e(TAG, "overrideAnimation : no hwext package");
                    }
                } else {
                    context = mContext;
                }
                if (this.mHwextContext != null) {
                    int anim = 0;
                    int hwAnimResId = 0;
                    String title = lp.getTitle().toString();
                    if (!(title == null || title.equals(""))) {
                        Resources resources;
                        StringBuilder stringBuilder;
                        if (IS_EMUI_LITE || IS_NOVA_PERF) {
                            resources = this.mHwextContext.getResources();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("HwAnimation_lite.");
                            stringBuilder.append(title);
                            hwAnimResId = resources.getIdentifier(stringBuilder.toString(), "style", packageName);
                        } else {
                            resources = this.mHwextContext.getResources();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("HwAnimation.");
                            stringBuilder.append(title);
                            hwAnimResId = resources.getIdentifier(stringBuilder.toString(), "style", packageName);
                        }
                        if (hwAnimResId != 0) {
                            ent = appTransition2.getCachedAnimations(packageName, hwAnimResId);
                            if (ent != null) {
                                context2 = ent.context;
                                anim = ent.array.getResourceId(i, 0);
                            }
                        }
                    }
                    if ((IS_EMUI_LITE || IS_NOVA_PERF) && hwAnimResId == 0) {
                        hwAnimResId = this.mHwextContext.getResources().getIdentifier("HwAnimation_lite", "style", packageName);
                        if (hwAnimResId != 0) {
                            ent = appTransition2.getCachedAnimations(packageName, hwAnimResId);
                            if (ent != null) {
                                context2 = ent.context;
                                anim = ent.array.getResourceId(i, 0);
                            }
                        }
                    }
                    if (anim == 0) {
                        hwAnimResId = this.mHwextContext.getResources().getIdentifier("HwAnimation", "style", packageName);
                        if (hwAnimResId != 0) {
                            ent = appTransition2.getCachedAnimations(packageName, hwAnimResId);
                            if (ent != null) {
                                context2 = ent.context;
                                anim = ent.array.getResourceId(i, 0);
                            }
                        }
                    }
                    int i2 = hwAnimResId;
                    hwAnimResId = anim;
                    anim = i2;
                    if (hwAnimResId == 0) {
                        return null;
                    }
                }
            }
            context = mContext;
            return ent;
        }
        context = mContext;
        return null;
    }
}
