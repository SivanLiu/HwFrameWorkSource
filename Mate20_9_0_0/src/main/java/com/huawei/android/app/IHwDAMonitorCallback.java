package com.huawei.android.app;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IHwDAMonitorCallback extends IInterface {

    public static abstract class Stub extends Binder implements IHwDAMonitorCallback {
        private static final String DESCRIPTOR = "com.huawei.android.app.IHwDAMonitorCallback";
        static final int TRANSACTION_DAMonitorReport = 6;
        static final int TRANSACTION_addPssToMap = 15;
        static final int TRANSACTION_getActivityImportCount = 1;
        static final int TRANSACTION_getCPUConfigGroupBG = 4;
        static final int TRANSACTION_getFirstDevSchedEventId = 5;
        static final int TRANSACTION_getRecentTask = 2;
        static final int TRANSACTION_isCPUConfigWhiteList = 3;
        static final int TRANSACTION_isExcludedInBGCheck = 28;
        static final int TRANSACTION_isResourceNeeded = 26;
        static final int TRANSACTION_killProcessGroupForQuickKill = 17;
        static final int TRANSACTION_noteActivityDisplayedStart = 29;
        static final int TRANSACTION_noteActivityStart = 14;
        static final int TRANSACTION_noteProcessStart = 18;
        static final int TRANSACTION_notifyActivityState = 7;
        static final int TRANSACTION_notifyAppEventToIaware = 12;
        static final int TRANSACTION_notifyProcessDied = 24;
        static final int TRANSACTION_notifyProcessGroupChange = 21;
        static final int TRANSACTION_notifyProcessGroupChangeCpu = 10;
        static final int TRANSACTION_notifyProcessStatusChange = 22;
        static final int TRANSACTION_notifyProcessWillDie = 23;
        static final int TRANSACTION_onPointerEvent = 13;
        static final int TRANSACTION_onWakefulnessChanged = 19;
        static final int TRANSACTION_recognizeFakeActivity = 20;
        static final int TRANSACTION_reportAppDiedMsg = 16;
        static final int TRANSACTION_reportCamera = 9;
        static final int TRANSACTION_reportData = 27;
        static final int TRANSACTION_reportScreenRecord = 8;
        static final int TRANSACTION_resetAppMngOomAdj = 25;
        static final int TRANSACTION_setVipThread = 11;

        private static class Proxy implements IHwDAMonitorCallback {
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

            public int getActivityImportCount() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getRecentTask() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int isCPUConfigWhiteList(String processName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(processName);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCPUConfigGroupBG() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getFirstDevSchedEventId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int DAMonitorReport(int tag, String msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(tag);
                    _data.writeString(msg);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyActivityState(String activityInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(activityInfo);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
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
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportCamera(int uid, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(status);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyProcessGroupChangeCpu(int pid, int uid, int grp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeInt(grp);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setVipThread(int pid, int renderThreadTid, boolean isSet) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(renderThreadTid);
                    _data.writeInt(isSet);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyAppEventToIaware(int type, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(packageName);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onPointerEvent(int action) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(action);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void noteActivityStart(String packageName, String processName, String activityName, int pid, int uid, boolean started) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(processName);
                    _data.writeString(activityName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeInt(started);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addPssToMap(String procName, int uid, int pid, int procState, long pss, long now, boolean test) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(procName);
                    _data.writeInt(uid);
                    _data.writeInt(pid);
                    _data.writeInt(procState);
                    _data.writeLong(pss);
                    _data.writeLong(now);
                    _data.writeInt(test);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportAppDiedMsg(int userId, String processName, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(processName);
                    _data.writeString(reason);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int killProcessGroupForQuickKill(int uid, int pid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(pid);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void noteProcessStart(String packageName, String processName, int pid, int uid, boolean started, String launcherMode, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(processName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeInt(started);
                    _data.writeString(launcherMode);
                    _data.writeString(reason);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onWakefulnessChanged(int wakefulness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(wakefulness);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void recognizeFakeActivity(String compName, boolean isScreenOn, int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(compName);
                    _data.writeInt(isScreenOn);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyProcessGroupChange(int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyProcessStatusChange(String pkg, String process, String hostingType, int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(process);
                    _data.writeString(hostingType);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyProcessWillDie(boolean byForceStop, boolean crashed, boolean byAnr, String packageName, int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(byForceStop);
                    _data.writeInt(crashed);
                    _data.writeInt(byAnr);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyProcessDied(int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int resetAppMngOomAdj(int maxAdj, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(maxAdj);
                    _data.writeString(packageName);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isResourceNeeded(String resourceid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(resourceid);
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

            public void reportData(String resourceid, long timestamp, Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(resourceid);
                    _data.writeLong(timestamp);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isExcludedInBGCheck(String pkg, String action) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(action);
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

            public void noteActivityDisplayedStart(String componentName, int uid, int pid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(componentName);
                    _data.writeInt(uid);
                    _data.writeInt(pid);
                    this.mRemote.transact(29, _data, _reply, 0);
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

        public static IHwDAMonitorCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IHwDAMonitorCallback)) {
                return new Proxy(obj);
            }
            return (IHwDAMonitorCallback) iin;
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
                boolean _arg1 = false;
                int _result;
                String _result2;
                int _result3;
                int _result4;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        _result = getActivityImportCount();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _result2 = getRecentTask();
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result3 = isCPUConfigWhiteList(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _result = getCPUConfigGroupBG();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _result = getFirstDevSchedEventId();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _result4 = DAMonitorReport(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        notifyActivityState(data.readString());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        reportScreenRecord(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        reportCamera(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        notifyProcessGroupChangeCpu(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result3 = data.readInt();
                        _result4 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setVipThread(_result3, _result4, _arg1);
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        notifyAppEventToIaware(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        onPointerEvent(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        noteActivityStart(data.readString(), data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        addPssToMap(data.readString(), data.readInt(), data.readInt(), data.readInt(), data.readLong(), data.readLong(), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        reportAppDiedMsg(data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _result4 = killProcessGroupForQuickKill(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        noteProcessStart(data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt() != 0, data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        onWakefulnessChanged(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        String _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        recognizeFakeActivity(_arg0, _arg1, data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        notifyProcessGroupChange(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        notifyProcessStatusChange(data.readString(), data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        notifyProcessWillDie(data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        notifyProcessDied(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _result4 = resetAppMngOomAdj(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        boolean _result5 = isResourceNeeded(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 27:
                        Bundle _arg2;
                        parcel.enforceInterface(descriptor);
                        _result2 = data.readString();
                        long _arg12 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg2 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg2 = null;
                        }
                        reportData(_result2, _arg12, _arg2);
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        boolean _result6 = isExcludedInBGCheck(data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        noteActivityDisplayedStart(data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    int DAMonitorReport(int i, String str) throws RemoteException;

    void addPssToMap(String str, int i, int i2, int i3, long j, long j2, boolean z) throws RemoteException;

    int getActivityImportCount() throws RemoteException;

    int getCPUConfigGroupBG() throws RemoteException;

    int getFirstDevSchedEventId() throws RemoteException;

    String getRecentTask() throws RemoteException;

    int isCPUConfigWhiteList(String str) throws RemoteException;

    boolean isExcludedInBGCheck(String str, String str2) throws RemoteException;

    boolean isResourceNeeded(String str) throws RemoteException;

    int killProcessGroupForQuickKill(int i, int i2) throws RemoteException;

    void noteActivityDisplayedStart(String str, int i, int i2) throws RemoteException;

    void noteActivityStart(String str, String str2, String str3, int i, int i2, boolean z) throws RemoteException;

    void noteProcessStart(String str, String str2, int i, int i2, boolean z, String str3, String str4) throws RemoteException;

    void notifyActivityState(String str) throws RemoteException;

    void notifyAppEventToIaware(int i, String str) throws RemoteException;

    void notifyProcessDied(int i, int i2) throws RemoteException;

    void notifyProcessGroupChange(int i, int i2) throws RemoteException;

    void notifyProcessGroupChangeCpu(int i, int i2, int i3) throws RemoteException;

    void notifyProcessStatusChange(String str, String str2, String str3, int i, int i2) throws RemoteException;

    void notifyProcessWillDie(boolean z, boolean z2, boolean z3, String str, int i, int i2) throws RemoteException;

    void onPointerEvent(int i) throws RemoteException;

    void onWakefulnessChanged(int i) throws RemoteException;

    void recognizeFakeActivity(String str, boolean z, int i, int i2) throws RemoteException;

    void reportAppDiedMsg(int i, String str, String str2) throws RemoteException;

    void reportCamera(int i, int i2) throws RemoteException;

    void reportData(String str, long j, Bundle bundle) throws RemoteException;

    void reportScreenRecord(int i, int i2, int i3) throws RemoteException;

    int resetAppMngOomAdj(int i, String str) throws RemoteException;

    void setVipThread(int i, int i2, boolean z) throws RemoteException;
}
