package com.android.internal.telephony;

import android.app.PendingIntent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface ISms extends IInterface {

    public static abstract class Stub extends Binder implements ISms {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ISms";
        static final int TRANSACTION_copyMessageToIccEfForSubscriber = 3;
        static final int TRANSACTION_createAppSpecificSmsToken = 29;
        static final int TRANSACTION_disableCellBroadcastForSubscriber = 13;
        static final int TRANSACTION_disableCellBroadcastRangeForSubscriber = 15;
        static final int TRANSACTION_enableCellBroadcastForSubscriber = 12;
        static final int TRANSACTION_enableCellBroadcastRangeForSubscriber = 14;
        static final int TRANSACTION_getAllMessagesFromIccEfForSubscriber = 1;
        static final int TRANSACTION_getImsSmsFormatForSubscriber = 25;
        static final int TRANSACTION_getMeidOrPesn = 35;
        static final int TRANSACTION_getPreferredSmsSubscription = 24;
        static final int TRANSACTION_getPremiumSmsPermission = 16;
        static final int TRANSACTION_getPremiumSmsPermissionForSubscriber = 17;
        static final int TRANSACTION_getSmscAddr = 30;
        static final int TRANSACTION_getSmscAddrForSubscriber = 32;
        static final int TRANSACTION_injectSmsPduForSubscriber = 9;
        static final int TRANSACTION_isImsSmsSupportedForSubscriber = 20;
        static final int TRANSACTION_isSMSPromptEnabled = 26;
        static final int TRANSACTION_isSmsSimPickActivityNeeded = 21;
        static final int TRANSACTION_isUimSupportMeid = 34;
        static final int TRANSACTION_sendDataForSubscriber = 4;
        static final int TRANSACTION_sendDataForSubscriberWithSelfPermissions = 5;
        static final int TRANSACTION_sendMultipartTextForSubscriber = 10;
        static final int TRANSACTION_sendMultipartTextForSubscriberWithOptions = 11;
        static final int TRANSACTION_sendStoredMultipartText = 28;
        static final int TRANSACTION_sendStoredText = 27;
        static final int TRANSACTION_sendTextForSubscriber = 6;
        static final int TRANSACTION_sendTextForSubscriberWithOptions = 8;
        static final int TRANSACTION_sendTextForSubscriberWithSelfPermissions = 7;
        static final int TRANSACTION_setCellBroadcastRangeListForSubscriber = 37;
        static final int TRANSACTION_setEnabledSingleShiftTables = 22;
        static final int TRANSACTION_setMeidOrPesn = 36;
        static final int TRANSACTION_setPremiumSmsPermission = 18;
        static final int TRANSACTION_setPremiumSmsPermissionForSubscriber = 19;
        static final int TRANSACTION_setSmsCodingNationalCode = 23;
        static final int TRANSACTION_setSmscAddr = 31;
        static final int TRANSACTION_setSmscAddrForSubscriber = 33;
        static final int TRANSACTION_updateMessageOnIccEfForSubscriber = 2;

        private static class Proxy implements ISms {
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

            public List<SmsRawData> getAllMessagesFromIccEfForSubscriber(int subId, String callingPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<SmsRawData> _result = _reply.createTypedArrayList(SmsRawData.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean updateMessageOnIccEfForSubscriber(int subId, String callingPkg, int messageIndex, int newStatus, byte[] pdu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeInt(messageIndex);
                    _data.writeInt(newStatus);
                    _data.writeByteArray(pdu);
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

            public boolean copyMessageToIccEfForSubscriber(int subId, String callingPkg, int status, byte[] pdu, byte[] smsc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeInt(status);
                    _data.writeByteArray(pdu);
                    _data.writeByteArray(smsc);
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

            public void sendDataForSubscriber(int subId, String callingPkg, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(destAddr);
                    _data.writeString(scAddr);
                    _data.writeInt(destPort);
                    _data.writeByteArray(data);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendDataForSubscriberWithSelfPermissions(int subId, String callingPkg, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(destAddr);
                    _data.writeString(scAddr);
                    _data.writeInt(destPort);
                    _data.writeByteArray(data);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendTextForSubscriber(int subId, String callingPkg, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(destAddr);
                    _data.writeString(scAddr);
                    _data.writeString(text);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(persistMessageForNonDefaultSmsApp);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendTextForSubscriberWithSelfPermissions(int subId, String callingPkg, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(destAddr);
                    _data.writeString(scAddr);
                    _data.writeString(text);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(persistMessage);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendTextForSubscriberWithOptions(int subId, String callingPkg, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod) throws RemoteException {
                Throwable th;
                String str;
                String str2;
                boolean z;
                int i;
                boolean z2;
                int i2;
                String str3;
                String str4;
                PendingIntent pendingIntent = sentIntent;
                PendingIntent pendingIntent2 = deliveryIntent;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeInt(subId);
                        try {
                            _data.writeString(callingPkg);
                            try {
                                _data.writeString(destAddr);
                            } catch (Throwable th2) {
                                th = th2;
                                str = scAddr;
                                str2 = text;
                                z = persistMessageForNonDefaultSmsApp;
                                i = priority;
                                z2 = expectMore;
                                i2 = validityPeriod;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            str3 = destAddr;
                            str = scAddr;
                            str2 = text;
                            z = persistMessageForNonDefaultSmsApp;
                            i = priority;
                            z2 = expectMore;
                            i2 = validityPeriod;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        str4 = callingPkg;
                        str3 = destAddr;
                        str = scAddr;
                        str2 = text;
                        z = persistMessageForNonDefaultSmsApp;
                        i = priority;
                        z2 = expectMore;
                        i2 = validityPeriod;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(scAddr);
                        try {
                            _data.writeString(text);
                            if (pendingIntent != null) {
                                _data.writeInt(1);
                                pendingIntent.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            if (pendingIntent2 != null) {
                                _data.writeInt(1);
                                pendingIntent2.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            z = persistMessageForNonDefaultSmsApp;
                            i = priority;
                            z2 = expectMore;
                            i2 = validityPeriod;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeInt(persistMessageForNonDefaultSmsApp);
                            try {
                                _data.writeInt(priority);
                            } catch (Throwable th6) {
                                th = th6;
                                z2 = expectMore;
                                i2 = validityPeriod;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            i = priority;
                            z2 = expectMore;
                            i2 = validityPeriod;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        str2 = text;
                        z = persistMessageForNonDefaultSmsApp;
                        i = priority;
                        z2 = expectMore;
                        i2 = validityPeriod;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(expectMore);
                        try {
                            _data.writeInt(validityPeriod);
                            try {
                                this.mRemote.transact(8, _data, _reply, 0);
                                _reply.readException();
                                _reply.recycle();
                                _data.recycle();
                            } catch (Throwable th9) {
                                th = th9;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        i2 = validityPeriod;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                    int i3 = subId;
                    str4 = callingPkg;
                    str3 = destAddr;
                    str = scAddr;
                    str2 = text;
                    z = persistMessageForNonDefaultSmsApp;
                    i = priority;
                    z2 = expectMore;
                    i2 = validityPeriod;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public void injectSmsPduForSubscriber(int subId, byte[] pdu, String format, PendingIntent receivedIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeByteArray(pdu);
                    _data.writeString(format);
                    if (receivedIntent != null) {
                        _data.writeInt(1);
                        receivedIntent.writeToParcel(_data, 0);
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

            public void sendMultipartTextForSubscriber(int subId, String callingPkg, String destinationAddress, String scAddress, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(destinationAddress);
                    _data.writeString(scAddress);
                    _data.writeStringList(parts);
                    _data.writeTypedList(sentIntents);
                    _data.writeTypedList(deliveryIntents);
                    _data.writeInt(persistMessageForNonDefaultSmsApp);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendMultipartTextForSubscriberWithOptions(int subId, String callingPkg, String destinationAddress, String scAddress, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp, int priority, boolean expectMore, int validityPeriod) throws RemoteException {
                Throwable th;
                List<String> list;
                List<PendingIntent> list2;
                List<PendingIntent> list3;
                boolean z;
                int i;
                boolean z2;
                int i2;
                String str;
                String str2;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    try {
                        _data.writeString(callingPkg);
                        try {
                            _data.writeString(destinationAddress);
                            try {
                                _data.writeString(scAddress);
                            } catch (Throwable th2) {
                                th = th2;
                                list = parts;
                                list2 = sentIntents;
                                list3 = deliveryIntents;
                                z = persistMessageForNonDefaultSmsApp;
                                i = priority;
                                z2 = expectMore;
                                i2 = validityPeriod;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            str = scAddress;
                            list = parts;
                            list2 = sentIntents;
                            list3 = deliveryIntents;
                            z = persistMessageForNonDefaultSmsApp;
                            i = priority;
                            z2 = expectMore;
                            i2 = validityPeriod;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        str2 = destinationAddress;
                        str = scAddress;
                        list = parts;
                        list2 = sentIntents;
                        list3 = deliveryIntents;
                        z = persistMessageForNonDefaultSmsApp;
                        i = priority;
                        z2 = expectMore;
                        i2 = validityPeriod;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeStringList(parts);
                        try {
                            _data.writeTypedList(sentIntents);
                            try {
                                _data.writeTypedList(deliveryIntents);
                            } catch (Throwable th5) {
                                th = th5;
                                z = persistMessageForNonDefaultSmsApp;
                                i = priority;
                                z2 = expectMore;
                                i2 = validityPeriod;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            list3 = deliveryIntents;
                            z = persistMessageForNonDefaultSmsApp;
                            i = priority;
                            z2 = expectMore;
                            i2 = validityPeriod;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        list2 = sentIntents;
                        list3 = deliveryIntents;
                        z = persistMessageForNonDefaultSmsApp;
                        i = priority;
                        z2 = expectMore;
                        i2 = validityPeriod;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(persistMessageForNonDefaultSmsApp);
                        try {
                            _data.writeInt(priority);
                            try {
                                _data.writeInt(expectMore);
                            } catch (Throwable th8) {
                                th = th8;
                                i2 = validityPeriod;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            z2 = expectMore;
                            i2 = validityPeriod;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        i = priority;
                        z2 = expectMore;
                        i2 = validityPeriod;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(validityPeriod);
                        try {
                            this.mRemote.transact(11, _data, _reply, 0);
                            _reply.readException();
                            _reply.recycle();
                            _data.recycle();
                        } catch (Throwable th11) {
                            th = th11;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th12) {
                        th = th12;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th13) {
                    th = th13;
                    String str3 = callingPkg;
                    str2 = destinationAddress;
                    str = scAddress;
                    list = parts;
                    list2 = sentIntents;
                    list3 = deliveryIntents;
                    z = persistMessageForNonDefaultSmsApp;
                    i = priority;
                    z2 = expectMore;
                    i2 = validityPeriod;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public boolean enableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(messageIdentifier);
                    _data.writeInt(ranType);
                    boolean z = false;
                    this.mRemote.transact(12, _data, _reply, 0);
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

            public boolean disableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(messageIdentifier);
                    _data.writeInt(ranType);
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

            public boolean enableCellBroadcastRangeForSubscriber(int subId, int startMessageId, int endMessageId, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(startMessageId);
                    _data.writeInt(endMessageId);
                    _data.writeInt(ranType);
                    boolean z = false;
                    this.mRemote.transact(14, _data, _reply, 0);
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

            public boolean disableCellBroadcastRangeForSubscriber(int subId, int startMessageId, int endMessageId, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(startMessageId);
                    _data.writeInt(endMessageId);
                    _data.writeInt(ranType);
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

            public int getPremiumSmsPermission(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPremiumSmsPermissionForSubscriber(int subId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(packageName);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPremiumSmsPermission(String packageName, int permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(permission);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPremiumSmsPermissionForSubscriber(int subId, String packageName, int permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(packageName);
                    _data.writeInt(permission);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isImsSmsSupportedForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(20, _data, _reply, 0);
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

            public boolean isSmsSimPickActivityNeeded(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(21, _data, _reply, 0);
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

            public void setEnabledSingleShiftTables(int[] tables) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(tables);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSmsCodingNationalCode(String code) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(code);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPreferredSmsSubscription() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getImsSmsFormatForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSMSPromptEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public void sendStoredText(int subId, String callingPkg, Uri messageUri, String scAddress, PendingIntent sentIntent, PendingIntent deliveryIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    if (messageUri != null) {
                        _data.writeInt(1);
                        messageUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(scAddress);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
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

            public void sendStoredMultipartText(int subId, String callingPkg, Uri messageUri, String scAddress, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    if (messageUri != null) {
                        _data.writeInt(1);
                        messageUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(scAddress);
                    _data.writeTypedList(sentIntents);
                    _data.writeTypedList(deliveryIntents);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String createAppSpecificSmsToken(int subId, String callingPkg, PendingIntent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getSmscAddr() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSmscAddr(String smscAddr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(smscAddr);
                    boolean z = false;
                    this.mRemote.transact(31, _data, _reply, 0);
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

            public String getSmscAddrForSubscriber(long subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(subId);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSmscAddrForSubscriber(long subId, String smscAddr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(subId);
                    _data.writeString(smscAddr);
                    boolean z = false;
                    this.mRemote.transact(33, _data, _reply, 0);
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

            public boolean isUimSupportMeid(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(34, _data, _reply, 0);
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

            public String getMeidOrPesn(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setMeidOrPesn(int subId, String meid, String pesn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(meid);
                    _data.writeString(pesn);
                    boolean z = false;
                    this.mRemote.transact(36, _data, _reply, 0);
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

            public boolean setCellBroadcastRangeListForSubscriber(int subId, int[] messageIds, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeIntArray(messageIds);
                    _data.writeInt(ranType);
                    boolean z = false;
                    this.mRemote.transact(37, _data, _reply, 0);
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
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISms asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISms)) {
                return new Proxy(obj);
            }
            return (ISms) iin;
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
                PendingIntent _arg3 = null;
                boolean _result;
                int _arg0;
                String _arg1;
                String _arg32;
                int _arg4;
                byte[] _arg5;
                PendingIntent _arg6;
                PendingIntent _arg7;
                String _arg42;
                PendingIntent _arg52;
                int _arg02;
                String _arg2;
                boolean _result2;
                boolean _result3;
                boolean _result4;
                String _result5;
                int _arg03;
                String _arg12;
                Uri _arg22;
                String _arg13;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        List<SmsRawData> _result6 = getAllMessagesFromIccEfForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result6);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _result = updateMessageOnIccEfForSubscriber(data.readInt(), data.readString(), data.readInt(), data.readInt(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result = copyMessageToIccEfForSubscriber(data.readInt(), data.readString(), data.readInt(), data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg1 = data.readString();
                        descriptor = data.readString();
                        _arg32 = data.readString();
                        _arg4 = data.readInt();
                        _arg5 = data.createByteArray();
                        if (data.readInt() != 0) {
                            _arg6 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg6 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg7 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg7 = null;
                        }
                        sendDataForSubscriber(_arg0, _arg1, descriptor, _arg32, _arg4, _arg5, _arg6, _arg7);
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg1 = data.readString();
                        descriptor = data.readString();
                        _arg32 = data.readString();
                        _arg4 = data.readInt();
                        _arg5 = data.createByteArray();
                        if (data.readInt() != 0) {
                            _arg6 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg6 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg7 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg7 = null;
                        }
                        sendDataForSubscriberWithSelfPermissions(_arg0, _arg1, descriptor, _arg32, _arg4, _arg5, _arg6, _arg7);
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg1 = data.readString();
                        descriptor = data.readString();
                        _arg32 = data.readString();
                        _arg42 = data.readString();
                        if (data.readInt() != 0) {
                            _arg52 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg52 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        sendTextForSubscriber(_arg0, _arg1, descriptor, _arg32, _arg42, _arg52, _arg3, data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg1 = data.readString();
                        descriptor = data.readString();
                        _arg32 = data.readString();
                        _arg42 = data.readString();
                        if (data.readInt() != 0) {
                            _arg52 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg52 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        sendTextForSubscriberWithSelfPermissions(_arg0, _arg1, descriptor, _arg32, _arg42, _arg52, _arg3, data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        int _arg04 = data.readInt();
                        _arg42 = data.readString();
                        String _arg23 = data.readString();
                        String _arg33 = data.readString();
                        String _arg43 = data.readString();
                        if (data.readInt() != 0) {
                            _arg52 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg52 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        sendTextForSubscriberWithOptions(_arg04, _arg42, _arg23, _arg33, _arg43, _arg52, _arg3, data.readInt() != 0, data.readInt(), data.readInt() != 0, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        byte[] _arg14 = data.createByteArray();
                        _arg2 = data.readString();
                        if (data.readInt() != 0) {
                            _arg3 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        injectSmsPduForSubscriber(_arg02, _arg14, _arg2, _arg3);
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        sendMultipartTextForSubscriber(data.readInt(), data.readString(), data.readString(), data.readString(), data.createStringArrayList(), parcel.createTypedArrayList(PendingIntent.CREATOR), parcel.createTypedArrayList(PendingIntent.CREATOR), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        sendMultipartTextForSubscriberWithOptions(data.readInt(), data.readString(), data.readString(), data.readString(), data.createStringArrayList(), parcel.createTypedArrayList(PendingIntent.CREATOR), parcel.createTypedArrayList(PendingIntent.CREATOR), data.readInt() != 0, data.readInt(), data.readInt() != 0, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result2 = enableCellBroadcastForSubscriber(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _result2 = disableCellBroadcastForSubscriber(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result3 = enableCellBroadcastRangeForSubscriber(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _result3 = disableCellBroadcastRangeForSubscriber(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        int _result7 = getPremiumSmsPermission(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        int _result8 = getPremiumSmsPermissionForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        setPremiumSmsPermission(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        setPremiumSmsPermissionForSubscriber(data.readInt(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _result4 = isImsSmsSupportedForSubscriber(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _result4 = isSmsSimPickActivityNeeded(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        setEnabledSingleShiftTables(data.createIntArray());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        setSmsCodingNationalCode(data.readString());
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getPreferredSmsSubscription();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _result5 = getImsSmsFormatForSubscriber(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        _result = isSMSPromptEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 27:
                        PendingIntent _arg44;
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readInt();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        String _arg34 = data.readString();
                        if (data.readInt() != 0) {
                            _arg44 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg44 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg52 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg52 = null;
                        }
                        sendStoredText(_arg03, _arg12, _arg22, _arg34, _arg44, _arg52);
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readInt();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        sendStoredMultipartText(_arg03, _arg12, _arg22, data.readString(), parcel.createTypedArrayList(PendingIntent.CREATOR), parcel.createTypedArrayList(PendingIntent.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        _arg13 = data.readString();
                        if (data.readInt() != 0) {
                            _arg3 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        _arg2 = createAppSpecificSmsToken(_arg02, _arg13, _arg3);
                        reply.writeNoException();
                        parcel2.writeString(_arg2);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        String _result9 = getSmscAddr();
                        reply.writeNoException();
                        parcel2.writeString(_result9);
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        _result4 = setSmscAddr(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        _arg13 = getSmscAddrForSubscriber(data.readLong());
                        reply.writeNoException();
                        parcel2.writeString(_arg13);
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        _result2 = setSmscAddrForSubscriber(data.readLong(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        _result4 = isUimSupportMeid(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        _result5 = getMeidOrPesn(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        _result2 = setMeidOrPesn(data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        _result2 = setCellBroadcastRangeListForSubscriber(data.readInt(), data.createIntArray(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    boolean copyMessageToIccEfForSubscriber(int i, String str, int i2, byte[] bArr, byte[] bArr2) throws RemoteException;

    String createAppSpecificSmsToken(int i, String str, PendingIntent pendingIntent) throws RemoteException;

    boolean disableCellBroadcastForSubscriber(int i, int i2, int i3) throws RemoteException;

    boolean disableCellBroadcastRangeForSubscriber(int i, int i2, int i3, int i4) throws RemoteException;

    boolean enableCellBroadcastForSubscriber(int i, int i2, int i3) throws RemoteException;

    boolean enableCellBroadcastRangeForSubscriber(int i, int i2, int i3, int i4) throws RemoteException;

    List<SmsRawData> getAllMessagesFromIccEfForSubscriber(int i, String str) throws RemoteException;

    String getImsSmsFormatForSubscriber(int i) throws RemoteException;

    String getMeidOrPesn(int i) throws RemoteException;

    int getPreferredSmsSubscription() throws RemoteException;

    int getPremiumSmsPermission(String str) throws RemoteException;

    int getPremiumSmsPermissionForSubscriber(int i, String str) throws RemoteException;

    String getSmscAddr() throws RemoteException;

    String getSmscAddrForSubscriber(long j) throws RemoteException;

    void injectSmsPduForSubscriber(int i, byte[] bArr, String str, PendingIntent pendingIntent) throws RemoteException;

    boolean isImsSmsSupportedForSubscriber(int i) throws RemoteException;

    boolean isSMSPromptEnabled() throws RemoteException;

    boolean isSmsSimPickActivityNeeded(int i) throws RemoteException;

    boolean isUimSupportMeid(int i) throws RemoteException;

    void sendDataForSubscriber(int i, String str, String str2, String str3, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void sendDataForSubscriberWithSelfPermissions(int i, String str, String str2, String str3, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void sendMultipartTextForSubscriber(int i, String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) throws RemoteException;

    void sendMultipartTextForSubscriberWithOptions(int i, String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z, int i2, boolean z2, int i3) throws RemoteException;

    void sendStoredMultipartText(int i, String str, Uri uri, String str2, List<PendingIntent> list, List<PendingIntent> list2) throws RemoteException;

    void sendStoredText(int i, String str, Uri uri, String str2, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void sendTextForSubscriber(int i, String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) throws RemoteException;

    void sendTextForSubscriberWithOptions(int i, String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, int i2, boolean z2, int i3) throws RemoteException;

    void sendTextForSubscriberWithSelfPermissions(int i, String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) throws RemoteException;

    boolean setCellBroadcastRangeListForSubscriber(int i, int[] iArr, int i2) throws RemoteException;

    void setEnabledSingleShiftTables(int[] iArr) throws RemoteException;

    boolean setMeidOrPesn(int i, String str, String str2) throws RemoteException;

    void setPremiumSmsPermission(String str, int i) throws RemoteException;

    void setPremiumSmsPermissionForSubscriber(int i, String str, int i2) throws RemoteException;

    void setSmsCodingNationalCode(String str) throws RemoteException;

    boolean setSmscAddr(String str) throws RemoteException;

    boolean setSmscAddrForSubscriber(long j, String str) throws RemoteException;

    boolean updateMessageOnIccEfForSubscriber(int i, String str, int i2, int i3, byte[] bArr) throws RemoteException;
}
