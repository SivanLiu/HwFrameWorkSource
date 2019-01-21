package android.service.textclassifier;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection.Request;

public interface ITextClassifierService extends IInterface {

    public static abstract class Stub extends Binder implements ITextClassifierService {
        private static final String DESCRIPTOR = "android.service.textclassifier.ITextClassifierService";
        static final int TRANSACTION_onClassifyText = 2;
        static final int TRANSACTION_onCreateTextClassificationSession = 5;
        static final int TRANSACTION_onDestroyTextClassificationSession = 6;
        static final int TRANSACTION_onGenerateLinks = 3;
        static final int TRANSACTION_onSelectionEvent = 4;
        static final int TRANSACTION_onSuggestSelection = 1;

        private static class Proxy implements ITextClassifierService {
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

            public void onSuggestSelection(TextClassificationSessionId sessionId, Request request, ITextSelectionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionId != null) {
                        _data.writeInt(1);
                        sessionId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onClassifyText(TextClassificationSessionId sessionId, TextClassification.Request request, ITextClassificationCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionId != null) {
                        _data.writeInt(1);
                        sessionId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onGenerateLinks(TextClassificationSessionId sessionId, TextLinks.Request request, ITextLinksCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionId != null) {
                        _data.writeInt(1);
                        sessionId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onSelectionEvent(TextClassificationSessionId sessionId, SelectionEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionId != null) {
                        _data.writeInt(1);
                        sessionId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onCreateTextClassificationSession(TextClassificationContext context, TextClassificationSessionId sessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (context != null) {
                        _data.writeInt(1);
                        context.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sessionId != null) {
                        _data.writeInt(1);
                        sessionId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onDestroyTextClassificationSession(TextClassificationSessionId sessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionId != null) {
                        _data.writeInt(1);
                        sessionId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITextClassifierService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ITextClassifierService)) {
                return new Proxy(obj);
            }
            return (ITextClassifierService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                Request _arg1 = null;
                TextClassificationSessionId _arg0;
                TextClassificationSessionId _arg12;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (TextClassificationSessionId) TextClassificationSessionId.CREATOR.createFromParcel(data);
                        } else {
                            _arg0 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg1 = (Request) Request.CREATOR.createFromParcel(data);
                        }
                        onSuggestSelection(_arg0, _arg1, android.service.textclassifier.ITextSelectionCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 2:
                        TextClassification.Request _arg13;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (TextClassificationSessionId) TextClassificationSessionId.CREATOR.createFromParcel(data);
                        } else {
                            _arg0 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (TextClassification.Request) TextClassification.Request.CREATOR.createFromParcel(data);
                        }
                        onClassifyText(_arg0, _arg13, android.service.textclassifier.ITextClassificationCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 3:
                        TextLinks.Request _arg14;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (TextClassificationSessionId) TextClassificationSessionId.CREATOR.createFromParcel(data);
                        } else {
                            _arg0 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg14 = (TextLinks.Request) TextLinks.Request.CREATOR.createFromParcel(data);
                        }
                        onGenerateLinks(_arg0, _arg14, android.service.textclassifier.ITextLinksCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 4:
                        SelectionEvent _arg15;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (TextClassificationSessionId) TextClassificationSessionId.CREATOR.createFromParcel(data);
                        } else {
                            _arg0 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg15 = (SelectionEvent) SelectionEvent.CREATOR.createFromParcel(data);
                        }
                        onSelectionEvent(_arg0, _arg15);
                        return true;
                    case 5:
                        TextClassificationContext _arg02;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (TextClassificationContext) TextClassificationContext.CREATOR.createFromParcel(data);
                        } else {
                            _arg02 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (TextClassificationSessionId) TextClassificationSessionId.CREATOR.createFromParcel(data);
                        }
                        onCreateTextClassificationSession(_arg02, _arg12);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg12 = (TextClassificationSessionId) TextClassificationSessionId.CREATOR.createFromParcel(data);
                        }
                        onDestroyTextClassificationSession(_arg12);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void onClassifyText(TextClassificationSessionId textClassificationSessionId, TextClassification.Request request, ITextClassificationCallback iTextClassificationCallback) throws RemoteException;

    void onCreateTextClassificationSession(TextClassificationContext textClassificationContext, TextClassificationSessionId textClassificationSessionId) throws RemoteException;

    void onDestroyTextClassificationSession(TextClassificationSessionId textClassificationSessionId) throws RemoteException;

    void onGenerateLinks(TextClassificationSessionId textClassificationSessionId, TextLinks.Request request, ITextLinksCallback iTextLinksCallback) throws RemoteException;

    void onSelectionEvent(TextClassificationSessionId textClassificationSessionId, SelectionEvent selectionEvent) throws RemoteException;

    void onSuggestSelection(TextClassificationSessionId textClassificationSessionId, Request request, ITextSelectionCallback iTextSelectionCallback) throws RemoteException;
}
