package android.view;

import android.app.IAssistDataReceiver;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import com.android.internal.os.IResultReceiver;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;

public interface IWindowManager extends IInterface {

    public static abstract class Stub extends Binder implements IWindowManager {
        private static final String DESCRIPTOR = "android.view.IWindowManager";
        static final int TRANSACTION_addWindowToken = 17;
        static final int TRANSACTION_clearForcedDisplayDensityForUser = 13;
        static final int TRANSACTION_clearForcedDisplaySize = 9;
        static final int TRANSACTION_clearWindowContentFrameStats = 77;
        static final int TRANSACTION_closeSystemDialogs = 44;
        static final int TRANSACTION_createInputConsumer = 88;
        static final int TRANSACTION_destroyInputConsumer = 89;
        static final int TRANSACTION_disableKeyguard = 37;
        static final int TRANSACTION_dismissKeyguard = 42;
        static final int TRANSACTION_dontOverrideDisplayInfo = 95;
        static final int TRANSACTION_enableScreenIfNeeded = 76;
        static final int TRANSACTION_endProlongedAnimations = 32;
        static final int TRANSACTION_executeAppTransition = 31;
        static final int TRANSACTION_exitKeyguardSecurely = 39;
        static final int TRANSACTION_freezeRotation = 60;
        static final int TRANSACTION_getAnimationScale = 46;
        static final int TRANSACTION_getAnimationScales = 47;
        static final int TRANSACTION_getBaseDisplayDensity = 11;
        static final int TRANSACTION_getBaseDisplaySize = 7;
        static final int TRANSACTION_getCurrentAnimatorScale = 50;
        static final int TRANSACTION_getCurrentImeTouchRegion = 90;
        static final int TRANSACTION_getDefaultDisplayRotation = 56;
        static final int TRANSACTION_getDockedStackSide = 79;
        static final int TRANSACTION_getFocusedAppComponentName = 98;
        static final int TRANSACTION_getHwInnerService = 97;
        static final int TRANSACTION_getInitialDisplayDensity = 10;
        static final int TRANSACTION_getInitialDisplaySize = 6;
        static final int TRANSACTION_getLayerIndex = 96;
        static final int TRANSACTION_getNavBarPosition = 73;
        static final int TRANSACTION_getPendingAppTransition = 21;
        static final int TRANSACTION_getPreferredOptionsPanelGravity = 59;
        static final int TRANSACTION_getStableInsets = 85;
        static final int TRANSACTION_getWindowContentFrameStats = 78;
        static final int TRANSACTION_hasNavigationBar = 72;
        static final int TRANSACTION_inputMethodClientHasFocus = 5;
        static final int TRANSACTION_isKeyguardLocked = 40;
        static final int TRANSACTION_isKeyguardSecure = 41;
        static final int TRANSACTION_isRotationFrozen = 62;
        static final int TRANSACTION_isSafeModeEnabled = 75;
        static final int TRANSACTION_isViewServerRunning = 3;
        static final int TRANSACTION_isWindowTraceEnabled = 93;
        static final int TRANSACTION_lockNow = 74;
        static final int TRANSACTION_openSession = 4;
        static final int TRANSACTION_overridePendingAppTransition = 22;
        static final int TRANSACTION_overridePendingAppTransitionAspectScaledThumb = 26;
        static final int TRANSACTION_overridePendingAppTransitionClipReveal = 24;
        static final int TRANSACTION_overridePendingAppTransitionInPlace = 28;
        static final int TRANSACTION_overridePendingAppTransitionMultiThumb = 27;
        static final int TRANSACTION_overridePendingAppTransitionMultiThumbFuture = 29;
        static final int TRANSACTION_overridePendingAppTransitionRemote = 30;
        static final int TRANSACTION_overridePendingAppTransitionScaleUp = 23;
        static final int TRANSACTION_overridePendingAppTransitionThumb = 25;
        static final int TRANSACTION_prepareAppTransition = 20;
        static final int TRANSACTION_reenableKeyguard = 38;
        static final int TRANSACTION_refreshScreenCaptureDisabled = 54;
        static final int TRANSACTION_registerDockedStackListener = 81;
        static final int TRANSACTION_registerPinnedStackListener = 82;
        static final int TRANSACTION_registerShortcutKey = 86;
        static final int TRANSACTION_registerWallpaperVisibilityListener = 64;
        static final int TRANSACTION_removeRotationWatcher = 58;
        static final int TRANSACTION_removeWindowToken = 18;
        static final int TRANSACTION_requestAppKeyboardShortcuts = 84;
        static final int TRANSACTION_requestAssistScreenshot = 66;
        static final int TRANSACTION_requestUserActivityNotification = 94;
        static final int TRANSACTION_screenshotWallpaper = 63;
        static final int TRANSACTION_setAnimationScale = 48;
        static final int TRANSACTION_setAnimationScales = 49;
        static final int TRANSACTION_setDockedStackDividerTouchRegion = 80;
        static final int TRANSACTION_setEventDispatching = 16;
        static final int TRANSACTION_setExitInfo = 45;
        static final int TRANSACTION_setFocusedApp = 19;
        static final int TRANSACTION_setForcedDisplayDensityAndSize = 87;
        static final int TRANSACTION_setForcedDisplayDensityForUser = 12;
        static final int TRANSACTION_setForcedDisplayScalingMode = 14;
        static final int TRANSACTION_setForcedDisplaySize = 8;
        static final int TRANSACTION_setInTouchMode = 51;
        static final int TRANSACTION_setNavBarVirtualKeyHapticFeedbackEnabled = 71;
        static final int TRANSACTION_setNewDisplayOverrideConfiguration = 34;
        static final int TRANSACTION_setOverscan = 15;
        static final int TRANSACTION_setPipVisibility = 69;
        static final int TRANSACTION_setRecentsVisibility = 68;
        static final int TRANSACTION_setResizeDimLayer = 83;
        static final int TRANSACTION_setShelfHeight = 70;
        static final int TRANSACTION_setStrictModeVisualIndicatorPreference = 53;
        static final int TRANSACTION_setSwitchingUser = 43;
        static final int TRANSACTION_showStrictModeViolation = 52;
        static final int TRANSACTION_startFreezingScreen = 35;
        static final int TRANSACTION_startViewServer = 1;
        static final int TRANSACTION_startWindowTrace = 91;
        static final int TRANSACTION_statusBarVisibilityChanged = 67;
        static final int TRANSACTION_stopFreezingScreen = 36;
        static final int TRANSACTION_stopViewServer = 2;
        static final int TRANSACTION_stopWindowTrace = 92;
        static final int TRANSACTION_thawRotation = 61;
        static final int TRANSACTION_unregisterWallpaperVisibilityListener = 65;
        static final int TRANSACTION_updateOrientationFromAppTokens = 33;
        static final int TRANSACTION_updateRotation = 55;
        static final int TRANSACTION_watchRotation = 57;

        private static class Proxy implements IWindowManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public boolean startViewServer(int port) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(port);
                    boolean z = false;
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean stopViewServer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isViewServerRunning() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IWindowSession openSession(IWindowSessionCallback callback, IInputMethodClient client, IInputContext inputContext) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    IBinder iBinder = null;
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    if (inputContext != null) {
                        iBinder = inputContext.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    IWindowSession _result = android.view.IWindowSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean inputMethodClientHasFocus(IInputMethodClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void getInitialDisplaySize(int displayId, Point size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        size.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void getBaseDisplaySize(int displayId, Point size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        size.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setForcedDisplaySize(int displayId, int width, int height) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearForcedDisplaySize(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getInitialDisplayDensity(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getBaseDisplayDensity(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setForcedDisplayDensityForUser(int displayId, int density, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(density);
                    _data.writeInt(userId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearForcedDisplayDensityForUser(int displayId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(userId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setForcedDisplayScalingMode(int displayId, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(mode);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setOverscan(int displayId, int left, int top, int right, int bottom) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(left);
                    _data.writeInt(top);
                    _data.writeInt(right);
                    _data.writeInt(bottom);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setEventDispatching(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addWindowToken(IBinder token, int type, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(type);
                    _data.writeInt(displayId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeWindowToken(IBinder token, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(displayId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setFocusedApp(IBinder token, boolean moveFocusNow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(moveFocusNow);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void prepareAppTransition(int transit, boolean alwaysKeepCurrent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(transit);
                    _data.writeInt(alwaysKeepCurrent);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPendingAppTransition() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransition(String packageName, int enterAnim, int exitAnim, IRemoteCallback startedCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(enterAnim);
                    _data.writeInt(exitAnim);
                    _data.writeStrongBinder(startedCallback != null ? startedCallback.asBinder() : null);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransitionScaleUp(int startX, int startY, int startWidth, int startHeight) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(startX);
                    _data.writeInt(startY);
                    _data.writeInt(startWidth);
                    _data.writeInt(startHeight);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransitionClipReveal(int startX, int startY, int startWidth, int startHeight) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(startX);
                    _data.writeInt(startY);
                    _data.writeInt(startWidth);
                    _data.writeInt(startHeight);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransitionThumb(GraphicBuffer srcThumb, int startX, int startY, IRemoteCallback startedCallback, boolean scaleUp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (srcThumb != null) {
                        _data.writeInt(1);
                        srcThumb.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(startX);
                    _data.writeInt(startY);
                    _data.writeStrongBinder(startedCallback != null ? startedCallback.asBinder() : null);
                    _data.writeInt(scaleUp);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransitionAspectScaledThumb(GraphicBuffer srcThumb, int startX, int startY, int targetWidth, int targetHeight, IRemoteCallback startedCallback, boolean scaleUp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (srcThumb != null) {
                        _data.writeInt(1);
                        srcThumb.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(startX);
                    _data.writeInt(startY);
                    _data.writeInt(targetWidth);
                    _data.writeInt(targetHeight);
                    _data.writeStrongBinder(startedCallback != null ? startedCallback.asBinder() : null);
                    _data.writeInt(scaleUp);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] specs, IRemoteCallback startedCallback, IRemoteCallback finishedCallback, boolean scaleUp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(specs, 0);
                    IBinder iBinder = null;
                    _data.writeStrongBinder(startedCallback != null ? startedCallback.asBinder() : null);
                    if (finishedCallback != null) {
                        iBinder = finishedCallback.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(scaleUp);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransitionInPlace(String packageName, int anim) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(anim);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture specsFuture, IRemoteCallback startedCallback, boolean scaleUp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    IBinder iBinder = null;
                    _data.writeStrongBinder(specsFuture != null ? specsFuture.asBinder() : null);
                    if (startedCallback != null) {
                        iBinder = startedCallback.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(scaleUp);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (remoteAnimationAdapter != null) {
                        _data.writeInt(1);
                        remoteAnimationAdapter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void executeAppTransition() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void endProlongedAnimations() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Configuration updateOrientationFromAppTokens(Configuration currentConfig, IBinder freezeThisOneIfNeeded, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Configuration _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (currentConfig != null) {
                        _data.writeInt(1);
                        currentConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(freezeThisOneIfNeeded);
                    _data.writeInt(displayId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Configuration) Configuration.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] setNewDisplayOverrideConfiguration(Configuration overrideConfig, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (overrideConfig != null) {
                        _data.writeInt(1);
                        overrideConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(displayId);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startFreezingScreen(int exitAnim, int enterAnim) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(exitAnim);
                    _data.writeInt(enterAnim);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopFreezingScreen() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableKeyguard(IBinder token, String tag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(tag);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reenableKeyguard(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void exitKeyguardSecurely(IOnKeyguardExitResult callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isKeyguardLocked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isKeyguardSecure() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dismissKeyguard(IKeyguardDismissCallback callback, CharSequence message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSwitchingUser(boolean switching) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(switching);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void closeSystemDialogs(String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(reason);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setExitInfo(float pivotX, float pivotY, int iconWidth, int iconHeight, Bitmap iconBitmap, int flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(pivotX);
                    _data.writeFloat(pivotY);
                    _data.writeInt(iconWidth);
                    _data.writeInt(iconHeight);
                    if (iconBitmap != null) {
                        _data.writeInt(1);
                        iconBitmap.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flag);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public float getAnimationScale(int which) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(which);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    float _result = _reply.readFloat();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public float[] getAnimationScales() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    float[] _result = _reply.createFloatArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAnimationScale(int which, float scale) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(which);
                    _data.writeFloat(scale);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAnimationScales(float[] scales) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloatArray(scales);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public float getCurrentAnimatorScale() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    float _result = _reply.readFloat();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInTouchMode(boolean showFocus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(showFocus);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showStrictModeViolation(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setStrictModeVisualIndicatorPreference(String enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(enabled);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void refreshScreenCaptureDisabled(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateRotation(boolean alwaysSendConfiguration, boolean forceRelayout) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(alwaysSendConfiguration);
                    _data.writeInt(forceRelayout);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDefaultDisplayRotation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int watchRotation(IRotationWatcher watcher, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(watcher != null ? watcher.asBinder() : null);
                    _data.writeInt(displayId);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeRotationWatcher(IRotationWatcher watcher) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(watcher != null ? watcher.asBinder() : null);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPreferredOptionsPanelGravity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void freezeRotation(int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(rotation);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void thawRotation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isRotationFrozen() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bitmap screenshotWallpaper() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bitmap _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bitmap) Bitmap.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean registerWallpaperVisibilityListener(IWallpaperVisibilityListener listener, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeInt(displayId);
                    boolean z = false;
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener listener, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeInt(displayId);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean requestAssistScreenshot(IAssistDataReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void statusBarVisibilityChanged(int visibility) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visibility);
                    this.mRemote.transact(67, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setRecentsVisibility(boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visible);
                    this.mRemote.transact(68, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setPipVisibility(boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visible);
                    this.mRemote.transact(69, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setShelfHeight(boolean visible, int shelfHeight) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visible);
                    _data.writeInt(shelfHeight);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setNavBarVirtualKeyHapticFeedbackEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean hasNavigationBar() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getNavBarPosition() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void lockNow(Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSafeModeEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableScreenIfNeeded() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean clearWindowContentFrameStats(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    boolean z = false;
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WindowContentFrameStats getWindowContentFrameStats(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    WindowContentFrameStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (WindowContentFrameStats) WindowContentFrameStats.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDockedStackSide() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDockedStackDividerTouchRegion(Rect touchableRegion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (touchableRegion != null) {
                        _data.writeInt(1);
                        touchableRegion.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerDockedStackListener(IDockedStackListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerPinnedStackListener(int displayId, IPinnedStackListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setResizeDimLayer(boolean visible, int targetWindowingMode, float alpha) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visible);
                    _data.writeInt(targetWindowingMode);
                    _data.writeFloat(alpha);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void requestAppKeyboardShortcuts(IResultReceiver receiver, int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void getStableInsets(int displayId, Rect outInsets) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        outInsets.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerShortcutKey(long shortcutCode, IShortcutService keySubscriber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(shortcutCode);
                    _data.writeStrongBinder(keySubscriber != null ? keySubscriber.asBinder() : null);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setForcedDisplayDensityAndSize(int displayId, int density, int width, int height) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(density);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    this.mRemote.transact(87, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void createInputConsumer(IBinder token, String name, InputChannel inputChannel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(name);
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        inputChannel.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean destroyInputConsumer(String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    boolean z = false;
                    this.mRemote.transact(89, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Region getCurrentImeTouchRegion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Region _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Region) Region.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startWindowTrace() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(91, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopWindowTrace() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isWindowTraceEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(93, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void requestUserActivityNotification() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(94, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dontOverrideDisplayInfo(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(95, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getLayerIndex(String appName, int windowType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(appName);
                    _data.writeInt(windowType);
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getHwInnerService() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(97, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getFocusedAppComponentName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(98, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWindowManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWindowManager)) {
                return new Proxy(obj);
            }
            return (IWindowManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                IBinder _arg0 = null;
                boolean _arg3 = false;
                boolean _result;
                boolean _result2;
                int _arg02;
                Point _arg1;
                int _result3;
                RemoteAnimationAdapter _arg03;
                IRemoteCallback _arg12;
                Configuration _arg04;
                Bundle _arg05;
                int _result4;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        _result = startViewServer(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _result2 = stopViewServer();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result2 = isViewServerRunning();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        IWindowSession _result5 = openSession(android.view.IWindowSessionCallback.Stub.asInterface(data.readStrongBinder()), com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder()), com.android.internal.view.IInputContext.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result5 != null) {
                            _arg0 = _result5.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg0);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _result = inputMethodClientHasFocus(com.android.internal.view.IInputMethodClient.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        _arg1 = new Point();
                        getInitialDisplaySize(_arg02, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(1);
                        _arg1.writeToParcel(parcel2, 1);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        _arg1 = new Point();
                        getBaseDisplaySize(_arg02, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(1);
                        _arg1.writeToParcel(parcel2, 1);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        setForcedDisplaySize(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        clearForcedDisplaySize(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _result3 = getInitialDisplayDensity(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result3 = getBaseDisplayDensity(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        setForcedDisplayDensityForUser(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        clearForcedDisplayDensityForUser(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        setForcedDisplayScalingMode(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        setOverscan(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setEventDispatching(_arg3);
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        addWindowToken(data.readStrongBinder(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        removeWindowToken(data.readStrongBinder(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setFocusedApp(_arg0, _arg3);
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        prepareAppTransition(_arg02, _arg3);
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getPendingAppTransition();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        overridePendingAppTransition(data.readString(), data.readInt(), data.readInt(), android.os.IRemoteCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        overridePendingAppTransitionScaleUp(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        overridePendingAppTransitionClipReveal(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (GraphicBuffer) GraphicBuffer.CREATOR.createFromParcel(parcel);
                        }
                        overridePendingAppTransitionThumb(_arg03, data.readInt(), data.readInt(), android.os.IRemoteCallback.Stub.asInterface(data.readStrongBinder()), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (GraphicBuffer) GraphicBuffer.CREATOR.createFromParcel(parcel);
                        }
                        overridePendingAppTransitionAspectScaledThumb(_arg03, data.readInt(), data.readInt(), data.readInt(), data.readInt(), android.os.IRemoteCallback.Stub.asInterface(data.readStrongBinder()), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        AppTransitionAnimationSpec[] _arg06 = (AppTransitionAnimationSpec[]) parcel.createTypedArray(AppTransitionAnimationSpec.CREATOR);
                        _arg12 = android.os.IRemoteCallback.Stub.asInterface(data.readStrongBinder());
                        IRemoteCallback _arg2 = android.os.IRemoteCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        overridePendingAppTransitionMultiThumb(_arg06, _arg12, _arg2, _arg3);
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        overridePendingAppTransitionInPlace(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        IAppTransitionAnimationSpecsFuture _arg07 = android.view.IAppTransitionAnimationSpecsFuture.Stub.asInterface(data.readStrongBinder());
                        _arg12 = android.os.IRemoteCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        overridePendingAppTransitionMultiThumbFuture(_arg07, _arg12, _arg3);
                        reply.writeNoException();
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (RemoteAnimationAdapter) RemoteAnimationAdapter.CREATOR.createFromParcel(parcel);
                        }
                        overridePendingAppTransitionRemote(_arg03);
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        executeAppTransition();
                        reply.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        endProlongedAnimations();
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (Configuration) Configuration.CREATOR.createFromParcel(parcel);
                        }
                        Configuration _result6 = updateOrientationFromAppTokens(_arg04, data.readStrongBinder(), data.readInt());
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (Configuration) Configuration.CREATOR.createFromParcel(parcel);
                        }
                        int[] _result7 = setNewDisplayOverrideConfiguration(_arg04, data.readInt());
                        reply.writeNoException();
                        parcel2.writeIntArray(_result7);
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        startFreezingScreen(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        stopFreezingScreen();
                        reply.writeNoException();
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        disableKeyguard(data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        reenableKeyguard(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        exitKeyguardSecurely(android.view.IOnKeyguardExitResult.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        _result2 = isKeyguardLocked();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        _result2 = isKeyguardSecure();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 42:
                        CharSequence _arg13;
                        parcel.enforceInterface(descriptor);
                        IKeyguardDismissCallback _arg08 = com.android.internal.policy.IKeyguardDismissCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                        }
                        dismissKeyguard(_arg08, _arg13);
                        reply.writeNoException();
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setSwitchingUser(_arg3);
                        reply.writeNoException();
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        closeSystemDialogs(data.readString());
                        reply.writeNoException();
                        return true;
                    case 45:
                        parcel.enforceInterface(descriptor);
                        float _arg09 = data.readFloat();
                        float _arg14 = data.readFloat();
                        int _arg22 = data.readInt();
                        int _arg32 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg05 = (Bitmap) Bitmap.CREATOR.createFromParcel(parcel);
                        }
                        setExitInfo(_arg09, _arg14, _arg22, _arg32, _arg05, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        float _result8 = getAnimationScale(data.readInt());
                        reply.writeNoException();
                        parcel2.writeFloat(_result8);
                        return true;
                    case 47:
                        parcel.enforceInterface(descriptor);
                        float[] _result9 = getAnimationScales();
                        reply.writeNoException();
                        parcel2.writeFloatArray(_result9);
                        return true;
                    case 48:
                        parcel.enforceInterface(descriptor);
                        setAnimationScale(data.readInt(), data.readFloat());
                        reply.writeNoException();
                        return true;
                    case 49:
                        parcel.enforceInterface(descriptor);
                        setAnimationScales(data.createFloatArray());
                        reply.writeNoException();
                        return true;
                    case 50:
                        parcel.enforceInterface(descriptor);
                        float _result10 = getCurrentAnimatorScale();
                        reply.writeNoException();
                        parcel2.writeFloat(_result10);
                        return true;
                    case 51:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setInTouchMode(_arg3);
                        reply.writeNoException();
                        return true;
                    case 52:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        showStrictModeViolation(_arg3);
                        reply.writeNoException();
                        return true;
                    case 53:
                        parcel.enforceInterface(descriptor);
                        setStrictModeVisualIndicatorPreference(data.readString());
                        reply.writeNoException();
                        return true;
                    case 54:
                        parcel.enforceInterface(descriptor);
                        refreshScreenCaptureDisabled(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 55:
                        parcel.enforceInterface(descriptor);
                        _result2 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        updateRotation(_result2, _arg3);
                        reply.writeNoException();
                        return true;
                    case 56:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getDefaultDisplayRotation();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 57:
                        parcel.enforceInterface(descriptor);
                        _result4 = watchRotation(android.view.IRotationWatcher.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 58:
                        parcel.enforceInterface(descriptor);
                        removeRotationWatcher(android.view.IRotationWatcher.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 59:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getPreferredOptionsPanelGravity();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 60:
                        parcel.enforceInterface(descriptor);
                        freezeRotation(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 61:
                        parcel.enforceInterface(descriptor);
                        thawRotation();
                        reply.writeNoException();
                        return true;
                    case 62:
                        parcel.enforceInterface(descriptor);
                        _result2 = isRotationFrozen();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 63:
                        parcel.enforceInterface(descriptor);
                        Bitmap _result11 = screenshotWallpaper();
                        reply.writeNoException();
                        if (_result11 != null) {
                            parcel2.writeInt(1);
                            _result11.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 64:
                        parcel.enforceInterface(descriptor);
                        _arg3 = registerWallpaperVisibilityListener(android.view.IWallpaperVisibilityListener.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg3);
                        return true;
                    case 65:
                        parcel.enforceInterface(descriptor);
                        unregisterWallpaperVisibilityListener(android.view.IWallpaperVisibilityListener.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 66:
                        parcel.enforceInterface(descriptor);
                        _result = requestAssistScreenshot(android.app.IAssistDataReceiver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 67:
                        parcel.enforceInterface(descriptor);
                        statusBarVisibilityChanged(data.readInt());
                        return true;
                    case 68:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setRecentsVisibility(_arg3);
                        return true;
                    case 69:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setPipVisibility(_arg3);
                        return true;
                    case 70:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setShelfHeight(_arg3, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 71:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setNavBarVirtualKeyHapticFeedbackEnabled(_arg3);
                        reply.writeNoException();
                        return true;
                    case 72:
                        parcel.enforceInterface(descriptor);
                        _result2 = hasNavigationBar();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 73:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getNavBarPosition();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 74:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg05 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        lockNow(_arg05);
                        reply.writeNoException();
                        return true;
                    case 75:
                        parcel.enforceInterface(descriptor);
                        _result2 = isSafeModeEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 76:
                        parcel.enforceInterface(descriptor);
                        enableScreenIfNeeded();
                        reply.writeNoException();
                        return true;
                    case 77:
                        parcel.enforceInterface(descriptor);
                        _result = clearWindowContentFrameStats(data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 78:
                        parcel.enforceInterface(descriptor);
                        WindowContentFrameStats _result12 = getWindowContentFrameStats(data.readStrongBinder());
                        reply.writeNoException();
                        if (_result12 != null) {
                            parcel2.writeInt(1);
                            _result12.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 79:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getDockedStackSide();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 80:
                        Rect _arg010;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg010 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        }
                        setDockedStackDividerTouchRegion(_arg010);
                        reply.writeNoException();
                        return true;
                    case 81:
                        parcel.enforceInterface(descriptor);
                        registerDockedStackListener(android.view.IDockedStackListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 82:
                        parcel.enforceInterface(descriptor);
                        registerPinnedStackListener(data.readInt(), android.view.IPinnedStackListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 83:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg3 = true;
                        }
                        setResizeDimLayer(_arg3, data.readInt(), data.readFloat());
                        reply.writeNoException();
                        return true;
                    case 84:
                        parcel.enforceInterface(descriptor);
                        requestAppKeyboardShortcuts(com.android.internal.os.IResultReceiver.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 85:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        Rect _arg15 = new Rect();
                        getStableInsets(_arg02, _arg15);
                        reply.writeNoException();
                        parcel2.writeInt(1);
                        _arg15.writeToParcel(parcel2, 1);
                        return true;
                    case 86:
                        parcel.enforceInterface(descriptor);
                        registerShortcutKey(data.readLong(), com.android.internal.policy.IShortcutService.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 87:
                        parcel.enforceInterface(descriptor);
                        setForcedDisplayDensityAndSize(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 88:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readStrongBinder();
                        String _arg16 = data.readString();
                        InputChannel _arg23 = new InputChannel();
                        createInputConsumer(_arg0, _arg16, _arg23);
                        reply.writeNoException();
                        parcel2.writeInt(1);
                        _arg23.writeToParcel(parcel2, 1);
                        return true;
                    case 89:
                        parcel.enforceInterface(descriptor);
                        _result = destroyInputConsumer(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 90:
                        parcel.enforceInterface(descriptor);
                        Region _result13 = getCurrentImeTouchRegion();
                        reply.writeNoException();
                        if (_result13 != null) {
                            parcel2.writeInt(1);
                            _result13.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 91:
                        parcel.enforceInterface(descriptor);
                        startWindowTrace();
                        reply.writeNoException();
                        return true;
                    case 92:
                        parcel.enforceInterface(descriptor);
                        stopWindowTrace();
                        reply.writeNoException();
                        return true;
                    case 93:
                        parcel.enforceInterface(descriptor);
                        _result2 = isWindowTraceEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 94:
                        parcel.enforceInterface(descriptor);
                        requestUserActivityNotification();
                        reply.writeNoException();
                        return true;
                    case 95:
                        parcel.enforceInterface(descriptor);
                        dontOverrideDisplayInfo(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 96:
                        parcel.enforceInterface(descriptor);
                        _result4 = getLayerIndex(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 97:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getHwInnerService();
                        reply.writeNoException();
                        parcel2.writeStrongBinder(_arg0);
                        return true;
                    case 98:
                        parcel.enforceInterface(descriptor);
                        String _result14 = getFocusedAppComponentName();
                        reply.writeNoException();
                        parcel2.writeString(_result14);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void addWindowToken(IBinder iBinder, int i, int i2) throws RemoteException;

    void clearForcedDisplayDensityForUser(int i, int i2) throws RemoteException;

    void clearForcedDisplaySize(int i) throws RemoteException;

    boolean clearWindowContentFrameStats(IBinder iBinder) throws RemoteException;

    void closeSystemDialogs(String str) throws RemoteException;

    void createInputConsumer(IBinder iBinder, String str, InputChannel inputChannel) throws RemoteException;

    boolean destroyInputConsumer(String str) throws RemoteException;

    void disableKeyguard(IBinder iBinder, String str) throws RemoteException;

    void dismissKeyguard(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) throws RemoteException;

    void dontOverrideDisplayInfo(int i) throws RemoteException;

    void enableScreenIfNeeded() throws RemoteException;

    void endProlongedAnimations() throws RemoteException;

    void executeAppTransition() throws RemoteException;

    void exitKeyguardSecurely(IOnKeyguardExitResult iOnKeyguardExitResult) throws RemoteException;

    void freezeRotation(int i) throws RemoteException;

    float getAnimationScale(int i) throws RemoteException;

    float[] getAnimationScales() throws RemoteException;

    int getBaseDisplayDensity(int i) throws RemoteException;

    void getBaseDisplaySize(int i, Point point) throws RemoteException;

    float getCurrentAnimatorScale() throws RemoteException;

    Region getCurrentImeTouchRegion() throws RemoteException;

    int getDefaultDisplayRotation() throws RemoteException;

    int getDockedStackSide() throws RemoteException;

    String getFocusedAppComponentName() throws RemoteException;

    IBinder getHwInnerService() throws RemoteException;

    int getInitialDisplayDensity(int i) throws RemoteException;

    void getInitialDisplaySize(int i, Point point) throws RemoteException;

    int getLayerIndex(String str, int i) throws RemoteException;

    int getNavBarPosition() throws RemoteException;

    int getPendingAppTransition() throws RemoteException;

    int getPreferredOptionsPanelGravity() throws RemoteException;

    void getStableInsets(int i, Rect rect) throws RemoteException;

    WindowContentFrameStats getWindowContentFrameStats(IBinder iBinder) throws RemoteException;

    boolean hasNavigationBar() throws RemoteException;

    boolean inputMethodClientHasFocus(IInputMethodClient iInputMethodClient) throws RemoteException;

    boolean isKeyguardLocked() throws RemoteException;

    boolean isKeyguardSecure() throws RemoteException;

    boolean isRotationFrozen() throws RemoteException;

    boolean isSafeModeEnabled() throws RemoteException;

    boolean isViewServerRunning() throws RemoteException;

    boolean isWindowTraceEnabled() throws RemoteException;

    void lockNow(Bundle bundle) throws RemoteException;

    IWindowSession openSession(IWindowSessionCallback iWindowSessionCallback, IInputMethodClient iInputMethodClient, IInputContext iInputContext) throws RemoteException;

    void overridePendingAppTransition(String str, int i, int i2, IRemoteCallback iRemoteCallback) throws RemoteException;

    void overridePendingAppTransitionAspectScaledThumb(GraphicBuffer graphicBuffer, int i, int i2, int i3, int i4, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException;

    void overridePendingAppTransitionClipReveal(int i, int i2, int i3, int i4) throws RemoteException;

    void overridePendingAppTransitionInPlace(String str, int i) throws RemoteException;

    void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] appTransitionAnimationSpecArr, IRemoteCallback iRemoteCallback, IRemoteCallback iRemoteCallback2, boolean z) throws RemoteException;

    void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException;

    void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) throws RemoteException;

    void overridePendingAppTransitionScaleUp(int i, int i2, int i3, int i4) throws RemoteException;

    void overridePendingAppTransitionThumb(GraphicBuffer graphicBuffer, int i, int i2, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException;

    void prepareAppTransition(int i, boolean z) throws RemoteException;

    void reenableKeyguard(IBinder iBinder) throws RemoteException;

    void refreshScreenCaptureDisabled(int i) throws RemoteException;

    void registerDockedStackListener(IDockedStackListener iDockedStackListener) throws RemoteException;

    void registerPinnedStackListener(int i, IPinnedStackListener iPinnedStackListener) throws RemoteException;

    void registerShortcutKey(long j, IShortcutService iShortcutService) throws RemoteException;

    boolean registerWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) throws RemoteException;

    void removeRotationWatcher(IRotationWatcher iRotationWatcher) throws RemoteException;

    void removeWindowToken(IBinder iBinder, int i) throws RemoteException;

    void requestAppKeyboardShortcuts(IResultReceiver iResultReceiver, int i) throws RemoteException;

    boolean requestAssistScreenshot(IAssistDataReceiver iAssistDataReceiver) throws RemoteException;

    void requestUserActivityNotification() throws RemoteException;

    Bitmap screenshotWallpaper() throws RemoteException;

    void setAnimationScale(int i, float f) throws RemoteException;

    void setAnimationScales(float[] fArr) throws RemoteException;

    void setDockedStackDividerTouchRegion(Rect rect) throws RemoteException;

    void setEventDispatching(boolean z) throws RemoteException;

    void setExitInfo(float f, float f2, int i, int i2, Bitmap bitmap, int i3) throws RemoteException;

    void setFocusedApp(IBinder iBinder, boolean z) throws RemoteException;

    void setForcedDisplayDensityAndSize(int i, int i2, int i3, int i4) throws RemoteException;

    void setForcedDisplayDensityForUser(int i, int i2, int i3) throws RemoteException;

    void setForcedDisplayScalingMode(int i, int i2) throws RemoteException;

    void setForcedDisplaySize(int i, int i2, int i3) throws RemoteException;

    void setInTouchMode(boolean z) throws RemoteException;

    void setNavBarVirtualKeyHapticFeedbackEnabled(boolean z) throws RemoteException;

    int[] setNewDisplayOverrideConfiguration(Configuration configuration, int i) throws RemoteException;

    void setOverscan(int i, int i2, int i3, int i4, int i5) throws RemoteException;

    void setPipVisibility(boolean z) throws RemoteException;

    void setRecentsVisibility(boolean z) throws RemoteException;

    void setResizeDimLayer(boolean z, int i, float f) throws RemoteException;

    void setShelfHeight(boolean z, int i) throws RemoteException;

    void setStrictModeVisualIndicatorPreference(String str) throws RemoteException;

    void setSwitchingUser(boolean z) throws RemoteException;

    void showStrictModeViolation(boolean z) throws RemoteException;

    void startFreezingScreen(int i, int i2) throws RemoteException;

    boolean startViewServer(int i) throws RemoteException;

    void startWindowTrace() throws RemoteException;

    void statusBarVisibilityChanged(int i) throws RemoteException;

    void stopFreezingScreen() throws RemoteException;

    boolean stopViewServer() throws RemoteException;

    void stopWindowTrace() throws RemoteException;

    void thawRotation() throws RemoteException;

    void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) throws RemoteException;

    Configuration updateOrientationFromAppTokens(Configuration configuration, IBinder iBinder, int i) throws RemoteException;

    void updateRotation(boolean z, boolean z2) throws RemoteException;

    int watchRotation(IRotationWatcher iRotationWatcher, int i) throws RemoteException;
}
