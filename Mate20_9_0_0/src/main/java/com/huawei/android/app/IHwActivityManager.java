package com.huawei.android.app;

import android.app.IHwActivityNotifier;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IMWThirdpartyCallback;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.server.mtm.taskstatus.ProcessInfo;
import java.util.List;
import java.util.Map;

public interface IHwActivityManager extends IInterface {

    public static abstract class Stub extends Binder implements IHwActivityManager {
        private static final String DESCRIPTOR = "com.huawei.android.app.IHwActivityManager";
        static final int TRANSACTION_cleanPackageRes = 4;
        static final int TRANSACTION_getLastResumedActivity = 25;
        static final int TRANSACTION_getPCTopTaskBounds = 29;
        static final int TRANSACTION_getPidWithUiFromUid = 14;
        static final int TRANSACTION_getProcessRecordFromMTM = 21;
        static final int TRANSACTION_getTopTaskIdInDisplay = 27;
        static final int TRANSACTION_handleANRFilterFIFO = 11;
        static final int TRANSACTION_handleShowAppEyeAnrUi = 12;
        static final int TRANSACTION_isFreeFormVisible = 8;
        static final int TRANSACTION_isProcessExistLocked = 19;
        static final int TRANSACTION_isProcessExistPidsSelfLocked = 26;
        static final int TRANSACTION_isTaskSupportResize = 28;
        static final int TRANSACTION_isTaskVisible = 13;
        static final int TRANSACTION_killProcessRecordFromIAware = 16;
        static final int TRANSACTION_killProcessRecordFromIAwareNative = 17;
        static final int TRANSACTION_killProcessRecordFromMTM = 18;
        static final int TRANSACTION_preloadApplication = 15;
        static final int TRANSACTION_registerDAMonitorCallback = 1;
        static final int TRANSACTION_registerHwActivityNotifier = 9;
        static final int TRANSACTION_registerThirdPartyCallBack = 5;
        static final int TRANSACTION_removePackageAlarm = 20;
        static final int TRANSACTION_reportAssocDisable = 24;
        static final int TRANSACTION_reportProcessDied = 23;
        static final int TRANSACTION_reportScreenRecord = 7;
        static final int TRANSACTION_setAndRestoreMaxAdjIfNeed = 22;
        static final int TRANSACTION_setCpusetSwitch = 2;
        static final int TRANSACTION_setWarmColdSwitch = 3;
        static final int TRANSACTION_unregisterHwActivityNotifier = 10;
        static final int TRANSACTION_unregisterThirdPartyCallBack = 6;

        private static class Proxy implements IHwActivityManager {
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

            public void registerDAMonitorCallback(IHwDAMonitorCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCpusetSwitch(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWarmColdSwitch(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean cleanPackageRes(List<String> packageList, Map alarmTags, int targetUid, boolean cleanAlarm, boolean isNative, boolean hasPerceptAlarm) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(packageList);
                    _data.writeMap(alarmTags);
                    _data.writeInt(targetUid);
                    _data.writeInt(cleanAlarm);
                    _data.writeInt(isNative);
                    _data.writeInt(hasPerceptAlarm);
                    boolean z = false;
                    this.mRemote.transact(4, _data, _reply, 0);
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

            public boolean registerThirdPartyCallBack(IMWThirdpartyCallback aCallBackHandler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(aCallBackHandler != null ? aCallBackHandler.asBinder() : null);
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

            public boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback aCallBackHandler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(aCallBackHandler != null ? aCallBackHandler.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(6, _data, _reply, 0);
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

            public void reportScreenRecord(int uid, int pid, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(pid);
                    _data.writeInt(status);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isFreeFormVisible() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(8, _data, _reply, 0);
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

            public void registerHwActivityNotifier(IHwActivityNotifier notifier, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(notifier != null ? notifier.asBinder() : null);
                    _data.writeString(reason);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterHwActivityNotifier(IHwActivityNotifier notifier) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(notifier != null ? notifier.asBinder() : null);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean handleANRFilterFIFO(int uid, int cmd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(cmd);
                    boolean z = false;
                    this.mRemote.transact(11, _data, _reply, 0);
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

            public void handleShowAppEyeAnrUi(int pid, int uid, String processName, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeString(processName);
                    _data.writeString(packageName);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isTaskVisible(int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    boolean z = false;
                    this.mRemote.transact(13, _data, _reply, 0);
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

            public List<String> getPidWithUiFromUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int preloadApplication(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean killProcessRecordFromIAware(ProcessInfo procInfo, boolean restartservice, boolean isAsynchronous, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (procInfo != null) {
                        _data.writeInt(1);
                        procInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(restartservice);
                    _data.writeInt(isAsynchronous);
                    _data.writeString(reason);
                    this.mRemote.transact(16, _data, _reply, 0);
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

            public boolean killProcessRecordFromIAwareNative(ProcessInfo procInfo, boolean restartservice, boolean isAsynchronous, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (procInfo != null) {
                        _data.writeInt(1);
                        procInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(restartservice);
                    _data.writeInt(isAsynchronous);
                    _data.writeString(reason);
                    this.mRemote.transact(17, _data, _reply, 0);
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

            public boolean killProcessRecordFromMTM(ProcessInfo procInfo, boolean restartservice, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (procInfo != null) {
                        _data.writeInt(1);
                        procInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(restartservice);
                    _data.writeString(reason);
                    this.mRemote.transact(18, _data, _reply, 0);
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

            public boolean isProcessExistLocked(String processName, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(processName);
                    _data.writeInt(uid);
                    boolean z = false;
                    this.mRemote.transact(19, _data, _reply, 0);
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

            public void removePackageAlarm(String pkg, List<String> tags, int targetUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStringList(tags);
                    _data.writeInt(targetUid);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getProcessRecordFromMTM(ProcessInfo procInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (procInfo != null) {
                        _data.writeInt(1);
                        procInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, _reply, 0);
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

            public void setAndRestoreMaxAdjIfNeed(List<String> adjCustPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(adjCustPkg);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportProcessDied(int pid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportAssocDisable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ActivityInfo getLastResumedActivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ActivityInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(_reply);
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

            public boolean isProcessExistPidsSelfLocked(String processName, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(processName);
                    _data.writeInt(uid);
                    boolean z = false;
                    this.mRemote.transact(26, _data, _reply, 0);
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

            public int getTopTaskIdInDisplay(int displayId, String pkgName, boolean invisibleAlso) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeString(pkgName);
                    _data.writeInt(invisibleAlso);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isTaskSupportResize(int taskId, boolean isFullscreen, boolean isMaximized) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeInt(isFullscreen);
                    _data.writeInt(isMaximized);
                    boolean z = false;
                    this.mRemote.transact(28, _data, _reply, 0);
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

            public Rect getPCTopTaskBounds(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Rect _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Rect) Rect.CREATOR.createFromParcel(_reply);
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
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IHwActivityManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IHwActivityManager)) {
                return new Proxy(obj);
            }
            return (IHwActivityManager) iin;
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
                ProcessInfo _arg0 = null;
                boolean _arg2 = false;
                boolean _result;
                boolean _result2;
                boolean _result3;
                boolean _result4;
                int _arg02;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        registerDAMonitorCallback(com.huawei.android.app.IHwDAMonitorCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setCpusetSwitch(_arg2);
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setWarmColdSwitch(_arg2);
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _result = cleanPackageRes(data.createStringArrayList(), parcel.readHashMap(getClass().getClassLoader()), data.readInt(), data.readInt() != 0, data.readInt() != 0, data.readInt() != 0);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _arg2 = registerThirdPartyCallBack(android.os.IMWThirdpartyCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg2);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _arg2 = unregisterThirdPartyCallBack(android.os.IMWThirdpartyCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg2);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        reportScreenRecord(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _result = isFreeFormVisible();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        registerHwActivityNotifier(android.app.IHwActivityNotifier.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        unregisterHwActivityNotifier(android.app.IHwActivityNotifier.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result2 = handleANRFilterFIFO(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        handleShowAppEyeAnrUi(data.readInt(), data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _arg2 = isTaskVisible(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg2);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        List<String> _result5 = getPidWithUiFromUid(data.readInt());
                        reply.writeNoException();
                        parcel2.writeStringList(_result5);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        int _result6 = preloadApplication(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ProcessInfo) ProcessInfo.CREATOR.createFromParcel(parcel);
                        }
                        _result2 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        _result3 = killProcessRecordFromIAware(_arg0, _result2, _arg2, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ProcessInfo) ProcessInfo.CREATOR.createFromParcel(parcel);
                        }
                        _result2 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        _result3 = killProcessRecordFromIAwareNative(_arg0, _result2, _arg2, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ProcessInfo) ProcessInfo.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        _result4 = killProcessRecordFromMTM(_arg0, _arg2, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _result2 = isProcessExistLocked(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        removePackageAlarm(data.readString(), data.createStringArrayList(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ProcessInfo) ProcessInfo.CREATOR.createFromParcel(parcel);
                        }
                        _arg2 = getProcessRecordFromMTM(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_arg2);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        setAndRestoreMaxAdjIfNeed(data.createStringArrayList());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        reportProcessDied(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        reportAssocDisable();
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        ActivityInfo _result7 = getLastResumedActivity();
                        reply.writeNoException();
                        if (_result7 != null) {
                            parcel2.writeInt(1);
                            _result7.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        _result2 = isProcessExistPidsSelfLocked(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        String _arg1 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        int _result8 = getTopTaskIdInDisplay(_arg02, _arg1, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        _result2 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        _result4 = isTaskSupportResize(_arg02, _result2, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        Rect _result9 = getPCTopTaskBounds(data.readInt());
                        reply.writeNoException();
                        if (_result9 != null) {
                            parcel2.writeInt(1);
                            _result9.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    boolean cleanPackageRes(List<String> list, Map map, int i, boolean z, boolean z2, boolean z3) throws RemoteException;

    ActivityInfo getLastResumedActivity() throws RemoteException;

    Rect getPCTopTaskBounds(int i) throws RemoteException;

    List<String> getPidWithUiFromUid(int i) throws RemoteException;

    boolean getProcessRecordFromMTM(ProcessInfo processInfo) throws RemoteException;

    int getTopTaskIdInDisplay(int i, String str, boolean z) throws RemoteException;

    boolean handleANRFilterFIFO(int i, int i2) throws RemoteException;

    void handleShowAppEyeAnrUi(int i, int i2, String str, String str2) throws RemoteException;

    boolean isFreeFormVisible() throws RemoteException;

    boolean isProcessExistLocked(String str, int i) throws RemoteException;

    boolean isProcessExistPidsSelfLocked(String str, int i) throws RemoteException;

    boolean isTaskSupportResize(int i, boolean z, boolean z2) throws RemoteException;

    boolean isTaskVisible(int i) throws RemoteException;

    boolean killProcessRecordFromIAware(ProcessInfo processInfo, boolean z, boolean z2, String str) throws RemoteException;

    boolean killProcessRecordFromIAwareNative(ProcessInfo processInfo, boolean z, boolean z2, String str) throws RemoteException;

    boolean killProcessRecordFromMTM(ProcessInfo processInfo, boolean z, String str) throws RemoteException;

    int preloadApplication(String str, int i) throws RemoteException;

    void registerDAMonitorCallback(IHwDAMonitorCallback iHwDAMonitorCallback) throws RemoteException;

    void registerHwActivityNotifier(IHwActivityNotifier iHwActivityNotifier, String str) throws RemoteException;

    boolean registerThirdPartyCallBack(IMWThirdpartyCallback iMWThirdpartyCallback) throws RemoteException;

    void removePackageAlarm(String str, List<String> list, int i) throws RemoteException;

    void reportAssocDisable() throws RemoteException;

    void reportProcessDied(int i) throws RemoteException;

    void reportScreenRecord(int i, int i2, int i3) throws RemoteException;

    void setAndRestoreMaxAdjIfNeed(List<String> list) throws RemoteException;

    void setCpusetSwitch(boolean z) throws RemoteException;

    void setWarmColdSwitch(boolean z) throws RemoteException;

    void unregisterHwActivityNotifier(IHwActivityNotifier iHwActivityNotifier) throws RemoteException;

    boolean unregisterThirdPartyCallBack(IMWThirdpartyCallback iMWThirdpartyCallback) throws RemoteException;
}
