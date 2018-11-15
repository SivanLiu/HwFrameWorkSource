package com.android.server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import org.simalliance.openmobileapi.service.ISmartcardService;
import org.simalliance.openmobileapi.service.ISmartcardServiceCallback;
import org.simalliance.openmobileapi.service.ISmartcardSystemService.Stub;
import org.simalliance.openmobileapi.service.SmartcardError;

public class SmartcardSystemService extends Stub {
    private static final int AID_APP = 10000;
    private static final int CONNECTED = 2;
    private static final int CONNECTING = 1;
    private static final int DISCONNECTED = 0;
    public static final String SMARTCARD_SERVICE_TAG = "SmartcardSystemService";
    private final ISmartcardServiceCallback callback = new ISmartcardServiceCallback.Stub() {
        public void notifyEvent(int eventType, String name) throws RemoteException {
        }
    };
    public volatile Exception lastException;
    private int mBindStatus = 0;
    private ServiceConnection mConnection;
    private Context mContext;
    private boolean mIccCardReady;
    private SmartcardSystemHandler mSmartcardSystemHandler;
    private volatile ISmartcardService smartcardService;

    private static class SmartcardSystemHandler extends Handler {
        public SmartcardSystemHandler(Looper looper) {
            super(looper, null, true);
        }
    }

    private static String bytesToString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", new Object[]{Integer.valueOf(bytes[i] & 255)}));
        }
        return sb.toString();
    }

    private byte[] stringToByteArray(String s) {
        byte[] b = new byte[(s.length() / 2)];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(s.substring(2 * i, (2 * i) + 2), 16);
        }
        return b;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[(len / 2)];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public SmartcardSystemService(Context context) {
        if (context != null) {
            this.mContext = context;
            this.mSmartcardSystemHandler = new SmartcardSystemHandler(BackgroundThread.get().getLooper());
            this.mConnection = new ServiceConnection() {
                public synchronized void onServiceConnected(ComponentName className, IBinder service) {
                    SmartcardSystemService.this.smartcardService = ISmartcardService.Stub.asInterface(service);
                    Log.i(SmartcardSystemService.SMARTCARD_SERVICE_TAG, "Smartcard system service onServiceConnected");
                    SmartcardSystemService.this.mBindStatus = 2;
                }

                public void onServiceDisconnected(ComponentName className) {
                    SmartcardSystemService.this.smartcardService = null;
                    Log.i(SmartcardSystemService.SMARTCARD_SERVICE_TAG, "Smartcard system service onServiceDisconnected");
                    SmartcardSystemService.this.mBindStatus = 0;
                }
            };
            BroadcastReceiver apduServiceLaunchedReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) && AwareJobSchedulerConstants.SIM_STATUS_READY.equals(intent.getStringExtra("ss"))) {
                        Log.i(SmartcardSystemService.SMARTCARD_SERVICE_TAG, "INTENT_VALUE_ICC_READY");
                        SmartcardSystemService.this.mIccCardReady = true;
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            context.registerReceiver(apduServiceLaunchedReceiver, intentFilter);
            return;
        }
        throw new NullPointerException("context must not be null");
    }

    public void closeChannel(long hChannel) throws RemoteException {
        if (checkAccessSmartcardAPI()) {
            String str = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("called: closeChannel(");
            stringBuilder.append(hChannel);
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
            SmartcardError error = new SmartcardError();
            this.smartcardService.closeChannel(hChannel, error);
            this.lastException = error.createException();
            String str2 = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SmartcardError: ");
            stringBuilder2.append(error.toString());
            Log.i(str2, stringBuilder2.toString());
            return;
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    public String getReaders() throws RemoteException {
        if (checkAccessSmartcardAPI()) {
            Log.i(SMARTCARD_SERVICE_TAG, "called: getReaders()");
            SmartcardError error = new SmartcardError();
            String[] result = this.smartcardService.getReaders(error);
            int i = 0;
            for (int i2 = 0; i2 < result.length; i2++) {
                String str = SMARTCARD_SERVICE_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getReaders(");
                stringBuilder.append(i2);
                stringBuilder.append(") returned: ");
                stringBuilder.append(result[i2]);
                Log.i(str, stringBuilder.toString());
            }
            this.lastException = error.createException();
            StringBuffer readerlist = new StringBuffer();
            while (i < result.length) {
                readerlist.append(result[i]);
                readerlist.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                i++;
            }
            return readerlist.toString();
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    public boolean isCardPresent(String reader) throws RemoteException {
        if (checkAccessSmartcardAPI()) {
            SmartcardError error = new SmartcardError();
            boolean result = this.smartcardService.isCardPresent(reader, error);
            String str = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isCardPresent(");
            stringBuilder.append(reader);
            stringBuilder.append(") returned: ");
            stringBuilder.append(result);
            Log.i(str, stringBuilder.toString());
            this.lastException = error.createException();
            return result;
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    public long openBasicChannel(String reader) throws RemoteException {
        if (checkAccessSmartcardAPI()) {
            String str = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("called: openBasicChannel(");
            stringBuilder.append(reader);
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
            SmartcardError error = new SmartcardError();
            long channelValue = this.smartcardService.openBasicChannel(reader, this.callback, error);
            this.lastException = error.createException();
            return channelValue;
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    public long openBasicChannelAid(String reader, String aid) throws RemoteException {
        if (checkAccessSmartcardAPI()) {
            String str = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("called: openBasicChannelAid (");
            stringBuilder.append(reader);
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
            SmartcardError error = new SmartcardError();
            long channelValue = this.smartcardService.openBasicChannelAid(reader, stringToByteArray(aid), this.callback, error);
            this.lastException = error.createException();
            return channelValue;
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    public long openLogicalChannel(String reader, String aid) throws RemoteException {
        if (checkAccessSmartcardAPI()) {
            String str = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("called: openLogicalChannel(");
            stringBuilder.append(reader);
            stringBuilder.append(", ");
            stringBuilder.append(aid);
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
            SmartcardError error = new SmartcardError();
            long channelValue = this.smartcardService.openLogicalChannel(reader, stringToByteArray(aid), this.callback, error);
            this.lastException = error.createException();
            return channelValue;
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    public String transmit(long hChannel, String command) throws RemoteException {
        if (checkAccessSmartcardAPI()) {
            String str = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("called: transmit(");
            stringBuilder.append(hChannel);
            stringBuilder.append(", ");
            stringBuilder.append(command);
            stringBuilder.append(")");
            Log.i(str, stringBuilder.toString());
            SmartcardError error = new SmartcardError();
            byte[] cmd = hexStringToByteArray(command);
            String str2 = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("transmitting: ");
            stringBuilder2.append(bytesToString(cmd));
            Log.i(str2, stringBuilder2.toString());
            str2 = "";
            String str3;
            StringBuilder stringBuilder3;
            try {
                byte[] rsp = this.smartcardService.transmit(hChannel, cmd, error);
                if (rsp != null) {
                    str3 = SMARTCARD_SERVICE_TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("transmit returned: ");
                    stringBuilder3.append(bytesToString(rsp));
                    Log.i(str3, stringBuilder3.toString());
                    str2 = bytesToString(rsp);
                }
            } catch (Exception e) {
                str3 = SMARTCARD_SERVICE_TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("transmit exception: ");
                stringBuilder3.append(e.toString());
                Log.w(str3, stringBuilder3.toString());
                str3 = SMARTCARD_SERVICE_TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("transmit Error object: ");
                stringBuilder3.append(error.toString());
                Log.w(str3, stringBuilder3.toString());
            }
            String str4 = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("transmit returned: ");
            stringBuilder4.append(str2);
            Log.i(str4, stringBuilder4.toString());
            this.lastException = error.createException();
            return str2;
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    public String getLastError() {
        if (checkAccessSmartcardAPI()) {
            Log.i(SMARTCARD_SERVICE_TAG, "called: getLastError");
            String strErrorMessage = "";
            if (this.lastException != null) {
                strErrorMessage = this.lastException.getMessage();
                if (strErrorMessage == null) {
                    strErrorMessage = this.lastException.toString();
                }
                String str = SMARTCARD_SERVICE_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getLastError - message ");
                stringBuilder.append(strErrorMessage);
                Log.w(str, stringBuilder.toString());
            }
            return strErrorMessage;
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    private void bindSmartCardService() {
        Log.i(SMARTCARD_SERVICE_TAG, "called bindSmartCardService");
        this.mSmartcardSystemHandler.post(new Runnable() {
            public void run() {
                if (SmartcardSystemService.this.mBindStatus == 0) {
                    Log.i(SmartcardSystemService.SMARTCARD_SERVICE_TAG, "begin bind SmartCardService");
                    Intent startIntent = new Intent(ISmartcardService.class.getName());
                    ComponentName comp = startIntent.resolveSystemService(SmartcardSystemService.this.mContext.getPackageManager(), 0);
                    startIntent.setComponent(comp);
                    if (comp != null) {
                        boolean result = SmartcardSystemService.this.mContext.bindService(startIntent, SmartcardSystemService.this.mConnection, 1);
                        String str = SmartcardSystemService.SMARTCARD_SERVICE_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("bindService result: ");
                        stringBuilder.append(result);
                        Log.i(str, stringBuilder.toString());
                        if (result) {
                            SmartcardSystemService.this.mBindStatus = 1;
                        }
                    } else {
                        Log.i(SmartcardSystemService.SMARTCARD_SERVICE_TAG, "SmartcardService not exist");
                        SmartcardSystemService.this.mBindStatus = 0;
                    }
                }
            }
        });
    }

    public boolean connectSmartCardService() throws RemoteException {
        if (checkAccessSmartcardAPI()) {
            Log.i(SMARTCARD_SERVICE_TAG, "connectSmartCardService");
            if (this.mIccCardReady) {
                Log.i(SMARTCARD_SERVICE_TAG, "Icc Card Ready");
                if (this.mBindStatus == 0) {
                    bindSmartCardService();
                }
            }
            String str = SMARTCARD_SERVICE_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bind result:");
            stringBuilder.append(this.mBindStatus);
            Log.i(str, stringBuilder.toString());
            if (this.mBindStatus == 2) {
                return true;
            }
            return false;
        }
        throw new SecurityException("Permission denied for accessing Smartcard API");
    }

    private boolean checkAccessSmartcardAPI() {
        if (Binder.getCallingUid() < 10000) {
            return true;
        }
        return false;
    }
}
