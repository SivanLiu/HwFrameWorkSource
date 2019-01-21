package android.telephony;

import android.os.ServiceManager;
import com.android.internal.telephony.ISmsInterception;
import com.android.internal.telephony.ISmsInterception.Stub;

public class SmsInterceptionManager {
    private static final String LOG_TAG = "SmsInterceptionManager";
    private static SmsInterceptionManager sInstance = null;

    public static synchronized SmsInterceptionManager getInstance() {
        SmsInterceptionManager smsInterceptionManager;
        synchronized (SmsInterceptionManager.class) {
            if (sInstance == null) {
                sInstance = new SmsInterceptionManager();
            }
            smsInterceptionManager = sInstance;
        }
        return smsInterceptionManager;
    }

    public void registerListener(SmsInterceptionListener listener, int priority) {
        try {
            ISmsInterception iSmsInterception = getISmsInterception();
            if (iSmsInterception != null) {
                iSmsInterception.registerListener(listener.callback, priority);
            }
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Problem while register listener: ");
            stringBuilder.append(e.getMessage());
            Rlog.e(str, stringBuilder.toString());
        }
    }

    public void unregisterListener(int priority) {
        try {
            ISmsInterception iSmsInterception = getISmsInterception();
            if (iSmsInterception != null) {
                iSmsInterception.unregisterListener(priority);
            }
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Problem while unregister listener: ");
            stringBuilder.append(e.getMessage());
            Rlog.e(str, stringBuilder.toString());
        }
    }

    private ISmsInterception getISmsInterception() {
        return Stub.asInterface(ServiceManager.getService("isms_interception"));
    }
}
