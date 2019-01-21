package android.content;

import android.content.res.AssetFileDescriptor;
import android.database.BulkCursorDescriptor;
import android.database.Cursor;
import android.database.CursorToBulkCursorAdaptor;
import android.database.DatabaseUtils;
import android.database.IContentObserver;
import android.database.IContentObserver.Stub;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.util.ArrayList;

public abstract class ContentProviderNative extends Binder implements IContentProvider {
    public abstract String getProviderName();

    public ContentProviderNative() {
        attachInterface(this, IContentProvider.descriptor);
    }

    public static IContentProvider asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        IContentProvider in = (IContentProvider) obj.queryLocalInterface(IContentProvider.descriptor);
        if (in != null) {
            return in;
        }
        return new ContentProviderProxy(obj);
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:8:0x001d, B:61:0x0271] */
    /* JADX WARNING: Missing block: B:70:0x0296, code skipped:
            if (r4 != null) goto L_0x0298;
     */
    /* JADX WARNING: Missing block: B:71:0x0298, code skipped:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:72:0x029b, code skipped:
            if (r1 != null) goto L_0x029d;
     */
    /* JADX WARNING: Missing block: B:73:0x029d, code skipped:
            r1.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        int i = code;
        Parcel parcel = data;
        Parcel parcel2 = reply;
        Exception e;
        if (i != 10) {
            int i2 = 0;
            Uri url;
            int i3;
            String type;
            switch (i) {
                case 1:
                    parcel.enforceInterface(IContentProvider.descriptor);
                    String callingPkg = data.readString();
                    url = (Uri) Uri.CREATOR.createFromParcel(parcel);
                    int num = data.readInt();
                    String[] projection = null;
                    if (num > 0) {
                        projection = new String[num];
                        for (i3 = 0; i3 < num; i3++) {
                            projection[i3] = data.readString();
                        }
                    }
                    String[] projection2 = projection;
                    Bundle queryArgs = data.readBundle();
                    IContentObserver observer = Stub.asInterface(data.readStrongBinder());
                    Cursor cursor = query(callingPkg, url, projection2, queryArgs, ICancellationSignal.Stub.asInterface(data.readStrongBinder()));
                    if (cursor != null) {
                        CursorToBulkCursorAdaptor adaptor = null;
                        cursor = null;
                        BulkCursorDescriptor d = new CursorToBulkCursorAdaptor(cursor, observer, getProviderName()).getBulkCursorDescriptor();
                        adaptor = null;
                        reply.writeNoException();
                        parcel2.writeInt(1);
                        d.writeToParcel(parcel2, 1);
                        if (adaptor != null) {
                            adaptor.close();
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    } else {
                        reply.writeNoException();
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface(IContentProvider.descriptor);
                    type = getType((Uri) Uri.CREATOR.createFromParcel(parcel));
                    reply.writeNoException();
                    parcel2.writeString(type);
                    return true;
                case 3:
                    parcel.enforceInterface(IContentProvider.descriptor);
                    url = insert(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel), (ContentValues) ContentValues.CREATOR.createFromParcel(parcel));
                    reply.writeNoException();
                    Uri.writeToParcel(parcel2, url);
                    return true;
                case 4:
                    parcel.enforceInterface(IContentProvider.descriptor);
                    i3 = delete(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel), data.readString(), data.readStringArray());
                    reply.writeNoException();
                    parcel2.writeInt(i3);
                    return true;
                default:
                    switch (i) {
                        case 13:
                            parcel.enforceInterface(IContentProvider.descriptor);
                            int count = bulkInsert(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel), (ContentValues[]) parcel.createTypedArray(ContentValues.CREATOR));
                            reply.writeNoException();
                            parcel2.writeInt(count);
                            return true;
                        case 14:
                            parcel.enforceInterface(IContentProvider.descriptor);
                            ParcelFileDescriptor fd = openFile(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel), data.readString(), ICancellationSignal.Stub.asInterface(data.readStrongBinder()), data.readStrongBinder());
                            reply.writeNoException();
                            if (fd != null) {
                                parcel2.writeInt(1);
                                fd.writeToParcel(parcel2, 1);
                            } else {
                                parcel2.writeInt(0);
                            }
                            return true;
                        case 15:
                            parcel.enforceInterface(IContentProvider.descriptor);
                            AssetFileDescriptor fd2 = openAssetFile(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel), data.readString(), ICancellationSignal.Stub.asInterface(data.readStrongBinder()));
                            reply.writeNoException();
                            if (fd2 != null) {
                                parcel2.writeInt(1);
                                fd2.writeToParcel(parcel2, 1);
                            } else {
                                parcel2.writeInt(0);
                            }
                            return true;
                        default:
                            Uri out;
                            switch (i) {
                                case 20:
                                    parcel.enforceInterface(IContentProvider.descriptor);
                                    type = data.readString();
                                    int numOperations = data.readInt();
                                    ArrayList<ContentProviderOperation> operations = new ArrayList(numOperations);
                                    for (i3 = 0; i3 < numOperations; i3++) {
                                        operations.add(i3, (ContentProviderOperation) ContentProviderOperation.CREATOR.createFromParcel(parcel));
                                    }
                                    ContentProviderResult[] results = applyBatch(type, operations);
                                    reply.writeNoException();
                                    parcel2.writeTypedArray(results, 0);
                                    return true;
                                case 21:
                                    parcel.enforceInterface(IContentProvider.descriptor);
                                    Bundle responseBundle = call(data.readString(), data.readString(), data.readString(), data.readBundle());
                                    reply.writeNoException();
                                    parcel2.writeBundle(responseBundle);
                                    return true;
                                case 22:
                                    parcel.enforceInterface(IContentProvider.descriptor);
                                    String[] types = getStreamTypes((Uri) Uri.CREATOR.createFromParcel(parcel), data.readString());
                                    reply.writeNoException();
                                    parcel2.writeStringArray(types);
                                    return true;
                                case 23:
                                    parcel.enforceInterface(IContentProvider.descriptor);
                                    AssetFileDescriptor fd3 = openTypedAssetFile(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel), data.readString(), data.readBundle(), ICancellationSignal.Stub.asInterface(data.readStrongBinder()));
                                    reply.writeNoException();
                                    if (fd3 != null) {
                                        parcel2.writeInt(1);
                                        fd3.writeToParcel(parcel2, 1);
                                    } else {
                                        parcel2.writeInt(0);
                                    }
                                    return true;
                                case 24:
                                    parcel.enforceInterface(IContentProvider.descriptor);
                                    ICancellationSignal cancellationSignal = createCancellationSignal();
                                    reply.writeNoException();
                                    parcel2.writeStrongBinder(cancellationSignal.asBinder());
                                    return true;
                                case 25:
                                    parcel.enforceInterface(IContentProvider.descriptor);
                                    out = canonicalize(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel));
                                    reply.writeNoException();
                                    Uri.writeToParcel(parcel2, out);
                                    return true;
                                case 26:
                                    parcel.enforceInterface(IContentProvider.descriptor);
                                    out = uncanonicalize(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel));
                                    reply.writeNoException();
                                    Uri.writeToParcel(parcel2, out);
                                    return true;
                                case 27:
                                    parcel.enforceInterface(IContentProvider.descriptor);
                                    boolean out2 = refresh(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel), data.readBundle(), ICancellationSignal.Stub.asInterface(data.readStrongBinder()));
                                    reply.writeNoException();
                                    if (!out2) {
                                        i2 = -1;
                                    }
                                    parcel2.writeInt(i2);
                                    return true;
                                default:
                                    return super.onTransact(code, data, reply, flags);
                            }
                    }
            }
            DatabaseUtils.writeExceptionToParcel(parcel2, e);
            return true;
        }
        parcel.enforceInterface(IContentProvider.descriptor);
        e = update(data.readString(), (Uri) Uri.CREATOR.createFromParcel(parcel), (ContentValues) ContentValues.CREATOR.createFromParcel(parcel), data.readString(), data.readStringArray());
        reply.writeNoException();
        parcel2.writeInt(e);
        return true;
    }

    public IBinder asBinder() {
        return this;
    }
}
