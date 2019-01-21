package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ImsiEncryptionInfo;

public interface IPhoneSubInfo extends IInterface {

    public static abstract class Stub extends Binder implements IPhoneSubInfo {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IPhoneSubInfo";
        static final int TRANSACTION_getCarrierInfoForImsiEncryption = 23;
        static final int TRANSACTION_getCompleteVoiceMailNumber = 21;
        static final int TRANSACTION_getCompleteVoiceMailNumberForSubscriber = 22;
        static final int TRANSACTION_getDeviceId = 1;
        static final int TRANSACTION_getDeviceIdForPhone = 4;
        static final int TRANSACTION_getDeviceSvn = 6;
        static final int TRANSACTION_getDeviceSvnUsingSubId = 7;
        static final int TRANSACTION_getGroupIdLevel1ForSubscriber = 10;
        static final int TRANSACTION_getIccSerialNumber = 11;
        static final int TRANSACTION_getIccSerialNumberForSubscriber = 12;
        static final int TRANSACTION_getIccSimChallengeResponse = 33;
        static final int TRANSACTION_getImeiForSubscriber = 5;
        static final int TRANSACTION_getIsimDomain = 29;
        static final int TRANSACTION_getIsimImpi = 28;
        static final int TRANSACTION_getIsimImpu = 30;
        static final int TRANSACTION_getIsimIst = 31;
        static final int TRANSACTION_getIsimPcscf = 32;
        static final int TRANSACTION_getLine1AlphaTag = 15;
        static final int TRANSACTION_getLine1AlphaTagForSubscriber = 16;
        static final int TRANSACTION_getLine1Number = 13;
        static final int TRANSACTION_getLine1NumberForSubscriber = 14;
        static final int TRANSACTION_getMsisdn = 17;
        static final int TRANSACTION_getMsisdnForSubscriber = 18;
        static final int TRANSACTION_getNaiForSubscriber = 2;
        static final int TRANSACTION_getPesn = 3;
        static final int TRANSACTION_getSubscriberId = 8;
        static final int TRANSACTION_getSubscriberIdForSubscriber = 9;
        static final int TRANSACTION_getVoiceMailAlphaTag = 26;
        static final int TRANSACTION_getVoiceMailAlphaTagForSubscriber = 27;
        static final int TRANSACTION_getVoiceMailNumber = 19;
        static final int TRANSACTION_getVoiceMailNumberForSubscriber = 20;
        static final int TRANSACTION_resetCarrierKeysForImsiEncryption = 25;
        static final int TRANSACTION_setCarrierInfoForImsiEncryption = 24;

        private static class Proxy implements IPhoneSubInfo {
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

            public String getDeviceId(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getNaiForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getPesn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getDeviceIdForPhone(int phoneId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getImeiForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getDeviceSvn(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getDeviceSvnUsingSubId(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getSubscriberId(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getSubscriberIdForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getGroupIdLevel1ForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getIccSerialNumber(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getIccSerialNumberForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getLine1Number(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getLine1NumberForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getLine1AlphaTag(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getLine1AlphaTagForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getMsisdn(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getMsisdnForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getVoiceMailNumber(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getVoiceMailNumberForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCompleteVoiceMailNumber() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCompleteVoiceMailNumberForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int subId, int keyType, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ImsiEncryptionInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(keyType);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ImsiEncryptionInfo) ImsiEncryptionInfo.CREATOR.createFromParcel(_reply);
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

            public void setCarrierInfoForImsiEncryption(int subId, String callingPackage, ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    if (imsiEncryptionInfo != null) {
                        _data.writeInt(1);
                        imsiEncryptionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void resetCarrierKeysForImsiEncryption(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getVoiceMailAlphaTag(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getVoiceMailAlphaTagForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getIsimImpi(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getIsimDomain(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getIsimImpu(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getIsimIst(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getIsimPcscf(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getIccSimChallengeResponse(int subId, int appType, int authType, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    _data.writeInt(authType);
                    _data.writeString(data);
                    this.mRemote.transact(33, _data, _reply, 0);
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

        public static IPhoneSubInfo asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IPhoneSubInfo)) {
                return new Proxy(obj);
            }
            return (IPhoneSubInfo) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                String _result;
                String _result2;
                String _result3;
                String[] _result4;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _result = getDeviceId(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _result2 = getNaiForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _result3 = getPesn();
                        reply.writeNoException();
                        reply.writeString(_result3);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _result2 = getDeviceIdForPhone(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _result2 = getImeiForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        _result = getDeviceSvn(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        _result2 = getDeviceSvnUsingSubId(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        _result = getSubscriberId(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        _result2 = getSubscriberIdForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        _result2 = getGroupIdLevel1ForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        _result = getIccSerialNumber(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        _result2 = getIccSerialNumberForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        _result = getLine1Number(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        _result2 = getLine1NumberForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        _result = getLine1AlphaTag(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        _result2 = getLine1AlphaTagForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 17:
                        data.enforceInterface(descriptor);
                        _result = getMsisdn(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 18:
                        data.enforceInterface(descriptor);
                        _result2 = getMsisdnForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 19:
                        data.enforceInterface(descriptor);
                        _result = getVoiceMailNumber(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 20:
                        data.enforceInterface(descriptor);
                        _result2 = getVoiceMailNumberForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 21:
                        data.enforceInterface(descriptor);
                        _result3 = getCompleteVoiceMailNumber();
                        reply.writeNoException();
                        reply.writeString(_result3);
                        return true;
                    case 22:
                        data.enforceInterface(descriptor);
                        _result = getCompleteVoiceMailNumberForSubscriber(data.readInt());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 23:
                        data.enforceInterface(descriptor);
                        ImsiEncryptionInfo _result5 = getCarrierInfoForImsiEncryption(data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        if (_result5 != null) {
                            reply.writeInt(1);
                            _result5.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 24:
                        ImsiEncryptionInfo _arg2;
                        data.enforceInterface(descriptor);
                        int _arg0 = data.readInt();
                        _result = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (ImsiEncryptionInfo) ImsiEncryptionInfo.CREATOR.createFromParcel(data);
                        } else {
                            _arg2 = null;
                        }
                        setCarrierInfoForImsiEncryption(_arg0, _result, _arg2);
                        reply.writeNoException();
                        return true;
                    case 25:
                        data.enforceInterface(descriptor);
                        resetCarrierKeysForImsiEncryption(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 26:
                        data.enforceInterface(descriptor);
                        _result = getVoiceMailAlphaTag(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 27:
                        data.enforceInterface(descriptor);
                        _result2 = getVoiceMailAlphaTagForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result2);
                        return true;
                    case 28:
                        data.enforceInterface(descriptor);
                        _result = getIsimImpi(data.readInt());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 29:
                        data.enforceInterface(descriptor);
                        _result = getIsimDomain(data.readInt());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 30:
                        data.enforceInterface(descriptor);
                        _result4 = getIsimImpu(data.readInt());
                        reply.writeNoException();
                        reply.writeStringArray(_result4);
                        return true;
                    case 31:
                        data.enforceInterface(descriptor);
                        _result = getIsimIst(data.readInt());
                        reply.writeNoException();
                        reply.writeString(_result);
                        return true;
                    case 32:
                        data.enforceInterface(descriptor);
                        _result4 = getIsimPcscf(data.readInt());
                        reply.writeNoException();
                        reply.writeStringArray(_result4);
                        return true;
                    case 33:
                        data.enforceInterface(descriptor);
                        String _result6 = getIccSimChallengeResponse(data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeString(_result6);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int i, int i2, String str) throws RemoteException;

    String getCompleteVoiceMailNumber() throws RemoteException;

    String getCompleteVoiceMailNumberForSubscriber(int i) throws RemoteException;

    String getDeviceId(String str) throws RemoteException;

    String getDeviceIdForPhone(int i, String str) throws RemoteException;

    String getDeviceSvn(String str) throws RemoteException;

    String getDeviceSvnUsingSubId(int i, String str) throws RemoteException;

    String getGroupIdLevel1ForSubscriber(int i, String str) throws RemoteException;

    String getIccSerialNumber(String str) throws RemoteException;

    String getIccSerialNumberForSubscriber(int i, String str) throws RemoteException;

    String getIccSimChallengeResponse(int i, int i2, int i3, String str) throws RemoteException;

    String getImeiForSubscriber(int i, String str) throws RemoteException;

    String getIsimDomain(int i) throws RemoteException;

    String getIsimImpi(int i) throws RemoteException;

    String[] getIsimImpu(int i) throws RemoteException;

    String getIsimIst(int i) throws RemoteException;

    String[] getIsimPcscf(int i) throws RemoteException;

    String getLine1AlphaTag(String str) throws RemoteException;

    String getLine1AlphaTagForSubscriber(int i, String str) throws RemoteException;

    String getLine1Number(String str) throws RemoteException;

    String getLine1NumberForSubscriber(int i, String str) throws RemoteException;

    String getMsisdn(String str) throws RemoteException;

    String getMsisdnForSubscriber(int i, String str) throws RemoteException;

    String getNaiForSubscriber(int i, String str) throws RemoteException;

    String getPesn() throws RemoteException;

    String getSubscriberId(String str) throws RemoteException;

    String getSubscriberIdForSubscriber(int i, String str) throws RemoteException;

    String getVoiceMailAlphaTag(String str) throws RemoteException;

    String getVoiceMailAlphaTagForSubscriber(int i, String str) throws RemoteException;

    String getVoiceMailNumber(String str) throws RemoteException;

    String getVoiceMailNumberForSubscriber(int i, String str) throws RemoteException;

    void resetCarrierKeysForImsiEncryption(int i, String str) throws RemoteException;

    void setCarrierInfoForImsiEncryption(int i, String str, ImsiEncryptionInfo imsiEncryptionInfo) throws RemoteException;
}
