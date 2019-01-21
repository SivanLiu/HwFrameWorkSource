package android.app;

import android.app.AlarmManager.AlarmClockInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.WorkSource;

public interface IAlarmManager extends IInterface {

    public static abstract class Stub extends Binder implements IAlarmManager {
        private static final String DESCRIPTOR = "android.app.IAlarmManager";
        static final int TRANSACTION_adjustHwRTCAlarm = 10;
        static final int TRANSACTION_checkHasHwRTCAlarm = 9;
        static final int TRANSACTION_currentNetworkTimeMillis = 13;
        static final int TRANSACTION_getNextAlarmClock = 6;
        static final int TRANSACTION_getNextWakeFromIdleTime = 5;
        static final int TRANSACTION_getWakeUpNum = 8;
        static final int TRANSACTION_remove = 4;
        static final int TRANSACTION_set = 1;
        static final int TRANSACTION_setHwAirPlaneStateProp = 11;
        static final int TRANSACTION_setHwRTCAlarm = 12;
        static final int TRANSACTION_setTime = 2;
        static final int TRANSACTION_setTimeZone = 3;
        static final int TRANSACTION_updateBlockedUids = 7;

        private static class Proxy implements IAlarmManager {
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

            public void set(String callingPackage, int type, long triggerAtTime, long windowLength, long interval, int flags, PendingIntent operation, IAlarmListener listener, String listenerTag, WorkSource workSource, AlarmClockInfo alarmClock) throws RemoteException {
                Throwable th;
                long j;
                long j2;
                long j3;
                int i;
                String str;
                int i2;
                PendingIntent pendingIntent = operation;
                WorkSource workSource2 = workSource;
                AlarmClockInfo alarmClockInfo = alarmClock;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeString(callingPackage);
                        try {
                            _data.writeInt(type);
                        } catch (Throwable th2) {
                            th = th2;
                            j = triggerAtTime;
                            j2 = windowLength;
                            j3 = interval;
                            i = flags;
                            str = listenerTag;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        i2 = type;
                        j = triggerAtTime;
                        j2 = windowLength;
                        j3 = interval;
                        i = flags;
                        str = listenerTag;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeLong(triggerAtTime);
                        try {
                            _data.writeLong(windowLength);
                            try {
                                _data.writeLong(interval);
                                try {
                                    _data.writeInt(flags);
                                    if (pendingIntent != null) {
                                        _data.writeInt(1);
                                        pendingIntent.writeToParcel(_data, 0);
                                    } else {
                                        _data.writeInt(0);
                                    }
                                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                                } catch (Throwable th4) {
                                    th = th4;
                                    str = listenerTag;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                i = flags;
                                str = listenerTag;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            j3 = interval;
                            i = flags;
                            str = listenerTag;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        j2 = windowLength;
                        j3 = interval;
                        i = flags;
                        str = listenerTag;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        int i3;
                        _data.writeString(listenerTag);
                        if (workSource2 != null) {
                            _data.writeInt(1);
                            i3 = 0;
                            workSource2.writeToParcel(_data, 0);
                        } else {
                            i3 = 0;
                            _data.writeInt(0);
                        }
                        if (alarmClockInfo != null) {
                            _data.writeInt(1);
                            alarmClockInfo.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(i3);
                        }
                        this.mRemote.transact(1, _data, _reply, 0);
                        _reply.readException();
                        _reply.recycle();
                        _data.recycle();
                    } catch (Throwable th8) {
                        th = th8;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    String str2 = callingPackage;
                    i2 = type;
                    j = triggerAtTime;
                    j2 = windowLength;
                    j3 = interval;
                    i = flags;
                    str = listenerTag;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public boolean setTime(long millis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(millis);
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

            public void setTimeZone(String zone) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(zone);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void remove(PendingIntent operation, IAlarmListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (operation != null) {
                        _data.writeInt(1);
                        operation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getNextWakeFromIdleTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public AlarmClockInfo getNextAlarmClock(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    AlarmClockInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (AlarmClockInfo) AlarmClockInfo.CREATOR.createFromParcel(_reply);
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

            public void updateBlockedUids(int uid, boolean isBlocked) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(isBlocked);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getWakeUpNum(int uid, String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(pkg);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long checkHasHwRTCAlarm(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void adjustHwRTCAlarm(boolean deskClockTime, boolean bootOnTime, int typeState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deskClockTime);
                    _data.writeInt(bootOnTime);
                    _data.writeInt(typeState);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setHwAirPlaneStateProp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setHwRTCAlarm() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long currentNetworkTimeMillis() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
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

        public static IAlarmManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IAlarmManager)) {
                return new Proxy(obj);
            }
            return (IAlarmManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            boolean z;
            if (i != 1598968902) {
                AlarmClockInfo _arg0 = null;
                boolean _arg1 = false;
                long _result;
                switch (i) {
                    case 1:
                        PendingIntent _arg6;
                        WorkSource _arg9;
                        parcel.enforceInterface(descriptor);
                        String _arg02 = data.readString();
                        int _arg12 = data.readInt();
                        long _arg2 = data.readLong();
                        long _arg3 = data.readLong();
                        long _arg4 = data.readLong();
                        int _arg5 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg6 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg6 = null;
                        }
                        IAlarmListener _arg7 = android.app.IAlarmListener.Stub.asInterface(data.readStrongBinder());
                        String _arg8 = data.readString();
                        if (data.readInt() != 0) {
                            _arg9 = (WorkSource) WorkSource.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg9 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg0 = (AlarmClockInfo) AlarmClockInfo.CREATOR.createFromParcel(parcel);
                        }
                        z = true;
                        set(_arg02, _arg12, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8, _arg9, _arg0);
                        reply.writeNoException();
                        return z;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        boolean _result2 = setTime(data.readLong());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        setTimeZone(data.readString());
                        reply.writeNoException();
                        return true;
                    case 4:
                        PendingIntent _arg03;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        remove(_arg03, android.app.IAlarmListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _result = getNextWakeFromIdleTime();
                        reply.writeNoException();
                        parcel2.writeLong(_result);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        AlarmClockInfo _result3 = getNextAlarmClock(data.readInt());
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel2.writeInt(1);
                            _result3.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        int _arg04 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        updateBlockedUids(_arg04, _arg1);
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        int _result4 = getWakeUpNum(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        long _result5 = checkHasHwRTCAlarm(data.readString());
                        reply.writeNoException();
                        parcel2.writeLong(_result5);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        boolean _arg05 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        adjustHwRTCAlarm(_arg05, _arg1, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        setHwAirPlaneStateProp();
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        setHwRTCAlarm();
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _result = currentNetworkTimeMillis();
                        reply.writeNoException();
                        parcel2.writeLong(_result);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            reply.writeString(descriptor);
            return z;
        }
    }

    void adjustHwRTCAlarm(boolean z, boolean z2, int i) throws RemoteException;

    long checkHasHwRTCAlarm(String str) throws RemoteException;

    long currentNetworkTimeMillis() throws RemoteException;

    AlarmClockInfo getNextAlarmClock(int i) throws RemoteException;

    long getNextWakeFromIdleTime() throws RemoteException;

    int getWakeUpNum(int i, String str) throws RemoteException;

    void remove(PendingIntent pendingIntent, IAlarmListener iAlarmListener) throws RemoteException;

    void set(String str, int i, long j, long j2, long j3, int i2, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str2, WorkSource workSource, AlarmClockInfo alarmClockInfo) throws RemoteException;

    void setHwAirPlaneStateProp() throws RemoteException;

    void setHwRTCAlarm() throws RemoteException;

    boolean setTime(long j) throws RemoteException;

    void setTimeZone(String str) throws RemoteException;

    void updateBlockedUids(int i, boolean z) throws RemoteException;
}
