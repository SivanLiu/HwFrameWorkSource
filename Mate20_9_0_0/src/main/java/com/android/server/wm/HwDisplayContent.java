package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.util.HwPCUtils;
import android.util.MutableBoolean;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.huawei.android.statistical.StatisticalUtils;
import huawei.android.os.HwGeneralManager;
import java.util.ArrayList;
import java.util.List;

public class HwDisplayContent extends DisplayContent {
    private WindowState appWin = null;
    private boolean mTmpshouldDropMotionEventForTouchPad;
    private int maxLayer = 0;
    private int minLayer = 0;
    private boolean screenshotReady = false;

    @FunctionalInterface
    private interface ScreenshoterForExternalDisplay<E> {
        E screenshotForExternalDisplay(IBinder iBinder, Rect rect, int i, int i2, int i3, int i4, boolean z, int i5);
    }

    public HwDisplayContent(Display display, WindowManagerService service, WallpaperController wallpaperController, DisplayWindowController controller) {
        super(display, service, wallpaperController, controller);
    }

    void computeScreenConfiguration(Configuration config) {
        super.computeScreenConfiguration(config);
        if (HwGeneralManager.getInstance().isSupportForce()) {
            DisplayInfo displayInfo = this.mService.getDefaultDisplayContentLocked().getDisplayInfo();
            this.mService.mInputManager.setDisplayWidthAndHeight(displayInfo.logicalWidth, displayInfo.logicalHeight);
        }
    }

    List taskIdFromTop() {
        List<Integer> tasks = new ArrayList();
        for (int stackNdx = this.mTaskStackContainers.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            TaskStack stack = (TaskStack) this.mTaskStackContainers.getChildAt(stackNdx);
            for (int taskNdx = stack.mChildren.size() - 1; taskNdx >= 0; taskNdx--) {
                Task task = (Task) stack.mChildren.get(taskNdx);
                if (task.getTopVisibleAppMainWindow() != null) {
                    int taskId = task.mTaskId;
                    if (taskId != -1) {
                        tasks.add(Integer.valueOf(taskId));
                        return tasks;
                    }
                }
            }
        }
        return tasks;
    }

    public void setDisplayRotationFR(int rotation) {
        IntelliServiceManager.setDisplayRotation(rotation);
    }

    public void togglePCMode(boolean pcMode) {
        if (!pcMode && HwPCUtils.isValidExtDisplayId(this.mDisplay.getDisplayId())) {
            try {
                WindowToken topChild = (WindowToken) this.mAboveAppWindowsContainers.getTopChild();
                while (topChild != null) {
                    topChild.removeImmediately();
                    topChild = (WindowToken) this.mAboveAppWindowsContainers.getTopChild();
                }
            } catch (Exception e) {
                HwPCUtils.log("PCManager", "togglePCMode failed!!!");
            }
        }
    }

    GraphicBuffer screenshotApplicationsToBufferForExternalDisplay(IBinder displayToken, IBinder appToken, int width, int height, boolean includeFullDisplay, float frameScale, boolean wallpaperOnly, boolean includeDecor) {
        return (GraphicBuffer) screenshotApplicationsForExternalDisplay(displayToken, appToken, width, height, includeFullDisplay, frameScale, wallpaperOnly, includeDecor, -$$Lambda$HwDisplayContent$hqzEBx8K3UQdYIGeQr7mOqvOIpU.INSTANCE);
    }

    /* JADX WARNING: Missing block: B:26:0x005b, code skipped:
            r6 = ((r11.mService.mPolicy.getWindowLayerFromTypeLw(2) + 1) * 10000) + 1000;
            r3 = new android.util.MutableBoolean(r45);
            r2 = r11.mService.mWindowMap;
     */
    /* JADX WARNING: Missing block: B:27:0x0075, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:29:?, code skipped:
            r11.appWin = null;
            r11.screenshotReady = false;
            r11.maxLayer = 0;
            r11.minLayer = 0;
     */
    /* JADX WARNING: Missing block: B:32:0x0080, code skipped:
            r1 = r1;
            r21 = r2;
            r22 = r3;
            r23 = r6;
            r0 = true;
            r24 = r9;
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            r1 = new com.android.server.wm.-$$Lambda$HwDisplayContent$UiMb2yAB1OHc6iiUwbMFKfLp2P8(r11, r6, r47, r5, r12, r22, r48, r9, r10);
     */
    /* JADX WARNING: Missing block: B:35:0x009a, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:36:0x009b, code skipped:
            r21 = r2;
            r34 = r3;
            r35 = r5;
            r23 = r6;
            r6 = r9;
            r38 = r10;
            r5 = r14;
            r7 = r15;
     */
    /* JADX WARNING: Missing block: B:37:0x00aa, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:38:0x00ab, code skipped:
            r35 = r5;
            r38 = r10;
            r5 = r14;
            r7 = r15;
            r34 = r22;
            r6 = r24;
     */
    /* JADX WARNING: Missing block: B:41:?, code skipped:
            forAllWindows(r20, r0);
     */
    /* JADX WARNING: Missing block: B:42:0x00bc, code skipped:
            if (r12 == null) goto L_0x00d6;
     */
    /* JADX WARNING: Missing block: B:45:0x00c0, code skipped:
            if (r11.appWin != null) goto L_0x00d6;
     */
    /* JADX WARNING: Missing block: B:46:0x00c2, code skipped:
            monitor-exit(r21);
     */
    /* JADX WARNING: Missing block: B:48:0x00c4, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:49:0x00c5, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:50:0x00c6, code skipped:
            r3 = r43;
            r2 = r44;
            r35 = r5;
            r38 = r10;
            r5 = r14;
            r7 = r15;
            r34 = r22;
            r6 = r24;
     */
    /* JADX WARNING: Missing block: B:53:0x00d8, code skipped:
            if (r11.screenshotReady != false) goto L_0x011e;
     */
    /* JADX WARNING: Missing block: B:55:?, code skipped:
            r0 = "WindowManager";
            r1 = new java.lang.StringBuilder();
            r1.append("Failed to capture screenshot of ");
            r1.append(r12);
            r1.append(" appWin=");
     */
    /* JADX WARNING: Missing block: B:56:0x00f0, code skipped:
            if (r11.appWin != null) goto L_0x00f5;
     */
    /* JADX WARNING: Missing block: B:57:0x00f2, code skipped:
            r2 = "null";
     */
    /* JADX WARNING: Missing block: B:58:0x00f5, code skipped:
            r2 = new java.lang.StringBuilder();
            r2.append(r11.appWin);
            r2.append(" drawState=");
            r2.append(r11.appWin.mWinAnimator.mDrawState);
            r2 = r2.toString();
     */
    /* JADX WARNING: Missing block: B:59:0x0111, code skipped:
            r1.append(r2);
            android.util.Slog.i(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:60:0x011b, code skipped:
            monitor-exit(r21);
     */
    /* JADX WARNING: Missing block: B:62:0x011d, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:64:?, code skipped:
            r1 = r11.maxLayer;
            r1 = r11.mDisplay.getRotation();
     */
    /* JADX WARNING: Missing block: B:66:0x0127, code skipped:
            if (r1 == r0) goto L_0x0131;
     */
    /* JADX WARNING: Missing block: B:67:0x0129, code skipped:
            if (r1 != true) goto L_0x012c;
     */
    /* JADX WARNING: Missing block: B:68:0x012c, code skipped:
            r3 = r43;
            r2 = r44;
     */
    /* JADX WARNING: Missing block: B:69:0x0131, code skipped:
            r2 = r43;
            r3 = r44;
     */
    /* JADX WARNING: Missing block: B:70:0x0136, code skipped:
            r4 = r22;
     */
    /* JADX WARNING: Missing block: B:73:0x013a, code skipped:
            if (r4.value != false) goto L_0x0156;
     */
    /* JADX WARNING: Missing block: B:74:0x013c, code skipped:
            r6 = r24;
     */
    /* JADX WARNING: Missing block: B:77:0x0143, code skipped:
            if (r6.intersect(0, 0, r15, r14) != false) goto L_0x015c;
     */
    /* JADX WARNING: Missing block: B:78:0x0145, code skipped:
            r6.setEmpty();
     */
    /* JADX WARNING: Missing block: B:79:0x0149, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:80:0x014a, code skipped:
            r16 = r1;
            r34 = r4;
            r35 = r5;
            r38 = r10;
     */
    /* JADX WARNING: Missing block: B:81:0x0152, code skipped:
            r5 = r14;
            r7 = r15;
     */
    /* JADX WARNING: Missing block: B:82:0x0156, code skipped:
            r6 = r24;
     */
    /* JADX WARNING: Missing block: B:84:?, code skipped:
            r6.set(0, 0, r15, r14);
     */
    /* JADX WARNING: Missing block: B:86:0x0160, code skipped:
            if (r6.isEmpty() == false) goto L_0x0165;
     */
    /* JADX WARNING: Missing block: B:88:?, code skipped:
            monitor-exit(r21);
     */
    /* JADX WARNING: Missing block: B:90:0x0164, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:91:0x0165, code skipped:
            if (r3 >= 0) goto L_0x016f;
     */
    /* JADX WARNING: Missing block: B:92:0x0167, code skipped:
            r3 = (int) (((float) r6.width()) * r46);
     */
    /* JADX WARNING: Missing block: B:93:0x016f, code skipped:
            if (r2 >= 0) goto L_0x0179;
     */
    /* JADX WARNING: Missing block: B:95:0x0175, code skipped:
            r2 = (int) (((float) r6.height()) * r46);
     */
    /* JADX WARNING: Missing block: B:97:?, code skipped:
            r9 = new android.graphics.Rect(r6);
     */
    /* JADX WARNING: Missing block: B:98:0x0182, code skipped:
            if (android.util.HwPCUtils.isPcCastModeInServer() == false) goto L_0x0218;
     */
    /* JADX WARNING: Missing block: B:101:0x018a, code skipped:
            if (android.util.HwPCUtils.isValidExtDisplayId(r11.mDisplayId) == false) goto L_0x0218;
     */
    /* JADX WARNING: Missing block: B:102:0x018c, code skipped:
            r7 = r11.mService.getPCScreenDisplayMode();
     */
    /* JADX WARNING: Missing block: B:103:0x0192, code skipped:
            if (r7 == false) goto L_0x0201;
     */
    /* JADX WARNING: Missing block: B:104:0x0194, code skipped:
            if (r7 != r0) goto L_0x019a;
     */
    /* JADX WARNING: Missing block: B:105:0x0196, code skipped:
            r16 = 0.95f;
     */
    /* JADX WARNING: Missing block: B:106:0x019a, code skipped:
            r16 = 0.9f;
     */
    /* JADX WARNING: Missing block: B:107:0x019d, code skipped:
            r8 = r16;
            r17 = (1.0f - r8) / 2.0f;
            r0 = ((float) r15) * r17;
            r34 = r4;
            r4 = ((float) r14) * r17;
     */
    /* JADX WARNING: Missing block: B:109:?, code skipped:
            r9.scale(r8);
     */
    /* JADX WARNING: Missing block: B:110:0x01b4, code skipped:
            r35 = r5;
     */
    /* JADX WARNING: Missing block: B:112:?, code skipped:
            r36 = r7;
            r37 = r8;
     */
    /* JADX WARNING: Missing block: B:113:0x01ca, code skipped:
            r38 = r10;
     */
    /* JADX WARNING: Missing block: B:115:?, code skipped:
            r9.set(r9.left + ((int) (r0 - 1065353216)), r9.top + ((int) (r4 - 1.0f)), (r9.width() + r9.left) + ((int) java.lang.Math.floor((double) r0)), (r9.height() + r9.top) + ((int) java.lang.Math.floor((double) r4)));
     */
    /* JADX WARNING: Missing block: B:116:0x01e8, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:117:0x01e9, code skipped:
            r16 = r1;
     */
    /* JADX WARNING: Missing block: B:118:0x01ed, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:119:0x01ee, code skipped:
            r38 = r10;
            r16 = r1;
            r5 = r14;
            r7 = r15;
     */
    /* JADX WARNING: Missing block: B:120:0x01f6, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:121:0x01f7, code skipped:
            r35 = r5;
            r38 = r10;
            r16 = r1;
            r5 = r14;
            r7 = r15;
     */
    /* JADX WARNING: Missing block: B:122:0x0201, code skipped:
            r34 = r4;
            r35 = r5;
            r38 = r10;
     */
    /* JADX WARNING: Missing block: B:123:0x0207, code skipped:
            r5 = r14;
            r7 = r15;
     */
    /* JADX WARNING: Missing block: B:124:0x020b, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:125:0x020c, code skipped:
            r34 = r4;
            r35 = r5;
            r38 = r10;
            r16 = r1;
            r5 = r14;
            r7 = r15;
     */
    /* JADX WARNING: Missing block: B:126:0x0218, code skipped:
            r34 = r4;
            r35 = r5;
            r38 = r10;
     */
    /* JADX WARNING: Missing block: B:129:0x0222, code skipped:
            if (r11.mService.mLazyModeOn == 0) goto L_0x023a;
     */
    /* JADX WARNING: Missing block: B:131:0x022a, code skipped:
            r5 = r14;
            r7 = r15;
     */
    /* JADX WARNING: Missing block: B:133:?, code skipped:
            r11.mService.setCropOnSingleHandMode(r11.mService.mLazyModeOn, false, r7, r5, r9);
     */
    /* JADX WARNING: Missing block: B:134:0x023a, code skipped:
            r5 = r14;
            r7 = r15;
     */
    /* JADX WARNING: Missing block: B:135:0x024c, code skipped:
            if ((((float) r3) / ((float) r6.width())) >= (((float) r2) / ((float) r6.height()))) goto L_0x026c;
     */
    /* JADX WARNING: Missing block: B:136:0x024e, code skipped:
            r9.right = r9.left + ((int) ((((float) r3) / ((float) r2)) * ((float) r6.height())));
     */
    /* JADX WARNING: Missing block: B:137:0x0263, code skipped:
            if (r9.right >= r6.width()) goto L_0x026b;
     */
    /* JADX WARNING: Missing block: B:138:0x0265, code skipped:
            r9.right = r6.width();
     */
    /* JADX WARNING: Missing block: B:140:0x026c, code skipped:
            r9.bottom = r9.top + ((int) ((((float) r2) / ((float) r3)) * ((float) r6.width())));
     */
    /* JADX WARNING: Missing block: B:142:0x027c, code skipped:
            if (r1 == true) goto L_0x0282;
     */
    /* JADX WARNING: Missing block: B:143:0x027e, code skipped:
            r4 = 3;
     */
    /* JADX WARNING: Missing block: B:144:0x027f, code skipped:
            if (r1 != true) goto L_0x0288;
     */
    /* JADX WARNING: Missing block: B:146:0x0282, code skipped:
            r4 = 3;
     */
    /* JADX WARNING: Missing block: B:147:0x0283, code skipped:
            if (r1 != true) goto L_0x0286;
     */
    /* JADX WARNING: Missing block: B:149:0x0286, code skipped:
            r4 = 1;
     */
    /* JADX WARNING: Missing block: B:150:0x0287, code skipped:
            r1 = r4;
     */
    /* JADX WARNING: Missing block: B:151:0x0288, code skipped:
            convertCropForSurfaceFlinger(r9, r1, r7, r5);
            r4 = r11.mService.mAnimator.getScreenRotationAnimationLocked(0);
     */
    /* JADX WARNING: Missing block: B:152:0x0294, code skipped:
            if (r4 == null) goto L_0x029f;
     */
    /* JADX WARNING: Missing block: B:154:0x029a, code skipped:
            if (r4.isAnimating() == false) goto L_0x029f;
     */
    /* JADX WARNING: Missing block: B:155:0x029c, code skipped:
            r31 = true;
     */
    /* JADX WARNING: Missing block: B:156:0x029f, code skipped:
            r31 = false;
     */
    /* JADX WARNING: Missing block: B:157:0x02a1, code skipped:
            android.view.SurfaceControl.openTransaction();
            android.view.SurfaceControl.closeTransactionSync();
            r0 = r49.screenshotForExternalDisplay(r41, r9, r3, r2, r11.minLayer, r11.maxLayer, r31, r1);
     */
    /* JADX WARNING: Missing block: B:158:0x02bf, code skipped:
            if (r0 != null) goto L_0x02ed;
     */
    /* JADX WARNING: Missing block: B:159:0x02c1, code skipped:
            r10 = new java.lang.StringBuilder();
            r10.append("Screenshot failure taking screenshot for (");
            r10.append(r7);
            r10.append("x");
            r10.append(r5);
            r10.append(") to layer ");
            r10.append(r11.maxLayer);
            android.util.Slog.w("WindowManager", r10.toString());
     */
    /* JADX WARNING: Missing block: B:160:0x02ea, code skipped:
            monitor-exit(r21);
     */
    /* JADX WARNING: Missing block: B:162:0x02ec, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:163:0x02ed, code skipped:
            monitor-exit(r21);
     */
    /* JADX WARNING: Missing block: B:164:0x02ee, code skipped:
            return r0;
     */
    /* JADX WARNING: Missing block: B:165:0x02ef, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:166:0x02f0, code skipped:
            r16 = r1;
     */
    /* JADX WARNING: Missing block: B:167:0x02f4, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:168:0x02f5, code skipped:
            r5 = r14;
            r7 = r15;
            r16 = r1;
     */
    /* JADX WARNING: Missing block: B:169:0x02fa, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:170:0x02fb, code skipped:
            r34 = r4;
            r35 = r5;
            r38 = r10;
            r5 = r14;
            r7 = r15;
            r16 = r1;
     */
    /* JADX WARNING: Missing block: B:171:0x0306, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:172:0x0307, code skipped:
            r34 = r4;
            r35 = r5;
            r38 = r10;
            r5 = r14;
            r7 = r15;
            r6 = r24;
            r16 = r1;
     */
    /* JADX WARNING: Missing block: B:173:0x0314, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:174:0x0315, code skipped:
            r35 = r5;
            r38 = r10;
            r5 = r14;
            r7 = r15;
            r34 = r22;
            r6 = r24;
            r3 = r43;
            r2 = r44;
     */
    /* JADX WARNING: Missing block: B:175:0x0324, code skipped:
            r3 = r43;
            r2 = r44;
     */
    /* JADX WARNING: Missing block: B:176:0x0329, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:177:0x032a, code skipped:
            r21 = r2;
            r34 = r3;
            r35 = r5;
            r23 = r6;
            r6 = r9;
            r38 = r10;
            r5 = r14;
            r7 = r15;
            r3 = r43;
            r2 = r44;
     */
    /* JADX WARNING: Missing block: B:179:?, code skipped:
            monitor-exit(r21);
     */
    /* JADX WARNING: Missing block: B:180:0x033c, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:181:0x033d, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private <E> E screenshotApplicationsForExternalDisplay(IBinder displayToken, IBinder appToken, int width, int height, boolean includeFullDisplay, float frameScale, boolean wallpaperOnly, boolean includeDecor, ScreenshoterForExternalDisplay<E> screenshoter) {
        Throwable th;
        Rect rect;
        Rect rect2;
        boolean z;
        IBinder iBinder = appToken;
        int dw = this.mDisplayInfo.logicalWidth;
        boolean dh = this.mDisplayInfo.logicalHeight;
        int i;
        if (dw == 0) {
            i = dw;
        } else if (dh) {
            Rect frame = new Rect();
            Rect stackBounds = new Rect();
            synchronized (this.mService.mWindowMap) {
                try {
                    AppWindowToken imeTargetAppToken;
                    if (this.mService.mInputMethodTarget != null) {
                        try {
                            imeTargetAppToken = this.mService.mInputMethodTarget.mAppToken;
                        } catch (Throwable th2) {
                            th = th2;
                            rect = frame;
                            rect2 = stackBounds;
                            z = dh;
                            i = dw;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    }
                    imeTargetAppToken = null;
                    z = (imeTargetAppToken == null || imeTargetAppToken.appToken == null || imeTargetAppToken.appToken.asBinder() != iBinder || this.mService.mInputMethodTarget.isInMultiWindowMode()) ? false : true;
                } catch (Throwable th4) {
                    th = th4;
                    rect = frame;
                    rect2 = stackBounds;
                    z = dh;
                    i = dw;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        } else {
            z = dh;
            i = dw;
        }
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:65:0x00c5  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x00c4 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ boolean lambda$screenshotApplicationsForExternalDisplay$0(HwDisplayContent hwDisplayContent, int aboveAppLayer, boolean wallpaperOnly, boolean includeImeInScreenshot, IBinder appToken, MutableBoolean mutableIncludeFullDisplay, boolean includeDecor, Rect frame, Rect stackBounds, WindowState w) {
        HwDisplayContent hwDisplayContent2 = hwDisplayContent;
        IBinder iBinder = appToken;
        MutableBoolean mutableBoolean = mutableIncludeFullDisplay;
        Rect rect = frame;
        WindowState windowState = w;
        if (!windowState.mHasSurface || windowState.mLayer >= aboveAppLayer) {
            return false;
        }
        if (wallpaperOnly && !windowState.mIsWallpaper) {
            return false;
        }
        boolean foundTargetWs;
        if (windowState.mIsImWindow) {
            if (!includeImeInScreenshot) {
                return false;
            }
        } else if (windowState.mIsWallpaper) {
            if (wallpaperOnly) {
                hwDisplayContent2.appWin = windowState;
            }
            if (hwDisplayContent2.appWin == null) {
                return false;
            }
        } else if (iBinder != null) {
            if (windowState.mAppToken == null || windowState.mAppToken.token != iBinder) {
                return false;
            }
            hwDisplayContent2.appWin = windowState;
        }
        WindowStateAnimator winAnim = windowState.mWinAnimator;
        int layer = winAnim.mSurfaceController.getLayer();
        if (hwDisplayContent2.maxLayer < layer) {
            hwDisplayContent2.maxLayer = layer;
        }
        if (hwDisplayContent2.minLayer > layer) {
            hwDisplayContent2.minLayer = layer;
        }
        if (!mutableBoolean.value && includeDecor) {
            TaskStack stack = w.getStack();
            if (stack != null) {
                stack.getBounds(rect);
            }
            rect.intersect(windowState.mFrame);
        } else if (!(mutableBoolean.value || windowState.mIsWallpaper)) {
            Rect wf = windowState.mFrame;
            Rect cr = windowState.mContentInsets;
            rect.union(wf.left + cr.left, wf.top + cr.top, wf.right - cr.right, wf.bottom - cr.bottom);
            windowState.getVisibleBounds(stackBounds);
            if (!Rect.intersects(frame, stackBounds)) {
                frame.setEmpty();
            }
            foundTargetWs = (windowState.mAppToken == null && windowState.mAppToken.token == iBinder) || (hwDisplayContent2.appWin != null && wallpaperOnly);
            if (foundTargetWs && winAnim.getShown()) {
                hwDisplayContent2.screenshotReady = true;
            }
            if (w.isObscuringDisplay()) {
                return false;
            }
            return true;
        }
        Rect rect2 = stackBounds;
        if (windowState.mAppToken == null) {
        }
        hwDisplayContent2.screenshotReady = true;
        if (w.isObscuringDisplay()) {
        }
    }

    private static /* synthetic */ void lambda$screenshotApplicationsForExternalDisplay$1(WindowState w) {
        WindowSurfaceController controller = w.mWinAnimator.mSurfaceController;
        String str = "WindowManager";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(w);
        stringBuilder.append(": ");
        stringBuilder.append(w.mLayer);
        stringBuilder.append(" animLayer=");
        stringBuilder.append(w.mWinAnimator.mAnimLayer);
        stringBuilder.append(" surfaceLayer=");
        stringBuilder.append(controller == null ? "null" : Integer.valueOf(controller.getLayer()));
        Slog.i(str, stringBuilder.toString());
    }

    protected boolean updateRotationUnchecked(boolean forceUpdate) {
        if (HwPCUtils.isPcCastModeInServer() && (HwPCUtils.isValidExtDisplayId(this.mDisplayId) || (HwPCUtils.enabledInPad() && getRotation() == 1))) {
            return false;
        }
        return super.updateRotationUnchecked(forceUpdate);
    }

    void performLayout(boolean initial, boolean updateInputWindows) {
        super.performLayout(initial, updateInputWindows);
    }

    void prepareSurfaces() {
        super.prepareSurfaces();
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(getDisplayId())) {
            int pcScreenDpMode = this.mService.getPCScreenDisplayMode();
            if (this.mService.mHwWMSEx != null) {
                this.mService.mHwWMSEx.computeShownFrameLockedByPCScreenDpMode(pcScreenDpMode);
            }
        }
    }

    protected void uploadOrientation(int rotation) {
        if (this.mService.mContext != null) {
            String rotationState;
            if (rotation == 1 || rotation == 3) {
                rotationState = "is horizontal screen";
            } else {
                rotationState = "is vertical screen";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ ");
            stringBuilder.append(rotationState);
            stringBuilder.append(" rotation:");
            stringBuilder.append(rotation);
            stringBuilder.append(" }");
            StatisticalUtils.reporte(this.mService.mContext, HwSecDiagnoseConstant.OEMINFO_ID_ROOT_CHECK, stringBuilder.toString());
        }
    }

    public boolean shouldDropMotionEventForTouchPad(float x, float y) {
        this.mTmpshouldDropMotionEventForTouchPad = false;
        forAllWindows(new -$$Lambda$HwDisplayContent$gpSSKJ2u9mSSHwlUQNX0an81APk(this, x, y), true);
        return this.mTmpshouldDropMotionEventForTouchPad;
    }

    public static /* synthetic */ boolean lambda$shouldDropMotionEventForTouchPad$2(HwDisplayContent hwDisplayContent, float x, float y, WindowState w) {
        String title = w.getAttrs().getTitle() == null ? null : w.getAttrs().getTitle().toString();
        if ("com.huawei.desktop.systemui/com.huawei.systemui.mk.activity.ImitateActivity".equalsIgnoreCase(title)) {
            hwDisplayContent.mTmpshouldDropMotionEventForTouchPad = false;
            return true;
        }
        if (w.isVisible()) {
            Region outRegion = new Region();
            w.getTouchableRegion(outRegion);
            if (outRegion.contains((int) x, (int) y)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("consume event in title = ");
                stringBuilder.append(title);
                Slog.d("WindowManager", stringBuilder.toString());
                hwDisplayContent.mTmpshouldDropMotionEventForTouchPad = true;
                return true;
            }
        }
        hwDisplayContent.mTmpshouldDropMotionEventForTouchPad = false;
        return false;
    }
}
