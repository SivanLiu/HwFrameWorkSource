package com.android.internal.telecom;

import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.telecom.CallAudioState;
import android.telecom.ConnectionRequest;
import android.telecom.Logging.Session.Info;
import android.telecom.PhoneAccountHandle;

public interface IConnectionService extends IInterface {

    public static abstract class Stub extends Binder implements IConnectionService {
        private static final String DESCRIPTOR = "com.android.internal.telecom.IConnectionService";
        static final int TRANSACTION_abort = 6;
        static final int TRANSACTION_addConnectionServiceAdapter = 1;
        static final int TRANSACTION_answer = 8;
        static final int TRANSACTION_answerVideo = 7;
        static final int TRANSACTION_conference = 19;
        static final int TRANSACTION_connectionServiceFocusGained = 33;
        static final int TRANSACTION_connectionServiceFocusLost = 32;
        static final int TRANSACTION_createConnection = 3;
        static final int TRANSACTION_createConnectionComplete = 4;
        static final int TRANSACTION_createConnectionFailed = 5;
        static final int TRANSACTION_deflect = 9;
        static final int TRANSACTION_disconnect = 12;
        static final int TRANSACTION_handoverComplete = 35;
        static final int TRANSACTION_handoverFailed = 34;
        static final int TRANSACTION_hold = 14;
        static final int TRANSACTION_mergeConference = 21;
        static final int TRANSACTION_onCallAudioStateChanged = 16;
        static final int TRANSACTION_onExtrasChanged = 26;
        static final int TRANSACTION_onPostDialContinue = 23;
        static final int TRANSACTION_playDtmfTone = 17;
        static final int TRANSACTION_pullExternalCall = 24;
        static final int TRANSACTION_reject = 10;
        static final int TRANSACTION_rejectWithMessage = 11;
        static final int TRANSACTION_removeConnectionServiceAdapter = 2;
        static final int TRANSACTION_respondToRttUpgradeRequest = 31;
        static final int TRANSACTION_sendCallEvent = 25;
        static final int TRANSACTION_setActiveSubscription = 28;
        static final int TRANSACTION_setLocalCallHold = 27;
        static final int TRANSACTION_silence = 13;
        static final int TRANSACTION_splitFromConference = 20;
        static final int TRANSACTION_startRtt = 29;
        static final int TRANSACTION_stopDtmfTone = 18;
        static final int TRANSACTION_stopRtt = 30;
        static final int TRANSACTION_swapConference = 22;
        static final int TRANSACTION_unhold = 15;

        private static class Proxy implements IConnectionService {
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

            public void addConnectionServiceAdapter(IConnectionServiceAdapter adapter, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(adapter != null ? adapter.asBinder() : null);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void removeConnectionServiceAdapter(IConnectionServiceAdapter adapter, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(adapter != null ? adapter.asBinder() : null);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void createConnection(PhoneAccountHandle connectionManagerPhoneAccount, String callId, ConnectionRequest request, boolean isIncoming, boolean isUnknown, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (connectionManagerPhoneAccount != null) {
                        _data.writeInt(1);
                        connectionManagerPhoneAccount.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callId);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(isIncoming);
                    _data.writeInt(isUnknown);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void createConnectionComplete(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void createConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, String callId, ConnectionRequest request, boolean isIncoming, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (connectionManagerPhoneAccount != null) {
                        _data.writeInt(1);
                        connectionManagerPhoneAccount.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callId);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(isIncoming);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void abort(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void answerVideo(String callId, int videoState, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(videoState);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void answer(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void deflect(String callId, Uri address, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (address != null) {
                        _data.writeInt(1);
                        address.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void reject(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void rejectWithMessage(String callId, String message, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(message);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void disconnect(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void silence(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void hold(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unhold(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onCallAudioStateChanged(String activeCallId, CallAudioState callAudioState, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(activeCallId);
                    if (callAudioState != null) {
                        _data.writeInt(1);
                        callAudioState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void playDtmfTone(String callId, char digit, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(digit);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void stopDtmfTone(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void conference(String conferenceCallId, String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(conferenceCallId);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void splitFromConference(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void mergeConference(String conferenceCallId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(conferenceCallId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void swapConference(String conferenceCallId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(conferenceCallId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPostDialContinue(String callId, boolean proceed, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(proceed);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void pullExternalCall(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void sendCallEvent(String callId, String event, Bundle extras, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(event);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onExtrasChanged(String callId, Bundle extras, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setLocalCallHold(String callId, int lchState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(lchState);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setActiveSubscription(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void startRtt(String callId, ParcelFileDescriptor fromInCall, ParcelFileDescriptor toInCall, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (fromInCall != null) {
                        _data.writeInt(1);
                        fromInCall.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (toInCall != null) {
                        _data.writeInt(1);
                        toInCall.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void stopRtt(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void respondToRttUpgradeRequest(String callId, ParcelFileDescriptor fromInCall, ParcelFileDescriptor toInCall, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (fromInCall != null) {
                        _data.writeInt(1);
                        fromInCall.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (toInCall != null) {
                        _data.writeInt(1);
                        toInCall.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void connectionServiceFocusLost(Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void connectionServiceFocusGained(Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void handoverFailed(String callId, ConnectionRequest request, int error, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(error);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void handoverComplete(String callId, Info sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (sessionInfo != null) {
                        _data.writeInt(1);
                        sessionInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IConnectionService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IConnectionService)) {
                return new Proxy(obj);
            }
            return (IConnectionService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                boolean _arg1 = false;
                Info _arg12 = null;
                IConnectionServiceAdapter _arg0;
                PhoneAccountHandle _arg02;
                ConnectionRequest _arg2;
                boolean _arg3;
                String _arg03;
                String _arg13;
                ParcelFileDescriptor _arg14;
                ParcelFileDescriptor _arg22;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        _arg0 = com.android.internal.telecom.IConnectionServiceAdapter.Stub.asInterface(parcel.readStrongBinder());
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        addConnectionServiceAdapter(_arg0, _arg12);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _arg0 = com.android.internal.telecom.IConnectionServiceAdapter.Stub.asInterface(parcel.readStrongBinder());
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        removeConnectionServiceAdapter(_arg0, _arg12);
                        return true;
                    case 3:
                        Info _arg5;
                        parcel.enforceInterface(descriptor);
                        if (parcel.readInt() != 0) {
                            _arg02 = (PhoneAccountHandle) PhoneAccountHandle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg02 = null;
                        }
                        String _arg15 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg2 = (ConnectionRequest) ConnectionRequest.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg2 = null;
                        }
                        _arg3 = parcel.readInt() != 0;
                        boolean _arg4 = parcel.readInt() != 0;
                        if (parcel.readInt() != 0) {
                            _arg5 = (Info) Info.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg5 = null;
                        }
                        createConnection(_arg02, _arg15, _arg2, _arg3, _arg4, _arg5);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        createConnectionComplete(_arg03, _arg12);
                        return true;
                    case 5:
                        Info _arg42;
                        parcel.enforceInterface(descriptor);
                        if (parcel.readInt() != 0) {
                            _arg02 = (PhoneAccountHandle) PhoneAccountHandle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg02 = null;
                        }
                        String _arg16 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg2 = (ConnectionRequest) ConnectionRequest.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg2 = null;
                        }
                        _arg3 = parcel.readInt() != 0;
                        if (parcel.readInt() != 0) {
                            _arg42 = (Info) Info.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg42 = null;
                        }
                        createConnectionFailed(_arg02, _arg16, _arg2, _arg3, _arg42);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        abort(_arg03, _arg12);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        int _arg17 = parcel.readInt();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        answerVideo(_arg03, _arg17, _arg12);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        answer(_arg03, _arg12);
                        return true;
                    case 9:
                        Uri _arg18;
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg18 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg18 = null;
                        }
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        deflect(_arg03, _arg18, _arg12);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        reject(_arg03, _arg12);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        _arg13 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        rejectWithMessage(_arg03, _arg13, _arg12);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        disconnect(_arg03, _arg12);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        silence(_arg03, _arg12);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        hold(_arg03, _arg12);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        unhold(_arg03, _arg12);
                        return true;
                    case 16:
                        CallAudioState _arg19;
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg19 = (CallAudioState) CallAudioState.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg19 = null;
                        }
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        onCallAudioStateChanged(_arg03, _arg19, _arg12);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        char _arg110 = (char) parcel.readInt();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        playDtmfTone(_arg03, _arg110, _arg12);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        stopDtmfTone(_arg03, _arg12);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        _arg13 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        conference(_arg03, _arg13, _arg12);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        splitFromConference(_arg03, _arg12);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        mergeConference(_arg03, _arg12);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        swapConference(_arg03, _arg12);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _arg13 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg1 = true;
                        }
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        onPostDialContinue(_arg13, _arg1, _arg12);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        pullExternalCall(_arg03, _arg12);
                        return true;
                    case 25:
                        Bundle _arg23;
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        _arg13 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg23 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg23 = null;
                        }
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        sendCallEvent(_arg03, _arg13, _arg23, _arg12);
                        return true;
                    case 26:
                        Bundle _arg111;
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg111 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg111 = null;
                        }
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        onExtrasChanged(_arg03, _arg111, _arg12);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        setLocalCallHold(parcel.readString(), parcel.readInt());
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        setActiveSubscription(parcel.readString());
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg14 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg14 = null;
                        }
                        if (parcel.readInt() != 0) {
                            _arg22 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        startRtt(_arg03, _arg14, _arg22, _arg12);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        stopRtt(_arg03, _arg12);
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg14 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg14 = null;
                        }
                        if (parcel.readInt() != 0) {
                            _arg22 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        respondToRttUpgradeRequest(_arg03, _arg14, _arg22, _arg12);
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        connectionServiceFocusLost(_arg12);
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        connectionServiceFocusGained(_arg12);
                        return true;
                    case 34:
                        ConnectionRequest _arg112;
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg112 = (ConnectionRequest) ConnectionRequest.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg112 = null;
                        }
                        int _arg24 = parcel.readInt();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        handoverFailed(_arg03, _arg112, _arg24, _arg12);
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        _arg03 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            _arg12 = (Info) Info.CREATOR.createFromParcel(parcel);
                        }
                        handoverComplete(_arg03, _arg12);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void abort(String str, Info info) throws RemoteException;

    void addConnectionServiceAdapter(IConnectionServiceAdapter iConnectionServiceAdapter, Info info) throws RemoteException;

    void answer(String str, Info info) throws RemoteException;

    void answerVideo(String str, int i, Info info) throws RemoteException;

    void conference(String str, String str2, Info info) throws RemoteException;

    void connectionServiceFocusGained(Info info) throws RemoteException;

    void connectionServiceFocusLost(Info info) throws RemoteException;

    void createConnection(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, boolean z, boolean z2, Info info) throws RemoteException;

    void createConnectionComplete(String str, Info info) throws RemoteException;

    void createConnectionFailed(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, boolean z, Info info) throws RemoteException;

    void deflect(String str, Uri uri, Info info) throws RemoteException;

    void disconnect(String str, Info info) throws RemoteException;

    void handoverComplete(String str, Info info) throws RemoteException;

    void handoverFailed(String str, ConnectionRequest connectionRequest, int i, Info info) throws RemoteException;

    void hold(String str, Info info) throws RemoteException;

    void mergeConference(String str, Info info) throws RemoteException;

    void onCallAudioStateChanged(String str, CallAudioState callAudioState, Info info) throws RemoteException;

    void onExtrasChanged(String str, Bundle bundle, Info info) throws RemoteException;

    void onPostDialContinue(String str, boolean z, Info info) throws RemoteException;

    void playDtmfTone(String str, char c, Info info) throws RemoteException;

    void pullExternalCall(String str, Info info) throws RemoteException;

    void reject(String str, Info info) throws RemoteException;

    void rejectWithMessage(String str, String str2, Info info) throws RemoteException;

    void removeConnectionServiceAdapter(IConnectionServiceAdapter iConnectionServiceAdapter, Info info) throws RemoteException;

    void respondToRttUpgradeRequest(String str, ParcelFileDescriptor parcelFileDescriptor, ParcelFileDescriptor parcelFileDescriptor2, Info info) throws RemoteException;

    void sendCallEvent(String str, String str2, Bundle bundle, Info info) throws RemoteException;

    void setActiveSubscription(String str) throws RemoteException;

    void setLocalCallHold(String str, int i) throws RemoteException;

    void silence(String str, Info info) throws RemoteException;

    void splitFromConference(String str, Info info) throws RemoteException;

    void startRtt(String str, ParcelFileDescriptor parcelFileDescriptor, ParcelFileDescriptor parcelFileDescriptor2, Info info) throws RemoteException;

    void stopDtmfTone(String str, Info info) throws RemoteException;

    void stopRtt(String str, Info info) throws RemoteException;

    void swapConference(String str, Info info) throws RemoteException;

    void unhold(String str, Info info) throws RemoteException;
}
