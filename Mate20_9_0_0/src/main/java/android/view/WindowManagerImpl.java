package android.view;

import android.content.Context;
import android.graphics.Region;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.HwPCUtils;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager.KeyboardShortcutsReceiver;
import com.android.internal.os.IResultReceiver.Stub;
import java.util.ArrayList;

public final class WindowManagerImpl implements WindowManager {
    private static final String TAG = "WindowManagerImpl";
    private ArrayList<View> mBlockInPCViews;
    private ArrayList<View> mBlockInVRViews;
    private final Context mContext;
    private IBinder mDefaultToken;
    private final WindowManagerGlobal mGlobal;
    private final Window mParentWindow;

    public WindowManagerImpl(Context context) {
        this(context, null);
    }

    private WindowManagerImpl(Context context, Window parentWindow) {
        this.mGlobal = WindowManagerGlobal.getInstance();
        this.mBlockInVRViews = new ArrayList();
        this.mBlockInPCViews = new ArrayList();
        this.mContext = context;
        this.mParentWindow = parentWindow;
    }

    public WindowManagerImpl createLocalWindowManager(Window parentWindow) {
        return new WindowManagerImpl(this.mContext, parentWindow);
    }

    public WindowManagerImpl createPresentationWindowManager(Context displayContext) {
        return new WindowManagerImpl(displayContext, this.mParentWindow);
    }

    public void setDefaultToken(IBinder token) {
        this.mDefaultToken = token;
    }

    public void addView(View view, LayoutParams params) {
        if (view == null || view.mContext == null || !"com.huawei.android.launcher".equals(view.mContext.getPackageName()) || !HwPCUtils.isValidExtDisplayId(view.mContext)) {
            applyDefaultToken(params);
            this.mGlobal.addView(view, params, this.mContext.getDisplay(), this.mParentWindow);
            return;
        }
        synchronized (this.mBlockInPCViews) {
            this.mBlockInPCViews.add(view);
        }
    }

    /* JADX WARNING: Missing block: B:8:0x000e, code skipped:
            r1 = r2.mBlockInPCViews;
     */
    /* JADX WARNING: Missing block: B:9:0x0010, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:12:0x0017, code skipped:
            if (r2.mBlockInPCViews.contains(r3) == false) goto L_0x001b;
     */
    /* JADX WARNING: Missing block: B:13:0x0019, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:14:0x001a, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:15:0x001b, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:16:0x001c, code skipped:
            applyDefaultToken(r4);
            r2.mGlobal.updateViewLayout(r3, r4);
     */
    /* JADX WARNING: Missing block: B:17:0x0024, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateViewLayout(View view, LayoutParams params) {
        synchronized (this.mBlockInVRViews) {
            if (this.mBlockInVRViews.contains(view)) {
            }
        }
    }

    private void applyDefaultToken(LayoutParams params) {
        if (this.mDefaultToken != null && this.mParentWindow == null) {
            if (params instanceof WindowManager.LayoutParams) {
                WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
                if (wparams.token == null) {
                    wparams.token = this.mDefaultToken;
                    return;
                }
                return;
            }
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0013, code skipped:
            r1 = r2.mBlockInPCViews;
     */
    /* JADX WARNING: Missing block: B:10:0x0015, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:13:0x001c, code skipped:
            if (r2.mBlockInPCViews.contains(r3) == false) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:14:0x001e, code skipped:
            r2.mBlockInPCViews.remove(r3);
     */
    /* JADX WARNING: Missing block: B:15:0x0023, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:16:0x0024, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:17:0x0025, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:18:0x0026, code skipped:
            r2.mGlobal.removeView(r3, false);
     */
    /* JADX WARNING: Missing block: B:19:0x002c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeView(View view) {
        synchronized (this.mBlockInVRViews) {
            if (this.mBlockInVRViews.contains(view)) {
                this.mBlockInVRViews.remove(view);
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0013, code skipped:
            r1 = r2.mBlockInPCViews;
     */
    /* JADX WARNING: Missing block: B:10:0x0015, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:13:0x001c, code skipped:
            if (r2.mBlockInPCViews.contains(r3) == false) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:14:0x001e, code skipped:
            r2.mBlockInPCViews.remove(r3);
     */
    /* JADX WARNING: Missing block: B:15:0x0023, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:16:0x0024, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:17:0x0025, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:18:0x0026, code skipped:
            r2.mGlobal.removeView(r3, true);
     */
    /* JADX WARNING: Missing block: B:19:0x002c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeViewImmediate(View view) {
        synchronized (this.mBlockInVRViews) {
            if (this.mBlockInVRViews.contains(view)) {
                this.mBlockInVRViews.remove(view);
            }
        }
    }

    public void requestAppKeyboardShortcuts(final KeyboardShortcutsReceiver receiver, int deviceId) {
        try {
            WindowManagerGlobal.getWindowManagerService().requestAppKeyboardShortcuts(new Stub() {
                public void send(int resultCode, Bundle resultData) throws RemoteException {
                    receiver.onKeyboardShortcutsReceived(resultData.getParcelableArrayList(WindowManager.PARCEL_KEY_SHORTCUTS_ARRAY));
                }
            }, deviceId);
        } catch (RemoteException e) {
        }
    }

    public Display getDefaultDisplay() {
        return this.mContext.getDisplay();
    }

    public Region getCurrentImeTouchRegion() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getCurrentImeTouchRegion();
        } catch (RemoteException e) {
            return null;
        }
    }
}
