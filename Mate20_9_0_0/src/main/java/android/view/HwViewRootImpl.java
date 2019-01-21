package android.view;

import android.content.Context;
import android.graphics.Point;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.HwPCUtils;
import android.util.HwStylusUtils;
import android.util.Log;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.WindowManager.LayoutParams;
import android.vkey.SettingsHelper;
import com.huawei.hsm.permission.StubController;
import huawei.android.provider.FingerSenseSettings;
import huawei.android.provider.HwSettings.System;

public class HwViewRootImpl implements IHwViewRootImpl {
    private static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    private static final int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
    private static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", "default"));
    private static final int MOTION_EVENT_INJECTION_DELAY_MILLIS = 20;
    private static final int NAVIGATION_DISABLE = 0;
    private static final int NAVIGATION_ENABLE = 1;
    static final boolean isHwNaviBar = SystemProperties.getBoolean("ro.config.hw_navigationbar", false);
    private static HwViewRootImpl mInstance = null;
    private boolean isDecorPointerEvent = false;
    Point mDisplayPoint;
    private MotionEvent mDownEvent = null;
    int mHitRegionToMax = 20;
    private boolean mIsRedispatchDownAction = false;
    private boolean mIsStylusEffective = true;
    private StylusTouchListener mStylusTouchListener = null;

    protected HwViewRootImpl() {
    }

    public static synchronized HwViewRootImpl getDefault() {
        HwViewRootImpl hwViewRootImpl;
        synchronized (HwViewRootImpl.class) {
            if (mInstance == null) {
                mInstance = new HwViewRootImpl();
            }
            hwViewRootImpl = mInstance;
        }
        return hwViewRootImpl;
    }

    public void setRealSize(Point point) {
        this.mDisplayPoint = point;
    }

    public void clearDisplayPoint() {
        this.mDisplayPoint = null;
    }

    /* JADX WARNING: Missing block: B:77:0x0103, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:78:0x0104, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean filterDecorPointerEvent(Context context, MotionEvent event, int action, LayoutParams windowattr, Display disp) {
        if (HwPCUtils.isValidExtDisplayId(context) || ((disp != null && HwPCUtils.isValidExtDisplayId(disp.getDisplayId())) || context == null || !isHwNaviBar || disp == null || windowattr == null || (windowattr.hwFlags & StubController.PERMISSION_WIFI) != 0 || !isNaviEnable(context) || SettingsHelper.isTouchPlusOn(context) || (windowattr.privateFlags & 1024) != 0)) {
            return false;
        }
        boolean z = true;
        Point pt;
        if (action == 0) {
            this.isDecorPointerEvent = false;
            pt = this.mDisplayPoint == null ? getDisplayPoint(disp) : this.mDisplayPoint;
            this.mHitRegionToMax = (int) (((double) context.getResources().getDimensionPixelSize(17105186)) / 3.5d);
            if (pt.y > pt.x) {
                this.isDecorPointerEvent = event.getRawY() > ((float) (pt.y - this.mHitRegionToMax));
            } else {
                this.isDecorPointerEvent = event.getRawX() > ((float) (pt.x - this.mHitRegionToMax));
            }
            boolean z2 = this.isDecorPointerEvent && Global.getInt(context.getContentResolver(), System.NAVIGATIONBAR_IS_MIN, 0) != 0;
            this.isDecorPointerEvent = z2;
            if (this.isDecorPointerEvent) {
                this.mDownEvent = event.copy();
                return true;
            }
            this.mDownEvent = null;
            this.mIsRedispatchDownAction = false;
        } else if (action == 3) {
            this.mDownEvent = null;
            this.isDecorPointerEvent = false;
        } else if (action == 1) {
            pt = this.mDisplayPoint == null ? getDisplayPoint(disp) : this.mDisplayPoint;
            if (!this.isDecorPointerEvent) {
                this.mIsRedispatchDownAction = false;
            } else if (pt.y > pt.x) {
                if (event.getRawY() <= ((float) (pt.y - this.mHitRegionToMax))) {
                    z = false;
                }
                this.mIsRedispatchDownAction = z;
            } else {
                if (event.getRawX() <= ((float) (pt.x - this.mHitRegionToMax))) {
                    z = false;
                }
                this.mIsRedispatchDownAction = z;
            }
            if (!this.mIsRedispatchDownAction) {
                this.mDownEvent = null;
            }
            this.isDecorPointerEvent = false;
        }
        return false;
    }

    public MotionEvent getRedispatchEvent() {
        if (!this.mIsRedispatchDownAction || this.mDownEvent == null) {
            return null;
        }
        MotionEvent mv = this.mDownEvent;
        this.mDownEvent = null;
        return mv;
    }

    private Point getDisplayPoint(Display disp) {
        if (this.mDisplayPoint == null) {
            Point pt = new Point();
            disp.getRealSize(pt);
            this.mDisplayPoint = pt;
        }
        return this.mDisplayPoint;
    }

    public boolean shouldQueueInputEvent(InputEvent event, Context context, View view, LayoutParams attr) {
        if (!(event instanceof MotionEvent)) {
            return true;
        }
        if (this.mStylusTouchListener == null && HwStylusUtils.hasStylusFeature(context)) {
            Log.d("stylus", "init stylus touchlistener.");
            this.mStylusTouchListener = new StylusTouchListener(context);
        }
        MotionEvent motionEvent = (MotionEvent) event;
        if (isStylusButtonPressed(context, attr.type, motionEvent)) {
            return false;
        }
        int pointerCount = motionEvent.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            if (motionEvent.getToolType(i) != 7) {
                return true;
            }
        }
        if (!FingerSenseSettings.isFingerSenseEnabled(context.getContentResolver()) || (attr.flags & 4096) != 0) {
            return true;
        }
        if (view == null) {
            return false;
        }
        Context viewContext = view.getContext();
        if (viewContext == null || !(viewContext instanceof ContextThemeWrapper)) {
            return false;
        }
        return ((ContextThemeWrapper) viewContext).getBaseContext() instanceof InputMethodService;
    }

    private boolean isStylusButtonPressed(Context context, int windowType, MotionEvent motionEvent) {
        if (HwStylusUtils.hasStylusFeature(context)) {
            boolean stylusPrimaryButtonPressed = motionEvent.getToolType(0) == 2 && motionEvent.getButtonState() == 32;
            if (motionEvent.getAction() == 0) {
                this.mIsStylusEffective = stylusPrimaryButtonPressed;
            }
            if (stylusPrimaryButtonPressed && this.mStylusTouchListener != null && this.mIsStylusEffective && isStylusEnable(context)) {
                this.mStylusTouchListener.updateViewContext(context, windowType);
                this.mStylusTouchListener.onTouchEvent(motionEvent);
                return true;
            } else if (motionEvent.getAction() == 1 || motionEvent.getAction() == 3) {
                this.mIsStylusEffective = true;
            }
        }
        return false;
    }

    private boolean isStylusEnable(Context context) {
        boolean z = true;
        if (Settings.System.getInt(context.getContentResolver(), "stylus_enable", 1) == 0) {
            z = false;
        }
        return z;
    }

    private boolean isNaviEnable(Context mContext) {
        return Settings.System.getInt(mContext.getContentResolver(), System.NAVIGATION_BAR_ENABLE, getDefaultNavConfig()) != 0;
    }

    private int getDefaultNavConfig() {
        if (!FRONT_FINGERPRINT_NAVIGATION) {
            return 1;
        }
        if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 0) {
            if (isChinaArea()) {
                return 0;
            }
            return 1;
        } else if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
            return 0;
        } else {
            return 1;
        }
    }

    private static boolean isChinaArea() {
        return SystemProperties.get("ro.config.hw_optb", System.FINGERSENSE_KNUCKLE_GESTURE_OFF).equals("156");
    }

    public boolean interceptMotionEvent(View view, MotionEvent event) {
        if ((event.getMetaState() & 4096) == 0 || (event.getSource() & 2) == 0 || event.getAction() != 8) {
            return false;
        }
        return multiPointerGesture(view, event.getX(), event.getY(), event.getAxisValue(1.3E-44f));
    }

    private boolean multiPointerGesture(View view, float x, float y, float value) {
        int ponterCount = 2;
        int guide = value > 0.0f ? 1 : -1;
        float pointerX1 = x - 200.0f;
        float pointerY1 = y - 200.0f;
        float pointerX2 = x + 200.0f;
        float pointerY2 = y + 200.0f;
        PointerCoords[][] ppCoords = new PointerCoords[2][];
        int i = 4;
        PointerCoords[] pointerCoordsX = new PointerCoords[4];
        PointerCoords[] pointerCoordsY = new PointerCoords[4];
        int index = 1;
        while (true) {
            int index2 = index;
            if (index2 <= i) {
                float dis = (30.0f * ((float) index2)) * ((float) guide);
                int ponterCount2 = ponterCount;
                ponterCount = getPonterCoords(pointerX1 - dis, pointerY1 - dis);
                pointerCoordsX[index2 - 1] = ponterCount;
                PointerCoords pcx = ponterCount;
                pointerCoordsY[index2 - 1] = getPonterCoords(pointerX2 + dis, pointerY2 + dis);
                index = index2 + 1;
                ponterCount = ponterCount2;
                i = 4;
            } else {
                ppCoords[0] = pointerCoordsX;
                ppCoords[1] = pointerCoordsY;
                return performMultiPointerGesture(view, ppCoords);
            }
        }
    }

    private PointerCoords getPonterCoords(float x, float y) {
        PointerCoords pc1 = new PointerCoords();
        pc1.x = x;
        pc1.y = y;
        pc1.pressure = 1.0f;
        pc1.size = 1.0f;
        return pc1;
    }

    private boolean performMultiPointerGesture(View view, PointerCoords[]... touches) {
        int x;
        int maxSteps;
        long j;
        MotionEvent motionEvent;
        View view2 = view;
        PointerCoords[][] pointerCoordsArr = touches;
        int maxSteps2 = 0;
        for (int x2 = 0; x2 < pointerCoordsArr.length; x2++) {
            maxSteps2 = maxSteps2 < pointerCoordsArr[x2].length ? pointerCoordsArr[x2].length : maxSteps2;
        }
        PointerProperties[] properties = new PointerProperties[pointerCoordsArr.length];
        PointerCoords[] pointerCoords = new PointerCoords[pointerCoordsArr.length];
        for (x = 0; x < pointerCoordsArr.length; x++) {
            PointerProperties prop = new PointerProperties();
            prop.id = x;
            prop.toolType = 1;
            properties[x] = prop;
            pointerCoords[x] = pointerCoordsArr[x][0];
        }
        long downTime = SystemClock.uptimeMillis();
        int x3 = 1;
        PointerCoords[] pointerCoords2 = pointerCoords;
        MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), 0, 1, properties, pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
        boolean ret = true & injectEventSync(view2, event);
        int x4 = x3;
        while (x4 < pointerCoordsArr.length) {
            maxSteps = maxSteps2;
            j = 20;
            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), getPointerAction(5, x4), x4 + 1, properties, pointerCoords2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
            ret &= injectEventSync(view2, event);
            SystemClock.sleep(j);
            x4++;
            motionEvent = event;
            maxSteps2 = maxSteps;
        }
        maxSteps = maxSteps2;
        j = 20;
        x4 = x3;
        while (x4 < maxSteps - 1) {
            for (x = 0; x < pointerCoordsArr.length; x++) {
                if (pointerCoordsArr[x].length > x4) {
                    pointerCoords2[x] = pointerCoordsArr[x][x4];
                } else {
                    pointerCoords2[x] = pointerCoordsArr[x][pointerCoordsArr[x].length - 1];
                }
            }
            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), 2, pointerCoordsArr.length, properties, pointerCoords2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
            ret &= injectEventSync(view2, event);
            SystemClock.sleep(j);
            x4++;
            motionEvent = event;
        }
        int x5 = 0;
        while (true) {
            x4 = x5;
            if (x4 >= pointerCoordsArr.length) {
                break;
            }
            pointerCoords2[x4] = pointerCoordsArr[x4][pointerCoordsArr[x4].length - 1];
            x5 = x4 + 1;
        }
        while (true) {
            x4 = x3;
            if (x4 < pointerCoordsArr.length) {
                MotionEvent event2 = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), getPointerAction(6, x4), x4 + 1, properties, pointerCoords2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
                ret &= injectEventSync(view2, event2);
                x3 = x4 + 1;
                motionEvent = event2;
            } else {
                return ret & injectEventSync(view2, MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), 1, 1, properties, pointerCoords2, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0));
            }
        }
    }

    private boolean injectEventSync(View view, MotionEvent event) {
        return view.dispatchPointerEvent(event);
    }

    private int getPointerAction(int motionEnvent, int index) {
        return (index << 8) + motionEnvent;
    }
}
