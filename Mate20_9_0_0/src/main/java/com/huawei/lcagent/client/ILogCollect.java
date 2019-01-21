package com.huawei.lcagent.client;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ILogCollect extends IInterface {

    public static abstract class Stub extends Binder implements ILogCollect {
        private static final String DESCRIPTOR = "com.huawei.lcagent.client.ILogCollect";
        static final int TRANSACTION_allowUploadAlways = 11;
        static final int TRANSACTION_allowUploadInMobileNetwork = 10;
        static final int TRANSACTION_cancelRdebugProcess = 41;
        static final int TRANSACTION_captureAllLog = 16;
        static final int TRANSACTION_captureLogMetric = 6;
        static final int TRANSACTION_captureLogMetricWithModule = 28;
        static final int TRANSACTION_captureLogMetricWithParameters = 7;
        static final int TRANSACTION_captureRemoteDebugLog = 38;
        static final int TRANSACTION_captureRemoteDebugLogWithRemark = 37;
        static final int TRANSACTION_clearLogMetric = 8;
        static final int TRANSACTION_closeRemoteDebug = 36;
        static final int TRANSACTION_configure = 15;
        static final int TRANSACTION_configureAPlogs = 23;
        static final int TRANSACTION_configureBluetoothlogcat = 21;
        static final int TRANSACTION_configureCoredump = 24;
        static final int TRANSACTION_configureGPS = 25;
        static final int TRANSACTION_configureLogcat = 22;
        static final int TRANSACTION_configureModemlogcat = 20;
        static final int TRANSACTION_configureUserType = 12;
        static final int TRANSACTION_configureWithPara = 26;
        static final int TRANSACTION_doEncrypt = 34;
        static final int TRANSACTION_feedbackUploadResult = 9;
        static final int TRANSACTION_forceUpload = 14;
        static final int TRANSACTION_getCompressInfo = 27;
        static final int TRANSACTION_getFirstErrorTime = 17;
        static final int TRANSACTION_getFirstErrorType = 19;
        static final int TRANSACTION_getMaxSizeOfLogFile = 39;
        static final int TRANSACTION_getUploadType = 33;
        static final int TRANSACTION_getUserType = 13;
        static final int TRANSACTION_getValueByType = 42;
        static final int TRANSACTION_postRemoteDebugCmd = 35;
        static final int TRANSACTION_resetFirstErrorTime = 18;
        static final int TRANSACTION_setMetricCommonHeader = 2;
        static final int TRANSACTION_setMetricCommonHeaderWithMcc = 30;
        static final int TRANSACTION_setMetricStoargeHeader = 1;
        static final int TRANSACTION_setMetricStoargeHeaderWithMcc = 29;
        static final int TRANSACTION_setMetricStoargeTail = 3;
        static final int TRANSACTION_setMetricStoargeTailWithMcc = 31;
        static final int TRANSACTION_shouldSubmitMetric = 5;
        static final int TRANSACTION_submitMetric = 4;
        static final int TRANSACTION_submitMetricWithMcc = 32;
        static final int TRANSACTION_testCheck = 46;
        static final int TRANSACTION_testInit = 43;
        static final int TRANSACTION_testPreset = 44;
        static final int TRANSACTION_testTrigger = 45;
        static final int TRANSACTION_uploadLogFile = 40;

        private static class Proxy implements ILogCollect {
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

            public int setMetricStoargeHeader(int metricID, byte[] payloadBytes, int payloadLen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeByteArray(payloadBytes);
                    _data.writeInt(payloadLen);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setMetricCommonHeader(int metricID, byte[] payloadBytes, int payloadLen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeByteArray(payloadBytes);
                    _data.writeInt(payloadLen);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setMetricStoargeTail(int metricID, byte[] payloadBytes, int payloadLen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeByteArray(payloadBytes);
                    _data.writeInt(payloadLen);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int submitMetric(int metricID, int level, byte[] payloadBytes, int payloadLen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeInt(level);
                    _data.writeByteArray(payloadBytes);
                    _data.writeInt(payloadLen);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean shouldSubmitMetric(int metricID, int level) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeInt(level);
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

            public LogMetricInfo captureLogMetric(int metricID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    LogMetricInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (LogMetricInfo) LogMetricInfo.CREATOR.createFromParcel(_reply);
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

            public LogMetricInfo captureLogMetricWithParameters(int metricID, String keyValuePairs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    LogMetricInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeString(keyValuePairs);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (LogMetricInfo) LogMetricInfo.CREATOR.createFromParcel(_reply);
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

            public void clearLogMetric(long id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(id);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int feedbackUploadResult(long hashId, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(hashId);
                    _data.writeInt(status);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int allowUploadInMobileNetwork(boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(allow);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int allowUploadAlways(boolean allow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(allow);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int configureUserType(int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getUserType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int forceUpload() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int configure(String strCommand) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(strCommand);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public LogMetricInfo captureAllLog() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    LogMetricInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (LogMetricInfo) LogMetricInfo.CREATOR.createFromParcel(_reply);
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

            public long getFirstErrorTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int resetFirstErrorTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getFirstErrorType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int configureModemlogcat(int mode, String parameters) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    _data.writeString(parameters);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int configureBluetoothlogcat(int enable, String parameters) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    _data.writeString(parameters);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int configureLogcat(int enable, String parameters) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    _data.writeString(parameters);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int configureAPlogs(int enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int configureCoredump(int enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int configureGPS(int enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void configureWithPara(String cmd, String parameters) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(cmd);
                    _data.writeString(parameters);
                    this.mRemote.transact(Stub.TRANSACTION_configureWithPara, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public CompressInfo getCompressInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CompressInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CompressInfo) CompressInfo.CREATOR.createFromParcel(_reply);
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

            public LogMetricInfo captureLogMetricWithModule(int metricID, String module) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    LogMetricInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeString(module);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (LogMetricInfo) LogMetricInfo.CREATOR.createFromParcel(_reply);
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

            public int setMetricStoargeHeaderWithMcc(int metricID, byte[] payloadBytes, int payloadLen, String mcc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeByteArray(payloadBytes);
                    _data.writeInt(payloadLen);
                    _data.writeString(mcc);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setMetricCommonHeaderWithMcc(int metricID, byte[] payloadBytes, int payloadLen, String mcc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeByteArray(payloadBytes);
                    _data.writeInt(payloadLen);
                    _data.writeString(mcc);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setMetricStoargeTailWithMcc(int metricID, byte[] payloadBytes, int payloadLen, String mcc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeByteArray(payloadBytes);
                    _data.writeInt(payloadLen);
                    _data.writeString(mcc);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int submitMetricWithMcc(int metricID, int level, byte[] payloadBytes, int payloadLen, String mcc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(metricID);
                    _data.writeInt(level);
                    _data.writeByteArray(payloadBytes);
                    _data.writeInt(payloadLen);
                    _data.writeString(mcc);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getUploadType(String mcc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(mcc);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String doEncrypt(String src) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(src);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int postRemoteDebugCmd(String msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msg);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int closeRemoteDebug(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int captureRemoteDebugLogWithRemark(ICaptureLogCallback callback, String remarkPath, String patchFilespath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(remarkPath);
                    _data.writeString(patchFilespath);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int captureRemoteDebugLog(ICaptureLogCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getMaxSizeOfLogFile() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getMaxSizeOfLogFile, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int uploadLogFile(String filename, int filetype, int uploadtime, IUploadLogCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(filename);
                    _data.writeInt(filetype);
                    _data.writeInt(uploadtime);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int cancelRdebugProcess() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getValueByType(int datatype) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(datatype);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String testInit(int testID, String jsonval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(testID);
                    _data.writeString(jsonval);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String testPreset(int testID, String jsonval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(testID);
                    _data.writeString(jsonval);
                    this.mRemote.transact(Stub.TRANSACTION_testPreset, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String testTrigger(int testID, String jsonval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(testID);
                    _data.writeString(jsonval);
                    this.mRemote.transact(Stub.TRANSACTION_testTrigger, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String testCheck(int testID, String jsonval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(testID);
                    _data.writeString(jsonval);
                    this.mRemote.transact(46, _data, _reply, 0);
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

        public static ILogCollect asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ILogCollect)) {
                return new Proxy(obj);
            }
            return (ILogCollect) iin;
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
                boolean _arg0 = false;
                int _result;
                int _result2;
                LogMetricInfo _result3;
                int _result4;
                int _result5;
                int _result6;
                String _result7;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        _result = setMetricStoargeHeader(data.readInt(), data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _result = setMetricCommonHeader(data.readInt(), data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result = setMetricStoargeTail(data.readInt(), data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _result2 = submitMetric(data.readInt(), data.readInt(), data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        boolean _result8 = shouldSubmitMetric(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        LogMetricInfo _result9 = captureLogMetric(data.readInt());
                        reply.writeNoException();
                        if (_result9 != null) {
                            parcel2.writeInt(1);
                            _result9.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _result3 = captureLogMetricWithParameters(data.readInt(), data.readString());
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel2.writeInt(1);
                            _result3.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        clearLogMetric(data.readLong());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _result = feedbackUploadResult(data.readLong(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        _result4 = allowUploadInMobileNetwork(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        _result4 = allowUploadAlways(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result4 = configureUserType(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _result5 = getUserType();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result5 = forceUpload();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _result4 = configure(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        LogMetricInfo _result10 = captureAllLog();
                        reply.writeNoException();
                        if (_result10 != null) {
                            parcel2.writeInt(1);
                            _result10.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        long _result11 = getFirstErrorTime();
                        reply.writeNoException();
                        parcel2.writeLong(_result11);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _result5 = resetFirstErrorTime();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        String _result12 = getFirstErrorType();
                        reply.writeNoException();
                        parcel2.writeString(_result12);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _result6 = configureModemlogcat(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _result6 = configureBluetoothlogcat(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        _result6 = configureLogcat(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _result4 = configureAPlogs(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        _result4 = configureCoredump(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _result4 = configureGPS(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case TRANSACTION_configureWithPara /*26*/:
                        parcel.enforceInterface(descriptor);
                        configureWithPara(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        CompressInfo _result13 = getCompressInfo();
                        reply.writeNoException();
                        if (_result13 != null) {
                            parcel2.writeInt(1);
                            _result13.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        _result3 = captureLogMetricWithModule(data.readInt(), data.readString());
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel2.writeInt(1);
                            _result3.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _result2 = setMetricStoargeHeaderWithMcc(data.readInt(), data.createByteArray(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        _result2 = setMetricCommonHeaderWithMcc(data.readInt(), data.createByteArray(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        _result2 = setMetricStoargeTailWithMcc(data.readInt(), data.createByteArray(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        _result5 = submitMetricWithMcc(data.readInt(), data.readInt(), data.createByteArray(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        _result4 = getUploadType(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        String _result14 = doEncrypt(data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result14);
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        _result4 = postRemoteDebugCmd(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        _result4 = closeRemoteDebug(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        _result = captureRemoteDebugLogWithRemark(com.huawei.lcagent.client.ICaptureLogCallback.Stub.asInterface(data.readStrongBinder()), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        _result4 = captureRemoteDebugLog(com.huawei.lcagent.client.ICaptureLogCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case TRANSACTION_getMaxSizeOfLogFile /*39*/:
                        parcel.enforceInterface(descriptor);
                        _result5 = getMaxSizeOfLogFile();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        _result2 = uploadLogFile(data.readString(), data.readInt(), data.readInt(), com.huawei.lcagent.client.IUploadLogCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        _result5 = cancelRdebugProcess();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        long _result15 = getValueByType(data.readInt());
                        reply.writeNoException();
                        parcel2.writeLong(_result15);
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        _result7 = testInit(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    case TRANSACTION_testPreset /*44*/:
                        parcel.enforceInterface(descriptor);
                        _result7 = testPreset(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    case TRANSACTION_testTrigger /*45*/:
                        parcel.enforceInterface(descriptor);
                        _result7 = testTrigger(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        _result7 = testCheck(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    int allowUploadAlways(boolean z) throws RemoteException;

    int allowUploadInMobileNetwork(boolean z) throws RemoteException;

    int cancelRdebugProcess() throws RemoteException;

    LogMetricInfo captureAllLog() throws RemoteException;

    LogMetricInfo captureLogMetric(int i) throws RemoteException;

    LogMetricInfo captureLogMetricWithModule(int i, String str) throws RemoteException;

    LogMetricInfo captureLogMetricWithParameters(int i, String str) throws RemoteException;

    int captureRemoteDebugLog(ICaptureLogCallback iCaptureLogCallback) throws RemoteException;

    int captureRemoteDebugLogWithRemark(ICaptureLogCallback iCaptureLogCallback, String str, String str2) throws RemoteException;

    void clearLogMetric(long j) throws RemoteException;

    int closeRemoteDebug(int i) throws RemoteException;

    int configure(String str) throws RemoteException;

    int configureAPlogs(int i) throws RemoteException;

    int configureBluetoothlogcat(int i, String str) throws RemoteException;

    int configureCoredump(int i) throws RemoteException;

    int configureGPS(int i) throws RemoteException;

    int configureLogcat(int i, String str) throws RemoteException;

    int configureModemlogcat(int i, String str) throws RemoteException;

    int configureUserType(int i) throws RemoteException;

    void configureWithPara(String str, String str2) throws RemoteException;

    String doEncrypt(String str) throws RemoteException;

    int feedbackUploadResult(long j, int i) throws RemoteException;

    int forceUpload() throws RemoteException;

    CompressInfo getCompressInfo() throws RemoteException;

    long getFirstErrorTime() throws RemoteException;

    String getFirstErrorType() throws RemoteException;

    int getMaxSizeOfLogFile() throws RemoteException;

    int getUploadType(String str) throws RemoteException;

    int getUserType() throws RemoteException;

    long getValueByType(int i) throws RemoteException;

    int postRemoteDebugCmd(String str) throws RemoteException;

    int resetFirstErrorTime() throws RemoteException;

    int setMetricCommonHeader(int i, byte[] bArr, int i2) throws RemoteException;

    int setMetricCommonHeaderWithMcc(int i, byte[] bArr, int i2, String str) throws RemoteException;

    int setMetricStoargeHeader(int i, byte[] bArr, int i2) throws RemoteException;

    int setMetricStoargeHeaderWithMcc(int i, byte[] bArr, int i2, String str) throws RemoteException;

    int setMetricStoargeTail(int i, byte[] bArr, int i2) throws RemoteException;

    int setMetricStoargeTailWithMcc(int i, byte[] bArr, int i2, String str) throws RemoteException;

    boolean shouldSubmitMetric(int i, int i2) throws RemoteException;

    int submitMetric(int i, int i2, byte[] bArr, int i3) throws RemoteException;

    int submitMetricWithMcc(int i, int i2, byte[] bArr, int i3, String str) throws RemoteException;

    String testCheck(int i, String str) throws RemoteException;

    String testInit(int i, String str) throws RemoteException;

    String testPreset(int i, String str) throws RemoteException;

    String testTrigger(int i, String str) throws RemoteException;

    int uploadLogFile(String str, int i, int i2, IUploadLogCallback iUploadLogCallback) throws RemoteException;
}
