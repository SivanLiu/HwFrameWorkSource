package android.nfc.cardemulation;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.net.wifi.WifiScanLog;
import android.nfc.INfcFCardEmulation;
import android.nfc.NfcAdapter;
import android.os.RemoteException;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public final class NfcFCardEmulation {
    static final String TAG = "NfcFCardEmulation";
    static HashMap<Context, NfcFCardEmulation> sCardEmus = new HashMap();
    static boolean sIsInitialized = false;
    static INfcFCardEmulation sService;
    final Context mContext;

    private NfcFCardEmulation(Context context, INfcFCardEmulation service) {
        this.mContext = context.getApplicationContext();
        sService = service;
    }

    public static synchronized NfcFCardEmulation getInstance(NfcAdapter adapter) {
        NfcFCardEmulation manager;
        synchronized (NfcFCardEmulation.class) {
            if (adapter != null) {
                Context context = adapter.getContext();
                if (context != null) {
                    if (!sIsInitialized) {
                        IPackageManager pm = ActivityThread.getPackageManager();
                        if (pm != null) {
                            try {
                                if (pm.hasSystemFeature("android.hardware.nfc.hcef", 0)) {
                                    sIsInitialized = true;
                                } else {
                                    Log.e(TAG, "This device does not support NFC-F card emulation");
                                    throw new UnsupportedOperationException();
                                }
                            } catch (RemoteException e) {
                                Log.e(TAG, "PackageManager query failed.");
                                throw new UnsupportedOperationException();
                            }
                        }
                        Log.e(TAG, "Cannot get PackageManager");
                        throw new UnsupportedOperationException();
                    }
                    manager = (NfcFCardEmulation) sCardEmus.get(context);
                    if (manager == null) {
                        INfcFCardEmulation service = adapter.getNfcFCardEmulationService();
                        if (service != null) {
                            manager = new NfcFCardEmulation(context, service);
                            sCardEmus.put(context, manager);
                        } else {
                            Log.e(TAG, "This device does not implement the INfcFCardEmulation interface.");
                            throw new UnsupportedOperationException();
                        }
                    }
                } else {
                    Log.e(TAG, "NfcAdapter context is null.");
                    throw new UnsupportedOperationException();
                }
            }
            throw new NullPointerException("NfcAdapter is null");
        }
        return manager;
    }

    public String getSystemCodeForService(ComponentName service) throws RuntimeException {
        if (service != null) {
            try {
                return sService.getSystemCodeForService(this.mContext.getUserId(), service);
            } catch (RemoteException e) {
                recoverService();
                if (sService == null) {
                    Log.e(TAG, "Failed to recover CardEmulationService.");
                    return null;
                }
                try {
                    return sService.getSystemCodeForService(this.mContext.getUserId(), service);
                } catch (RemoteException ee) {
                    Log.e(TAG, "Failed to reach CardEmulationService.");
                    ee.rethrowAsRuntimeException();
                    return null;
                }
            }
        }
        throw new NullPointerException("service is null");
    }

    public boolean registerSystemCodeForService(ComponentName service, String systemCode) throws RuntimeException {
        if (service == null || systemCode == null) {
            throw new NullPointerException("service or systemCode is null");
        }
        try {
            return sService.registerSystemCodeForService(this.mContext.getUserId(), service, systemCode);
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return false;
            }
            try {
                return sService.registerSystemCodeForService(this.mContext.getUserId(), service, systemCode);
            } catch (RemoteException ee) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                ee.rethrowAsRuntimeException();
                return false;
            }
        }
    }

    public boolean unregisterSystemCodeForService(ComponentName service) throws RuntimeException {
        if (service != null) {
            try {
                return sService.removeSystemCodeForService(this.mContext.getUserId(), service);
            } catch (RemoteException e) {
                recoverService();
                if (sService == null) {
                    Log.e(TAG, "Failed to recover CardEmulationService.");
                    return false;
                }
                try {
                    return sService.removeSystemCodeForService(this.mContext.getUserId(), service);
                } catch (RemoteException ee) {
                    Log.e(TAG, "Failed to reach CardEmulationService.");
                    ee.rethrowAsRuntimeException();
                    return false;
                }
            }
        }
        throw new NullPointerException("service is null");
    }

    public String getNfcid2ForService(ComponentName service) throws RuntimeException {
        if (service != null) {
            try {
                return sService.getNfcid2ForService(this.mContext.getUserId(), service);
            } catch (RemoteException e) {
                recoverService();
                if (sService == null) {
                    Log.e(TAG, "Failed to recover CardEmulationService.");
                    return null;
                }
                try {
                    return sService.getNfcid2ForService(this.mContext.getUserId(), service);
                } catch (RemoteException ee) {
                    Log.e(TAG, "Failed to reach CardEmulationService.");
                    ee.rethrowAsRuntimeException();
                    return null;
                }
            }
        }
        throw new NullPointerException("service is null");
    }

    public boolean setNfcid2ForService(ComponentName service, String nfcid2) throws RuntimeException {
        if (service == null || nfcid2 == null) {
            throw new NullPointerException("service or nfcid2 is null");
        }
        try {
            return sService.setNfcid2ForService(this.mContext.getUserId(), service, nfcid2);
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return false;
            }
            try {
                return sService.setNfcid2ForService(this.mContext.getUserId(), service, nfcid2);
            } catch (RemoteException ee) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                ee.rethrowAsRuntimeException();
                return false;
            }
        }
    }

    public boolean enableService(Activity activity, ComponentName service) throws RuntimeException {
        if (activity == null || service == null) {
            throw new NullPointerException("activity or service is null");
        } else if (activity.isResumed()) {
            try {
                return sService.enableNfcFForegroundService(service);
            } catch (RemoteException e) {
                recoverService();
                if (sService == null) {
                    Log.e(TAG, "Failed to recover CardEmulationService.");
                    return false;
                }
                try {
                    return sService.enableNfcFForegroundService(service);
                } catch (RemoteException ee) {
                    Log.e(TAG, "Failed to reach CardEmulationService.");
                    ee.rethrowAsRuntimeException();
                    return false;
                }
            }
        } else {
            throw new IllegalArgumentException("Activity must be resumed.");
        }
    }

    public boolean disableService(Activity activity) throws RuntimeException {
        if (activity == null) {
            throw new NullPointerException("activity is null");
        } else if (activity.isResumed()) {
            try {
                return sService.disableNfcFForegroundService();
            } catch (RemoteException e) {
                recoverService();
                if (sService == null) {
                    Log.e(TAG, "Failed to recover CardEmulationService.");
                    return false;
                }
                try {
                    return sService.disableNfcFForegroundService();
                } catch (RemoteException ee) {
                    Log.e(TAG, "Failed to reach CardEmulationService.");
                    ee.rethrowAsRuntimeException();
                    return false;
                }
            }
        } else {
            throw new IllegalArgumentException("Activity must be resumed.");
        }
    }

    public List<NfcFServiceInfo> getNfcFServices() {
        try {
            return sService.getNfcFServices(this.mContext.getUserId());
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return null;
            }
            try {
                return sService.getNfcFServices(this.mContext.getUserId());
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                return null;
            }
        }
    }

    public int getMaxNumOfRegisterableSystemCodes() {
        try {
            return sService.getMaxNumOfRegisterableSystemCodes();
        } catch (RemoteException e) {
            recoverService();
            if (sService == null) {
                Log.e(TAG, "Failed to recover CardEmulationService.");
                return -1;
            }
            try {
                return sService.getMaxNumOfRegisterableSystemCodes();
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to reach CardEmulationService.");
                return -1;
            }
        }
    }

    public static boolean isValidSystemCode(String systemCode) {
        if (systemCode == null) {
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        if (systemCode.length() != 4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("System Code ");
            stringBuilder.append(systemCode);
            stringBuilder.append(" is not a valid System Code.");
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (!systemCode.startsWith(WifiScanLog.EVENT_KEY4) || systemCode.toUpperCase(Locale.US).endsWith("FF")) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("System Code ");
            stringBuilder.append(systemCode);
            stringBuilder.append(" is not a valid System Code.");
            Log.e(str, stringBuilder.toString());
            return false;
        } else {
            try {
                Integer.parseInt(systemCode, 16);
                return true;
            } catch (NumberFormatException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("System Code ");
                stringBuilder2.append(systemCode);
                stringBuilder2.append(" is not a valid System Code.");
                Log.e(str2, stringBuilder2.toString());
                return false;
            }
        }
    }

    public static boolean isValidNfcid2(String nfcid2) {
        if (nfcid2 == null) {
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        if (nfcid2.length() != 16) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NFCID2 ");
            stringBuilder.append(nfcid2);
            stringBuilder.append(" is not a valid NFCID2.");
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (nfcid2.toUpperCase(Locale.US).startsWith("02FE")) {
            try {
                Long.parseLong(nfcid2, 16);
                return true;
            } catch (NumberFormatException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("NFCID2 ");
                stringBuilder2.append(nfcid2);
                stringBuilder2.append(" is not a valid NFCID2.");
                Log.e(str2, stringBuilder2.toString());
                return false;
            }
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NFCID2 ");
            stringBuilder.append(nfcid2);
            stringBuilder.append(" is not a valid NFCID2.");
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    void recoverService() {
        sService = NfcAdapter.getDefaultAdapter(this.mContext).getNfcFCardEmulationService();
    }
}
