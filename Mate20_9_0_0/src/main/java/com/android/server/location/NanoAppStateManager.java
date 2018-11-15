package com.android.server.location;

import android.hardware.contexthub.V1_0.HubAppInfo;
import android.hardware.location.NanoAppInstanceInfo;
import android.util.Log;
import com.android.server.os.HwBootFail;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

class NanoAppStateManager {
    private static final boolean ENABLE_LOG_DEBUG = true;
    private static final String TAG = "NanoAppStateManager";
    private final HashMap<Integer, NanoAppInstanceInfo> mNanoAppHash = new HashMap();
    private int mNextHandle = 0;

    NanoAppStateManager() {
    }

    synchronized NanoAppInstanceInfo getNanoAppInstanceInfo(int nanoAppHandle) {
        return (NanoAppInstanceInfo) this.mNanoAppHash.get(Integer.valueOf(nanoAppHandle));
    }

    synchronized Collection<NanoAppInstanceInfo> getNanoAppInstanceInfoCollection() {
        return this.mNanoAppHash.values();
    }

    synchronized int getNanoAppHandle(int contextHubId, long nanoAppId) {
        for (NanoAppInstanceInfo info : this.mNanoAppHash.values()) {
            if (info.getContexthubId() == contextHubId && info.getAppId() == nanoAppId) {
                return info.getHandle();
            }
        }
        return -1;
    }

    synchronized void addNanoAppInstance(int contextHubId, long nanoAppId, int nanoAppVersion) {
        synchronized (this) {
            removeNanoAppInstance(contextHubId, nanoAppId);
            if (this.mNanoAppHash.size() == HwBootFail.STAGE_BOOT_SUCCESS) {
                Log.e(TAG, "Error adding nanoapp instance: max limit exceeded");
                return;
            }
            String str;
            StringBuilder stringBuilder;
            int i = 0;
            int nanoAppHandle = this.mNextHandle;
            int i2 = 0;
            while (i2 <= HwBootFail.STAGE_BOOT_SUCCESS) {
                if (this.mNanoAppHash.containsKey(Integer.valueOf(nanoAppHandle))) {
                    nanoAppHandle = nanoAppHandle == HwBootFail.STAGE_BOOT_SUCCESS ? 0 : nanoAppHandle + 1;
                    i2++;
                } else {
                    this.mNanoAppHash.put(Integer.valueOf(nanoAppHandle), new NanoAppInstanceInfo(nanoAppHandle, nanoAppId, nanoAppVersion, contextHubId));
                    if (nanoAppHandle != HwBootFail.STAGE_BOOT_SUCCESS) {
                        i = nanoAppHandle + 1;
                    }
                    this.mNextHandle = i;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Added app instance with handle ");
                    stringBuilder.append(nanoAppHandle);
                    stringBuilder.append(" to hub ");
                    stringBuilder.append(contextHubId);
                    stringBuilder.append(": ID=0x");
                    stringBuilder.append(Long.toHexString(nanoAppId));
                    stringBuilder.append(", version=0x");
                    stringBuilder.append(Integer.toHexString(nanoAppVersion));
                    Log.v(str, stringBuilder.toString());
                }
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Added app instance with handle ");
            stringBuilder.append(nanoAppHandle);
            stringBuilder.append(" to hub ");
            stringBuilder.append(contextHubId);
            stringBuilder.append(": ID=0x");
            stringBuilder.append(Long.toHexString(nanoAppId));
            stringBuilder.append(", version=0x");
            stringBuilder.append(Integer.toHexString(nanoAppVersion));
            Log.v(str, stringBuilder.toString());
        }
    }

    synchronized void removeNanoAppInstance(int contextHubId, long nanoAppId) {
        this.mNanoAppHash.remove(Integer.valueOf(getNanoAppHandle(contextHubId, nanoAppId)));
    }

    synchronized void updateCache(int contextHubId, List<HubAppInfo> nanoAppInfoList) {
        HashSet<Long> nanoAppIdSet = new HashSet();
        for (HubAppInfo appInfo : nanoAppInfoList) {
            handleQueryAppEntry(contextHubId, appInfo.appId, appInfo.version);
            nanoAppIdSet.add(Long.valueOf(appInfo.appId));
        }
        Iterator<NanoAppInstanceInfo> iterator = this.mNanoAppHash.values().iterator();
        while (iterator.hasNext()) {
            NanoAppInstanceInfo info = (NanoAppInstanceInfo) iterator.next();
            if (info.getContexthubId() == contextHubId && !nanoAppIdSet.contains(Long.valueOf(info.getAppId()))) {
                iterator.remove();
            }
        }
    }

    private void handleQueryAppEntry(int contextHubId, long nanoAppId, int nanoAppVersion) {
        int nanoAppHandle = getNanoAppHandle(contextHubId, nanoAppId);
        if (nanoAppHandle == -1) {
            addNanoAppInstance(contextHubId, nanoAppId, nanoAppVersion);
        } else if (((NanoAppInstanceInfo) this.mNanoAppHash.get(Integer.valueOf(nanoAppHandle))).getAppVersion() != nanoAppVersion) {
            this.mNanoAppHash.put(Integer.valueOf(nanoAppHandle), new NanoAppInstanceInfo(nanoAppHandle, nanoAppId, nanoAppVersion, contextHubId));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Updated app instance with handle ");
            stringBuilder.append(nanoAppHandle);
            stringBuilder.append(" at hub ");
            stringBuilder.append(contextHubId);
            stringBuilder.append(": ID=0x");
            stringBuilder.append(Long.toHexString(nanoAppId));
            stringBuilder.append(", version=0x");
            stringBuilder.append(Integer.toHexString(nanoAppVersion));
            Log.v(str, stringBuilder.toString());
        }
    }
}
