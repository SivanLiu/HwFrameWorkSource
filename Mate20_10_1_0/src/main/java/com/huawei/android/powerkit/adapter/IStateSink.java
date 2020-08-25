package com.huawei.android.powerkit.adapter;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IStateSink extends IInterface {
    void onPowerOverUsing(String str, int i, long j, long j2, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements IStateSink {
        private static final String DESCRIPTOR = "com.huawei.android.powerkit.adapter.IStateSink";
        static final int TRANSACTION_onPowerOverUsing = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStateSink asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IStateSink)) {
                return new Proxy(obj);
            }
            return (IStateSink) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    onPowerOverUsing(data.readString(), data.readInt(), data.readLong(), data.readLong(), data.readString());
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IStateSink {
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

            @Override // com.huawei.android.powerkit.adapter.IStateSink
            public void onPowerOverUsing(String module, int resourceType, long stats_duration, long hold_time, String extend) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(module);
                    _data.writeInt(resourceType);
                    _data.writeLong(stats_duration);
                    _data.writeLong(hold_time);
                    _data.writeString(extend);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
