package android.view;

import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRecentsAnimationRunner extends IInterface {

    public static abstract class Stub extends Binder implements IRecentsAnimationRunner {
        private static final String DESCRIPTOR = "android.view.IRecentsAnimationRunner";
        static final int TRANSACTION_onAnimationCanceled = 2;
        static final int TRANSACTION_onAnimationStart = 3;

        private static class Proxy implements IRecentsAnimationRunner {
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

            public void onAnimationCanceled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onAnimationStart(IRecentsAnimationController controller, RemoteAnimationTarget[] apps, Rect homeContentInsets, Rect minimizedHomeBounds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(controller != null ? controller.asBinder() : null);
                    _data.writeTypedArray(apps, 0);
                    if (homeContentInsets != null) {
                        _data.writeInt(1);
                        homeContentInsets.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (minimizedHomeBounds != null) {
                        _data.writeInt(1);
                        minimizedHomeBounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRecentsAnimationRunner asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IRecentsAnimationRunner)) {
                return new Proxy(obj);
            }
            return (IRecentsAnimationRunner) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                switch (code) {
                    case 2:
                        data.enforceInterface(descriptor);
                        onAnimationCanceled();
                        return true;
                    case 3:
                        Rect _arg2;
                        data.enforceInterface(descriptor);
                        IRecentsAnimationController _arg0 = android.view.IRecentsAnimationController.Stub.asInterface(data.readStrongBinder());
                        RemoteAnimationTarget[] _arg1 = (RemoteAnimationTarget[]) data.createTypedArray(RemoteAnimationTarget.CREATOR);
                        Rect _arg3 = null;
                        if (data.readInt() != 0) {
                            _arg2 = (Rect) Rect.CREATOR.createFromParcel(data);
                        } else {
                            _arg2 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (Rect) Rect.CREATOR.createFromParcel(data);
                        }
                        onAnimationStart(_arg0, _arg1, _arg2, _arg3);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void onAnimationCanceled() throws RemoteException;

    void onAnimationStart(IRecentsAnimationController iRecentsAnimationController, RemoteAnimationTarget[] remoteAnimationTargetArr, Rect rect, Rect rect2) throws RemoteException;
}
