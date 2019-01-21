package android.view.autofill;

import android.content.ComponentName;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.autofill.FillEventHistory;
import android.service.autofill.UserData;
import java.util.List;

public interface IAutoFillManager extends IInterface {

    public static abstract class Stub extends Binder implements IAutoFillManager {
        private static final String DESCRIPTOR = "android.view.autofill.IAutoFillManager";
        static final int TRANSACTION_addClient = 1;
        static final int TRANSACTION_cancelSession = 10;
        static final int TRANSACTION_disableOwnedAutofillServices = 13;
        static final int TRANSACTION_finishSession = 9;
        static final int TRANSACTION_getAutofillServiceComponentName = 21;
        static final int TRANSACTION_getAvailableFieldClassificationAlgorithms = 22;
        static final int TRANSACTION_getDefaultFieldClassificationAlgorithm = 23;
        static final int TRANSACTION_getFillEventHistory = 4;
        static final int TRANSACTION_getUserData = 17;
        static final int TRANSACTION_getUserDataId = 18;
        static final int TRANSACTION_isFieldClassificationEnabled = 20;
        static final int TRANSACTION_isServiceEnabled = 15;
        static final int TRANSACTION_isServiceSupported = 14;
        static final int TRANSACTION_onPendingSaveUi = 16;
        static final int TRANSACTION_removeClient = 2;
        static final int TRANSACTION_restoreSession = 5;
        static final int TRANSACTION_setAuthenticationResult = 11;
        static final int TRANSACTION_setAutofillFailure = 8;
        static final int TRANSACTION_setHasCallback = 12;
        static final int TRANSACTION_setUserData = 19;
        static final int TRANSACTION_startSession = 3;
        static final int TRANSACTION_updateOrRestartSession = 7;
        static final int TRANSACTION_updateSession = 6;

        private static class Proxy implements IAutoFillManager {
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

            public int addClient(IAutoFillManagerClient client, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeClient(IAutoFillManagerClient client, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int startSession(IBinder activityToken, IBinder appCallback, AutofillId autoFillId, Rect bounds, AutofillValue value, int userId, boolean hasCallback, int flags, ComponentName componentName, boolean compatMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    _data.writeStrongBinder(appCallback);
                    if (autoFillId != null) {
                        _data.writeInt(1);
                        autoFillId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (bounds != null) {
                        _data.writeInt(1);
                        bounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (value != null) {
                        _data.writeInt(1);
                        value.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    _data.writeInt(hasCallback);
                    _data.writeInt(flags);
                    if (componentName != null) {
                        _data.writeInt(1);
                        componentName.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(compatMode);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public FillEventHistory getFillEventHistory() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    FillEventHistory _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (FillEventHistory) FillEventHistory.CREATOR.createFromParcel(_reply);
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

            public boolean restoreSession(int sessionId, IBinder activityToken, IBinder appCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    _data.writeStrongBinder(activityToken);
                    _data.writeStrongBinder(appCallback);
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

            public void updateSession(int sessionId, AutofillId id, Rect bounds, AutofillValue value, int action, int flags, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    if (id != null) {
                        _data.writeInt(1);
                        id.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (bounds != null) {
                        _data.writeInt(1);
                        bounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (value != null) {
                        _data.writeInt(1);
                        value.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(action);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateOrRestartSession(IBinder activityToken, IBinder appCallback, AutofillId autoFillId, Rect bounds, AutofillValue value, int userId, boolean hasCallback, int flags, ComponentName componentName, int sessionId, int action, boolean compatMode) throws RemoteException {
                Throwable th;
                int i;
                boolean z;
                int i2;
                int i3;
                int i4;
                boolean z2;
                IBinder iBinder;
                AutofillId autofillId = autoFillId;
                Rect rect = bounds;
                AutofillValue autofillValue = value;
                ComponentName componentName2 = componentName;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeStrongBinder(activityToken);
                        try {
                            _data.writeStrongBinder(appCallback);
                            if (autofillId != null) {
                                _data.writeInt(1);
                                autofillId.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            if (rect != null) {
                                _data.writeInt(1);
                                rect.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            if (autofillValue != null) {
                                _data.writeInt(1);
                                autofillValue.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            i = userId;
                            z = hasCallback;
                            i2 = flags;
                            i3 = sessionId;
                            i4 = action;
                            z2 = compatMode;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeInt(userId);
                            try {
                                _data.writeInt(hasCallback);
                            } catch (Throwable th3) {
                                th = th3;
                                i2 = flags;
                                i3 = sessionId;
                                i4 = action;
                                z2 = compatMode;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            z = hasCallback;
                            i2 = flags;
                            i3 = sessionId;
                            i4 = action;
                            z2 = compatMode;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        iBinder = appCallback;
                        i = userId;
                        z = hasCallback;
                        i2 = flags;
                        i3 = sessionId;
                        i4 = action;
                        z2 = compatMode;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(flags);
                        if (componentName2 != null) {
                            _data.writeInt(1);
                            componentName2.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        try {
                            _data.writeInt(sessionId);
                            try {
                                _data.writeInt(action);
                                try {
                                    _data.writeInt(compatMode);
                                } catch (Throwable th6) {
                                    th = th6;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                z2 = compatMode;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            i4 = action;
                            z2 = compatMode;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            this.mRemote.transact(7, _data, _reply, 0);
                            _reply.readException();
                            int _result = _reply.readInt();
                            _reply.recycle();
                            _data.recycle();
                            return _result;
                        } catch (Throwable th9) {
                            th = th9;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        i3 = sessionId;
                        i4 = action;
                        z2 = compatMode;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th11) {
                    th = th11;
                    IBinder iBinder2 = activityToken;
                    iBinder = appCallback;
                    i = userId;
                    z = hasCallback;
                    i2 = flags;
                    i3 = sessionId;
                    i4 = action;
                    z2 = compatMode;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public void setAutofillFailure(int sessionId, List<AutofillId> ids, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    _data.writeTypedList(ids);
                    _data.writeInt(userId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void finishSession(int sessionId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    _data.writeInt(userId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void cancelSession(int sessionId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    _data.writeInt(userId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAuthenticationResult(Bundle data, int sessionId, int authenticationId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(sessionId);
                    _data.writeInt(authenticationId);
                    _data.writeInt(userId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setHasCallback(int sessionId, int userId, boolean hasIt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    _data.writeInt(userId);
                    _data.writeInt(hasIt);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableOwnedAutofillServices(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isServiceSupported(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
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

            public boolean isServiceEnabled(int userId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(packageName);
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

            public void onPendingSaveUi(int operation, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(operation);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public UserData getUserData() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    UserData _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (UserData) UserData.CREATOR.createFromParcel(_reply);
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

            public String getUserDataId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUserData(UserData userData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (userData != null) {
                        _data.writeInt(1);
                        userData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isFieldClassificationEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public ComponentName getAutofillServiceComponentName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ComponentName _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ComponentName) ComponentName.CREATOR.createFromParcel(_reply);
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

            public String[] getAvailableFieldClassificationAlgorithms() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getDefaultFieldClassificationAlgorithm() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
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

        public static IAutoFillManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IAutoFillManager)) {
                return new Proxy(obj);
            }
            return (IAutoFillManager) iin;
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
                boolean _arg2 = false;
                ComponentName _arg0 = null;
                int _result;
                IBinder _arg02;
                IBinder _arg1;
                AutofillId _arg22;
                Rect _arg3;
                AutofillValue _arg4;
                int _arg5;
                boolean _arg6;
                int _arg7;
                int _result2;
                Bundle _arg03;
                UserData _result3;
                String _result4;
                switch (i) {
                    case 1:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _result = addClient(android.view.autofill.IAutoFillManagerClient.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return z;
                    case 2:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        removeClient(android.view.autofill.IAutoFillManagerClient.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        return z;
                    case 3:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        _arg1 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg22 = (AutofillId) AutofillId.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg3 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg4 = (AutofillValue) AutofillValue.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg4 = null;
                        }
                        _arg5 = data.readInt();
                        _arg6 = data.readInt() != 0 ? z : false;
                        _arg7 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result2 = startSession(_arg02, _arg1, _arg22, _arg3, _arg4, _arg5, _arg6, _arg7, _arg0, data.readInt() != 0 ? z : null);
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return z;
                    case 4:
                        i = 1;
                        parcel.enforceInterface(descriptor);
                        FillEventHistory _result5 = getFillEventHistory();
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(i);
                            _result5.writeToParcel(parcel2, i);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return i;
                    case 5:
                        z = true;
                        parcel.enforceInterface(descriptor);
                        boolean _result6 = restoreSession(data.readInt(), data.readStrongBinder(), data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return z;
                    case 6:
                        AutofillId _arg12;
                        Rect _arg23;
                        AutofillValue _arg32;
                        z = true;
                        parcel.enforceInterface(descriptor);
                        int _arg04 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = (AutofillId) AutofillId.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg12 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg23 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg23 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg32 = (AutofillValue) AutofillValue.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg32 = null;
                        }
                        updateSession(_arg04, _arg12, _arg23, _arg32, data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return z;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readStrongBinder();
                        _arg1 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg22 = (AutofillId) AutofillId.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg3 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg4 = (AutofillValue) AutofillValue.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg4 = null;
                        }
                        _arg5 = data.readInt();
                        _arg6 = data.readInt() != 0;
                        _arg7 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg03 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        z = true;
                        _result2 = updateOrRestartSession(_arg02, _arg1, _arg22, _arg3, _arg4, _arg5, _arg6, _arg7, _arg03, data.readInt(), data.readInt(), data.readInt() != 0);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return z;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        setAutofillFailure(data.readInt(), parcel.createTypedArrayList(AutofillId.CREATOR), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        finishSession(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        cancelSession(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        setAuthenticationResult(_arg03, data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        int _arg05 = data.readInt();
                        _result = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setHasCallback(_arg05, _result, _arg2);
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        disableOwnedAutofillServices(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        boolean _result7 = isServiceSupported(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        boolean _result8 = isServiceEnabled(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        onPendingSaveUi(data.readInt(), data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _result3 = getUserData();
                        reply.writeNoException();
                        if (_result3 != null) {
                            parcel2.writeInt(1);
                            _result3.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _result4 = getUserDataId();
                        reply.writeNoException();
                        parcel2.writeString(_result4);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result3 = (UserData) UserData.CREATOR.createFromParcel(parcel);
                        }
                        setUserData(_result3);
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _arg2 = isFieldClassificationEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_arg2);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getAutofillServiceComponentName();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            parcel2.writeInt(1);
                            _arg0.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        String[] _result9 = getAvailableFieldClassificationAlgorithms();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result9);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _result4 = getDefaultFieldClassificationAlgorithm();
                        reply.writeNoException();
                        parcel2.writeString(_result4);
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

    int addClient(IAutoFillManagerClient iAutoFillManagerClient, int i) throws RemoteException;

    void cancelSession(int i, int i2) throws RemoteException;

    void disableOwnedAutofillServices(int i) throws RemoteException;

    void finishSession(int i, int i2) throws RemoteException;

    ComponentName getAutofillServiceComponentName() throws RemoteException;

    String[] getAvailableFieldClassificationAlgorithms() throws RemoteException;

    String getDefaultFieldClassificationAlgorithm() throws RemoteException;

    FillEventHistory getFillEventHistory() throws RemoteException;

    UserData getUserData() throws RemoteException;

    String getUserDataId() throws RemoteException;

    boolean isFieldClassificationEnabled() throws RemoteException;

    boolean isServiceEnabled(int i, String str) throws RemoteException;

    boolean isServiceSupported(int i) throws RemoteException;

    void onPendingSaveUi(int i, IBinder iBinder) throws RemoteException;

    void removeClient(IAutoFillManagerClient iAutoFillManagerClient, int i) throws RemoteException;

    boolean restoreSession(int i, IBinder iBinder, IBinder iBinder2) throws RemoteException;

    void setAuthenticationResult(Bundle bundle, int i, int i2, int i3) throws RemoteException;

    void setAutofillFailure(int i, List<AutofillId> list, int i2) throws RemoteException;

    void setHasCallback(int i, int i2, boolean z) throws RemoteException;

    void setUserData(UserData userData) throws RemoteException;

    int startSession(IBinder iBinder, IBinder iBinder2, AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i, boolean z, int i2, ComponentName componentName, boolean z2) throws RemoteException;

    int updateOrRestartSession(IBinder iBinder, IBinder iBinder2, AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i, boolean z, int i2, ComponentName componentName, int i3, int i4, boolean z2) throws RemoteException;

    void updateSession(int i, AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i2, int i3, int i4) throws RemoteException;
}
