package com.huawei.nb.query.bulkcursor;

import android.database.CursorWindow;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public abstract class BulkCursorNative extends Binder implements IBulkCursor {
    public BulkCursorNative() {
        attachInterface(this, "android.content.IBulkCursor");
    }

    public static IBulkCursor asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        IInterface localInterface = obj.queryLocalInterface("android.content.IBulkCursor");
        if (localInterface == null || !(localInterface instanceof IBulkCursor)) {
            return new BulkCursorProxy(obj);
        }
        return (IBulkCursor) localInterface;
    }

    @Override // android.os.Binder
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        data.enforceInterface("android.content.IBulkCursor");
        switch (code) {
            case 1:
                CursorWindow window = getWindow(data.readInt());
                reply.writeNoException();
                if (window == null) {
                    reply.writeInt(0);
                    return true;
                }
                reply.writeInt(1);
                window.writeToParcel(reply, 1);
                return true;
            case 2:
                deactivate();
                reply.writeNoException();
                return true;
            case 3:
            default:
                return super.onTransact(code, data, reply, flags);
            case 4:
                onMove(data.readInt());
                reply.writeNoException();
                return true;
            case 5:
                Bundle extras = getExtras();
                reply.writeNoException();
                reply.writeBundle(extras);
                return true;
            case 6:
                Bundle returnExtras = respond(data.readBundle());
                reply.writeNoException();
                reply.writeBundle(returnExtras);
                return true;
            case 7:
                close();
                reply.writeNoException();
                return true;
        }
    }

    public IBinder asBinder() {
        return this;
    }
}
