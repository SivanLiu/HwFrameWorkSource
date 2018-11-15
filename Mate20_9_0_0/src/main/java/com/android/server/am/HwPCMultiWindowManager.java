package com.android.server.am;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ActivityInfo.WindowLayout;
import android.content.res.HwPCMultiWindowCompatibility;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.HwPCUtils;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayInfo;
import com.android.internal.R;
import com.android.server.AttributeCache;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.security.trustcircle.utils.LogHelper;
import com.android.server.wm.HwWindowManagerService;
import com.huawei.pgmng.plug.PGSdk;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HwPCMultiWindowManager {
    private static final int DP_WINDOW_OVERLAP_OFFSET = 30;
    private static final int DP_WINDOW_OVERLAP_OFFSET_MIN = 15;
    static final int FIX_ORIENTATION_LANDSCAPE = 2;
    static final int FIX_ORIENTATION_NONE = 0;
    static final int FIX_ORIENTATION_PORTRAIT = 1;
    private static final int MAX_TIMES_TO_ADJUST_BOUNDS = 30;
    private static final int MSG_REQUEST_ORIENTATION = 1;
    private static final int MSG_REQUEST_ORIENTATION_DELAY = 200;
    static final int SPECIAL_PACKAGE_TYPE_FULLSCREEN_NO_NAVIGATIONBAR = 7;
    static final int SPECIAL_PACKAGE_TYPE_MAXIMIZED_ONLY = 3;
    static final int SPECIAL_PACKAGE_TYPE_NEED_DELAY = 2;
    static final int SPECIAL_PACKAGE_TYPE_PAD_FULLSCREEN = 6;
    static final int SPECIAL_PACKAGE_TYPE_PORTRAIT = 4;
    static final int SPECIAL_PACKAGE_TYPE_PORTRAIT_MAXIMIZED = 5;
    static final int SPECIAL_PACKAGE_TYPE_VIDEO_NEED_FULLSCREEN = 1;
    static final String TAG = "HwPCMultiWindowManager";
    private static final Object mLock = new Object();
    private static volatile HwPCMultiWindowManager mSingleInstance = null;
    private int mCurDisplayId = -1;
    private int mDecorHeight = 0;
    final HashMap<String, HashMap<String, Entry>> mEntries;
    boolean mFixOrientationChanged = false;
    final List<String> mFullscreenNoNavigationBar = new ArrayList();
    final Handler mHandler;
    int mLastRequestedOrientation = 0;
    final List<String> mMaximizedOnlyList = new ArrayList();
    final List<String> mNeedDelayList = new ArrayList();
    private Point mNonDecorScreenSize = new Point();
    final List<String> mPadFullscreenList = new ArrayList();
    final List<String> mPortraitMaximizedPkgList = new ArrayList();
    final List<String> mPortraitPkgList = new ArrayList();
    final ActivityManagerService mService;
    final HwPCMultiWindowSettingsWriter mSettingsWriter;
    final List<String> mSpecialVideosList = new ArrayList();
    private Set<Integer> mUpdateTaskSet = Collections.synchronizedSet(new HashSet());

    public static class Entry {
        public int originalWindowState = this.windowState;
        public final String pkgName;
        public Rect windowBounds = new Rect();
        public int windowState = 1;

        public Entry(String _name) {
            this.pkgName = _name;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pkgName=");
            stringBuilder.append(this.pkgName);
            stringBuilder.append("; windowState=");
            stringBuilder.append(Integer.toHexString(this.windowState));
            stringBuilder.append("; originalWindowState=");
            stringBuilder.append(Integer.toHexString(this.originalWindowState));
            stringBuilder.append("; bounds=");
            stringBuilder.append(this.windowBounds == null ? "null" : this.windowBounds.toShortString());
            return stringBuilder.toString();
        }
    }

    private final class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                synchronized (HwPCMultiWindowManager.this.mService) {
                    TaskRecord tr = HwPCMultiWindowManager.this.mService.mStackSupervisor.anyTaskForIdLocked(msg.arg2);
                    if (tr != null) {
                        HwPCMultiWindowManager.this.updateTaskByRequestedOrientationInternal(tr, msg.arg1);
                    }
                }
            }
        }
    }

    public static HwPCMultiWindowManager getInstance(ActivityManagerService service) {
        if (mSingleInstance == null) {
            synchronized (mLock) {
                if (mSingleInstance == null) {
                    mSingleInstance = new HwPCMultiWindowManager(service);
                }
            }
        }
        return mSingleInstance;
    }

    private HwPCMultiWindowManager(ActivityManagerService service) {
        this.mService = service;
        this.mEntries = new HashMap();
        HwPCMultiWindowPolicy.initialize(getRightContext(this.mService.mContext), this);
        this.mSettingsWriter = new HwPCMultiWindowSettingsWriter(this);
        this.mHandler = new WorkerHandler(this.mService.mHandler.getLooper());
        this.mUpdateTaskSet.clear();
    }

    public void putEntry(String deviceKey, String entryKey, Entry entry) {
        if (!TextUtils.isEmpty(deviceKey) && !TextUtils.isEmpty(entryKey)) {
            if (!this.mEntries.containsKey(deviceKey)) {
                this.mEntries.put(deviceKey, new HashMap());
            }
            ((HashMap) this.mEntries.get(deviceKey)).put(entryKey, entry);
        }
    }

    public void putEntryForCurDevice(String entryKey, Entry entry) {
        putEntry(getCurDeviceKey(), entryKey, entry);
    }

    public Entry getEntry(String entryKey) {
        String deviceKey = getCurDeviceKey();
        if (this.mEntries.containsKey(deviceKey)) {
            return (Entry) ((HashMap) this.mEntries.get(deviceKey)).get(entryKey);
        }
        return null;
    }

    private void removeEntry(String entryKey) {
        String deviceKey = getCurDeviceKey();
        if (this.mEntries.containsKey(deviceKey) && ((HashMap) this.mEntries.get(deviceKey)).containsKey(entryKey)) {
            ((HashMap) this.mEntries.get(deviceKey)).remove(entryKey);
        }
    }

    private String getCurDeviceKey() {
        Point deviceSize = new Point();
        Display display = getExtDisplay(null);
        if (display != null) {
            display.getRealSize(deviceSize);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(deviceSize.x);
        stringBuilder.append(Constant.RESULT_SEPERATE);
        stringBuilder.append(deviceSize.y);
        return stringBuilder.toString();
    }

    public void storeTaskSettings(TaskRecord record) {
        if (record != null && record.mRootActivityInfo != null) {
            String name = record.mRootActivityInfo.packageName;
            Entry entry = getEntry(name);
            if (entry == null) {
                entry = new Entry(name);
            }
            calStoredEntry(record, entry);
            putEntryForCurDevice(name, entry);
            this.mSettingsWriter.scheduleWrite();
        }
    }

    private void calStoredEntry(TaskRecord record, Entry entry) {
        if (record != null && record.mRootActivityInfo != null && entry != null) {
            entry.originalWindowState = record.mOriginalWindowState;
            boolean hasBounds = HwPCMultiWindowCompatibility.isLayoutHadBounds(record.mWindowState);
            if ((record instanceof HwTaskRecord) && ((HwTaskRecord) record).mSaveBounds) {
                entry.windowState = ((HwTaskRecord) record).mWindowState;
                if (hasBounds && isInScreen(record.getOverrideBounds())) {
                    entry.windowBounds.set(record.getOverrideBounds());
                }
            } else if (hasBounds && isInScreen(record.getOverrideBounds())) {
                entry.windowBounds.offsetTo(record.getOverrideBounds().left, record.getOverrideBounds().top);
            }
        }
    }

    public void restoreTaskWindowState(TaskRecord record) {
        if (record != null && record.mRootActivityInfo != null) {
            record.mOriginalWindowState = getWindowStateByDefault(record, record.mRootActivityInfo.screenOrientation);
            record.mNextWindowState = getWindowState(record);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("restoreTaskWindowState: (N:");
            stringBuilder.append(Integer.toHexString(record.mNextWindowState));
            stringBuilder.append(", O:");
            stringBuilder.append(Integer.toHexString(record.mOriginalWindowState));
            stringBuilder.append(", C:");
            stringBuilder.append(Integer.toHexString(record.mWindowState));
            stringBuilder.append(")");
            HwPCUtils.log(str, stringBuilder.toString());
        }
    }

    private int getWindowState(TaskRecord record) {
        int windowState = getWindowStateBySaved(record);
        if (windowState < 0) {
            return record.mOriginalWindowState;
        }
        return windowState;
    }

    private int getWindowStateBySaved(TaskRecord record) {
        String pkgName = record.mRootActivityInfo.packageName;
        Entry entry = getEntry(pkgName);
        if (entry == null) {
            return -1;
        }
        if (record.mOriginalWindowState == entry.originalWindowState) {
            return entry.windowState;
        }
        removeEntry(pkgName);
        return -1;
    }

    private boolean isPadFullscreen(TaskRecord record) {
        if (!HwPCUtils.enabledInPad() || record == null || record.mRootActivityInfo == null || (!this.mPadFullscreenList.contains(record.mRootActivityInfo.packageName) && !this.mPadFullscreenList.contains(record.mRootActivityInfo.name.toLowerCase()))) {
            return false;
        }
        return true;
    }

    private boolean isMaximizedOnly(TaskRecord record) {
        if (record == null || record.mRootActivityInfo == null || !this.mMaximizedOnlyList.contains(record.mRootActivityInfo.packageName)) {
            return false;
        }
        return true;
    }

    private boolean isFullscreenNoNavigationBar(TaskRecord record) {
        if (record == null || record.mRootActivityInfo == null || !this.mFullscreenNoNavigationBar.contains(record.mRootActivityInfo.packageName.toLowerCase())) {
            return false;
        }
        return true;
    }

    private boolean isMaximizedButPortrait(TaskRecord record) {
        if (record == null || record.mRootActivityInfo == null || !this.mPortraitMaximizedPkgList.contains(record.mRootActivityInfo.packageName)) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:35:0x0083, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isFullscreenOnly(TaskRecord record) {
        boolean z = false;
        if (record == null || record.mRootActivityInfo == null || record.mRootActivityInfo.packageName.equals("com.android.browser")) {
            return false;
        }
        if (HwPCUtils.enabledInPad() && record.mRootActivityInfo.packageName.equals("com.example.android.notepad")) {
            return false;
        }
        int realTheme = record.mRootActivityInfo.getThemeResource();
        if (realTheme == 0) {
            realTheme = record.mRootActivityInfo.applicationInfo.targetSdkVersion < 11 ? 16973829 : 16973931;
        }
        com.android.server.AttributeCache.Entry ent = AttributeCache.instance().get(record.mRootActivityInfo.packageName, realTheme, R.styleable.Window, UserHandle.getUserId(record.mRootActivityInfo.applicationInfo.uid));
        boolean isFloating = ent != null && ent.array.getBoolean(4, false);
        if (isFloating) {
            return true;
        }
        try {
            if (PGSdk.getInstance().getPkgType(this.mService.mContext, record.mRootActivityInfo.packageName) == 5) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPortraitApp(TaskRecord record) {
        if (record == null || record.mRootActivityInfo == null) {
            return false;
        }
        return isPortraitOnly(record, record.mRootActivityInfo.screenOrientation);
    }

    /* JADX WARNING: Missing block: B:20:0x002f, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isPortraitOnly(TaskRecord record, int requestOrientation) {
        if (record == null || record.mRootActivityInfo == null) {
            return false;
        }
        if (requestOrientation == 1 || requestOrientation == 7 || requestOrientation == 9 || requestOrientation == 12 || ((requestOrientation == 14 && !HwPCUtils.enabledInPad()) || this.mPortraitPkgList.contains(record.mRootActivityInfo.packageName))) {
            return true;
        }
        return false;
    }

    private boolean isResizeable(TaskRecord record) {
        boolean z = false;
        if (record == null || record.mRootActivityInfo == null) {
            return false;
        }
        if ((record.mRootActivityInfo.applicationInfo.flags & 1) == 0) {
            return ActivityInfo.isResizeableMode(record.mResizeMode);
        }
        if (2 == record.mResizeMode) {
            z = true;
        }
        return z;
    }

    private int getWindowStateByDefault(TaskRecord record, int requestOrientation) {
        if (record == null || record.mRootActivityInfo == null) {
            return HwPCMultiWindowCompatibility.getLandscapeWithPartAction();
        }
        if (isFullscreenNoNavigationBar(record)) {
            return 132612;
        }
        if (isPadFullscreen(record)) {
            return 4;
        }
        if (isMaximizedOnly(record)) {
            return 3;
        }
        if (isFullscreenOnly(record)) {
            return 4;
        }
        if (isMaximizedButPortrait(record)) {
            return 513;
        }
        if (isPortraitOnly(record, requestOrientation)) {
            if (isResizeablePortraitType(record, record.mRootActivityInfo.packageName)) {
                return 1538;
            }
            return 1;
        } else if (isResizeable(record)) {
            return HwPCMultiWindowCompatibility.getLandscapeWithAllAction();
        } else {
            return HwPCMultiWindowCompatibility.getLandscapeWithPartAction();
        }
    }

    private boolean isResizeablePortraitType(TaskRecord record, String pkgName) {
        return isResizeable(record) && !this.mPortraitPkgList.contains(pkgName);
    }

    static boolean isFixedOrientationPortrait(int screenOrientation) {
        return screenOrientation == 1 || screenOrientation == 7 || screenOrientation == 9 || screenOrientation == 12;
    }

    static boolean isFixedOrientationLandscape(int screenOrientation) {
        return screenOrientation == 0 || screenOrientation == 6 || screenOrientation == 8 || screenOrientation == 11;
    }

    public Rect getWindowBounds(TaskRecord record) {
        Rect bounds = getWindowBoundsBySaved(record);
        if (bounds == null || bounds.isEmpty()) {
            return getWindowBoundsByDefault(getWindowStateByDefault(record, record.mRootActivityInfo.screenOrientation));
        }
        return bounds;
    }

    private Rect getWindowBoundsBySaved(TaskRecord record) {
        Entry entry = getEntry(record.mRootActivityInfo.packageName);
        if (entry == null) {
            return null;
        }
        return entry.windowBounds;
    }

    private boolean isInScreen(Rect bounds) {
        boolean z = false;
        if (bounds == null || bounds.isEmpty()) {
            return false;
        }
        Point size = getExtDisplaySize();
        if (bounds.left >= 0 && bounds.right <= size.x && bounds.top >= 0 && bounds.bottom <= size.y) {
            z = true;
        }
        return z;
    }

    private Rect getWindowBoundsByDefault(int windowState) {
        HwPCMultiWindowPolicy.updateDefaultSize(getRightContext(this.mService.mContext), this);
        int layoutState = HwPCMultiWindowCompatibility.getWindowStateLayout(windowState);
        Rect bounds = new Rect();
        switch (layoutState) {
            case 1:
                getBounds(true, bounds);
                return bounds;
            case 2:
                getBounds(false, bounds);
                return bounds;
            case 3:
                Point size = getExtDisplaySize();
                bounds.set(0, 0, size.x, size.y);
                return bounds;
            default:
                return null;
        }
    }

    public Rect getLaunchBounds(TaskRecord record) {
        Rect outRect = new Rect();
        if (record == null || record.mRootActivityInfo == null) {
            return outRect;
        }
        String str;
        if (!HwPCMultiWindowCompatibility.isLayoutHadBounds(record.mNextWindowState)) {
            switch (HwPCMultiWindowCompatibility.getWindowStateLayout(record.mNextWindowState)) {
                case 3:
                    outRect.set(getMaximizedBounds());
                    break;
                case 4:
                    outRect = null;
                    break;
            }
        }
        outRect = getWindowBounds(record);
        if (!(outRect == null || record.mRootActivityInfo.windowLayout == null)) {
            int width = getFinalWidth(record.mRootActivityInfo.windowLayout);
            if (width > 0) {
                outRect.right = outRect.left + width;
            }
            int height = getFinalHeight(record.mRootActivityInfo.windowLayout);
            if (height > 0) {
                outRect.bottom = outRect.top + height;
            }
        }
        outRect = adjustBounds(outRect, record.taskId);
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLaunchBounds: ");
        if (outRect == null) {
            str = "null";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(outRect.toShortString());
            stringBuilder2.append(" (");
            stringBuilder2.append(outRect.width());
            stringBuilder2.append(", ");
            stringBuilder2.append(outRect.height());
            stringBuilder2.append(")");
            str = stringBuilder2.toString();
        }
        stringBuilder.append(str);
        HwPCUtils.log(str2, stringBuilder.toString());
        return outRect;
    }

    private boolean isRectHasWindowSize(Rect rect) {
        Point size = getExtDisplaySize();
        Rect screenRect = new Rect(0, 0, size.x, size.y);
        if (rect == null || rect.isEmpty() || rect.equals(screenRect)) {
            return false;
        }
        return true;
    }

    private Rect adjustBounds(Rect rect, int taskId) {
        HwPCMultiWindowManager hwPCMultiWindowManager = this;
        Rect rect2 = rect;
        if (!isRectHasWindowSize(rect)) {
            return rect2;
        }
        int displayNdx;
        int childCount;
        int i;
        int i2;
        Point size = getExtDisplaySize();
        Rect screenRect = new Rect(0, 0, size.x, size.y);
        ArrayList<Rect> bounds = new ArrayList();
        SparseArray<ActivityDisplay> activityDisplays = hwPCMultiWindowManager.mService.mStackSupervisor.mActivityDisplays;
        ArrayList<ActivityStack> stacks = new ArrayList();
        boolean tryForwardY = true;
        for (displayNdx = activityDisplays.size() - 1; displayNdx >= 0; displayNdx--) {
            ActivityDisplay activityDisplay = (ActivityDisplay) activityDisplays.valueAt(displayNdx);
            childCount = activityDisplay.getChildCount();
            for (i = 0; i < childCount; i++) {
                stacks.add(activityDisplay.getChildAt(i));
            }
        }
        displayNdx = stacks.size();
        for (int i3 = 0; i3 < displayNdx; i3++) {
            ActivityStack stack = (ActivityStack) stacks.get(i3);
            if (!(stack.mDisplayId == 0 || stack.mDisplayId == -1)) {
                TaskRecord task = stack.topTask();
                if (task != null) {
                    if (!(taskId == task.taskId || task.getOverrideBounds() == null || task.getOverrideBounds().equals(screenRect))) {
                        bounds.add(task.getOverrideBounds());
                    }
                }
            }
            i2 = taskId;
        }
        i2 = taskId;
        Rect newRect = new Rect(rect2);
        childCount = HwPCMultiWindowPolicy.dpToPx(hwPCMultiWindowManager.getRightContext(hwPCMultiWindowManager.mService.mContext), 15);
        int i4 = 30;
        i = HwPCMultiWindowPolicy.dpToPx(hwPCMultiWindowManager.getRightContext(hwPCMultiWindowManager.mService.mContext), 30);
        int i5 = 0;
        boolean tryForwardX = true;
        while (hwPCMultiWindowManager.isWindowOverlapped(newRect, bounds)) {
            int i6 = i5 + 1;
            if (i5 >= i4) {
                break;
            }
            int offsetY;
            i5 = 0;
            int offsetY2 = 0;
            boolean noX = false;
            boolean noY = false;
            if (tryForwardX && newRect.right + childCount < size.x) {
                i5 = newRect.right + i > size.x ? size.x - newRect.right : i;
            } else if (newRect.left - childCount > 0) {
                i5 = newRect.left - i > 0 ? 0 - i : 0 - newRect.left;
                tryForwardX = false;
            } else {
                noX = true;
            }
            if (!tryForwardY || newRect.bottom + childCount >= size.y) {
                if (newRect.top - childCount > 0) {
                    offsetY2 = newRect.top - i > 0 ? 0 - i : 0 - newRect.top;
                    tryForwardY = false;
                } else {
                    noY = true;
                }
                offsetY = offsetY2;
            } else {
                offsetY = newRect.bottom + i > size.y ? size.y - newRect.bottom : i;
            }
            if (noX && noY) {
                break;
            }
            newRect.offset(i5, offsetY);
            i5 = i6;
            hwPCMultiWindowManager = this;
            i4 = 30;
        }
        return newRect;
    }

    private boolean isWindowOverlapped(Rect rect, ArrayList<Rect> taskBounds) {
        int taskBoundsSize = taskBounds.size();
        for (int i = 0; i < taskBoundsSize; i++) {
            if (rect.equals((Rect) taskBounds.get(i))) {
                HwPCUtils.log(TAG, "isWindowOverlapped return true.");
                return true;
            }
        }
        return false;
    }

    private void getBounds(boolean isPortrait, Rect out) {
        int width;
        int height;
        Point size = getExtDisplaySize();
        if (isPortrait) {
            width = HwPCMultiWindowPolicy.mDefPortraitWidth;
            height = (int) (((float) HwPCMultiWindowPolicy.mDefPortraitWidth) / HwPCMultiWindowPolicy.mPortraitRatio);
        } else {
            height = HwPCMultiWindowPolicy.mDefLandscapeHeight;
            width = (int) (((float) HwPCMultiWindowPolicy.mDefLandscapeHeight) * HwPCMultiWindowPolicy.mLandscapeRatio);
        }
        height += this.mDecorHeight;
        if (width > size.x) {
            width = size.x;
            height = (int) (((float) size.x) / (isPortrait ? HwPCMultiWindowPolicy.mPortraitRatio : HwPCMultiWindowPolicy.mLandscapeRatio));
        }
        if (height > size.y) {
            height = size.y;
            width = (int) (((float) size.y) * (isPortrait ? HwPCMultiWindowPolicy.mPortraitRatio : HwPCMultiWindowPolicy.mLandscapeRatio));
        }
        out.set(HwPCMultiWindowPolicy.mWindowMarginLeft, HwPCMultiWindowPolicy.mWindowMarginTop, HwPCMultiWindowPolicy.mWindowMarginLeft + width, HwPCMultiWindowPolicy.mWindowMarginTop + height);
    }

    private boolean isNeedDelay(TaskRecord record) {
        if (record == null || record.mRootActivityInfo == null) {
            return false;
        }
        String pkgName = record.mRootActivityInfo.packageName;
        if (TextUtils.isEmpty(pkgName) || !this.mNeedDelayList.contains(pkgName)) {
            return false;
        }
        HwPCUtils.log(TAG, "isNeedDelay return true.");
        return true;
    }

    public void updateTaskByRequestedOrientation(TaskRecord record, int requestedOrientation) {
        if (isNeedDelay(record)) {
            if (requestedOrientation != 0) {
                if (!(this.mLastRequestedOrientation == 0 || this.mLastRequestedOrientation == requestedOrientation)) {
                    this.mFixOrientationChanged = true;
                }
                this.mLastRequestedOrientation = requestedOrientation;
            }
            Message msg = Message.obtain();
            this.mHandler.removeMessages(1);
            msg.what = 1;
            msg.arg1 = requestedOrientation;
            msg.arg2 = record.taskId;
            this.mHandler.sendMessageDelayed(msg, 200);
            return;
        }
        this.mFixOrientationChanged = false;
        this.mLastRequestedOrientation = 0;
        updateTaskByRequestedOrientationInternal(record, requestedOrientation);
    }

    public void updateTaskByRequestedOrientationInternal(TaskRecord record, int customRequestedOrientation) {
        if (customRequestedOrientation != 0) {
            int currentOrientation = 1;
            if (!(HwPCMultiWindowCompatibility.getWindowStateLayout(record.mWindowState) == 1)) {
                currentOrientation = 2;
            }
            if (currentOrientation != customRequestedOrientation || ((this.mFixOrientationChanged && customRequestedOrientation == 2) || this.mUpdateTaskSet.contains(Integer.valueOf(record.taskId)))) {
                this.mFixOrientationChanged = false;
                this.mLastRequestedOrientation = 0;
                record.mNextWindowState = getWindowStateByDefault(record, customRequestedOrientation);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateTaskByRequestedOrientation-pre by default: ");
                stringBuilder.append(Integer.toHexString(record.mWindowState));
                stringBuilder.append(" to ");
                stringBuilder.append(Integer.toHexString(record.mNextWindowState));
                HwPCUtils.log(str, stringBuilder.toString());
                if (record.mWindowState != record.mNextWindowState || this.mUpdateTaskSet.contains(Integer.valueOf(record.taskId))) {
                    String str2;
                    Rect rect = getWindowBoundsByDefault(record.mNextWindowState);
                    if (!(record.getOverrideBounds() == null || record.getOverrideBounds().isEmpty() || !isRectHasWindowSize(rect))) {
                        rect.offsetTo(record.getOverrideBounds().left, record.getOverrideBounds().top);
                    }
                    rect = adjustBounds(rect, record.taskId);
                    if (record instanceof HwTaskRecord) {
                        ((HwTaskRecord) record).mSaveBounds = false;
                    }
                    String str3 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateTaskByRequestedOrientation: ");
                    stringBuilder.append(Integer.toHexString(record.mNextWindowState));
                    stringBuilder.append(LogHelper.SEPARATOR);
                    if (rect == null) {
                        str2 = "null";
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(rect.toShortString());
                        stringBuilder2.append(" (");
                        stringBuilder2.append(rect.width());
                        stringBuilder2.append(", ");
                        stringBuilder2.append(rect.height());
                        stringBuilder2.append(")");
                        str2 = stringBuilder2.toString();
                    }
                    stringBuilder.append(str2);
                    HwPCUtils.log(str3, stringBuilder.toString());
                }
                this.mUpdateTaskSet.remove(Integer.valueOf(record.taskId));
            }
        }
    }

    void resizeTaskFromPC(TaskRecord record, Rect rect) {
        record.resize(rect, 3, true, false);
    }

    private Display getExtDisplay(boolean isInternal) {
        DisplayManager displayManager = null;
        if (this.mService.mWindowManager instanceof HwWindowManagerService) {
            displayManager = ((HwWindowManagerService) this.mService.mWindowManager).getDisplayManager();
        }
        if (displayManager == null) {
            return null;
        }
        int i = 0;
        if (isInternal) {
            return displayManager.getDisplay(0);
        }
        Display[] displays = displayManager.getDisplays();
        int length = displays.length;
        while (i < length) {
            Display d = displays[i];
            if (HwPCUtils.isValidExtDisplayId(d.getDisplayId())) {
                return d;
            }
            i++;
        }
        return null;
    }

    private Point getExtDisplaySize() {
        Display display = getExtDisplay(false);
        if (!(display == null || display.getDisplayId() == this.mCurDisplayId)) {
            this.mNonDecorScreenSize.x = 0;
            this.mNonDecorScreenSize.y = 0;
            this.mCurDisplayId = display.getDisplayId();
        }
        if ((this.mNonDecorScreenSize.x == 0 || this.mNonDecorScreenSize.y == 0) && display != null && (this.mService.mWindowManager instanceof HwWindowManagerService)) {
            WindowManagerPolicy policy = ((HwWindowManagerService) this.mService.mWindowManager).getPolicy();
            Point fullScreenSize = new Point();
            display.getRealSize(fullScreenSize);
            if (policy != null) {
                this.mNonDecorScreenSize.x = policy.getNonDecorDisplayWidth(fullScreenSize.x, fullScreenSize.y, 0, 0, display.getDisplayId(), null);
                this.mNonDecorScreenSize.y = policy.getNonDecorDisplayHeight(fullScreenSize.x, fullScreenSize.y, 0, 0, display.getDisplayId(), null);
                this.mDecorHeight = fullScreenSize.y - this.mNonDecorScreenSize.y;
            }
        }
        return this.mNonDecorScreenSize;
    }

    private Context getRightContext(Context ctx) {
        Display display = getExtDisplay(null);
        if (display != null) {
            return ctx.createDisplayContext(display);
        }
        return ctx;
    }

    public Rect getMaximizedBounds() {
        Point size = getExtDisplaySize();
        return new Rect(0, 0, size.x, size.y);
    }

    public boolean isSupportResize(TaskRecord record, boolean isFullscreen, boolean isMaximized) {
        if (record == null) {
            return false;
        }
        if (isFullscreen) {
            return HwPCMultiWindowCompatibility.isFullscreenable(record.mWindowState);
        }
        if (isMaximized) {
            return HwPCMultiWindowCompatibility.isMaximizeable(record.mWindowState);
        }
        return ActivityInfo.isResizeableMode(record.mResizeMode);
    }

    private int getFinalWidth(WindowLayout windowLayout) {
        int width = 0;
        if (windowLayout.width > 0) {
            width = computeExtWidth(windowLayout.width);
        }
        if (windowLayout.widthFraction > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            return (int) (((float) getExtDisplaySize().x) * windowLayout.widthFraction);
        }
        return width;
    }

    private int getFinalHeight(WindowLayout windowLayout) {
        int height = 0;
        if (windowLayout.height > 0) {
            height = computeExtWidth(windowLayout.height);
        }
        if (windowLayout.heightFraction > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            return (int) (((float) getExtDisplaySize().y) * windowLayout.heightFraction);
        }
        return height;
    }

    private int computeExtWidth(int val) {
        int internalDPI = 1;
        int externalDPI = 1;
        DisplayInfo info = new DisplayInfo();
        Display dis = getExtDisplay(true);
        if (dis != null) {
            dis.getDisplayInfo(info);
            internalDPI = info.logicalDensityDpi;
        }
        dis = getExtDisplay(false);
        if (dis != null) {
            dis.getDisplayInfo(info);
            externalDPI = info.logicalDensityDpi;
        }
        return (val * externalDPI) / internalDPI;
    }

    public boolean isSpecialVideo(String pkgName) {
        return this.mSpecialVideosList.contains(pkgName);
    }

    public boolean isOlnyFullscreen(String pkgName) {
        return this.mPadFullscreenList.contains(pkgName) || this.mFullscreenNoNavigationBar.contains(pkgName);
    }

    public void setForceUpdateTask(int taskId) {
        this.mUpdateTaskSet.add(Integer.valueOf(taskId));
    }
}
