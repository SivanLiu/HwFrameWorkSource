package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.os.Debug;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.InputChannel;
import android.view.InputEventReceiver;
import android.view.InputEventReceiver.Factory;
import android.view.KeyEvent;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputManagerService.WindowManagerCallbacks;
import com.android.server.input.InputWindowHandle;
import com.android.server.policy.WindowManagerPolicy.InputConsumer;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

final class InputMonitor implements WindowManagerCallbacks {
    private boolean mAddInputConsumerHandle;
    private boolean mAddPipInputConsumerHandle;
    private boolean mAddRecentsAnimationInputConsumerHandle;
    private boolean mAddWallpaperInputConsumerHandle;
    private boolean mDisableWallpaperTouchEvents;
    private InputWindowHandle mFocusedInputWindowHandle;
    private boolean mHasLighterViewInPCCastMode;
    private boolean mHasLighterViewInPCCastModeTemp;
    private final ArrayMap<String, InputConsumerImpl> mInputConsumers = new ArrayMap();
    private boolean mInputDevicesReady;
    private final Object mInputDevicesReadyMonitor = new Object();
    private boolean mInputDispatchEnabled;
    private boolean mInputDispatchFrozen;
    private WindowState mInputFocus;
    private String mInputFreezeReason = null;
    private int mInputWindowHandleCount;
    private InputWindowHandle mInputWindowHandleInExtDisplay;
    private InputWindowHandle[] mInputWindowHandles;
    private final WindowManagerService mService;
    private final Rect mTmpRect = new Rect();
    private final UpdateInputForAllWindowsConsumer mUpdateInputForAllWindowsConsumer = new UpdateInputForAllWindowsConsumer();
    private boolean mUpdateInputWindowsNeeded = true;

    private final class UpdateInputForAllWindowsConsumer implements Consumer<WindowState> {
        boolean inDrag;
        InputConsumerImpl navInputConsumer;
        InputConsumerImpl pipInputConsumer;
        InputConsumerImpl recentsAnimationInputConsumer;
        WallpaperController wallpaperController;
        InputConsumerImpl wallpaperInputConsumer;

        private UpdateInputForAllWindowsConsumer() {
        }

        private void updateInputWindows(boolean inDrag) {
            this.navInputConsumer = InputMonitor.this.getInputConsumer("nav_input_consumer", 0);
            this.pipInputConsumer = InputMonitor.this.getInputConsumer("pip_input_consumer", 0);
            this.wallpaperInputConsumer = InputMonitor.this.getInputConsumer("wallpaper_input_consumer", 0);
            this.recentsAnimationInputConsumer = InputMonitor.this.getInputConsumer("recents_animation_input_consumer", 0);
            InputMonitor.this.mAddInputConsumerHandle = this.navInputConsumer != null;
            InputMonitor.this.mAddPipInputConsumerHandle = this.pipInputConsumer != null;
            InputMonitor.this.mAddWallpaperInputConsumerHandle = this.wallpaperInputConsumer != null;
            InputMonitor.this.mAddRecentsAnimationInputConsumerHandle = this.recentsAnimationInputConsumer != null;
            InputMonitor.this.mTmpRect.setEmpty();
            InputMonitor.this.mDisableWallpaperTouchEvents = false;
            this.inDrag = inDrag;
            this.wallpaperController = InputMonitor.this.mService.mRoot.mWallpaperController;
            InputMonitor.this.mService.mRoot.forAllWindows((Consumer) this, true);
            if (InputMonitor.this.mAddWallpaperInputConsumerHandle) {
                InputMonitor.this.addInputWindowHandle(this.wallpaperInputConsumer.mWindowHandle);
            }
            InputMonitor.this.mService.mInputManager.setInputWindows(InputMonitor.this.mInputWindowHandles, InputMonitor.this.mFocusedInputWindowHandle);
            InputMonitor.this.mHasLighterViewInPCCastMode = InputMonitor.this.mHasLighterViewInPCCastModeTemp;
            InputMonitor.this.mHasLighterViewInPCCastModeTemp = false;
            InputMonitor.this.setFousedWinExtDisplayInPCCastMode();
            InputMonitor.this.clearInputWindowHandlesLw();
        }

        public void accept(WindowState w) {
            WindowState windowState = w;
            InputChannel inputChannel = windowState.mInputChannel;
            InputWindowHandle inputWindowHandle = windowState.mInputWindowHandle;
            if (inputChannel != null && inputWindowHandle != null && !windowState.mRemoved && !w.canReceiveTouchInput()) {
                int flags = windowState.mAttrs.flags;
                int privateFlags = windowState.mAttrs.privateFlags;
                int type = windowState.mAttrs.type;
                boolean hasFocus = windowState == InputMonitor.this.mInputFocus;
                boolean isVisible = w.isVisibleLw();
                if (InputMonitor.this.mAddRecentsAnimationInputConsumerHandle) {
                    RecentsAnimationController recentsAnimationController = InputMonitor.this.mService.getRecentsAnimationController();
                    if (recentsAnimationController != null && recentsAnimationController.hasInputConsumerForApp(windowState.mAppToken)) {
                        if (recentsAnimationController.updateInputConsumerForApp(this.recentsAnimationInputConsumer, hasFocus)) {
                            InputMonitor.this.addInputWindowHandle(this.recentsAnimationInputConsumer.mWindowHandle);
                            InputMonitor.this.mAddRecentsAnimationInputConsumerHandle = false;
                        }
                        return;
                    }
                }
                if (w.inPinnedWindowingMode()) {
                    if (InputMonitor.this.mAddPipInputConsumerHandle && inputWindowHandle.layer <= this.pipInputConsumer.mWindowHandle.layer) {
                        windowState.getBounds(InputMonitor.this.mTmpRect);
                        this.pipInputConsumer.mWindowHandle.touchableRegion.set(InputMonitor.this.mTmpRect);
                        InputMonitor.this.addInputWindowHandle(this.pipInputConsumer.mWindowHandle);
                        InputMonitor.this.mAddPipInputConsumerHandle = false;
                    }
                    if (!hasFocus) {
                        return;
                    }
                }
                if (InputMonitor.this.mAddInputConsumerHandle && inputWindowHandle.layer <= this.navInputConsumer.mWindowHandle.layer) {
                    InputMonitor.this.addInputWindowHandle(this.navInputConsumer.mWindowHandle);
                    InputMonitor.this.mAddInputConsumerHandle = false;
                }
                if (InputMonitor.this.mAddWallpaperInputConsumerHandle && windowState.mAttrs.type == 2013 && w.isVisibleLw()) {
                    InputMonitor.this.addInputWindowHandle(this.wallpaperInputConsumer.mWindowHandle);
                    InputMonitor.this.mAddWallpaperInputConsumerHandle = false;
                }
                if ((privateFlags & 2048) != 0) {
                    InputMonitor.this.mDisableWallpaperTouchEvents = true;
                }
                boolean hasWallpaper = this.wallpaperController.isWallpaperTarget(windowState) && (privateFlags & 1024) == 0 && !InputMonitor.this.mDisableWallpaperTouchEvents;
                if (this.inDrag && isVisible && w.getDisplayContent().isDefaultDisplay) {
                    InputMonitor.this.mService.mDragDropController.sendDragStartedIfNeededLocked(windowState);
                }
                InputMonitor.this.addInputWindowHandle(inputWindowHandle, windowState, flags, type, isVisible, hasFocus, hasWallpaper);
            }
        }
    }

    private static final class EventReceiverInputConsumer extends InputConsumerImpl implements InputConsumer {
        private final InputEventReceiver mInputEventReceiver;
        private InputMonitor mInputMonitor;

        EventReceiverInputConsumer(WindowManagerService service, InputMonitor monitor, Looper looper, String name, Factory inputEventReceiverFactory, int clientPid, UserHandle clientUser) {
            super(service, null, name, null, clientPid, clientUser);
            this.mInputMonitor = monitor;
            this.mInputEventReceiver = inputEventReceiverFactory.createInputEventReceiver(this.mClientChannel, looper);
        }

        public void dismiss() {
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mInputMonitor.destroyInputConsumer(this.mWindowHandle.name)) {
                        this.mInputEventReceiver.dispose();
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    public InputMonitor(WindowManagerService service) {
        this.mService = service;
    }

    private void addInputConsumer(String name, InputConsumerImpl consumer) {
        this.mInputConsumers.put(name, consumer);
        consumer.linkToDeathRecipient();
        updateInputWindowsLw(true);
    }

    boolean destroyInputConsumer(String name) {
        if (!disposeInputConsumer((InputConsumerImpl) this.mInputConsumers.remove(name))) {
            return false;
        }
        updateInputWindowsLw(true);
        return true;
    }

    private boolean disposeInputConsumer(InputConsumerImpl consumer) {
        if (consumer == null) {
            return false;
        }
        consumer.disposeChannelsLw();
        return true;
    }

    InputConsumerImpl getInputConsumer(String name, int displayId) {
        return displayId == 0 ? (InputConsumerImpl) this.mInputConsumers.get(name) : null;
    }

    void layoutInputConsumers(int dw, int dh) {
        for (int i = this.mInputConsumers.size() - 1; i >= 0; i--) {
            ((InputConsumerImpl) this.mInputConsumers.valueAt(i)).layout(dw, dh);
        }
    }

    InputConsumer createInputConsumer(Looper looper, String name, Factory inputEventReceiverFactory) {
        if (this.mInputConsumers.containsKey(name)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Existing input consumer found with name: ");
            stringBuilder.append(name);
            throw new IllegalStateException(stringBuilder.toString());
        }
        EventReceiverInputConsumer eventReceiverInputConsumer = new EventReceiverInputConsumer(this.mService, this, looper, name, inputEventReceiverFactory, Process.myPid(), UserHandle.SYSTEM);
        addInputConsumer(name, eventReceiverInputConsumer);
        return eventReceiverInputConsumer;
    }

    void createInputConsumer(IBinder token, String name, InputChannel inputChannel, int clientPid, UserHandle clientUser) {
        if (this.mInputConsumers.containsKey(name)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Existing input consumer found with name: ");
            stringBuilder.append(name);
            throw new IllegalStateException(stringBuilder.toString());
        }
        InputConsumerImpl inputConsumerImpl = new InputConsumerImpl(this.mService, token, name, inputChannel, clientPid, clientUser);
        boolean z = true;
        int hashCode = name.hashCode();
        if (hashCode != 1024719987) {
            if (hashCode == 1415830696 && name.equals("wallpaper_input_consumer")) {
                z = false;
            }
        } else if (name.equals("pip_input_consumer")) {
            z = true;
        }
        switch (z) {
            case false:
                inputConsumerImpl.mWindowHandle.hasWallpaper = true;
                break;
            case true:
                InputWindowHandle inputWindowHandle = inputConsumerImpl.mWindowHandle;
                inputWindowHandle.layoutParamsFlags |= 32;
                break;
        }
        addInputConsumer(name, inputConsumerImpl);
    }

    public void notifyInputChannelBroken(InputWindowHandle inputWindowHandle) {
        if (inputWindowHandle != null) {
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = inputWindowHandle.windowState;
                    if (windowState != null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("WINDOW DIED ");
                        stringBuilder.append(windowState);
                        Slog.i("WindowManager", stringBuilder.toString());
                        windowState.removeIfPossible();
                    }
                } finally {
                    while (true) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    public long notifyANR(InputApplicationHandle inputApplicationHandle, InputWindowHandle inputWindowHandle, String reason) {
        boolean abort;
        AppWindowToken appWindowToken = null;
        WindowState windowState = null;
        boolean aboveSystem = false;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (inputWindowHandle != null) {
                    windowState = (WindowState) inputWindowHandle.windowState;
                    if (windowState != null) {
                        appWindowToken = windowState.mAppToken;
                    }
                }
                if (appWindowToken == null && inputApplicationHandle != null) {
                    appWindowToken = (AppWindowToken) inputApplicationHandle.appWindowToken;
                }
                abort = false;
                StringBuilder stringBuilder;
                if (windowState != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Input event dispatching timed out sending to ");
                    stringBuilder.append(windowState.mAttrs.getTitle());
                    stringBuilder.append(".  Reason: ");
                    stringBuilder.append(reason);
                    Slog.i("WindowManager", stringBuilder.toString());
                    aboveSystem = windowState.mBaseLayer > this.mService.mPolicy.getWindowLayerFromTypeLw(2038, windowState.mOwnerCanAddInternalSystemWindow);
                } else if (appWindowToken != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Input event dispatching timed out sending to application ");
                    stringBuilder.append(appWindowToken.stringName);
                    stringBuilder.append(".  Reason: ");
                    stringBuilder.append(reason);
                    Slog.i("WindowManager", stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Input event dispatching timed out .  Reason: ");
                    stringBuilder.append(reason);
                    Slog.i("WindowManager", stringBuilder.toString());
                }
                this.mService.saveANRStateLocked(appWindowToken, windowState, reason);
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        this.mService.mAmInternal.saveANRState(reason);
        if (appWindowToken != null && appWindowToken.appToken != null) {
            AppWindowContainerController controller = appWindowToken.getController();
            if (controller != null) {
                if (controller.keyDispatchingTimedOut(reason, windowState != null ? windowState.mSession.mPid : -1)) {
                    abort = true;
                }
            }
            if (!abort) {
                return appWindowToken.mInputDispatchingTimeoutNanos;
            }
        } else if (windowState != null) {
            try {
                long timeout = ActivityManager.getService().inputDispatchingTimedOut(windowState.mSession.mPid, aboveSystem, reason);
                if (timeout >= 0) {
                    return 1000000 * timeout;
                }
            } catch (RemoteException e) {
            }
        }
        return 0;
    }

    private void addInputWindowHandle(InputWindowHandle windowHandle) {
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(windowHandle.displayId)) {
            WindowState ws = windowHandle.windowState;
            if (!(ws == null || ws.getAttrs().getTitle() == null || !"com.huawei.systemui.mk.lighterdrawer.LighterDrawView".equals(ws.getAttrs().getTitle().toString()))) {
                this.mHasLighterViewInPCCastModeTemp = true;
            }
        }
        if (this.mInputWindowHandles == null) {
            this.mInputWindowHandles = new InputWindowHandle[16];
        }
        if (this.mInputWindowHandleCount >= this.mInputWindowHandles.length) {
            this.mInputWindowHandles = (InputWindowHandle[]) Arrays.copyOf(this.mInputWindowHandles, this.mInputWindowHandleCount * 2);
        }
        InputWindowHandle[] inputWindowHandleArr = this.mInputWindowHandles;
        int i = this.mInputWindowHandleCount;
        this.mInputWindowHandleCount = i + 1;
        inputWindowHandleArr[i] = windowHandle;
    }

    void addInputWindowHandle(InputWindowHandle inputWindowHandle, WindowState child, int flags, int type, boolean isVisible, boolean hasFocus, boolean hasWallpaper) {
        inputWindowHandle.name = child.toString();
        inputWindowHandle.layoutParamsFlags = child.getTouchableRegion(inputWindowHandle.touchableRegion, flags);
        boolean z = false;
        if (inputWindowHandle.windowState != null && (inputWindowHandle.windowState instanceof WindowState)) {
            WindowState windowState = inputWindowHandle.windowState;
            inputWindowHandle.layoutParamsPrivateFlags = windowState.isWindowUsingNotch() ? 65536 : 0;
            int i = 131072;
            boolean isExcludeTransferEventWindow = (windowState.getAttrs().hwFlags & 131072) != 0;
            int i2 = inputWindowHandle.layoutParamsPrivateFlags;
            if (!isExcludeTransferEventWindow) {
                i = 0;
            }
            inputWindowHandle.layoutParamsPrivateFlags = i | i2;
        }
        inputWindowHandle.layoutParamsType = type;
        inputWindowHandle.dispatchingTimeoutNanos = child.getInputDispatchingTimeoutNanos();
        inputWindowHandle.visible = isVisible;
        inputWindowHandle.canReceiveKeys = child.canReceiveKeys();
        inputWindowHandle.hasFocus = hasFocus;
        inputWindowHandle.hasWallpaper = hasWallpaper;
        if (child.mAppToken != null) {
            z = child.mAppToken.paused;
        }
        inputWindowHandle.paused = z;
        inputWindowHandle.layer = child.mLayer;
        inputWindowHandle.ownerPid = child.mSession.mPid;
        inputWindowHandle.ownerUid = child.mSession.mUid;
        inputWindowHandle.inputFeatures = child.mAttrs.inputFeatures;
        Rect frame = child.mFrame;
        inputWindowHandle.frameLeft = frame.left;
        inputWindowHandle.frameTop = frame.top;
        inputWindowHandle.frameRight = frame.right;
        inputWindowHandle.frameBottom = frame.bottom;
        inputWindowHandle.displayId = child.getDisplayId();
        if (child.mGlobalScale != 1.0f) {
            inputWindowHandle.scaleFactor = 1.0f / child.mGlobalScale;
        } else {
            inputWindowHandle.scaleFactor = 1.0f;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addInputWindowHandle: ");
            stringBuilder.append(child);
            stringBuilder.append(", ");
            stringBuilder.append(inputWindowHandle);
            Slog.d("WindowManager", stringBuilder.toString());
        }
        addInputWindowHandle(inputWindowHandle);
        if (hasFocus) {
            this.mFocusedInputWindowHandle = inputWindowHandle;
        }
    }

    private void clearInputWindowHandlesLw() {
        while (this.mInputWindowHandleCount != 0) {
            InputWindowHandle[] inputWindowHandleArr = this.mInputWindowHandles;
            int i = this.mInputWindowHandleCount - 1;
            this.mInputWindowHandleCount = i;
            inputWindowHandleArr[i] = null;
        }
        this.mFocusedInputWindowHandle = null;
    }

    void setUpdateInputWindowsNeededLw() {
        this.mUpdateInputWindowsNeeded = true;
    }

    void updateInputWindowsLw(boolean force) {
        if (force || this.mUpdateInputWindowsNeeded) {
            this.mUpdateInputWindowsNeeded = false;
            boolean inDrag = this.mService.mDragDropController.dragDropActiveLocked();
            if (inDrag) {
                InputWindowHandle dragWindowHandle = this.mService.mDragDropController.getInputWindowHandleLocked();
                if (dragWindowHandle != null) {
                    addInputWindowHandle(dragWindowHandle);
                } else {
                    Slog.w("WindowManager", "Drag is in progress but there is no drag window handle.");
                }
            }
            if (this.mService.mTaskPositioningController.isPositioningLocked()) {
                InputWindowHandle dragWindowHandle2 = this.mService.mTaskPositioningController.getDragWindowHandleLocked();
                if (dragWindowHandle2 != null) {
                    addInputWindowHandle(dragWindowHandle2);
                } else {
                    Slog.e("WindowManager", "Repositioning is in progress but there is no drag window handle.");
                }
            }
            this.mUpdateInputForAllWindowsConsumer.updateInputWindows(inDrag);
        }
    }

    public void notifyConfigurationChanged() {
        this.mService.sendNewConfiguration(0);
        synchronized (this.mInputDevicesReadyMonitor) {
            if (!this.mInputDevicesReady) {
                this.mInputDevicesReady = true;
                this.mInputDevicesReadyMonitor.notifyAll();
            }
        }
    }

    public boolean waitForInputDevicesReady(long timeoutMillis) {
        boolean z;
        synchronized (this.mInputDevicesReadyMonitor) {
            if (!this.mInputDevicesReady) {
                try {
                    this.mInputDevicesReadyMonitor.wait(timeoutMillis);
                } catch (InterruptedException e) {
                }
            }
            z = this.mInputDevicesReady;
        }
        return z;
    }

    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        this.mService.mPolicy.notifyLidSwitchChanged(whenNanos, lidOpen);
    }

    public void notifyCameraLensCoverSwitchChanged(long whenNanos, boolean lensCovered) {
        this.mService.mPolicy.notifyCameraLensCoverSwitchChanged(whenNanos, lensCovered);
    }

    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        return this.mService.mPolicy.interceptKeyBeforeQueueing(event, policyFlags);
    }

    public int interceptMotionBeforeQueueingNonInteractive(long whenNanos, int policyFlags) {
        return this.mService.mPolicy.interceptMotionBeforeQueueingNonInteractive(whenNanos, policyFlags);
    }

    public long interceptKeyBeforeDispatching(InputWindowHandle focus, KeyEvent event, int policyFlags) {
        return this.mService.mPolicy.interceptKeyBeforeDispatching(focus != null ? (WindowState) focus.windowState : null, event, policyFlags);
    }

    public KeyEvent dispatchUnhandledKey(InputWindowHandle focus, KeyEvent event, int policyFlags) {
        return this.mService.mPolicy.dispatchUnhandledKey(focus != null ? (WindowState) focus.windowState : null, event, policyFlags);
    }

    public int getPointerLayer() {
        return (this.mService.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 1000;
    }

    public void setInputFocusLw(WindowState newWindow, boolean updateInputWindows) {
        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT || WindowManagerDebugConfig.DEBUG_INPUT) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Input focus has changed to ");
            stringBuilder.append(newWindow);
            Slog.d("WindowManager", stringBuilder.toString());
        }
        if (newWindow != this.mInputFocus) {
            if (newWindow != null && newWindow.canReceiveKeys()) {
                newWindow.mToken.paused = false;
            }
            this.mInputFocus = newWindow;
            setUpdateInputWindowsNeededLw();
            if (updateInputWindows) {
                updateInputWindowsLw(false);
            }
        }
    }

    public void setFocusedAppLw(AppWindowToken newApp) {
        if (newApp == null) {
            this.mService.mInputManager.setFocusedApplication(null);
            return;
        }
        InputApplicationHandle handle = newApp.mInputApplicationHandle;
        handle.name = newApp.toString();
        handle.dispatchingTimeoutNanos = newApp.mInputDispatchingTimeoutNanos;
        this.mService.mInputManager.setFocusedApplication(handle);
    }

    public void pauseDispatchingLw(WindowToken window) {
        if (!window.paused) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Pausing WindowToken ");
                stringBuilder.append(window);
                Slog.v("WindowManager", stringBuilder.toString());
            }
            window.paused = true;
            updateInputWindowsLw(true);
        }
    }

    public void resumeDispatchingLw(WindowToken window) {
        if (window.paused) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Resuming WindowToken ");
                stringBuilder.append(window);
                Slog.v("WindowManager", stringBuilder.toString());
            }
            window.paused = false;
            updateInputWindowsLw(true);
        }
    }

    public void freezeInputDispatchingLw() {
        if (!this.mInputDispatchFrozen) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v("WindowManager", "Freezing input dispatching");
            }
            this.mInputDispatchFrozen = true;
            boolean z = WindowManagerDebugConfig.DEBUG_INPUT;
            this.mInputFreezeReason = Debug.getCallers(6);
            updateInputDispatchModeLw();
        }
    }

    public void thawInputDispatchingLw() {
        if (this.mInputDispatchFrozen) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v("WindowManager", "Thawing input dispatching");
            }
            this.mInputDispatchFrozen = false;
            this.mInputFreezeReason = null;
            updateInputDispatchModeLw();
        }
    }

    public void setEventDispatchingLw(boolean enabled) {
        if (this.mInputDispatchEnabled != enabled) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Setting event dispatching to ");
                stringBuilder.append(enabled);
                Slog.v("WindowManager", stringBuilder.toString());
            }
            this.mInputDispatchEnabled = enabled;
            updateInputDispatchModeLw();
        }
    }

    private void updateInputDispatchModeLw() {
        this.mService.mInputManager.setInputDispatchMode(this.mInputDispatchEnabled, this.mInputDispatchFrozen);
    }

    void dump(PrintWriter pw, String prefix) {
        if (this.mInputFreezeReason != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mInputFreezeReason=");
            stringBuilder.append(this.mInputFreezeReason);
            pw.println(stringBuilder.toString());
        }
        Set<String> inputConsumerKeys = this.mInputConsumers.keySet();
        if (!inputConsumerKeys.isEmpty()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("InputConsumers:");
            pw.println(stringBuilder2.toString());
            for (String key : inputConsumerKeys) {
                ((InputConsumerImpl) this.mInputConsumers.get(key)).dump(pw, key, prefix);
            }
        }
    }

    public void notifyANRWarning(int pid) {
    }

    public boolean hasLighterViewInPCCastMode() {
        return HwPCUtils.isPcCastModeInServer() && this.mHasLighterViewInPCCastMode;
    }

    private void setFousedWinExtDisplayInPCCastMode() {
        if (HwPCUtils.isPcCastModeInServer()) {
            for (InputWindowHandle inputWindowHandle : this.mInputWindowHandles) {
                if (inputWindowHandle != null && HwPCUtils.isValidExtDisplayId(inputWindowHandle.displayId) && inputWindowHandle.canReceiveKeys) {
                    this.mInputWindowHandleInExtDisplay = inputWindowHandle;
                    return;
                }
            }
            this.mInputWindowHandleInExtDisplay = null;
        }
    }

    public InputWindowHandle getFousedWinExtDisplayInPCCastMode() {
        return this.mInputWindowHandleInExtDisplay;
    }
}
