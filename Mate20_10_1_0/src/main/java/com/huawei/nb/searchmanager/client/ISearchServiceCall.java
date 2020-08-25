package com.huawei.nb.searchmanager.client;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.nb.query.bulkcursor.BulkCursorDescriptor;
import com.huawei.nb.searchmanager.callback.IIndexChangeCallback;
import com.huawei.nb.searchmanager.client.ISearchSession;
import com.huawei.nb.searchmanager.client.model.IndexData;
import com.huawei.nb.searchmanager.client.model.IndexForm;
import java.util.List;
import java.util.Map;

public interface ISearchServiceCall extends IInterface {
    ISearchSession beginSearch(String str, String str2, String str3) throws RemoteException;

    int clearIndex(String str, String str2, Map map, String str3) throws RemoteException;

    int clearIndexForm(String str, String str2) throws RemoteException;

    List<IndexData> delete(String str, String str2, List<IndexData> list, String str3) throws RemoteException;

    int deleteByQuery(String str, String str2, String str3, String str4) throws RemoteException;

    List<String> deleteByTerm(String str, String str2, String str3, List<String> list, String str4) throws RemoteException;

    void endSearch(String str, String str2, ISearchSession iSearchSession, String str3) throws RemoteException;

    List<Word> executeAnalyzeText(String str, String str2, String str3) throws RemoteException;

    void executeClearData(String str, int i, String str2) throws RemoteException;

    void executeDBCrawl(String str, List<String> list, int i, String str2) throws RemoteException;

    int executeDeleteIndex(String str, List<String> list, List<Attributes> list2, String str2) throws RemoteException;

    void executeFileCrawl(String str, String str2, boolean z, int i, String str3) throws RemoteException;

    int executeInsertIndex(String str, List<SearchIndexData> list, List<Attributes> list2, String str2) throws RemoteException;

    List<SearchIntentItem> executeIntentSearch(String str, String str2, List<String> list, String str3, String str4) throws RemoteException;

    BulkCursorDescriptor executeMultiSearch(String str, Bundle bundle, String str2) throws RemoteException;

    BulkCursorDescriptor executeSearch(String str, String str2, List<String> list, List<Attributes> list2, String str3) throws RemoteException;

    int executeUpdateIndex(String str, List<SearchIndexData> list, List<Attributes> list2, String str2) throws RemoteException;

    List<IndexForm> getIndexForm(String str, String str2) throws RemoteException;

    int getIndexFormVersion(String str, String str2) throws RemoteException;

    String grantFilePermission(String str, String str2, String str3, int i, String str4) throws RemoteException;

    List<IndexData> insert(String str, String str2, List<IndexData> list, String str3) throws RemoteException;

    void registerClientDeathBinder(IBinder iBinder, String str) throws RemoteException;

    void registerIndexChangeListener(String str, String str2, IIndexChangeCallback iIndexChangeCallback, String str3) throws RemoteException;

    String revokeFilePermission(String str, String str2, String str3, int i, String str4) throws RemoteException;

    int setIndexForm(String str, int i, List<IndexForm> list, String str2) throws RemoteException;

    void setSearchSwitch(String str, boolean z, String str2) throws RemoteException;

    void unRegisterClientDeathBinder(IBinder iBinder, String str) throws RemoteException;

    void unRegisterIndexChangeListener(String str, String str2, IIndexChangeCallback iIndexChangeCallback, String str3) throws RemoteException;

    List<IndexData> update(String str, String str2, List<IndexData> list, String str3) throws RemoteException;

    public static abstract class Stub extends Binder implements ISearchServiceCall {
        private static final String DESCRIPTOR = "com.huawei.nb.searchmanager.client.ISearchServiceCall";
        static final int TRANSACTION_beginSearch = 24;
        static final int TRANSACTION_clearIndex = 23;
        static final int TRANSACTION_clearIndexForm = 14;
        static final int TRANSACTION_delete = 20;
        static final int TRANSACTION_deleteByQuery = 22;
        static final int TRANSACTION_deleteByTerm = 21;
        static final int TRANSACTION_endSearch = 25;
        static final int TRANSACTION_executeAnalyzeText = 9;
        static final int TRANSACTION_executeClearData = 3;
        static final int TRANSACTION_executeDBCrawl = 1;
        static final int TRANSACTION_executeDeleteIndex = 7;
        static final int TRANSACTION_executeFileCrawl = 4;
        static final int TRANSACTION_executeInsertIndex = 5;
        static final int TRANSACTION_executeIntentSearch = 8;
        static final int TRANSACTION_executeMultiSearch = 13;
        static final int TRANSACTION_executeSearch = 2;
        static final int TRANSACTION_executeUpdateIndex = 6;
        static final int TRANSACTION_getIndexForm = 17;
        static final int TRANSACTION_getIndexFormVersion = 16;
        static final int TRANSACTION_grantFilePermission = 11;
        static final int TRANSACTION_insert = 18;
        static final int TRANSACTION_registerClientDeathBinder = 28;
        static final int TRANSACTION_registerIndexChangeListener = 26;
        static final int TRANSACTION_revokeFilePermission = 12;
        static final int TRANSACTION_setIndexForm = 15;
        static final int TRANSACTION_setSearchSwitch = 10;
        static final int TRANSACTION_unRegisterClientDeathBinder = 29;
        static final int TRANSACTION_unRegisterIndexChangeListener = 27;
        static final int TRANSACTION_update = 19;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISearchServiceCall asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISearchServiceCall)) {
                return new Proxy(obj);
            }
            return (ISearchServiceCall) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Bundle _arg1;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    executeDBCrawl(data.readString(), data.createStringArrayList(), data.readInt(), data.readString());
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    BulkCursorDescriptor _result = executeSearch(data.readString(), data.readString(), data.createStringArrayList(), data.createTypedArrayList(Attributes.CREATOR), data.readString());
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    executeClearData(data.readString(), data.readInt(), data.readString());
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    executeFileCrawl(data.readString(), data.readString(), data.readInt() != 0, data.readInt(), data.readString());
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _result2 = executeInsertIndex(data.readString(), data.createTypedArrayList(SearchIndexData.CREATOR), data.createTypedArrayList(Attributes.CREATOR), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _result3 = executeUpdateIndex(data.readString(), data.createTypedArrayList(SearchIndexData.CREATOR), data.createTypedArrayList(Attributes.CREATOR), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _result4 = executeDeleteIndex(data.readString(), data.createStringArrayList(), data.createTypedArrayList(Attributes.CREATOR), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    List<SearchIntentItem> _result5 = executeIntentSearch(data.readString(), data.readString(), data.createStringArrayList(), data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result5);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    List<Word> _result6 = executeAnalyzeText(data.readString(), data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result6);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    setSearchSwitch(data.readString(), data.readInt() != 0, data.readString());
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _result7 = grantFilePermission(data.readString(), data.readString(), data.readString(), data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeString(_result7);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _result8 = revokeFilePermission(data.readString(), data.readString(), data.readString(), data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeString(_result8);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    if (data.readInt() != 0) {
                        _arg1 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    BulkCursorDescriptor _result9 = executeMultiSearch(_arg0, _arg1, data.readString());
                    reply.writeNoException();
                    if (_result9 != null) {
                        reply.writeInt(1);
                        _result9.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _result10 = clearIndexForm(data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result10);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    int _result11 = setIndexForm(data.readString(), data.readInt(), data.createTypedArrayList(IndexForm.CREATOR), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result11);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _result12 = getIndexFormVersion(data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    List<IndexForm> _result13 = getIndexForm(data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result13);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    List<IndexData> _result14 = insert(data.readString(), data.readString(), data.createTypedArrayList(IndexData.CREATOR), data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result14);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    List<IndexData> _result15 = update(data.readString(), data.readString(), data.createTypedArrayList(IndexData.CREATOR), data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result15);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    List<IndexData> _result16 = delete(data.readString(), data.readString(), data.createTypedArrayList(IndexData.CREATOR), data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result16);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _result17 = deleteByTerm(data.readString(), data.readString(), data.readString(), data.createStringArrayList(), data.readString());
                    reply.writeNoException();
                    reply.writeStringList(_result17);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _result18 = deleteByQuery(data.readString(), data.readString(), data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result18);
                    return true;
                case TRANSACTION_clearIndex /*{ENCODED_INT: 23}*/:
                    data.enforceInterface(DESCRIPTOR);
                    int _result19 = clearIndex(data.readString(), data.readString(), data.readHashMap(getClass().getClassLoader()), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result19);
                    return true;
                case TRANSACTION_beginSearch /*{ENCODED_INT: 24}*/:
                    data.enforceInterface(DESCRIPTOR);
                    ISearchSession _result20 = beginSearch(data.readString(), data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeStrongBinder(_result20 != null ? _result20.asBinder() : null);
                    return true;
                case TRANSACTION_endSearch /*{ENCODED_INT: 25}*/:
                    data.enforceInterface(DESCRIPTOR);
                    endSearch(data.readString(), data.readString(), ISearchSession.Stub.asInterface(data.readStrongBinder()), data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_registerIndexChangeListener /*{ENCODED_INT: 26}*/:
                    data.enforceInterface(DESCRIPTOR);
                    registerIndexChangeListener(data.readString(), data.readString(), IIndexChangeCallback.Stub.asInterface(data.readStrongBinder()), data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_unRegisterIndexChangeListener /*{ENCODED_INT: 27}*/:
                    data.enforceInterface(DESCRIPTOR);
                    unRegisterIndexChangeListener(data.readString(), data.readString(), IIndexChangeCallback.Stub.asInterface(data.readStrongBinder()), data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_registerClientDeathBinder /*{ENCODED_INT: 28}*/:
                    data.enforceInterface(DESCRIPTOR);
                    registerClientDeathBinder(data.readStrongBinder(), data.readString());
                    reply.writeNoException();
                    return true;
                case TRANSACTION_unRegisterClientDeathBinder /*{ENCODED_INT: 29}*/:
                    data.enforceInterface(DESCRIPTOR);
                    unRegisterClientDeathBinder(data.readStrongBinder(), data.readString());
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ISearchServiceCall {
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

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void executeDBCrawl(String pkgName, List<String> idList, int op, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeStringList(idList);
                    _data.writeInt(op);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public BulkCursorDescriptor executeSearch(String pkgName, String queryString, List<String> fieldList, List<Attributes> attrsList, String callingPkgName) throws RemoteException {
                BulkCursorDescriptor _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(queryString);
                    _data.writeStringList(fieldList);
                    _data.writeTypedList(attrsList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = BulkCursorDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void executeClearData(String pkgName, int userId, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeInt(userId);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void executeFileCrawl(String pkgName, String filePath, boolean crawlContent, int op, String callingPkgName) throws RemoteException {
                int i = 0;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(filePath);
                    if (crawlContent) {
                        i = 1;
                    }
                    _data.writeInt(i);
                    _data.writeInt(op);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public int executeInsertIndex(String pkgName, List<SearchIndexData> dataList, List<Attributes> attrsList, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeTypedList(dataList);
                    _data.writeTypedList(attrsList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public int executeUpdateIndex(String pkgName, List<SearchIndexData> dataList, List<Attributes> attrsList, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeTypedList(dataList);
                    _data.writeTypedList(attrsList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public int executeDeleteIndex(String pkgName, List<String> idList, List<Attributes> attrsList, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeStringList(idList);
                    _data.writeTypedList(attrsList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public List<SearchIntentItem> executeIntentSearch(String pkgName, String queryString, List<String> fieldList, String type, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(queryString);
                    _data.writeStringList(fieldList);
                    _data.writeString(type);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(SearchIntentItem.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public List<Word> executeAnalyzeText(String pkgName, String text, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(text);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(Word.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void setSearchSwitch(String pkgName, boolean isSwitchOn, String callingPkgName) throws RemoteException {
                int i = 0;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    if (isSwitchOn) {
                        i = 1;
                    }
                    _data.writeInt(i);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public String grantFilePermission(String pkgName, String paraType, String pathOrUri, int modeFlags, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(paraType);
                    _data.writeString(pathOrUri);
                    _data.writeInt(modeFlags);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public String revokeFilePermission(String pkgName, String paraType, String pathOrUri, int modeFlags, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(paraType);
                    _data.writeString(pathOrUri);
                    _data.writeInt(modeFlags);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public BulkCursorDescriptor executeMultiSearch(String pkgName, Bundle searchParas, String callingPkgName) throws RemoteException {
                BulkCursorDescriptor _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    if (searchParas != null) {
                        _data.writeInt(1);
                        searchParas.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = BulkCursorDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public int clearIndexForm(String pkgName, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public int setIndexForm(String pkgName, int version, List<IndexForm> indexFormList, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeInt(version);
                    _data.writeTypedList(indexFormList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public int getIndexFormVersion(String pkgName, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public List<IndexForm> getIndexForm(String pkgName, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(IndexForm.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public List<IndexData> insert(String groupId, String pkgName, List<IndexData> indexDataList, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeTypedList(indexDataList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(IndexData.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public List<IndexData> update(String groupId, String pkgName, List<IndexData> indexDataList, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeTypedList(indexDataList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(IndexData.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public List<IndexData> delete(String groupId, String pkgName, List<IndexData> indexDataList, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeTypedList(indexDataList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(IndexData.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public List<String> deleteByTerm(String groupId, String pkgName, String indexFieldName, List<String> indexFieldValueList, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeString(indexFieldName);
                    _data.writeStringList(indexFieldValueList);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createStringArrayList();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public int deleteByQuery(String groupId, String pkgName, String queryJsonStr, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeString(queryJsonStr);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public int clearIndex(String groupId, String pkgName, Map deviceIds, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeMap(deviceIds);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(Stub.TRANSACTION_clearIndex, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public ISearchSession beginSearch(String groupId, String pkgName, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(Stub.TRANSACTION_beginSearch, _data, _reply, 0);
                    _reply.readException();
                    return ISearchSession.Stub.asInterface(_reply.readStrongBinder());
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void endSearch(String groupId, String pkgName, ISearchSession searchSession, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeStrongBinder(searchSession != null ? searchSession.asBinder() : null);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(Stub.TRANSACTION_endSearch, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void registerIndexChangeListener(String groupId, String pkgName, IIndexChangeCallback callback, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(Stub.TRANSACTION_registerIndexChangeListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void unRegisterIndexChangeListener(String groupId, String pkgName, IIndexChangeCallback callback, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(Stub.TRANSACTION_unRegisterIndexChangeListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void registerClientDeathBinder(IBinder clientDeathBinder, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(clientDeathBinder);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(Stub.TRANSACTION_registerClientDeathBinder, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.client.ISearchServiceCall
            public void unRegisterClientDeathBinder(IBinder clientDeathBinder, String callingPkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(clientDeathBinder);
                    _data.writeString(callingPkgName);
                    this.mRemote.transact(Stub.TRANSACTION_unRegisterClientDeathBinder, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
