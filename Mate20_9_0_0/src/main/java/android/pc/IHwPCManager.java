package android.pc;

import android.app.HwRecentTaskInfo;
import android.app.ITaskStackListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.PointerIcon;
import java.util.List;

public interface IHwPCManager extends IInterface {

    public static abstract class Stub extends Binder implements IHwPCManager {
        private static final String DESCRIPTOR = "android.pc.IHwPCManager";
        static final int TRANSACTION_LaunchMKForWifiMode = 42;
        static final int TRANSACTION_closeTopWindow = 19;
        static final int TRANSACTION_dispatchKeyEventForExclusiveKeyboard = 32;
        static final int TRANSACTION_execVoiceCmd = 39;
        static final int TRANSACTION_forceDisplayMode = 24;
        static final int TRANSACTION_getAllSupportPcAppList = 3;
        static final int TRANSACTION_getCastMode = 1;
        static final int TRANSACTION_getDisplayBitmap = 14;
        static final int TRANSACTION_getHwRecentTaskInfo = 11;
        static final int TRANSACTION_getMutiResumeAppList = 40;
        static final int TRANSACTION_getPCDisplayId = 38;
        static final int TRANSACTION_getPackageSupportPcState = 2;
        static final int TRANSACTION_getPointerCoordinateAxis = 23;
        static final int TRANSACTION_getTaskThumbnailEx = 26;
        static final int TRANSACTION_getWindowState = 10;
        static final int TRANSACTION_hideImeStatusIcon = 34;
        static final int TRANSACTION_hwResizeTask = 9;
        static final int TRANSACTION_hwRestoreTask = 8;
        static final int TRANSACTION_injectInputEventExternal = 22;
        static final int TRANSACTION_isPackageRunningOnPCMode = 29;
        static final int TRANSACTION_isScreenPowerOn = 30;
        static final int TRANSACTION_lockScreen = 28;
        static final int TRANSACTION_notifyDpState = 37;
        static final int TRANSACTION_registHwSystemUIController = 15;
        static final int TRANSACTION_registerHwTaskStackListener = 12;
        static final int TRANSACTION_relaunchIMEIfNecessary = 7;
        static final int TRANSACTION_saveAppIntent = 25;
        static final int TRANSACTION_scheduleDisplayAdded = 4;
        static final int TRANSACTION_scheduleDisplayChanged = 5;
        static final int TRANSACTION_scheduleDisplayRemoved = 6;
        static final int TRANSACTION_screenshotPc = 18;
        static final int TRANSACTION_setCustomPointerIcon = 35;
        static final int TRANSACTION_setPointerIconType = 36;
        static final int TRANSACTION_setScreenPower = 31;
        static final int TRANSACTION_shouldInterceptInputEvent = 41;
        static final int TRANSACTION_showImeStatusIcon = 33;
        static final int TRANSACTION_showStartMenu = 17;
        static final int TRANSACTION_showTopBar = 16;
        static final int TRANSACTION_toggleHome = 21;
        static final int TRANSACTION_triggerSwitchTaskView = 20;
        static final int TRANSACTION_unRegisterHwTaskStackListener = 13;
        static final int TRANSACTION_userActivityOnDesktop = 27;

        private static class Proxy implements IHwPCManager {
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

            public boolean getCastMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public int getPackageSupportPcState(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getAllSupportPcAppList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void scheduleDisplayAdded(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void scheduleDisplayChanged(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void scheduleDisplayRemoved(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void relaunchIMEIfNecessary() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void hwRestoreTask(int taskId, float x, float y) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeFloat(x);
                    _data.writeFloat(y);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void hwResizeTask(int taskId, Rect bounds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(taskId);
                    if (bounds != null) {
                        _data.writeInt(1);
                        bounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getWindowState(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public HwRecentTaskInfo getHwRecentTaskInfo(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwRecentTaskInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (HwRecentTaskInfo) HwRecentTaskInfo.CREATOR.createFromParcel(_reply);
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

            public void registerHwTaskStackListener(ITaskStackListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unRegisterHwTaskStackListener(ITaskStackListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bitmap getDisplayBitmap(int displayId, int width, int height) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bitmap _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    this.mRemote.transact(14, _data, _reply, 0);
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

            public void registHwSystemUIController(Messenger messenger) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showTopBar() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showStartMenu() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void screenshotPc() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void closeTopWindow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void triggerSwitchTaskView(boolean show) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(show);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void toggleHome() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean injectInputEventExternal(InputEvent ev, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (ev != null) {
                        _data.writeInt(1);
                        ev.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(mode);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public float[] getPointerCoordinateAxis() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    float[] _result = _reply.createFloatArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int forceDisplayMode(int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void saveAppIntent(List<Intent> intents) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(intents);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bitmap getTaskThumbnailEx(int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bitmap _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    this.mRemote.transact(26, _data, _reply, 0);
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

            public void userActivityOnDesktop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void lockScreen(boolean lock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(lock);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isPackageRunningOnPCMode(String packageName, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(uid);
                    boolean z = false;
                    this.mRemote.transact(29, _data, _reply, 0);
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

            public boolean isScreenPowerOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(30, _data, _reply, 0);
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

            public void setScreenPower(boolean powerOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(powerOn);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dispatchKeyEventForExclusiveKeyboard(KeyEvent ke) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ke != null) {
                        _data.writeInt(1);
                        ke.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void showImeStatusIcon(int iconResId, String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(iconResId);
                    _data.writeString(pkgName);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void hideImeStatusIcon(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCustomPointerIcon(PointerIcon icon, boolean keep) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (icon != null) {
                        _data.writeInt(1);
                        icon.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(keep);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPointerIconType(int iconId, boolean keep) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(iconId);
                    _data.writeInt(keep);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyDpState(boolean dpState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(dpState);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPCDisplayId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void execVoiceCmd(Message message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (message != null) {
                        _data.writeInt(1);
                        message.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(39, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public List<String> getMutiResumeAppList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean shouldInterceptInputEvent(KeyEvent ev, boolean forScroll) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (ev != null) {
                        _data.writeInt(1);
                        ev.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(forScroll);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void LaunchMKForWifiMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IHwPCManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IHwPCManager)) {
                return new Proxy(obj);
            }
            return (IHwPCManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                Rect _arg0 = null;
                boolean _arg1 = false;
                boolean _result;
                int _result2;
                List<String> _result3;
                boolean _result4;
                KeyEvent _arg02;
                int _arg03;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _result = getCastMode();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _result2 = getPackageSupportPcState(data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _result3 = getAllSupportPcAppList();
                        reply.writeNoException();
                        reply.writeStringList(_result3);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        scheduleDisplayAdded(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        scheduleDisplayChanged(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        scheduleDisplayRemoved(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        relaunchIMEIfNecessary();
                        reply.writeNoException();
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        hwRestoreTask(data.readInt(), data.readFloat(), data.readFloat());
                        reply.writeNoException();
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        _result2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg0 = (Rect) Rect.CREATOR.createFromParcel(data);
                        }
                        hwResizeTask(_result2, _arg0);
                        reply.writeNoException();
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        _result2 = getWindowState(data.readStrongBinder());
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        HwRecentTaskInfo _result5 = getHwRecentTaskInfo(data.readInt());
                        reply.writeNoException();
                        if (_result5 != null) {
                            reply.writeInt(1);
                            _result5.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        registerHwTaskStackListener(android.app.ITaskStackListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        unRegisterHwTaskStackListener(android.app.ITaskStackListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        Bitmap _result6 = getDisplayBitmap(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        if (_result6 != null) {
                            reply.writeInt(1);
                            _result6.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 15:
                        Messenger _arg04;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (Messenger) Messenger.CREATOR.createFromParcel(data);
                        }
                        registHwSystemUIController(_arg04);
                        reply.writeNoException();
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        showTopBar();
                        reply.writeNoException();
                        return true;
                    case 17:
                        data.enforceInterface(descriptor);
                        showStartMenu();
                        reply.writeNoException();
                        return true;
                    case 18:
                        data.enforceInterface(descriptor);
                        screenshotPc();
                        reply.writeNoException();
                        return true;
                    case 19:
                        data.enforceInterface(descriptor);
                        closeTopWindow();
                        reply.writeNoException();
                        return true;
                    case 20:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        triggerSwitchTaskView(_arg1);
                        reply.writeNoException();
                        return true;
                    case 21:
                        data.enforceInterface(descriptor);
                        toggleHome();
                        reply.writeNoException();
                        return true;
                    case 22:
                        InputEvent _arg05;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg05 = (InputEvent) InputEvent.CREATOR.createFromParcel(data);
                        }
                        _result4 = injectInputEventExternal(_arg05, data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 23:
                        data.enforceInterface(descriptor);
                        float[] _result7 = getPointerCoordinateAxis();
                        reply.writeNoException();
                        reply.writeFloatArray(_result7);
                        return true;
                    case 24:
                        data.enforceInterface(descriptor);
                        _result2 = forceDisplayMode(data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 25:
                        data.enforceInterface(descriptor);
                        saveAppIntent(data.createTypedArrayList(Intent.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 26:
                        data.enforceInterface(descriptor);
                        Bitmap _result8 = getTaskThumbnailEx(data.readInt());
                        reply.writeNoException();
                        if (_result8 != null) {
                            reply.writeInt(1);
                            _result8.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 27:
                        data.enforceInterface(descriptor);
                        userActivityOnDesktop();
                        reply.writeNoException();
                        return true;
                    case 28:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        lockScreen(_arg1);
                        reply.writeNoException();
                        return true;
                    case 29:
                        data.enforceInterface(descriptor);
                        _result4 = isPackageRunningOnPCMode(data.readString(), data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 30:
                        data.enforceInterface(descriptor);
                        _result = isScreenPowerOn();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 31:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setScreenPower(_arg1);
                        reply.writeNoException();
                        return true;
                    case 32:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (KeyEvent) KeyEvent.CREATOR.createFromParcel(data);
                        }
                        dispatchKeyEventForExclusiveKeyboard(_arg02);
                        reply.writeNoException();
                        return true;
                    case 33:
                        data.enforceInterface(descriptor);
                        showImeStatusIcon(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 34:
                        data.enforceInterface(descriptor);
                        hideImeStatusIcon(data.readString());
                        reply.writeNoException();
                        return true;
                    case 35:
                        PointerIcon _arg06;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (PointerIcon) PointerIcon.CREATOR.createFromParcel(data);
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setCustomPointerIcon(_arg06, _arg1);
                        reply.writeNoException();
                        return true;
                    case 36:
                        data.enforceInterface(descriptor);
                        _arg03 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setPointerIconType(_arg03, _arg1);
                        reply.writeNoException();
                        return true;
                    case 37:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        notifyDpState(_arg1);
                        reply.writeNoException();
                        return true;
                    case 38:
                        data.enforceInterface(descriptor);
                        _arg03 = getPCDisplayId();
                        reply.writeNoException();
                        reply.writeInt(_arg03);
                        return true;
                    case 39:
                        Message _arg07;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg07 = (Message) Message.CREATOR.createFromParcel(data);
                        }
                        execVoiceCmd(_arg07);
                        return true;
                    case 40:
                        data.enforceInterface(descriptor);
                        _result3 = getMutiResumeAppList();
                        reply.writeNoException();
                        reply.writeStringList(_result3);
                        return true;
                    case 41:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (KeyEvent) KeyEvent.CREATOR.createFromParcel(data);
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result4 = shouldInterceptInputEvent(_arg02, _arg1);
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 42:
                        data.enforceInterface(descriptor);
                        LaunchMKForWifiMode();
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void LaunchMKForWifiMode() throws RemoteException;

    void closeTopWindow() throws RemoteException;

    void dispatchKeyEventForExclusiveKeyboard(KeyEvent keyEvent) throws RemoteException;

    void execVoiceCmd(Message message) throws RemoteException;

    int forceDisplayMode(int i) throws RemoteException;

    List<String> getAllSupportPcAppList() throws RemoteException;

    boolean getCastMode() throws RemoteException;

    Bitmap getDisplayBitmap(int i, int i2, int i3) throws RemoteException;

    HwRecentTaskInfo getHwRecentTaskInfo(int i) throws RemoteException;

    List<String> getMutiResumeAppList() throws RemoteException;

    int getPCDisplayId() throws RemoteException;

    int getPackageSupportPcState(String str) throws RemoteException;

    float[] getPointerCoordinateAxis() throws RemoteException;

    Bitmap getTaskThumbnailEx(int i) throws RemoteException;

    int getWindowState(IBinder iBinder) throws RemoteException;

    void hideImeStatusIcon(String str) throws RemoteException;

    void hwResizeTask(int i, Rect rect) throws RemoteException;

    void hwRestoreTask(int i, float f, float f2) throws RemoteException;

    boolean injectInputEventExternal(InputEvent inputEvent, int i) throws RemoteException;

    boolean isPackageRunningOnPCMode(String str, int i) throws RemoteException;

    boolean isScreenPowerOn() throws RemoteException;

    void lockScreen(boolean z) throws RemoteException;

    void notifyDpState(boolean z) throws RemoteException;

    void registHwSystemUIController(Messenger messenger) throws RemoteException;

    void registerHwTaskStackListener(ITaskStackListener iTaskStackListener) throws RemoteException;

    void relaunchIMEIfNecessary() throws RemoteException;

    void saveAppIntent(List<Intent> list) throws RemoteException;

    void scheduleDisplayAdded(int i) throws RemoteException;

    void scheduleDisplayChanged(int i) throws RemoteException;

    void scheduleDisplayRemoved(int i) throws RemoteException;

    void screenshotPc() throws RemoteException;

    void setCustomPointerIcon(PointerIcon pointerIcon, boolean z) throws RemoteException;

    void setPointerIconType(int i, boolean z) throws RemoteException;

    void setScreenPower(boolean z) throws RemoteException;

    boolean shouldInterceptInputEvent(KeyEvent keyEvent, boolean z) throws RemoteException;

    void showImeStatusIcon(int i, String str) throws RemoteException;

    void showStartMenu() throws RemoteException;

    void showTopBar() throws RemoteException;

    void toggleHome() throws RemoteException;

    void triggerSwitchTaskView(boolean z) throws RemoteException;

    void unRegisterHwTaskStackListener(ITaskStackListener iTaskStackListener) throws RemoteException;

    void userActivityOnDesktop() throws RemoteException;
}
