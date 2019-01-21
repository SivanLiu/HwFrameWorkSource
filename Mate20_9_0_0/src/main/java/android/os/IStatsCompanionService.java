package android.os;

public interface IStatsCompanionService extends IInterface {

    public static abstract class Stub extends Binder implements IStatsCompanionService {
        private static final String DESCRIPTOR = "android.os.IStatsCompanionService";
        static final int TRANSACTION_cancelAlarmForSubscriberTriggering = 7;
        static final int TRANSACTION_cancelAnomalyAlarm = 3;
        static final int TRANSACTION_cancelPullingAlarm = 5;
        static final int TRANSACTION_pullData = 8;
        static final int TRANSACTION_sendDataBroadcast = 9;
        static final int TRANSACTION_sendSubscriberBroadcast = 10;
        static final int TRANSACTION_setAlarmForSubscriberTriggering = 6;
        static final int TRANSACTION_setAnomalyAlarm = 2;
        static final int TRANSACTION_setPullingAlarm = 4;
        static final int TRANSACTION_statsdReady = 1;
        static final int TRANSACTION_triggerUidSnapshot = 11;

        private static class Proxy implements IStatsCompanionService {
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

            public void statsdReady() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setAnomalyAlarm(long timestampMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(timestampMs);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void cancelAnomalyAlarm() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setPullingAlarm(long nextPullTimeMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(nextPullTimeMs);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void cancelPullingAlarm() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setAlarmForSubscriberTriggering(long timestampMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(timestampMs);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void cancelAlarmForSubscriberTriggering() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public StatsLogEventWrapper[] pullData(int pullCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(pullCode);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    StatsLogEventWrapper[] _result = (StatsLogEventWrapper[]) _reply.createTypedArray(StatsLogEventWrapper.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendDataBroadcast(IBinder intentSender, long lastReportTimeNs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(intentSender);
                    _data.writeLong(lastReportTimeNs);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void sendSubscriberBroadcast(IBinder intentSender, long configUid, long configId, long subscriptionId, long subscriptionRuleId, String[] cookies, StatsDimensionsValue dimensionsValue) throws RemoteException {
                Throwable th;
                long j;
                long j2;
                long j3;
                long j4;
                String[] strArr;
                StatsDimensionsValue statsDimensionsValue = dimensionsValue;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeStrongBinder(intentSender);
                    } catch (Throwable th2) {
                        th = th2;
                        j = configUid;
                        j2 = configId;
                        j3 = subscriptionId;
                        j4 = subscriptionRuleId;
                        strArr = cookies;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeLong(configUid);
                        try {
                            _data.writeLong(configId);
                            try {
                                _data.writeLong(subscriptionId);
                            } catch (Throwable th3) {
                                th = th3;
                                j4 = subscriptionRuleId;
                                strArr = cookies;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            j3 = subscriptionId;
                            j4 = subscriptionRuleId;
                            strArr = cookies;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        j2 = configId;
                        j3 = subscriptionId;
                        j4 = subscriptionRuleId;
                        strArr = cookies;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeLong(subscriptionRuleId);
                        try {
                            _data.writeStringArray(cookies);
                            if (statsDimensionsValue != null) {
                                _data.writeInt(1);
                                statsDimensionsValue.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                this.mRemote.transact(10, _data, null, 1);
                                _data.recycle();
                            } catch (Throwable th6) {
                                th = th6;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        strArr = cookies;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    IBinder iBinder = intentSender;
                    j = configUid;
                    j2 = configId;
                    j3 = subscriptionId;
                    j4 = subscriptionRuleId;
                    strArr = cookies;
                    _data.recycle();
                    throw th;
                }
            }

            public void triggerUidSnapshot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStatsCompanionService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IStatsCompanionService)) {
                return new Proxy(obj);
            }
            return (IStatsCompanionService) iin;
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
                switch (i) {
                    case 1:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        statsdReady();
                        return z;
                    case 2:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        setAnomalyAlarm(data.readLong());
                        return z;
                    case 3:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        cancelAnomalyAlarm();
                        return z;
                    case 4:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        setPullingAlarm(data.readLong());
                        return z;
                    case 5:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        cancelPullingAlarm();
                        return z;
                    case 6:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        setAlarmForSubscriberTriggering(data.readLong());
                        return z;
                    case 7:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        cancelAlarmForSubscriberTriggering();
                        return z;
                    case 8:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        StatsLogEventWrapper[] _result = pullData(data.readInt());
                        reply.writeNoException();
                        parcel2.writeTypedArray(_result, i);
                        return i;
                    case 9:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        sendDataBroadcast(data.readStrongBinder(), data.readLong());
                        return z;
                    case 10:
                        StatsDimensionsValue statsDimensionsValue;
                        parcel.enforceInterface(descriptor);
                        IBinder _arg0 = data.readStrongBinder();
                        long _arg1 = data.readLong();
                        long _arg2 = data.readLong();
                        long _arg3 = data.readLong();
                        long _arg4 = data.readLong();
                        String[] _arg5 = data.createStringArray();
                        if (data.readInt() != 0) {
                            statsDimensionsValue = (StatsDimensionsValue) StatsDimensionsValue.CREATOR.createFromParcel(parcel);
                        } else {
                            statsDimensionsValue = null;
                        }
                        z = true;
                        sendSubscriberBroadcast(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, statsDimensionsValue);
                        return z;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        triggerUidSnapshot();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            parcel2.writeString(descriptor);
            return z;
        }
    }

    void cancelAlarmForSubscriberTriggering() throws RemoteException;

    void cancelAnomalyAlarm() throws RemoteException;

    void cancelPullingAlarm() throws RemoteException;

    StatsLogEventWrapper[] pullData(int i) throws RemoteException;

    void sendDataBroadcast(IBinder iBinder, long j) throws RemoteException;

    void sendSubscriberBroadcast(IBinder iBinder, long j, long j2, long j3, long j4, String[] strArr, StatsDimensionsValue statsDimensionsValue) throws RemoteException;

    void setAlarmForSubscriberTriggering(long j) throws RemoteException;

    void setAnomalyAlarm(long j) throws RemoteException;

    void setPullingAlarm(long j) throws RemoteException;

    void statsdReady() throws RemoteException;

    void triggerUidSnapshot() throws RemoteException;
}
