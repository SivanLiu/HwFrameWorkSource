package com.huawei.netassistant.service;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetAssistantService extends IInterface {

    public static abstract class Stub extends Binder implements INetAssistantService {
        private static final String DESCRIPTOR = "com.huawei.netassistant.service.INetAssistantService";
        static final int TRANSACTION_clearDailyWarnPreference = 35;
        static final int TRANSACTION_clearMonthLimitPreference = 36;
        static final int TRANSACTION_clearMonthWarnPreference = 37;
        static final int TRANSACTION_getAdjustBrand = 29;
        static final int TRANSACTION_getAdjustCity = 27;
        static final int TRANSACTION_getAdjustDate = 25;
        static final int TRANSACTION_getAdjustPackageValue = 24;
        static final int TRANSACTION_getAdjustProvider = 28;
        static final int TRANSACTION_getAdjustProvince = 26;
        static final int TRANSACTION_getMonthMobileTotalBytes = 1;
        static final int TRANSACTION_getMonthlyTotalBytes = 2;
        static final int TRANSACTION_getNetworkUsageDays = 39;
        static final int TRANSACTION_getSettingBeginDate = 16;
        static final int TRANSACTION_getSettingExcessMontyType = 18;
        static final int TRANSACTION_getSettingNotify = 22;
        static final int TRANSACTION_getSettingOverMarkDay = 20;
        static final int TRANSACTION_getSettingOverMarkMonth = 19;
        static final int TRANSACTION_getSettingRegularAdjustType = 17;
        static final int TRANSACTION_getSettingSpeedNotify = 23;
        static final int TRANSACTION_getSettingTotalPackage = 15;
        static final int TRANSACTION_getSettingUnlockScreen = 21;
        static final int TRANSACTION_getSimCardOperatorName = 32;
        static final int TRANSACTION_getSimProfileDes = 38;
        static final int TRANSACTION_getTodayMobileTotalBytes = 3;
        static final int TRANSACTION_sendAdjustSMS = 31;
        static final int TRANSACTION_setNetAccessInfo = 30;
        static final int TRANSACTION_setOperatorInfo = 5;
        static final int TRANSACTION_setProvinceInfo = 4;
        static final int TRANSACTION_setSettingBeginDate = 7;
        static final int TRANSACTION_setSettingExcessMontyType = 9;
        static final int TRANSACTION_setSettingNotify = 13;
        static final int TRANSACTION_setSettingOverMarkDay = 11;
        static final int TRANSACTION_setSettingOverMarkMonth = 10;
        static final int TRANSACTION_setSettingRegularAdjustType = 8;
        static final int TRANSACTION_setSettingSpeedNotify = 14;
        static final int TRANSACTION_setSettingTotalPackage = 6;
        static final int TRANSACTION_setSettingUnlockScreen = 12;
        static final int TRANSACTION_startSpeedUpdate = 33;
        static final int TRANSACTION_stopSpeedUpdate = 34;

        private static class Proxy implements INetAssistantService {
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

            public long getMonthMobileTotalBytes(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getMonthlyTotalBytes(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getTodayMobileTotalBytes(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setProvinceInfo(String imsi, String provinceCode, String cityId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeString(provinceCode);
                    _data.writeString(cityId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setOperatorInfo(String imsi, String providerCode, String brandCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeString(providerCode);
                    _data.writeString(brandCode);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingTotalPackage(String imsi, long value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeLong(value);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingBeginDate(String imsi, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeInt(value);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingRegularAdjustType(String imsi, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeInt(value);
                    this.mRemote.transact(Stub.TRANSACTION_setSettingRegularAdjustType, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingExcessMontyType(String imsi, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeInt(value);
                    this.mRemote.transact(Stub.TRANSACTION_setSettingExcessMontyType, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingOverMarkMonth(String imsi, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeInt(value);
                    this.mRemote.transact(Stub.TRANSACTION_setSettingOverMarkMonth, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingOverMarkDay(String imsi, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeInt(value);
                    this.mRemote.transact(Stub.TRANSACTION_setSettingOverMarkDay, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingUnlockScreen(String imsi, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeInt(value);
                    this.mRemote.transact(Stub.TRANSACTION_setSettingUnlockScreen, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingNotify(String imsi, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeInt(value);
                    this.mRemote.transact(Stub.TRANSACTION_setSettingNotify, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSettingSpeedNotify(String imsi, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    _data.writeInt(value);
                    this.mRemote.transact(Stub.TRANSACTION_setSettingSpeedNotify, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getSettingTotalPackage(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingTotalPackage, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSettingBeginDate(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingBeginDate, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSettingRegularAdjustType(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingRegularAdjustType, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSettingExcessMontyType(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingExcessMontyType, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSettingOverMarkMonth(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingOverMarkMonth, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSettingOverMarkDay(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingOverMarkDay, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSettingUnlockScreen(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingUnlockScreen, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSettingNotify(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingNotify, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSettingSpeedNotify(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getSettingSpeedNotify, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getAdjustPackageValue(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getAdjustPackageValue, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getAdjustDate(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getAdjustDate, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getAdjustProvince(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getAdjustProvince, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getAdjustCity(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getAdjustCity, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getAdjustProvider(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getAdjustProvider, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getAdjustBrand(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getAdjustBrand, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setNetAccessInfo(int uid, int setNetAccessInfos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(setNetAccessInfos);
                    this.mRemote.transact(Stub.TRANSACTION_setNetAccessInfo, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendAdjustSMS(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_sendAdjustSMS, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getSimCardOperatorName(int slot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slot);
                    this.mRemote.transact(Stub.TRANSACTION_getSimCardOperatorName, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startSpeedUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_startSpeedUpdate, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopSpeedUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_stopSpeedUpdate, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearDailyWarnPreference(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_clearDailyWarnPreference, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearMonthLimitPreference(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_clearMonthLimitPreference, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearMonthWarnPreference(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_clearMonthWarnPreference, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bundle getSimProfileDes() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle bundle;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getSimProfileDes, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        bundle = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return bundle;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getNetworkUsageDays(String imsi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imsi);
                    this.mRemote.transact(Stub.TRANSACTION_getNetworkUsageDays, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
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

        public static INetAssistantService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INetAssistantService)) {
                return new Proxy(obj);
            }
            return (INetAssistantService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            long _result;
            boolean _result2;
            int _result3;
            String _result4;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getMonthMobileTotalBytes(data.readString());
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getMonthlyTotalBytes(data.readString());
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getTodayMobileTotalBytes(data.readString());
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setProvinceInfo(data.readString(), data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setOperatorInfo(data.readString(), data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingTotalPackage(data.readString(), data.readLong());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingBeginDate(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_setSettingRegularAdjustType /*8*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingRegularAdjustType(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_setSettingExcessMontyType /*9*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingExcessMontyType(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_setSettingOverMarkMonth /*10*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingOverMarkMonth(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_setSettingOverMarkDay /*11*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingOverMarkDay(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_setSettingUnlockScreen /*12*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingUnlockScreen(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_setSettingNotify /*13*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingNotify(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_setSettingSpeedNotify /*14*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setSettingSpeedNotify(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_getSettingTotalPackage /*15*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getSettingTotalPackage(data.readString());
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                case TRANSACTION_getSettingBeginDate /*16*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getSettingBeginDate(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case TRANSACTION_getSettingRegularAdjustType /*17*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getSettingRegularAdjustType(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case TRANSACTION_getSettingExcessMontyType /*18*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getSettingExcessMontyType(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case TRANSACTION_getSettingOverMarkMonth /*19*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getSettingOverMarkMonth(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case TRANSACTION_getSettingOverMarkDay /*20*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getSettingOverMarkDay(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case TRANSACTION_getSettingUnlockScreen /*21*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getSettingUnlockScreen(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case TRANSACTION_getSettingNotify /*22*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getSettingNotify(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case TRANSACTION_getSettingSpeedNotify /*23*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getSettingSpeedNotify(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case TRANSACTION_getAdjustPackageValue /*24*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getAdjustPackageValue(data.readString());
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                case TRANSACTION_getAdjustDate /*25*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result = getAdjustDate(data.readString());
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                case TRANSACTION_getAdjustProvince /*26*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getAdjustProvince(data.readString());
                    reply.writeNoException();
                    reply.writeString(_result4);
                    return true;
                case TRANSACTION_getAdjustCity /*27*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getAdjustCity(data.readString());
                    reply.writeNoException();
                    reply.writeString(_result4);
                    return true;
                case TRANSACTION_getAdjustProvider /*28*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getAdjustProvider(data.readString());
                    reply.writeNoException();
                    reply.writeString(_result4);
                    return true;
                case TRANSACTION_getAdjustBrand /*29*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getAdjustBrand(data.readString());
                    reply.writeNoException();
                    reply.writeString(_result4);
                    return true;
                case TRANSACTION_setNetAccessInfo /*30*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result2 = setNetAccessInfo(data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case TRANSACTION_sendAdjustSMS /*31*/:
                    data.enforceInterface(DESCRIPTOR);
                    sendAdjustSMS(data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_getSimCardOperatorName /*32*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result4 = getSimCardOperatorName(data.readInt());
                    reply.writeNoException();
                    reply.writeString(_result4);
                    return true;
                case TRANSACTION_startSpeedUpdate /*33*/:
                    data.enforceInterface(DESCRIPTOR);
                    startSpeedUpdate();
                    reply.writeNoException();
                    return true;
                case TRANSACTION_stopSpeedUpdate /*34*/:
                    data.enforceInterface(DESCRIPTOR);
                    stopSpeedUpdate();
                    reply.writeNoException();
                    return true;
                case TRANSACTION_clearDailyWarnPreference /*35*/:
                    data.enforceInterface(DESCRIPTOR);
                    clearDailyWarnPreference(data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_clearMonthLimitPreference /*36*/:
                    data.enforceInterface(DESCRIPTOR);
                    clearMonthLimitPreference(data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_clearMonthWarnPreference /*37*/:
                    data.enforceInterface(DESCRIPTOR);
                    clearMonthWarnPreference(data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_getSimProfileDes /*38*/:
                    data.enforceInterface(DESCRIPTOR);
                    Bundle _result5 = getSimProfileDes();
                    reply.writeNoException();
                    if (_result5 != null) {
                        reply.writeInt(1);
                        _result5.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case TRANSACTION_getNetworkUsageDays /*39*/:
                    data.enforceInterface(DESCRIPTOR);
                    _result3 = getNetworkUsageDays(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void clearDailyWarnPreference(String str) throws RemoteException;

    void clearMonthLimitPreference(String str) throws RemoteException;

    void clearMonthWarnPreference(String str) throws RemoteException;

    String getAdjustBrand(String str) throws RemoteException;

    String getAdjustCity(String str) throws RemoteException;

    long getAdjustDate(String str) throws RemoteException;

    long getAdjustPackageValue(String str) throws RemoteException;

    String getAdjustProvider(String str) throws RemoteException;

    String getAdjustProvince(String str) throws RemoteException;

    long getMonthMobileTotalBytes(String str) throws RemoteException;

    long getMonthlyTotalBytes(String str) throws RemoteException;

    int getNetworkUsageDays(String str) throws RemoteException;

    int getSettingBeginDate(String str) throws RemoteException;

    int getSettingExcessMontyType(String str) throws RemoteException;

    int getSettingNotify(String str) throws RemoteException;

    int getSettingOverMarkDay(String str) throws RemoteException;

    int getSettingOverMarkMonth(String str) throws RemoteException;

    int getSettingRegularAdjustType(String str) throws RemoteException;

    int getSettingSpeedNotify(String str) throws RemoteException;

    long getSettingTotalPackage(String str) throws RemoteException;

    int getSettingUnlockScreen(String str) throws RemoteException;

    String getSimCardOperatorName(int i) throws RemoteException;

    Bundle getSimProfileDes() throws RemoteException;

    long getTodayMobileTotalBytes(String str) throws RemoteException;

    void sendAdjustSMS(String str) throws RemoteException;

    boolean setNetAccessInfo(int i, int i2) throws RemoteException;

    boolean setOperatorInfo(String str, String str2, String str3) throws RemoteException;

    boolean setProvinceInfo(String str, String str2, String str3) throws RemoteException;

    boolean setSettingBeginDate(String str, int i) throws RemoteException;

    boolean setSettingExcessMontyType(String str, int i) throws RemoteException;

    boolean setSettingNotify(String str, int i) throws RemoteException;

    boolean setSettingOverMarkDay(String str, int i) throws RemoteException;

    boolean setSettingOverMarkMonth(String str, int i) throws RemoteException;

    boolean setSettingRegularAdjustType(String str, int i) throws RemoteException;

    boolean setSettingSpeedNotify(String str, int i) throws RemoteException;

    boolean setSettingTotalPackage(String str, long j) throws RemoteException;

    boolean setSettingUnlockScreen(String str, int i) throws RemoteException;

    void startSpeedUpdate() throws RemoteException;

    void stopSpeedUpdate() throws RemoteException;
}
