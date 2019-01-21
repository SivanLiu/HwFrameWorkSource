package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsServiceController;
import android.telephony.ims.aidl.IImsServiceController.Stub;
import android.telephony.ims.stub.ImsFeatureConfiguration.FeatureSlotPair;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ImsServiceFeatureQueryManager {
    private final Map<ComponentName, ImsServiceFeatureQuery> mActiveQueries = new HashMap();
    private final Context mContext;
    private final Listener mListener;
    private final Object mLock = new Object();

    private final class ImsServiceFeatureQuery implements ServiceConnection {
        private static final String LOG_TAG = "ImsServiceFeatureQuery";
        private final String mIntentFilter;
        private final ComponentName mName;

        ImsServiceFeatureQuery(ComponentName name, String intentFilter) {
            this.mName = name;
            this.mIntentFilter = intentFilter;
        }

        public boolean start() {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start: intent filter=");
            stringBuilder.append(this.mIntentFilter);
            stringBuilder.append(", name=");
            stringBuilder.append(this.mName);
            Log.d(str, stringBuilder.toString());
            boolean bindStarted = ImsServiceFeatureQueryManager.this.mContext.bindService(new Intent(this.mIntentFilter).setComponent(this.mName), this, 67108929);
            if (!bindStarted) {
                cleanup();
            }
            return bindStarted;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceConnected for component: ");
            stringBuilder.append(name);
            Log.i(str, stringBuilder.toString());
            if (service != null) {
                queryImsFeatures(Stub.asInterface(service));
                return;
            }
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceConnected: ");
            stringBuilder.append(name);
            stringBuilder.append(" binder null, cleaning up.");
            Log.w(str, stringBuilder.toString());
            cleanup();
        }

        public void onServiceDisconnected(ComponentName name) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceDisconnected for component: ");
            stringBuilder.append(name);
            Log.w(str, stringBuilder.toString());
        }

        private void queryImsFeatures(IImsServiceController controller) {
            try {
                Set<FeatureSlotPair> servicePairs = controller.querySupportedImsFeatures().getServiceFeatures();
                cleanup();
                ImsServiceFeatureQueryManager.this.mListener.onComplete(this.mName, servicePairs);
            } catch (RemoteException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("queryImsFeatures - error: ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
                cleanup();
                ImsServiceFeatureQueryManager.this.mListener.onError(this.mName);
            }
        }

        private void cleanup() {
            ImsServiceFeatureQueryManager.this.mContext.unbindService(this);
            synchronized (ImsServiceFeatureQueryManager.this.mLock) {
                ImsServiceFeatureQueryManager.this.mActiveQueries.remove(this.mName);
            }
        }
    }

    public interface Listener {
        void onComplete(ComponentName componentName, Set<FeatureSlotPair> set);

        void onError(ComponentName componentName);
    }

    public ImsServiceFeatureQueryManager(Context context, Listener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    public boolean startQuery(ComponentName name, String intentFilter) {
        synchronized (this.mLock) {
            if (this.mActiveQueries.containsKey(name)) {
                return true;
            }
            ImsServiceFeatureQuery query = new ImsServiceFeatureQuery(name, intentFilter);
            this.mActiveQueries.put(name, query);
            boolean start = query.start();
            return start;
        }
    }

    public boolean isQueryInProgress() {
        int isEmpty;
        synchronized (this.mLock) {
            isEmpty = this.mActiveQueries.isEmpty() ^ 1;
        }
        return isEmpty;
    }
}
