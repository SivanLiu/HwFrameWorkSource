package com.huawei.nb.searchmanager.service;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.huawei.nb.model.search.SearchTaskItem;
import com.huawei.nb.query.bulkcursor.BulkCursorDescriptor;
import com.huawei.nb.searchmanager.client.Attributes;
import com.huawei.nb.searchmanager.client.ISearchSession;
import com.huawei.nb.searchmanager.client.SearchIndexData;
import com.huawei.nb.searchmanager.client.SearchIntentItem;
import com.huawei.nb.searchmanager.client.Word;
import com.huawei.nb.searchmanager.client.model.IndexData;
import com.huawei.nb.searchmanager.client.model.IndexForm;
import com.huawei.nb.searchmanager.emuiclient.query.bulkcursor.BulkCursorDescriptorEx;
import java.util.List;
import java.util.Map;

public interface IRemoteSearchServiceCall extends IInterface {
    ISearchSession beginSearch(IndexInfo indexInfo, int i) throws RemoteException;

    int clearIndex(int i, String str, String str2, Map map) throws RemoteException;

    List<IndexData> delete(int i, String str, String str2, List<IndexData> list, List<IndexForm> list2) throws RemoteException;

    int deleteByQuery(int i, String str, String str2, String str3) throws RemoteException;

    List<String> deleteByTerm(int i, String str, String str2, String str3, List<String> list) throws RemoteException;

    void endSearch(IndexInfo indexInfo, ISearchSession iSearchSession, int i) throws RemoteException;

    List<Word> executeAnalyzeText(String str) throws RemoteException;

    void executeClearData(String str, int i) throws RemoteException;

    int executeDeleteIndex(String str, List<String> list, List<Attributes> list2) throws RemoteException;

    BulkCursorDescriptorEx executeEmuiSearch(String str, String str2, List<String> list, List<Attributes> list2, String str3) throws RemoteException;

    int executeInsertIndex(String str, List<SearchIndexData> list, List<Attributes> list2) throws RemoteException;

    List<SearchIntentItem> executeIntentSearch(String str, String str2, List<String> list, String str3) throws RemoteException;

    BulkCursorDescriptor executeMultiSearch(String str, Bundle bundle, String str2) throws RemoteException;

    BulkCursorDescriptor executeSearch(String str, String str2, List<String> list, List<Attributes> list2, String str3) throws RemoteException;

    int executeUpdateIndex(String str, List<SearchIndexData> list, List<Attributes> list2) throws RemoteException;

    String grantFilePermission(String str, String str2, String str3, int i) throws RemoteException;

    void handleIndexTask(int i, SearchTaskItem searchTaskItem, boolean z) throws RemoteException;

    List<IndexData> insert(int i, String str, String str2, List<IndexData> list, List<IndexForm> list2) throws RemoteException;

    boolean isAllowMonitor() throws RemoteException;

    void persistStatisticsToDb() throws RemoteException;

    String revokeFilePermission(String str, String str2, String str3, int i) throws RemoteException;

    void scheduleIndexTask(int i, boolean z) throws RemoteException;

    List<IndexData> update(int i, String str, String str2, List<IndexData> list, List<IndexForm> list2) throws RemoteException;

    public static abstract class Stub extends Binder implements IRemoteSearchServiceCall {
        private static final String DESCRIPTOR = "com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall";
        static final int TRANSACTION_beginSearch = 22;
        static final int TRANSACTION_clearIndex = 21;
        static final int TRANSACTION_delete = 18;
        static final int TRANSACTION_deleteByQuery = 20;
        static final int TRANSACTION_deleteByTerm = 19;
        static final int TRANSACTION_endSearch = 23;
        static final int TRANSACTION_executeAnalyzeText = 9;
        static final int TRANSACTION_executeClearData = 5;
        static final int TRANSACTION_executeDeleteIndex = 8;
        static final int TRANSACTION_executeEmuiSearch = 2;
        static final int TRANSACTION_executeInsertIndex = 6;
        static final int TRANSACTION_executeIntentSearch = 3;
        static final int TRANSACTION_executeMultiSearch = 4;
        static final int TRANSACTION_executeSearch = 1;
        static final int TRANSACTION_executeUpdateIndex = 7;
        static final int TRANSACTION_grantFilePermission = 10;
        static final int TRANSACTION_handleIndexTask = 12;
        static final int TRANSACTION_insert = 16;
        static final int TRANSACTION_isAllowMonitor = 14;
        static final int TRANSACTION_persistStatisticsToDb = 15;
        static final int TRANSACTION_revokeFilePermission = 11;
        static final int TRANSACTION_scheduleIndexTask = 13;
        static final int TRANSACTION_update = 17;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRemoteSearchServiceCall asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IRemoteSearchServiceCall)) {
                return new Proxy(obj);
            }
            return (IRemoteSearchServiceCall) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            IndexInfo _arg0;
            IndexInfo _arg02;
            SearchTaskItem _arg1;
            Bundle _arg12;
            switch (code) {
                case 1:
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
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    BulkCursorDescriptorEx _result2 = executeEmuiSearch(data.readString(), data.readString(), data.createStringArrayList(), data.createTypedArrayList(Attributes.CREATOR), data.readString());
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    List<SearchIntentItem> _result3 = executeIntentSearch(data.readString(), data.readString(), data.createStringArrayList(), data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    if (data.readInt() != 0) {
                        _arg12 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    BulkCursorDescriptor _result4 = executeMultiSearch(_arg03, _arg12, data.readString());
                    reply.writeNoException();
                    if (_result4 != null) {
                        reply.writeInt(1);
                        _result4.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    executeClearData(data.readString(), data.readInt());
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _result5 = executeInsertIndex(data.readString(), data.createTypedArrayList(SearchIndexData.CREATOR), data.createTypedArrayList(Attributes.CREATOR));
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _result6 = executeUpdateIndex(data.readString(), data.createTypedArrayList(SearchIndexData.CREATOR), data.createTypedArrayList(Attributes.CREATOR));
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _result7 = executeDeleteIndex(data.readString(), data.createStringArrayList(), data.createTypedArrayList(Attributes.CREATOR));
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    List<Word> _result8 = executeAnalyzeText(data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result8);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _result9 = grantFilePermission(data.readString(), data.readString(), data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeString(_result9);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _result10 = revokeFilePermission(data.readString(), data.readString(), data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeString(_result10);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = SearchTaskItem.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    handleIndexTask(_arg04, _arg1, data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    scheduleIndexTask(data.readInt(), data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result11 = isAllowMonitor();
                    reply.writeNoException();
                    reply.writeInt(_result11 ? 1 : 0);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    persistStatisticsToDb();
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    List<IndexData> _result12 = insert(data.readInt(), data.readString(), data.readString(), data.createTypedArrayList(IndexData.CREATOR), data.createTypedArrayList(IndexForm.CREATOR));
                    reply.writeNoException();
                    reply.writeTypedList(_result12);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    List<IndexData> _result13 = update(data.readInt(), data.readString(), data.readString(), data.createTypedArrayList(IndexData.CREATOR), data.createTypedArrayList(IndexForm.CREATOR));
                    reply.writeNoException();
                    reply.writeTypedList(_result13);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    List<IndexData> _result14 = delete(data.readInt(), data.readString(), data.readString(), data.createTypedArrayList(IndexData.CREATOR), data.createTypedArrayList(IndexForm.CREATOR));
                    reply.writeNoException();
                    reply.writeTypedList(_result14);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _result15 = deleteByTerm(data.readInt(), data.readString(), data.readString(), data.readString(), data.createStringArrayList());
                    reply.writeNoException();
                    reply.writeStringList(_result15);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _result16 = deleteByQuery(data.readInt(), data.readString(), data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result16);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _result17 = clearIndex(data.readInt(), data.readString(), data.readString(), data.readHashMap(getClass().getClassLoader()));
                    reply.writeNoException();
                    reply.writeInt(_result17);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = IndexInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    ISearchSession _result18 = beginSearch(_arg02, data.readInt());
                    reply.writeNoException();
                    reply.writeStrongBinder(_result18 != null ? _result18.asBinder() : null);
                    return true;
                case TRANSACTION_endSearch /*{ENCODED_INT: 23}*/:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = IndexInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    endSearch(_arg0, ISearchSession.Stub.asInterface(data.readStrongBinder()), data.readInt());
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IRemoteSearchServiceCall {
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

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
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
                    this.mRemote.transact(1, _data, _reply, 0);
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

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public BulkCursorDescriptorEx executeEmuiSearch(String pkgName, String queryString, List<String> fieldList, List<Attributes> attrsList, String callingPkgName) throws RemoteException {
                BulkCursorDescriptorEx _result;
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
                        _result = BulkCursorDescriptorEx.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public List<SearchIntentItem> executeIntentSearch(String pkgName, String queryString, List<String> fieldList, String type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(queryString);
                    _data.writeStringList(fieldList);
                    _data.writeString(type);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(SearchIntentItem.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
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
                    this.mRemote.transact(4, _data, _reply, 0);
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

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public void executeClearData(String pkgName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public int executeInsertIndex(String pkgName, List<SearchIndexData> dataList, List<Attributes> attrsList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeTypedList(dataList);
                    _data.writeTypedList(attrsList);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public int executeUpdateIndex(String pkgName, List<SearchIndexData> dataList, List<Attributes> attrsList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeTypedList(dataList);
                    _data.writeTypedList(attrsList);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public int executeDeleteIndex(String pkgName, List<String> idList, List<Attributes> attrsList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeStringList(idList);
                    _data.writeTypedList(attrsList);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public List<Word> executeAnalyzeText(String text) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(text);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(Word.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public String grantFilePermission(String pkgName, String paraType, String pathOrUri, int modeFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(paraType);
                    _data.writeString(pathOrUri);
                    _data.writeInt(modeFlags);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public String revokeFilePermission(String pkgName, String paraType, String pathOrUri, int modeFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(paraType);
                    _data.writeString(pathOrUri);
                    _data.writeInt(modeFlags);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public void handleIndexTask(int userId, SearchTaskItem taskItem, boolean delayControl) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    if (taskItem != null) {
                        _data.writeInt(1);
                        taskItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (!delayControl) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public void scheduleIndexTask(int searchTaskItemType, boolean delayControl) throws RemoteException {
                int i = 0;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(searchTaskItemType);
                    if (delayControl) {
                        i = 1;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public boolean isAllowMonitor() throws RemoteException {
                boolean _result = false;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public void persistStatisticsToDb() throws RemoteException {
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

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public List<IndexData> insert(int userId, String groupId, String pkgName, List<IndexData> dataList, List<IndexForm> indexForms) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeTypedList(dataList);
                    _data.writeTypedList(indexForms);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(IndexData.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public List<IndexData> update(int userId, String groupId, String pkgName, List<IndexData> dataList, List<IndexForm> indexForms) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeTypedList(dataList);
                    _data.writeTypedList(indexForms);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(IndexData.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public List<IndexData> delete(int userId, String groupId, String pkgName, List<IndexData> dataList, List<IndexForm> indexForms) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeTypedList(dataList);
                    _data.writeTypedList(indexForms);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createTypedArrayList(IndexData.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public List<String> deleteByTerm(int userId, String groupId, String pkgName, String indexFieldName, List<String> indexFieldValueList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeString(indexFieldName);
                    _data.writeStringList(indexFieldValueList);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    return _reply.createStringArrayList();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public int deleteByQuery(int userId, String groupId, String pkgName, String queryJsonStr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeString(queryJsonStr);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public int clearIndex(int userId, String groupId, String pkgName, Map deviceIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(groupId);
                    _data.writeString(pkgName);
                    _data.writeMap(deviceIds);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public ISearchSession beginSearch(IndexInfo indexInfo, int incNum) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (indexInfo != null) {
                        _data.writeInt(1);
                        indexInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(incNum);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    return ISearchSession.Stub.asInterface(_reply.readStrongBinder());
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.huawei.nb.searchmanager.service.IRemoteSearchServiceCall
            public void endSearch(IndexInfo indexInfo, ISearchSession searchSession, int decNum) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (indexInfo != null) {
                        _data.writeInt(1);
                        indexInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(searchSession != null ? searchSession.asBinder() : null);
                    _data.writeInt(decNum);
                    this.mRemote.transact(Stub.TRANSACTION_endSearch, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
