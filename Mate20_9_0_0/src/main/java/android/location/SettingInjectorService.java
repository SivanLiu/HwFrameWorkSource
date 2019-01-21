package android.location;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public abstract class SettingInjectorService extends Service {
    public static final String ACTION_INJECTED_SETTING_CHANGED = "android.location.InjectedSettingChanged";
    public static final String ACTION_SERVICE_INTENT = "android.location.SettingInjectorService";
    public static final String ATTRIBUTES_NAME = "injected-location-setting";
    public static final String ENABLED_KEY = "enabled";
    public static final String MESSENGER_KEY = "messenger";
    public static final String META_DATA_NAME = "android.location.SettingInjectorService";
    private static final String TAG = "SettingInjectorService";
    private final String mName;

    protected abstract boolean onGetEnabled();

    @Deprecated
    protected abstract String onGetSummary();

    public SettingInjectorService(String name) {
        this.mName = name;
    }

    public final IBinder onBind(Intent intent) {
        return null;
    }

    public final void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    public final int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        stopSelf(startId);
        return 2;
    }

    private void onHandleIntent(Intent intent) {
        try {
            sendStatus(intent, onGetEnabled());
        } catch (RuntimeException e) {
            sendStatus(intent, true);
            throw e;
        }
    }

    private void sendStatus(Intent intent, boolean enabled) {
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putBoolean("enabled", enabled);
        message.setData(bundle);
        if (Log.isLoggable(TAG, 3)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mName);
            stringBuilder.append(": received ");
            stringBuilder.append(intent);
            stringBuilder.append(", enabled=");
            stringBuilder.append(enabled);
            stringBuilder.append(", sending message: ");
            stringBuilder.append(message);
            Log.d(str, stringBuilder.toString());
        }
        try {
            ((Messenger) intent.getParcelableExtra(MESSENGER_KEY)).send(message);
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mName);
            stringBuilder2.append(": sending dynamic status failed");
            Log.e(str2, stringBuilder2.toString(), e);
        } catch (NullPointerException e2) {
            Log.e(TAG, "Messenger is Null");
        }
    }
}
