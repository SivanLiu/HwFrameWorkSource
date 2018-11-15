package com.android.systemui.shared.recents.model;

import android.app.ActivityManager.TaskDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.trustspace.TrustSpaceManager;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.util.LruCache;
import com.android.systemui.shared.R;
import com.android.systemui.shared.recents.model.Task.TaskKey;
import com.android.systemui.shared.system.PackageManagerWrapper;

public abstract class IconLoader {
    public static final int BADGED_TYPE_CLONE = 1;
    public static final int BADGED_TYPE_NONE = 0;
    public static final int BADGED_TYPE_TRUSTSPACE = 2;
    private static final String TAG = "IconLoader";
    protected final LruCache<ComponentName, ActivityInfo> mActivityInfoCache;
    protected final Context mContext;
    protected final IconDrawableFactory mDrawableFactory;
    protected final TaskKeyLruCache<Drawable> mIconCache;

    public static class DefaultIconLoader extends IconLoader {
        private final BitmapDrawable mDefaultIcon;

        public DefaultIconLoader(Context context, TaskKeyLruCache<Drawable> iconCache, LruCache<ComponentName, ActivityInfo> activityInfoCache) {
            super(context, iconCache, activityInfoCache);
            Bitmap icon = Bitmap.createBitmap(1, 1, Config.ALPHA_8);
            icon.eraseColor(0);
            this.mDefaultIcon = new BitmapDrawable(context.getResources(), icon);
        }

        public Drawable getDefaultIcon(int userId) {
            return this.mDefaultIcon;
        }

        protected Drawable createBadgedDrawable(Drawable icon, int userId, TaskDescription desc) {
            return createBadgedDrawable(icon, userId, 0);
        }

        protected Drawable getBadgedActivityIcon(ActivityInfo info, int userId, TaskDescription desc) {
            return getBadgedActivityIcon(info, userId, 0);
        }

        protected Drawable getBadgedActivityIcon(ActivityInfo info, int userId, int badgedIconType) {
            return getBadgedIcon(this.mDrawableFactory.getBadgedIcon(info, info.applicationInfo, userId), userId, badgedIconType);
        }
    }

    protected abstract Drawable createBadgedDrawable(Drawable drawable, int i, TaskDescription taskDescription);

    protected abstract Drawable getBadgedActivityIcon(ActivityInfo activityInfo, int i, TaskDescription taskDescription);

    public abstract Drawable getDefaultIcon(int i);

    public IconLoader(Context context, TaskKeyLruCache<Drawable> iconCache, LruCache<ComponentName, ActivityInfo> activityInfoCache) {
        this.mContext = context;
        this.mIconCache = iconCache;
        this.mActivityInfoCache = activityInfoCache;
        this.mDrawableFactory = IconDrawableFactory.newInstance(context);
    }

    public ActivityInfo getAndUpdateActivityInfo(TaskKey taskKey) {
        ComponentName cn = taskKey.getComponent();
        ActivityInfo activityInfo = (ActivityInfo) this.mActivityInfoCache.get(cn);
        if (activityInfo == null) {
            activityInfo = PackageManagerWrapper.getInstance().getActivityInfo(cn, taskKey.userId);
            if (cn == null || activityInfo == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected null component name or activity info: ");
                stringBuilder.append(cn);
                stringBuilder.append(", ");
                stringBuilder.append(activityInfo);
                Log.e(str, stringBuilder.toString());
                return null;
            }
            this.mActivityInfoCache.put(cn, activityInfo);
        }
        return activityInfo;
    }

    public Drawable getIcon(Task t) {
        Drawable cachedIcon = (Drawable) this.mIconCache.get(t.key);
        if (cachedIcon != null) {
            return cachedIcon;
        }
        cachedIcon = createNewIconForTask(t.key, t.taskDescription, true);
        this.mIconCache.put(t.key, cachedIcon);
        return cachedIcon;
    }

    public Drawable getAndInvalidateIfModified(TaskKey taskKey, TaskDescription td, boolean loadIfNotCached) {
        Drawable icon = (Drawable) this.mIconCache.getAndInvalidateIfModified(taskKey);
        if (icon != null) {
            return icon;
        }
        if (loadIfNotCached) {
            icon = createNewIconForTask(taskKey, td, false);
            if (icon != null) {
                this.mIconCache.put(taskKey, icon);
                return icon;
            }
        }
        return null;
    }

    private Drawable createNewIconForTask(TaskKey taskKey, TaskDescription desc, boolean returnDefault) {
        int userId = taskKey.userId;
        int badgedIconType = getBadgedIconType(taskKey.baseIntent.getHwFlags(), taskKey.getComponent().getPackageName());
        Bitmap tdIcon = desc.getInMemoryIcon();
        if (tdIcon != null) {
            return createDrawableFromBitmap(tdIcon, userId, badgedIconType);
        }
        if (desc.getIconResource() != 0) {
            try {
                return createBadgedDrawable(this.mContext.getDrawable(desc.getIconResource()), userId, badgedIconType);
            } catch (NotFoundException e) {
                Log.e(TAG, "Could not find icon drawable from resource", e);
            }
        }
        tdIcon = TaskDescription.loadTaskDescriptionIcon(desc.getIconFilename(), userId);
        if (tdIcon != null) {
            return createDrawableFromBitmap(tdIcon, userId, badgedIconType);
        }
        ActivityInfo activityInfo = getAndUpdateActivityInfo(taskKey);
        if (activityInfo != null) {
            Drawable icon = getBadgedActivityIcon(activityInfo, userId, badgedIconType);
            if (icon != null) {
                return icon;
            }
        }
        return returnDefault ? getDefaultIcon(userId) : null;
    }

    protected Drawable createDrawableFromBitmap(Bitmap icon, int userId, TaskDescription desc) {
        return createBadgedDrawable(new BitmapDrawable(this.mContext.getResources(), icon), userId, desc);
    }

    protected Drawable createBadgedDrawable(Drawable icon, int userId, int badgedIconType) {
        if (userId != UserHandle.myUserId()) {
            return this.mContext.getPackageManager().getUserBadgedIcon(icon, new UserHandle(userId));
        }
        return icon;
    }

    protected Drawable createDrawableFromBitmap(Bitmap icon, int userId, int badgedIconType) {
        return createBadgedDrawable(new BitmapDrawable(this.mContext.getResources(), icon), userId, badgedIconType);
    }

    protected Drawable getBadgedActivityIcon(ActivityInfo info, int userId, int badgedIconType) {
        return getBadgedIcon(this.mDrawableFactory.getBadgedIcon(info, info.applicationInfo, userId), userId, badgedIconType);
    }

    protected Drawable getBadgedIcon(Drawable icon, int userId, int badgedIconType) {
        if (userId != UserHandle.myUserId()) {
            return this.mContext.getPackageManager().getUserBadgedIcon(icon, new UserHandle(userId));
        }
        if (badgedIconType == 1) {
            return this.mContext.getPackageManager().getUserBadgedIcon(icon, new UserHandle(2147383647));
        }
        if (badgedIconType == 2) {
            return getTrustSpaceBadgedDrawable(this.mContext.getResources(), icon);
        }
        return icon;
    }

    public static Drawable getTrustSpaceBadgedDrawable(Resources res, Drawable drawable) {
        int badgedWidth = drawable.getIntrinsicWidth();
        int badgedHeight = drawable.getIntrinsicHeight();
        try {
            Drawable badgeDrawable = res.getDrawable(R.drawable.ic_trustspace_badge, null);
            Bitmap bitmap = Bitmap.createBitmap(badgedWidth, badgedHeight, Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, badgedWidth, badgedHeight);
            drawable.draw(canvas);
            badgeDrawable.setBounds(0, 0, badgedWidth, badgedHeight);
            badgeDrawable.draw(canvas);
            return new BitmapDrawable(bitmap);
        } catch (NotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not find badgeDrawable from resource");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return drawable;
        }
    }

    public static int getBadgedIconType(int hwFlags, String packageName) {
        int i = 0;
        if ((hwFlags & 1) != 0) {
            return 1;
        }
        if (TrustSpaceManager.getDefault().isIntentProtectedApp(packageName)) {
            i = 2;
        }
        return i;
    }
}
