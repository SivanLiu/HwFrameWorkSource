package com.huawei.nb.searchmanager.client;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.nb.searchmanager.client.model.IndexData;
import com.huawei.nb.searchmanager.client.model.Recommendation;
import com.huawei.nb.searchmanager.client.model.Token;
import java.util.List;
import java.util.Map;

public interface ISearchSession extends IInterface {
    Map coverSearch(String str, List<String> list, int i) throws RemoteException;

    int getSearchHitCount(String str) throws RemoteException;

    List<String> getTopFieldValues(String str, int i) throws RemoteException;

    List<Recommendation> groupSearch(String str, int i) throws RemoteException;

    List<Recommendation> groupTimeline(String str, String str2, Token token) throws RemoteException;

    List<IndexData> search(String str, int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements ISearchSession {
        private static final String DESCRIPTOR = "com.huawei.nb.searchmanager.client.ISearchSession";
        static final int TRANSACTION_coverSearch = 6;
        static final int TRANSACTION_getSearchHitCount = 2;
        static final int TRANSACTION_getTopFieldValues = 1;
        static final int TRANSACTION_groupSearch = 4;
        static final int TRANSACTION_groupTimeline = 5;
        static final int TRANSACTION_search = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISearchSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISearchSession)) {
                return new Proxy(obj);
            }
            return (ISearchSession) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Token _arg2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _result = getTopFieldValues(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeStringList(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _result2 = getSearchHitCount(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    List<IndexData> _result3 = search(data.readString(), data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeTypedList(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    List<Recommendation> _result4 = groupSearch(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeTypedList(_result4);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    String _arg1 = data.readString();
                    if (data.readInt() != 0) {
                        _arg2 = Token.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    List<Recommendation> _result5 = groupTimeline(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    reply.writeTypedList(_result5);
                    if (_arg2 != null) {
                        reply.writeInt(1);
                        _arg2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    Map _result6 = coverSearch(data.readString(), data.createStringArrayList(), data.readInt());
                    reply.writeNoException();
                    reply.writeMap(_result6);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ISearchSession {
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

            @Override // com.huawei.nb.searchmanager.client.ISearchSession
            public List<String> getTopFieldValues(String fieldName, int limit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fieldName);
                    _data.writeInt(limit);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createStringArrayList();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchSession
            public int getSearchHitCount(String queryJsonStr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(queryJsonStr);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchSession
            public List<IndexData> search(String queryJsonStr, int start, int limit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(queryJsonStr);
                    _data.writeInt(start);
                    _data.writeInt(limit);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(IndexData.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchSession
            public List<Recommendation> groupSearch(String queryJsonStr, int mGroupLimit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(queryJsonStr);
                    _data.writeInt(mGroupLimit);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(Recommendation.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchSession
            public List<Recommendation> groupTimeline(String queryJsonStr, String field, Token token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(queryJsonStr);
                    _data.writeString(field);
                    if (token != null) {
                        _data.writeInt(1);
                        token.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    List<Recommendation> _result = _reply.createTypedArrayList(Recommendation.CREATOR);
                    if (_reply.readInt() != 0) {
                        token.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchSession
            public Map coverSearch(String queryJsonStr, List<String> groupFields, int groupLimit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(queryJsonStr);
                    _data.writeStringList(groupFields);
                    _data.writeInt(groupLimit);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readHashMap(getClass().getClassLoader());
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
