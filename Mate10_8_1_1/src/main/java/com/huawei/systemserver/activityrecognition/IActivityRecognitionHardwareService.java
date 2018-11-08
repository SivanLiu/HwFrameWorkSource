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
        static final int TRANSACTION_getCurrentActivity = 8;
        static final int TRANSACTION_getCurrentEnvironment = 13;
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
                IBinder iBinder = null;
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    if (sink != null) {
                        iBinder = sink.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean unregisterSink(String packageName, IActivityRecognitionHardwareSink sink) throws RemoteException {
                IBinder iBinder = null;
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    if (sink != null) {
                        iBinder = sink.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean enableActivityEvent(String packageName, String activity, int eventType, long reportLatencyNs) throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(activity);
                    _data.writeInt(eventType);
                    _data.writeLong(reportLatencyNs);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean enableActivityExtendEvent(String packageName, String activity, int eventType, long reportLatencyNs, OtherParameters params) throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(activity);
                    _data.writeInt(eventType);
                    _data.writeLong(reportLatencyNs);
                    if (params == null) {
                        _data.writeInt(0);
                    } else {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
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
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(activity);
                    _data.writeInt(eventType);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
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
                    HwActivityChangedExtendEvent hwActivityChangedExtendEvent;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCurrentActivity, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        hwActivityChangedExtendEvent = null;
                    } else {
                        hwActivityChangedExtendEvent = (HwActivityChangedExtendEvent) HwActivityChangedExtendEvent.CREATOR.createFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return hwActivityChangedExtendEvent;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean flush() throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_flush, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
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
                    this.mRemote.transact(Stub.TRANSACTION_getSupportedEnvironments, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean initEnvironmentFunction(String packageName, String environment, OtherParameters params) throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(environment);
                    if (params == null) {
                        _data.writeInt(0);
                    } else {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_initEnvironmentFunction, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
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
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(environment);
                    if (params == null) {
                        _data.writeInt(0);
                    } else {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_exitEnvironmentFunction, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
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
                    HwEnvironmentChangedEvent hwEnvironmentChangedEvent;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCurrentEnvironment, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        hwEnvironmentChangedEvent = null;
                    } else {
                        hwEnvironmentChangedEvent = (HwEnvironmentChangedEvent) HwEnvironmentChangedEvent.CREATOR.createFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return hwEnvironmentChangedEvent;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean enableEnvironmentEvent(String packageName, String environment, int eventType, long reportLatencyNs, OtherParameters params) throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(environment);
                    _data.writeInt(eventType);
                    _data.writeLong(reportLatencyNs);
                    if (params == null) {
                        _data.writeInt(0);
                    } else {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_enableEnvironmentEvent, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
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
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(environment);
                    _data.writeInt(eventType);
                    this.mRemote.transact(Stub.TRANSACTION_disableEnvironmentEvent, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
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
            if (iin != null && (iin instanceof IActivityRecognitionHardwareService)) {
                return (IActivityRecognitionHardwareService) iin;
            }
            return new Proxy(obj);
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String[] _result;
            boolean _result2;
            String _arg0;
            String _arg1;
            int _arg2;
            long _arg3;
            OtherParameters otherParameters;
            OtherParameters otherParameters2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _result3 = getSupportedModule();
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getSupportedActivities();
                    reply.writeNoException();
                    reply.writeStringArray(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = registerSink(data.readString(), com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = unregisterSink(data.readString(), com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = enableActivityEvent(data.readString(), data.readString(), data.readInt(), data.readLong());
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    _arg1 = data.readString();
                    _arg2 = data.readInt();
                    _arg3 = data.readLong();
                    if (data.readInt() == 0) {
                        otherParameters = null;
                    } else {
                        otherParameters = (OtherParameters) OtherParameters.CREATOR.createFromParcel(data);
                    }
                    _result2 = enableActivityExtendEvent(_arg0, _arg1, _arg2, _arg3, otherParameters);
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = disableActivityEvent(data.readString(), data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case TRANSACTION_getCurrentActivity /*8*/:
                    data.enforceInterface(DESCRIPTOR);
                    HwActivityChangedExtendEvent _result4 = getCurrentActivity();
                    reply.writeNoException();
                    if (_result4 == null) {
                        reply.writeInt(0);
                    } else {
                        reply.writeInt(1);
                        _result4.writeToParcel(reply, 1);
                    }
                    return true;
                case TRANSACTION_flush /*9*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = flush();
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case TRANSACTION_getSupportedEnvironments /*10*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getSupportedEnvironments();
                    reply.writeNoException();
                    reply.writeStringArray(_result);
                    return true;
                case TRANSACTION_initEnvironmentFunction /*11*/:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    _arg1 = data.readString();
                    if (data.readInt() == 0) {
                        otherParameters2 = null;
                    } else {
                        otherParameters2 = (OtherParameters) OtherParameters.CREATOR.createFromParcel(data);
                    }
                    _result2 = initEnvironmentFunction(_arg0, _arg1, otherParameters2);
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case TRANSACTION_exitEnvironmentFunction /*12*/:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    _arg1 = data.readString();
                    if (data.readInt() == 0) {
                        otherParameters2 = null;
                    } else {
                        otherParameters2 = (OtherParameters) OtherParameters.CREATOR.createFromParcel(data);
                    }
                    _result2 = exitEnvironmentFunction(_arg0, _arg1, otherParameters2);
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case TRANSACTION_getCurrentEnvironment /*13*/:
                    data.enforceInterface(DESCRIPTOR);
                    HwEnvironmentChangedEvent _result5 = getCurrentEnvironment();
                    reply.writeNoException();
                    if (_result5 == null) {
                        reply.writeInt(0);
                    } else {
                        reply.writeInt(1);
                        _result5.writeToParcel(reply, 1);
                    }
                    return true;
                case TRANSACTION_enableEnvironmentEvent /*14*/:
                    data.enforceInterface(DESCRIPTOR);
                    _arg0 = data.readString();
                    _arg1 = data.readString();
                    _arg2 = data.readInt();
                    _arg3 = data.readLong();
                    if (data.readInt() == 0) {
                        otherParameters = null;
                    } else {
                        otherParameters = (OtherParameters) OtherParameters.CREATOR.createFromParcel(data);
                    }
                    _result2 = enableEnvironmentEvent(_arg0, _arg1, _arg2, _arg3, otherParameters);
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case TRANSACTION_disableEnvironmentEvent /*15*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = disableEnvironmentEvent(data.readString(), data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(!_result2 ? 0 : 1);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    boolean disableActivityEvent(String str, String str2, int i) throws RemoteException;

    boolean disableEnvironmentEvent(String str, String str2, int i) throws RemoteException;

    boolean enableActivityEvent(String str, String str2, int i, long j) throws RemoteException;

    boolean enableActivityExtendEvent(String str, String str2, int i, long j, OtherParameters otherParameters) throws RemoteException;

    boolean enableEnvironmentEvent(String str, String str2, int i, long j, OtherParameters otherParameters) throws RemoteException;

    boolean exitEnvironmentFunction(String str, String str2, OtherParameters otherParameters) throws RemoteException;

    boolean flush() throws RemoteException;

    HwActivityChangedExtendEvent getCurrentActivity() throws RemoteException;

    HwEnvironmentChangedEvent getCurrentEnvironment() throws RemoteException;

    String[] getSupportedActivities() throws RemoteException;

    String[] getSupportedEnvironments() throws RemoteException;

    int getSupportedModule() throws RemoteException;

    boolean initEnvironmentFunction(String str, String str2, OtherParameters otherParameters) throws RemoteException;

    boolean registerSink(String str, IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink) throws RemoteException;

    boolean unregisterSink(String str, IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink) throws RemoteException;
}
