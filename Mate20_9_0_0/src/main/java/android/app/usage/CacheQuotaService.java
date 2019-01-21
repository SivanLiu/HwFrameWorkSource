package android.app.usage;

import android.annotation.SystemApi;
import android.app.Service;
import android.app.usage.ICacheQuotaService.Stub;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallback;
import android.util.Log;
import android.util.Pair;
import java.util.List;

@SystemApi
public abstract class CacheQuotaService extends Service {
    public static final String REQUEST_LIST_KEY = "requests";
    public static final String SERVICE_INTERFACE = "android.app.usage.CacheQuotaService";
    private static final String TAG = "CacheQuotaService";
    private Handler mHandler;
    private CacheQuotaServiceWrapper mWrapper;

    private final class ServiceHandler extends Handler {
        public static final int MSG_SEND_LIST = 1;

        public ServiceHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int action = msg.what;
            if (action != 1) {
                String str = CacheQuotaService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Handling unknown message: ");
                stringBuilder.append(action);
                Log.w(str, stringBuilder.toString());
                return;
            }
            Pair<RemoteCallback, List<CacheQuotaHint>> pair = msg.obj;
            List<CacheQuotaHint> processed = CacheQuotaService.this.onComputeCacheQuotaHints((List) pair.second);
            Bundle data = new Bundle();
            data.putParcelableList(CacheQuotaService.REQUEST_LIST_KEY, processed);
            pair.first.sendResult(data);
        }
    }

    private final class CacheQuotaServiceWrapper extends Stub {
        private CacheQuotaServiceWrapper() {
        }

        public void computeCacheQuotaHints(RemoteCallback callback, List<CacheQuotaHint> requests) {
            CacheQuotaService.this.mHandler.sendMessage(CacheQuotaService.this.mHandler.obtainMessage(1, Pair.create(callback, requests)));
        }
    }

    public abstract List<CacheQuotaHint> onComputeCacheQuotaHints(List<CacheQuotaHint> list);

    public void onCreate() {
        super.onCreate();
        this.mWrapper = new CacheQuotaServiceWrapper();
        this.mHandler = new ServiceHandler(getMainLooper());
    }

    public IBinder onBind(Intent intent) {
        return this.mWrapper;
    }
}
