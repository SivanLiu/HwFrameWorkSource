package com.huawei.android.os;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdCleanerManagerEx {
    private static final int CODE_AD_DEBUG = 1019;
    private static final int CODE_CLEAN_AD_STRATEGY = 1018;
    private static final int CODE_SET_AD_STRATEGY = 1017;
    private static final String DESCRIPTOR_ADCLEANER_MANAGER_Ex = "android.os.AdCleanerManagerEx";
    private static final String TAG = "AdCleanerManagerEx";

    public static int printRuleMaps() {
        int flag = 0;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("network_management");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_ADCLEANER_MANAGER_Ex);
                _data.writeInt(0);
                b.transact(1019, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
                flag = 1;
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        return flag;
    }

    public static int cleanAdFilterRules(List<String> adAppList, boolean needRest) {
        int flag = 0;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("network_management");
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR_ADCLEANER_MANAGER_Ex);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CleanAdFilterRules needRest: ");
                stringBuilder.append(needRest);
                Log.d(str, stringBuilder.toString());
                if (needRest) {
                    _data.writeInt(1);
                } else if (adAppList == null) {
                    _data.writeInt(-1);
                } else {
                    _data.writeInt(0);
                    _data.writeStringList(adAppList);
                    Log.d(TAG, "CleanAdFilterRules adAppList: ");
                    for (int i = 0; i < adAppList.size(); i++) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(i);
                        stringBuilder2.append(" = ");
                        stringBuilder2.append((String) adAppList.get(i));
                        Log.d(str2, stringBuilder2.toString());
                    }
                }
                b.transact(1018, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                Log.d(TAG, "------- err: CleanAdFilterRules() RemoteException ! ");
                localRemoteException.printStackTrace();
                flag = 1;
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        return flag;
    }

    public static int setAdFilterRules(Map<String, List<String>> adViewMap, Map<String, List<String>> adIdMap, boolean needRest) {
        Map<String, List<String>> map = adViewMap;
        Map<String, List<String>> map2 = adIdMap;
        boolean z = needRest;
        int flag = 0;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("network_management");
        if (b != null) {
            try {
                int size;
                String keyString;
                String key;
                _data.writeInterfaceToken(DESCRIPTOR_ADCLEANER_MANAGER_Ex);
                _data.writeInt(z);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAdFilterRules needRest: ");
                stringBuilder.append(z);
                Log.d(str, stringBuilder.toString());
                if (map == null) {
                    _data.writeInt(-1);
                } else {
                    size = adViewMap.size();
                    _data.writeInt(size);
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("adViewMap size = ");
                    stringBuilder2.append(size);
                    Log.d(str2, stringBuilder2.toString());
                    Set<String> keysSet = adViewMap.keySet();
                    List<String> keysList = new ArrayList();
                    for (String keyString2 : keysSet) {
                        keysList.add(keyString2);
                    }
                    for (int i = 0; i < size; i++) {
                        key = (String) keysList.get(i);
                        List<String> value = (List) map.get(key);
                        _data.writeString(key);
                        _data.writeStringList(value);
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("i=");
                        stringBuilder3.append(i);
                        stringBuilder3.append(", send adViewMap key: ");
                        stringBuilder3.append(key);
                        Log.d(str3, stringBuilder3.toString());
                    }
                }
                if (map2 == null) {
                    _data.writeInt(-1);
                } else {
                    int size2 = adIdMap.size();
                    _data.writeInt(size2);
                    String str4 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(" adIdMap size = ");
                    stringBuilder4.append(size2);
                    Log.d(str4, stringBuilder4.toString());
                    Set<String> keysSet2 = adIdMap.keySet();
                    List<String> keysList2 = new ArrayList();
                    for (String key2 : keysSet2) {
                        keysList2.add(key2);
                    }
                    for (size = 0; size < size2; size++) {
                        String key3 = (String) keysList2.get(size);
                        List<String> value2 = (List) map2.get(key3);
                        _data.writeString(key3);
                        _data.writeStringList(value2);
                        keyString2 = TAG;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("i=");
                        stringBuilder5.append(size);
                        stringBuilder5.append(", send adIdMap key: ");
                        stringBuilder5.append(key3);
                        Log.d(keyString2, stringBuilder5.toString());
                    }
                }
                b.transact(1017, _data, _reply, 0);
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                Log.d(TAG, "------- err: setAdFilterRules() RemoteException ! ");
                localRemoteException.printStackTrace();
                flag = 1;
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        return flag;
    }
}
