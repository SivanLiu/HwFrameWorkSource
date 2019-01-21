package com.android.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsConferenceState;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.ImsSuppServiceNotification;

public interface IImsCallSessionListener extends IInterface {

    public static abstract class Stub extends Binder implements IImsCallSessionListener {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsCallSessionListener";
        static final int TRANSACTION_callSessionConferenceExtendFailed = 18;
        static final int TRANSACTION_callSessionConferenceExtendReceived = 19;
        static final int TRANSACTION_callSessionConferenceExtended = 17;
        static final int TRANSACTION_callSessionConferenceStateUpdated = 24;
        static final int TRANSACTION_callSessionHandover = 26;
        static final int TRANSACTION_callSessionHandoverFailed = 27;
        static final int TRANSACTION_callSessionHeld = 5;
        static final int TRANSACTION_callSessionHoldFailed = 6;
        static final int TRANSACTION_callSessionHoldReceived = 7;
        static final int TRANSACTION_callSessionInviteParticipantsRequestDelivered = 20;
        static final int TRANSACTION_callSessionInviteParticipantsRequestFailed = 21;
        static final int TRANSACTION_callSessionMayHandover = 28;
        static final int TRANSACTION_callSessionMergeComplete = 12;
        static final int TRANSACTION_callSessionMergeFailed = 13;
        static final int TRANSACTION_callSessionMergeStarted = 11;
        static final int TRANSACTION_callSessionMultipartyStateChanged = 30;
        static final int TRANSACTION_callSessionProgressing = 1;
        static final int TRANSACTION_callSessionRemoveParticipantsRequestDelivered = 22;
        static final int TRANSACTION_callSessionRemoveParticipantsRequestFailed = 23;
        static final int TRANSACTION_callSessionResumeFailed = 9;
        static final int TRANSACTION_callSessionResumeReceived = 10;
        static final int TRANSACTION_callSessionResumed = 8;
        static final int TRANSACTION_callSessionRttMessageReceived = 34;
        static final int TRANSACTION_callSessionRttModifyRequestReceived = 32;
        static final int TRANSACTION_callSessionRttModifyResponseReceived = 33;
        static final int TRANSACTION_callSessionStartFailed = 3;
        static final int TRANSACTION_callSessionStarted = 2;
        static final int TRANSACTION_callSessionSuppServiceReceived = 31;
        static final int TRANSACTION_callSessionTerminated = 4;
        static final int TRANSACTION_callSessionTtyModeReceived = 29;
        static final int TRANSACTION_callSessionUpdateFailed = 15;
        static final int TRANSACTION_callSessionUpdateReceived = 16;
        static final int TRANSACTION_callSessionUpdated = 14;
        static final int TRANSACTION_callSessionUssdMessageReceived = 25;

        private static class Proxy implements IImsCallSessionListener {
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

            public void callSessionProgressing(IImsCallSession session, ImsStreamMediaProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionStarted(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionStartFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionTerminated(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionHeld(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionHoldFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionHoldReceived(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionResumed(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionResumeFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionResumeReceived(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionMergeStarted(IImsCallSession session, IImsCallSession newSession, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeStrongBinder(newSession != null ? newSession.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionMergeComplete(IImsCallSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionMergeFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionUpdated(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionUpdateFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionUpdateReceived(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionConferenceExtended(IImsCallSession session, IImsCallSession newSession, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeStrongBinder(newSession != null ? newSession.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionConferenceExtendFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionConferenceExtendReceived(IImsCallSession session, IImsCallSession newSession, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeStrongBinder(newSession != null ? newSession.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionInviteParticipantsRequestDelivered(IImsCallSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionInviteParticipantsRequestFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionRemoveParticipantsRequestDelivered(IImsCallSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionRemoveParticipantsRequestFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionConferenceStateUpdated(IImsCallSession session, ImsConferenceState state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (state != null) {
                        _data.writeInt(1);
                        state.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionUssdMessageReceived(IImsCallSession session, int mode, String ussdMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(mode);
                    _data.writeString(ussdMessage);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionHandover(IImsCallSession session, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(srcAccessTech);
                    _data.writeInt(targetAccessTech);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionHandoverFailed(IImsCallSession session, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(srcAccessTech);
                    _data.writeInt(targetAccessTech);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionMayHandover(IImsCallSession session, int srcAccessTech, int targetAccessTech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(srcAccessTech);
                    _data.writeInt(targetAccessTech);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionTtyModeReceived(IImsCallSession session, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(mode);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionMultipartyStateChanged(IImsCallSession session, boolean isMultiParty) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(isMultiParty);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionSuppServiceReceived(IImsCallSession session, ImsSuppServiceNotification suppSrvNotification) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (suppSrvNotification != null) {
                        _data.writeInt(1);
                        suppSrvNotification.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionRttModifyRequestReceived(IImsCallSession session, ImsCallProfile callProfile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (callProfile != null) {
                        _data.writeInt(1);
                        callProfile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionRttModifyResponseReceived(int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void callSessionRttMessageReceived(String rttMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(rttMessage);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsCallSessionListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IImsCallSessionListener)) {
                return new Proxy(obj);
            }
            return (IImsCallSessionListener) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                ImsStreamMediaProfile _arg1 = null;
                IImsCallSession _arg0;
                ImsCallProfile _arg12;
                ImsReasonInfo _arg13;
                IImsCallSession _arg14;
                int _arg15;
                int _arg2;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg1 = (ImsStreamMediaProfile) ImsStreamMediaProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionProgressing(_arg0, _arg1);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionStarted(_arg0, _arg12);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionStartFailed(_arg0, _arg13);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionTerminated(_arg0, _arg13);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionHeld(_arg0, _arg12);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionHoldFailed(_arg0, _arg13);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionHoldReceived(_arg0, _arg12);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionResumed(_arg0, _arg12);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionResumeFailed(_arg0, _arg13);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionResumeReceived(_arg0, _arg12);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        _arg14 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionMergeStarted(_arg0, _arg14, _arg12);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        callSessionMergeComplete(com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionMergeFailed(_arg0, _arg13);
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionUpdated(_arg0, _arg12);
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionUpdateFailed(_arg0, _arg13);
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionUpdateReceived(_arg0, _arg12);
                        return true;
                    case 17:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        _arg14 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionConferenceExtended(_arg0, _arg14, _arg12);
                        return true;
                    case 18:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionConferenceExtendFailed(_arg0, _arg13);
                        return true;
                    case 19:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        _arg14 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionConferenceExtendReceived(_arg0, _arg14, _arg12);
                        return true;
                    case 20:
                        data.enforceInterface(descriptor);
                        callSessionInviteParticipantsRequestDelivered(com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 21:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionInviteParticipantsRequestFailed(_arg0, _arg13);
                        return true;
                    case 22:
                        data.enforceInterface(descriptor);
                        callSessionRemoveParticipantsRequestDelivered(com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 23:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionRemoveParticipantsRequestFailed(_arg0, _arg13);
                        return true;
                    case 24:
                        ImsConferenceState _arg16;
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg16 = (ImsConferenceState) ImsConferenceState.CREATOR.createFromParcel(data);
                        }
                        callSessionConferenceStateUpdated(_arg0, _arg16);
                        return true;
                    case 25:
                        data.enforceInterface(descriptor);
                        callSessionUssdMessageReceived(com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readString());
                        return true;
                    case 26:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        _arg15 = data.readInt();
                        _arg2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionHandover(_arg0, _arg15, _arg2, _arg13);
                        return true;
                    case 27:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        _arg15 = data.readInt();
                        _arg2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg13 = (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(data);
                        }
                        callSessionHandoverFailed(_arg0, _arg15, _arg2, _arg13);
                        return true;
                    case 28:
                        data.enforceInterface(descriptor);
                        callSessionMayHandover(com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt());
                        return true;
                    case 29:
                        data.enforceInterface(descriptor);
                        callSessionTtyModeReceived(com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    case 30:
                        data.enforceInterface(descriptor);
                        callSessionMultipartyStateChanged(com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder()), data.readInt() != 0);
                        return true;
                    case 31:
                        ImsSuppServiceNotification _arg17;
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg17 = (ImsSuppServiceNotification) ImsSuppServiceNotification.CREATOR.createFromParcel(data);
                        }
                        callSessionSuppServiceReceived(_arg0, _arg17);
                        return true;
                    case 32:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.ims.internal.IImsCallSession.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(data);
                        }
                        callSessionRttModifyRequestReceived(_arg0, _arg12);
                        return true;
                    case 33:
                        data.enforceInterface(descriptor);
                        callSessionRttModifyResponseReceived(data.readInt());
                        return true;
                    case 34:
                        data.enforceInterface(descriptor);
                        callSessionRttMessageReceived(data.readString());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void callSessionConferenceExtendFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionConferenceExtendReceived(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionConferenceExtended(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionConferenceStateUpdated(IImsCallSession iImsCallSession, ImsConferenceState imsConferenceState) throws RemoteException;

    void callSessionHandover(IImsCallSession iImsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionHandoverFailed(IImsCallSession iImsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionHeld(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionHoldFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionHoldReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionInviteParticipantsRequestDelivered(IImsCallSession iImsCallSession) throws RemoteException;

    void callSessionInviteParticipantsRequestFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionMayHandover(IImsCallSession iImsCallSession, int i, int i2) throws RemoteException;

    void callSessionMergeComplete(IImsCallSession iImsCallSession) throws RemoteException;

    void callSessionMergeFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionMergeStarted(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionMultipartyStateChanged(IImsCallSession iImsCallSession, boolean z) throws RemoteException;

    void callSessionProgressing(IImsCallSession iImsCallSession, ImsStreamMediaProfile imsStreamMediaProfile) throws RemoteException;

    void callSessionRemoveParticipantsRequestDelivered(IImsCallSession iImsCallSession) throws RemoteException;

    void callSessionRemoveParticipantsRequestFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionResumeFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionResumeReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionResumed(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionRttMessageReceived(String str) throws RemoteException;

    void callSessionRttModifyRequestReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionRttModifyResponseReceived(int i) throws RemoteException;

    void callSessionStartFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionStarted(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionSuppServiceReceived(IImsCallSession iImsCallSession, ImsSuppServiceNotification imsSuppServiceNotification) throws RemoteException;

    void callSessionTerminated(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionTtyModeReceived(IImsCallSession iImsCallSession, int i) throws RemoteException;

    void callSessionUpdateFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionUpdateReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionUpdated(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionUssdMessageReceived(IImsCallSession iImsCallSession, int i, String str) throws RemoteException;
}
