package com.nxp.nfc.gsma.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.AidGroup;
import android.nfc.cardemulation.NxpAidGroup;
import android.nfc.cardemulation.NxpApduServiceInfo;
import android.nfc.cardemulation.NxpApduServiceInfo.ESeInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.nxp.nfc.NxpConstants;
import com.nxp.nfc.NxpNfcAdapter;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NxpNfcController {
    public static final int BATTERY_ALL_STATES = 2;
    public static final int BATTERY_OPERATIONAL_STATE = 1;
    private static final int MW_PROTOCOL_MASK_ISO_DEP = 8;
    public static final int PROTOCOL_ISO_DEP = 16;
    public static final int SCREEN_ALL_MODES = 2;
    public static final int SCREEN_ON_AND_LOCKED_MODE = 1;
    static final String TAG = "NxpNfcController";
    public static final int TECHNOLOGY_NFC_A = 1;
    public static final int TECHNOLOGY_NFC_B = 2;
    public static final int TECHNOLOGY_NFC_F = 4;
    private NxpCallbacks mCallBack = null;
    Context mContext;
    private boolean mDialogBoxFlag = false;
    private NfcAdapter mNfcAdapter = null;
    private INxpNfcController mNfcControllerService = null;
    private NxpNfcAdapter mNxpNfcAdapter = null;
    private final BroadcastReceiver mOwnerReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int state = intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 0);
            String str = NxpNfcController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onReceive: action: ");
            stringBuilder.append(action);
            stringBuilder.append("mState: ");
            stringBuilder.append(NxpNfcController.this.mState);
            Log.d(str, stringBuilder.toString());
            if (state == 3 && NxpNfcController.this.mState && NxpNfcController.this.mDialogBoxFlag) {
                NxpNfcController.this.mCallBack.onNxpEnableNfcController(true);
                NxpNfcController.this.mDialogBoxFlag = false;
                NxpNfcController.this.mState = false;
                NxpNfcController.this.mContext.unregisterReceiver(NxpNfcController.this.mOwnerReceiver);
                NxpNfcController.this.mContext.unregisterReceiver(NxpNfcController.this.mReceiver);
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NxpConstants.ACTION_GSMA_ENABLE_SET_FLAG) && intent.getExtras() != null) {
                NxpNfcController.this.mState = intent.getExtras().getBoolean("ENABLE_STATE");
            }
            if (NxpNfcController.this.mState) {
                NxpNfcController.this.mDialogBoxFlag = true;
                return;
            }
            NxpNfcController.this.mCallBack.onNxpEnableNfcController(false);
            NxpNfcController.this.mContext.unregisterReceiver(NxpNfcController.this.mOwnerReceiver);
            NxpNfcController.this.mContext.unregisterReceiver(NxpNfcController.this.mReceiver);
        }
    };
    private final HashMap<String, NxpApduServiceInfo> mSeNameApduService = new HashMap();
    private boolean mState = false;

    public interface Callbacks {
        void onGetOffHostService(boolean z, String str, String str2, int i, List<String> list, List<AidGroup> list2);
    }

    public interface NxpCallbacks {
        void onNxpEnableNfcController(boolean z);
    }

    public NxpNfcController(Context context) {
        this.mContext = context;
        this.mNfcAdapter = NfcAdapter.getNfcAdapter(this.mContext);
        if (this.mNfcAdapter != null) {
            this.mNxpNfcAdapter = NxpNfcAdapter.getNxpNfcAdapter(this.mNfcAdapter);
        }
        if (this.mNxpNfcAdapter != null) {
            this.mNfcControllerService = this.mNxpNfcAdapter.getNxpNfcControllerInterface();
        }
    }

    public boolean isNxpNfcEnabled() {
        return this.mNfcAdapter.isEnabled();
    }

    public void enableNxpNfcController(NxpCallbacks cb) {
        this.mCallBack = cb;
        IntentFilter ownerFilter = new IntentFilter();
        ownerFilter.addAction("android.nfc.action.ADAPTER_STATE_CHANGED");
        this.mContext.registerReceiver(this.mOwnerReceiver, ownerFilter);
        IntentFilter filter = new IntentFilter();
        filter.addAction(NxpConstants.ACTION_GSMA_ENABLE_SET_FLAG);
        this.mContext.registerReceiver(this.mReceiver, filter, NxpConstants.PERMISSIONS_NFC, null);
        Intent enableNfc = new Intent();
        enableNfc.setAction(NxpConstants.ACTION_GSMA_ENABLE_NFC);
        this.mContext.sendBroadcast(enableNfc, NxpConstants.PERMISSIONS_NFC);
    }

    private NxpOffHostService convertApduServiceToOffHostService(PackageManager pm, NxpApduServiceInfo apduService) {
        String sEname = null;
        ResolveInfo resolveInfo = apduService.getResolveInfo();
        String description = apduService.getDescription();
        int seId = apduService.getSEInfo().getSeId();
        if (2 == seId) {
            sEname = NxpConstants.UICC_ID;
        } else if (4 == seId) {
            sEname = "SIM2";
        } else if (1 == seId) {
            sEname = "eSE";
        } else {
            Log.e(TAG, "Wrong SE ID");
        }
        boolean modifiable = apduService.getModifiable();
        int bannerId = apduService.getBannerId();
        Drawable banner = apduService.loadBanner(pm);
        int userId = apduService.getUid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("convertApduServiceToOffHostService begin,modifiable=");
        stringBuilder.append(modifiable);
        Log.d(str, stringBuilder.toString());
        Drawable banner2 = banner;
        NxpOffHostService mService = new NxpOffHostService(userId, description, sEname, resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name, modifiable);
        Iterator it;
        if (modifiable) {
            it = apduService.getDynamicNxpAidGroups().iterator();
            while (it.hasNext()) {
                mService.mNxpAidGroupList.add((NxpAidGroup) it.next());
            }
        } else {
            it = apduService.getStaticNxpAidGroups().iterator();
            while (it.hasNext()) {
                mService.mNxpAidGroupList.add((NxpAidGroup) it.next());
            }
        }
        mService.setContext(this.mContext);
        mService.setBannerId(bannerId);
        mService.setBanner(banner2);
        mService.setNxpNfcController(this);
        return mService;
    }

    private NxpApduServiceInfo convertOffhostServiceToApduService(NxpOffHostService mService, int userId, String pkg) {
        NxpOffHostService nxpOffHostService = mService;
        String description = mService.getDescription();
        boolean modifiable = mService.getModifiable();
        ArrayList<NxpAidGroup> dynamicNxpAidGroup = new ArrayList();
        dynamicNxpAidGroup.addAll(nxpOffHostService.mNxpAidGroupList);
        Drawable banner = mService.getBanner();
        byte[] byteArrayBanner = null;
        if (banner != null) {
            Bitmap bitmap = ((BitmapDrawable) banner).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(CompressFormat.PNG, 100, stream);
            byteArrayBanner = stream.toByteArray();
        }
        byte[] byteArrayBanner2 = byteArrayBanner;
        int seId = 0;
        String seName = mService.getLocation();
        int bannerId = nxpOffHostService.mBannerId;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.applicationInfo = new ApplicationInfo();
        resolveInfo.serviceInfo.packageName = pkg;
        resolveInfo.serviceInfo.name = mService.getServiceName();
        if (seName != null) {
            if (seName.equals(NxpConstants.UICC_ID) || seName.equals("SIM1")) {
                seId = 2;
            } else if (seName.equals("SIM2")) {
                seId = 4;
            } else if (seName.equals("eSE")) {
                seId = 1;
            } else {
                Log.e(TAG, "wrong Se name");
            }
        }
        int seId2 = seId;
        int powerstate = -1;
        return new NxpApduServiceInfo(resolveInfo, false, description, null, dynamicNxpAidGroup, false, bannerId, userId, pkg, new ESeInfo(seId2, -1), null, byteArrayBanner2, modifiable);
    }

    public boolean deleteOffHostService(int userId, String packageName, NxpOffHostService service) {
        boolean result = false;
        try {
            result = this.mNfcControllerService.deleteOffHostService(userId, packageName, convertOffhostServiceToApduService(service, userId, packageName));
        } catch (RemoteException e) {
            Log.e(TAG, "Exception:deleteOffHostService failed", e);
        }
        if (result) {
            return true;
        }
        Log.d(TAG, "GSMA: deleteOffHostService failed");
        return false;
    }

    public ArrayList<NxpOffHostService> getOffHostServices(int userId, String packageName) {
        Log.d(TAG, "getOffHostServices enter");
        List<NxpApduServiceInfo> apduServices = new ArrayList();
        ArrayList<NxpOffHostService> mService = new ArrayList();
        PackageManager pm = this.mContext.getPackageManager();
        try {
            apduServices = this.mNfcControllerService.getOffHostServices(userId, packageName);
            if (apduServices == null || apduServices.isEmpty()) {
                return null;
            }
            for (NxpApduServiceInfo service : apduServices) {
                mService.add(convertApduServiceToOffHostService(pm, service));
            }
            return mService;
        } catch (RemoteException e) {
            Log.e(TAG, "getOffHostServices failed", e);
            return null;
        }
    }

    public NxpOffHostService getDefaultOffHostService(int userId, String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getDefaultOffHostService packageName");
            stringBuilder.append(packageName);
            Log.d(str, stringBuilder.toString());
            NxpApduServiceInfo apduService = this.mNfcControllerService.getDefaultOffHostService(userId, packageName);
            if (apduService != null) {
                return convertApduServiceToOffHostService(pm, apduService);
            }
            Log.d(TAG, "getDefaultOffHostService: Service is NULL");
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "getDefaultOffHostService failed", e);
            return null;
        }
    }

    public boolean commitOffHostService(int userId, String packageName, NxpOffHostService service) {
        boolean result = false;
        String serviceName = service.getServiceName();
        NxpApduServiceInfo newService = convertOffhostServiceToApduService(service, userId, packageName);
        try {
            if (this.mNfcControllerService != null) {
                result = this.mNfcControllerService.commitOffHostService(userId, packageName, serviceName, newService);
            }
            if (result) {
                return true;
            }
            Log.d(TAG, "GSMA: commitOffHostService Failed");
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Exception:commitOffHostService failed", e);
            return false;
        }
    }

    public boolean commitOffHostService(String packageName, String seName, String description, int bannerResId, int uid, List<String> list, List<NxpAidGroup> nxpAidGroups) {
        RemoteException e;
        String str = packageName;
        String str2 = seName;
        int userId = UserHandle.myUserId();
        ArrayList<NxpAidGroup> dynamicNxpAidGroup = new ArrayList();
        dynamicNxpAidGroup.addAll(nxpAidGroups);
        int seId = 0;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.applicationInfo = new ApplicationInfo();
        resolveInfo.serviceInfo.packageName = str;
        resolveInfo.serviceInfo.name = str2;
        String secureElement = null;
        if (str2.equals(NxpConstants.UICC_ID) || str2.equals("SIM1")) {
            secureElement = NxpConstants.UICC_ID;
        } else if (str2.equals("SIM2")) {
            secureElement = NxpConstants.UICC2_ID;
        } else if (str2.equals("eSE1") || str2.equals("eSE")) {
            secureElement = NxpConstants.SMART_MX_ID;
        } else {
            Log.e(TAG, "wrong Se name");
        }
        String secureElement2 = secureElement;
        if (secureElement2.equals(NxpConstants.UICC_ID)) {
            seId = 2;
        } else if (secureElement2.equals(NxpConstants.UICC2_ID)) {
            seId = 4;
        } else if (secureElement2.equals(NxpConstants.SMART_MX_ID)) {
            seId = 1;
        } else if (secureElement2.equals(NxpConstants.HOST_ID)) {
            seId = 0;
        } else {
            Log.e(TAG, "wrong Se name");
        }
        int seId2 = seId;
        int powerstate = -1;
        int userId2 = userId;
        NxpApduServiceInfo newService = new NxpApduServiceInfo(resolveInfo, false, description, null, dynamicNxpAidGroup, false, bannerResId, userId, str, new ESeInfo(seId2, -1), null, null, true);
        String str3 = seName;
        this.mSeNameApduService.put(str3, newService);
        String str4;
        try {
            boolean result;
            if (this.mNfcControllerService != null) {
                try {
                    result = this.mNfcControllerService.commitOffHostService(userId2, packageName, str3, newService);
                } catch (RemoteException e2) {
                    e = e2;
                    Log.e(TAG, "Exception:commitOffHostService failed", e);
                    return false;
                }
            }
            str4 = packageName;
            result = false;
            if (result) {
                return true;
            }
            Log.d(TAG, "GSMA: commitOffHostService Failed");
            return false;
        } catch (RemoteException e3) {
            e = e3;
            seId2 = userId2;
            str4 = packageName;
            Log.e(TAG, "Exception:commitOffHostService failed", e);
            return false;
        }
    }

    public boolean deleteOffHostService(String packageName, String seName) {
        boolean result = false;
        try {
            result = this.mNfcControllerService.deleteOffHostService(UserHandle.myUserId(), packageName, (NxpApduServiceInfo) this.mSeNameApduService.get(seName));
        } catch (RemoteException e) {
            Log.e(TAG, "Exception:deleteOffHostService failed", e);
        }
        if (result) {
            return true;
        }
        Log.d(TAG, "GSMA: deleteOffHostService failed");
        return false;
    }

    public boolean getOffHostServices(String packageName, Callbacks callbacks) {
        RemoteException e;
        boolean isLast = false;
        List<NxpApduServiceInfo> apduServices = null;
        try {
            try {
                apduServices = this.mNfcControllerService.getOffHostServices(UserHandle.myUserId(), packageName);
                for (int i = 0; i < apduServices.size(); i++) {
                    String seName;
                    if (i == apduServices.size() - 1) {
                        isLast = true;
                    }
                    int seId = ((NxpApduServiceInfo) apduServices.get(i)).getSEInfo().getSeId();
                    if (2 == seId) {
                        seName = NxpConstants.UICC_ID;
                    } else if (4 == seId) {
                        seName = "SIM2";
                    } else if (1 == seId) {
                        seName = "eSE";
                    } else {
                        seName = null;
                        Log.e(TAG, "Wrong SE ID");
                    }
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getOffHostServices: seName = ");
                    stringBuilder.append(seName);
                    Log.d(str, stringBuilder.toString());
                    ArrayList<String> groupDescription = new ArrayList();
                    Iterator it = ((NxpApduServiceInfo) apduServices.get(i)).getNxpAidGroups().iterator();
                    while (it.hasNext()) {
                        groupDescription.add(((NxpAidGroup) it.next()).getDescription());
                    }
                    callbacks.onGetOffHostService(isLast, ((NxpApduServiceInfo) apduServices.get(i)).getDescription(), seName, ((NxpApduServiceInfo) apduServices.get(i)).getBannerId(), groupDescription, ((NxpApduServiceInfo) apduServices.get(i)).getAidGroups());
                }
                return true;
            } catch (RemoteException e2) {
                e = e2;
                Log.e(TAG, "getOffHostServices failed", e);
                return false;
            }
        } catch (RemoteException e3) {
            e = e3;
            String str2 = packageName;
            Log.e(TAG, "getOffHostServices failed", e);
            return false;
        }
    }

    public boolean getDefaultOffHostService(String packageName, Callbacks callbacks) {
        RemoteException e;
        boolean seId;
        Log.d(TAG, "getDefaultOffHostService: Enter");
        String seName = null;
        String str;
        try {
            try {
                NxpApduServiceInfo apduService = this.mNfcControllerService.getDefaultOffHostService(UserHandle.myUserId(), packageName);
                if (apduService == null) {
                    Log.w(TAG, "apduService is null, return false");
                    return false;
                }
                int seId2 = apduService.getSEInfo().getSeId();
                if (2 == seId2) {
                    try {
                        seName = NxpConstants.UICC_ID;
                    } catch (RemoteException e2) {
                        e = e2;
                        str = null;
                        Log.e(TAG, "getDefaultOffHostService failed", e);
                        return false;
                    }
                } else if (4 == seId2) {
                    seName = "SIM2";
                } else if (1 == seId2) {
                    seName = "eSE";
                } else {
                    Log.e(TAG, "Wrong SE ID");
                }
                str = seName;
                try {
                    seName = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getDefaultOffHostService: seName = ");
                    stringBuilder.append(str);
                    Log.d(seName, stringBuilder.toString());
                    ArrayList<String> groupDescription = new ArrayList();
                    Iterator it = apduService.getNxpAidGroups().iterator();
                    while (it.hasNext()) {
                        groupDescription.add(((NxpAidGroup) it.next()).getDescription());
                    }
                    callbacks.onGetOffHostService(true, apduService.getDescription(), str, apduService.getBannerId(), groupDescription, apduService.getAidGroups());
                    Log.d(TAG, "getDefaultOffHostService: End");
                    return true;
                } catch (RemoteException e3) {
                    e = e3;
                    Log.e(TAG, "getDefaultOffHostService failed", e);
                    return false;
                }
            } catch (RemoteException e4) {
                e = e4;
                str = null;
                seId = false;
                Log.e(TAG, "getDefaultOffHostService failed", e);
                return false;
            }
        } catch (RemoteException e5) {
            e = e5;
            String str2 = packageName;
            str = null;
            seId = false;
            Log.e(TAG, "getDefaultOffHostService failed", e);
            return false;
        }
    }

    public void enableMultiReception(String seName, String packageName) {
        try {
            this.mNfcControllerService.enableMultiReception(packageName, seName);
        } catch (RemoteException e) {
            Log.e(TAG, "enableMultiReception failed", e);
        }
    }

    public boolean isStaticOffhostService(int userId, String packageName, NxpOffHostService service) {
        boolean isStatic = false;
        List<NxpApduServiceInfo> nxpApduServices = new ArrayList();
        try {
            nxpApduServices = this.mNfcControllerService.getOffHostServices(userId, packageName);
            for (int i = 0; i < nxpApduServices.size(); i++) {
                NxpApduServiceInfo sService = (NxpApduServiceInfo) nxpApduServices.get(i);
                if (!sService.getModifiable() && service.getServiceName().compareTo(sService.getResolveInfo().serviceInfo.name) == 0) {
                    isStatic = true;
                }
            }
            return isStatic;
        } catch (RemoteException e) {
            Log.e(TAG, "getOffHostServices failed", e);
            return true;
        }
    }
}
