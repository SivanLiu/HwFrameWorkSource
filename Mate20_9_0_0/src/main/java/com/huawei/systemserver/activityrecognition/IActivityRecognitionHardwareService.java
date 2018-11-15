package com.huawei.systemserver.activityrecognition;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IActivityRecognitionHardwareService extends IInterface {

    public static abstract class Stub extends Binder implements IActivityRecognitionHardwareService {
        private static final String DESCRIPTOR = "com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareService";
        static final int TRANSACTION_disableActivityEvent = 7;
        static final int TRANSACTION_disableEnvironmentEvent = 15;
        static final int TRANSACTION_enableActivityEvent = 5;
        static final int TRANSACTION_enableActivityExtendEvent = 6;
        static final int TRANSACTION_enableEnvironmentEvent = 14;
        static final int TRANSACTION_exitEnvironmentFunction = 12;
        static final int TRANSACTION_flush = 9;
        static final int TRANSACTION_getARVersion = 16;
        static final int TRANSACTION_getCurrentActivity = 8;
        static final int TRANSACTION_getCurrentActivityV1_1 = 17;
        static final int TRANSACTION_getCurrentEnvironment = 13;
        static final int TRANSACTION_getCurrentEnvironmentV1_1 = 18;
        static final int TRANSACTION_getSupportedActivities = 2;
        static final int TRANSACTION_getSupportedEnvironments = 10;
        static final int TRANSACTION_getSupportedModule = 1;
        static final int TRANSACTION_initEnvironmentFunction = 11;
        static final int TRANSACTION_registerSink = 3;
        static final int TRANSACTION_unregisterSink = 4;

        private static class Proxy implements IActivityRecognitionHardwareService {
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

            public int getSupportedModule() throws RemoteException {
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

            public String[] getSupportedActivities() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean registerSink(String packageName, IActivityRecognitionHardwareSink sink) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(sink != null ? sink.asBinder() : null);
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

            public boolean unregisterSink(String packageName, IActivityRecognitionHardwareSink sink) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(sink != null ? sink.asBinder() : null);
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

            public boolean enableActivityEvent(String packageName, String activity, int eventType, long reportLatencyNs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(activity);
                    _data.writeInt(eventType);
                    _data.writeLong(reportLatencyNs);
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

            public boolean enableActivityExtendEvent(String packageName, String activity, int eventType, long reportLatencyNs, OtherParameters params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(activity);
                    _data.writeInt(eventType);
                    _data.writeLong(reportLatencyNs);
                    boolean _result = true;
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
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

            public boolean disableActivityEvent(String packageName, String activity, int eventType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(activity);
                    _data.writeInt(eventType);
                    boolean z = false;
                    this.mRemote.transact(7, _data, _reply, 0);
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

            public HwActivityChangedExtendEvent getCurrentActivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwActivityChangedExtendEvent _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (HwActivityChangedExtendEvent) HwActivityChangedExtendEvent.CREATOR.createFromParcel(_reply);
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

            public boolean flush() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(9, _data, _reply, 0);
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

            public String[] getSupportedEnvironments() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean initEnvironmentFunction(String packageName, String environment, OtherParameters params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(environment);
                    boolean _result = true;
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
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

            public boolean exitEnvironmentFunction(String packageName, String environment, OtherParameters params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(environment);
                    boolean _result = true;
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, _reply, 0);
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

            public HwEnvironmentChangedEvent getCurrentEnvironment() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwEnvironmentChangedEvent _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (HwEnvironmentChangedEvent) HwEnvironmentChangedEvent.CREATOR.createFromParcel(_reply);
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

            public boolean enableEnvironmentEvent(String packageName, String environment, int eventType, long reportLatencyNs, OtherParameters params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(environment);
                    _data.writeInt(eventType);
                    _data.writeLong(reportLatencyNs);
                    boolean _result = true;
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, _reply, 0);
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

            public boolean disableEnvironmentEvent(String packageName, String environment, int eventType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(environment);
                    _data.writeInt(eventType);
                    boolean z = false;
                    this.mRemote.transact(15, _data, _reply, 0);
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

            public int getARVersion(String packageName, int sdkVersion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(sdkVersion);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public HwActivityChangedExtendEvent getCurrentActivityV1_1() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwActivityChangedExtendEvent _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (HwActivityChangedExtendEvent) HwActivityChangedExtendEvent.CREATOR.createFromParcel(_reply);
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

            public HwEnvironmentChangedEvent getCurrentEnvironmentV1_1() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwEnvironmentChangedEvent _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (HwEnvironmentChangedEvent) HwEnvironmentChangedEvent.CREATOR.createFromParcel(_reply);
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

        public static IActivityRecognitionHardwareService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IActivityRecognitionHardwareService)) {
                return new Proxy(obj);
            }
            return (IActivityRecognitionHardwareService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            if (i != 1598968902) {
                OtherParameters _arg2 = null;
                String[] _result;
                boolean _result2;
                boolean _result3;
                String _arg0;
                String _arg1;
                int _arg22;
                long _arg3;
                boolean _result4;
                HwActivityChangedExtendEvent _result5;
                String _arg02;
                String _arg12;
                HwEnvironmentChangedEvent _result6;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(DESCRIPTOR);
                        int _result7 = getSupportedModule();
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 2:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result = getSupportedActivities();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result);
                        return true;
                    case 3:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result2 = registerSink(data.readString(), com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 4:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result2 = unregisterSink(data.readString(), com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 5:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result3 = enableActivityEvent(data.readString(), data.readString(), data.readInt(), data.readLong());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 6:
                        parcel.enforceInterface(DESCRIPTOR);
                        _arg0 = data.readString();
                        _arg1 = data.readString();
                        _arg22 = data.readInt();
                        _arg3 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg2 = (OtherParameters) OtherParameters.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = enableActivityExtendEvent(_arg0, _arg1, _arg22, _arg3, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 7:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result4 = disableActivityEvent(data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 8:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result5 = getCurrentActivity();
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(1);
                            _result5.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 9:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result3 = flush();
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 10:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result = getSupportedEnvironments();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result);
                        return true;
                    case 11:
                        parcel.enforceInterface(DESCRIPTOR);
                        _arg02 = data.readString();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (OtherParameters) OtherParameters.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = initEnvironmentFunction(_arg02, _arg12, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 12:
                        parcel.enforceInterface(DESCRIPTOR);
                        _arg02 = data.readString();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (OtherParameters) OtherParameters.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = exitEnvironmentFunction(_arg02, _arg12, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 13:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result6 = getCurrentEnvironment();
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 14:
                        parcel.enforceInterface(DESCRIPTOR);
                        _arg0 = data.readString();
                        _arg1 = data.readString();
                        _arg22 = data.readInt();
                        _arg3 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg2 = (OtherParameters) OtherParameters.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = enableEnvironmentEvent(_arg0, _arg1, _arg22, _arg3, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 15:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result4 = disableEnvironmentEvent(data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 16:
                        parcel.enforceInterface(DESCRIPTOR);
                        int _result8 = getARVersion(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 17:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result5 = getCurrentActivityV1_1();
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(1);
                            _result5.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 18:
                        parcel.enforceInterface(DESCRIPTOR);
                        _result6 = getCurrentEnvironmentV1_1();
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(DESCRIPTOR);
            return true;
        }
    }

    boolean disableActivityEvent(String str, String str2, int i) throws RemoteException;

    boolean disableEnvironmentEvent(String str, String str2, int i) throws RemoteException;

    boolean enableActivityEvent(String str, String str2, int i, long j) throws RemoteException;

    boolean enableActivityExtendEvent(String str, String str2, int i, long j, OtherParameters otherParameters) throws RemoteException;

    boolean enableEnvironmentEvent(String str, String str2, int i, long j, OtherParameters otherParameters) throws RemoteException;

    boolean exitEnvironmentFunction(String str, String str2, OtherParameters otherParameters) throws RemoteException;

    boolean flush() throws RemoteException;

    int getARVersion(String str, int i) throws RemoteException;

    HwActivityChangedExtendEvent getCurrentActivity() throws RemoteException;

    HwActivityChangedExtendEvent getCurrentActivityV1_1() throws RemoteException;

    HwEnvironmentChangedEvent getCurrentEnvironment() throws RemoteException;

    HwEnvironmentChangedEvent getCurrentEnvironmentV1_1() throws RemoteException;

    String[] getSupportedActivities() throws RemoteException;

    String[] getSupportedEnvironments() throws RemoteException;

    int getSupportedModule() throws RemoteException;

    boolean initEnvironmentFunction(String str, String str2, OtherParameters otherParameters) throws RemoteException;

    boolean registerSink(String str, IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink) throws RemoteException;

    boolean unregisterSink(String str, IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink) throws RemoteException;
}
