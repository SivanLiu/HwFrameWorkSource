package android.view.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.IWindow;
import java.util.List;

public interface IAccessibilityManager extends IInterface {

    public static abstract class Stub extends Binder implements IAccessibilityManager {
        private static final String DESCRIPTOR = "android.view.accessibility.IAccessibilityManager";
        static final int TRANSACTION_addAccessibilityInteractionConnection = 6;
        static final int TRANSACTION_addClient = 3;
        static final int TRANSACTION_getEnabledAccessibilityServiceList = 5;
        static final int TRANSACTION_getInstalledAccessibilityServiceList = 4;
        static final int TRANSACTION_getWindowToken = 12;
        static final int TRANSACTION_interrupt = 1;
        static final int TRANSACTION_notifyAccessibilityButtonClicked = 13;
        static final int TRANSACTION_notifyAccessibilityButtonVisibilityChanged = 14;
        static final int TRANSACTION_performAccessibilityShortcut = 15;
        static final int TRANSACTION_registerUiTestAutomationService = 9;
        static final int TRANSACTION_removeAccessibilityInteractionConnection = 7;
        static final int TRANSACTION_sendAccessibilityEvent = 2;
        static final int TRANSACTION_sendFingerprintGesture = 16;
        static final int TRANSACTION_setPictureInPictureActionReplacingConnection = 8;
        static final int TRANSACTION_temporaryEnableAccessibilityStateUntilKeyguardRemoved = 11;
        static final int TRANSACTION_unregisterUiTestAutomationService = 10;

        private static class Proxy implements IAccessibilityManager {
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

            public void interrupt(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void sendAccessibilityEvent(AccessibilityEvent uiEvent, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (uiEvent != null) {
                        _data.writeInt(1);
                        uiEvent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public long addClient(IAccessibilityManagerClient client, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    List<AccessibilityServiceInfo> _result = _reply.createTypedArrayList(AccessibilityServiceInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackType, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(feedbackType);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    List<AccessibilityServiceInfo> _result = _reply.createTypedArrayList(AccessibilityServiceInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int addAccessibilityInteractionConnection(IWindow windowToken, IAccessibilityInteractionConnection connection, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    IBinder iBinder = null;
                    _data.writeStrongBinder(windowToken != null ? windowToken.asBinder() : null);
                    if (connection != null) {
                        iBinder = connection.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeAccessibilityInteractionConnection(IWindow windowToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken != null ? windowToken.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection connection) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(connection != null ? connection.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerUiTestAutomationService(IBinder owner, IAccessibilityServiceClient client, AccessibilityServiceInfo info, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(owner);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterUiTestAutomationService(IAccessibilityServiceClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(client != null ? client.asBinder() : null);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName service, boolean touchExplorationEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(touchExplorationEnabled);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getWindowToken(int windowId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(windowId);
                    _data.writeInt(userId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyAccessibilityButtonClicked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyAccessibilityButtonVisibilityChanged(boolean available) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(available);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void performAccessibilityShortcut() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean sendFingerprintGesture(int gestureKeyCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(gestureKeyCode);
                    boolean z = false;
                    this.mRemote.transact(16, _data, _reply, 0);
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

        public static IAccessibilityManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IAccessibilityManager)) {
                return new Proxy(obj);
            }
            return (IAccessibilityManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                boolean _arg0 = false;
                AccessibilityEvent _arg2 = null;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        interrupt(data.readInt());
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = (AccessibilityEvent) AccessibilityEvent.CREATOR.createFromParcel(data);
                        }
                        sendAccessibilityEvent(_arg2, data.readInt());
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        long _result = addClient(android.view.accessibility.IAccessibilityManagerClient.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        reply.writeLong(_result);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        List<AccessibilityServiceInfo> _result2 = getInstalledAccessibilityServiceList(data.readInt());
                        reply.writeNoException();
                        reply.writeTypedList(_result2);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        List<AccessibilityServiceInfo> _result3 = getEnabledAccessibilityServiceList(data.readInt(), data.readInt());
                        reply.writeNoException();
                        reply.writeTypedList(_result3);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        int _result4 = addAccessibilityInteractionConnection(android.view.IWindow.Stub.asInterface(data.readStrongBinder()), android.view.accessibility.IAccessibilityInteractionConnection.Stub.asInterface(data.readStrongBinder()), data.readString(), data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        removeAccessibilityInteractionConnection(android.view.IWindow.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        setPictureInPictureActionReplacingConnection(android.view.accessibility.IAccessibilityInteractionConnection.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 9:
                        AccessibilityServiceInfo _arg22;
                        data.enforceInterface(descriptor);
                        IBinder _arg02 = data.readStrongBinder();
                        IAccessibilityServiceClient _arg1 = android.accessibilityservice.IAccessibilityServiceClient.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg22 = (AccessibilityServiceInfo) AccessibilityServiceInfo.CREATOR.createFromParcel(data);
                        }
                        registerUiTestAutomationService(_arg02, _arg1, _arg22, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        unregisterUiTestAutomationService(android.accessibilityservice.IAccessibilityServiceClient.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 11:
                        ComponentName _arg03;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        temporaryEnableAccessibilityStateUntilKeyguardRemoved(_arg03, _arg0);
                        reply.writeNoException();
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        IBinder _result5 = getWindowToken(data.readInt(), data.readInt());
                        reply.writeNoException();
                        reply.writeStrongBinder(_result5);
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        notifyAccessibilityButtonClicked();
                        reply.writeNoException();
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        notifyAccessibilityButtonVisibilityChanged(_arg0);
                        reply.writeNoException();
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        performAccessibilityShortcut();
                        reply.writeNoException();
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        boolean _result6 = sendFingerprintGesture(data.readInt());
                        reply.writeNoException();
                        reply.writeInt(_result6);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    int addAccessibilityInteractionConnection(IWindow iWindow, IAccessibilityInteractionConnection iAccessibilityInteractionConnection, String str, int i) throws RemoteException;

    long addClient(IAccessibilityManagerClient iAccessibilityManagerClient, int i) throws RemoteException;

    List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int i, int i2) throws RemoteException;

    List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int i) throws RemoteException;

    IBinder getWindowToken(int i, int i2) throws RemoteException;

    void interrupt(int i) throws RemoteException;

    void notifyAccessibilityButtonClicked() throws RemoteException;

    void notifyAccessibilityButtonVisibilityChanged(boolean z) throws RemoteException;

    void performAccessibilityShortcut() throws RemoteException;

    void registerUiTestAutomationService(IBinder iBinder, IAccessibilityServiceClient iAccessibilityServiceClient, AccessibilityServiceInfo accessibilityServiceInfo, int i) throws RemoteException;

    void removeAccessibilityInteractionConnection(IWindow iWindow) throws RemoteException;

    void sendAccessibilityEvent(AccessibilityEvent accessibilityEvent, int i) throws RemoteException;

    boolean sendFingerprintGesture(int i) throws RemoteException;

    void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection iAccessibilityInteractionConnection) throws RemoteException;

    void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName componentName, boolean z) throws RemoteException;

    void unregisterUiTestAutomationService(IAccessibilityServiceClient iAccessibilityServiceClient) throws RemoteException;
}
