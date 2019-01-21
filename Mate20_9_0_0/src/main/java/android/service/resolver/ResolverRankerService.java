package android.service.resolver;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.resolver.IResolverRankerService.Stub;
import android.util.Log;
import java.util.List;

@SystemApi
public abstract class ResolverRankerService extends Service {
    public static final String BIND_PERMISSION = "android.permission.BIND_RESOLVER_RANKER_SERVICE";
    private static final boolean DEBUG = false;
    private static final String HANDLER_THREAD_NAME = "RESOLVER_RANKER_SERVICE";
    public static final String HOLD_PERMISSION = "android.permission.PROVIDE_RESOLVER_RANKER_SERVICE";
    public static final String SERVICE_INTERFACE = "android.service.resolver.ResolverRankerService";
    private static final String TAG = "ResolverRankerService";
    private volatile Handler mHandler;
    private HandlerThread mHandlerThread;
    private ResolverRankerServiceWrapper mWrapper = null;

    private class ResolverRankerServiceWrapper extends Stub {
        private ResolverRankerServiceWrapper() {
        }

        public void predict(final List<ResolverTarget> targets, final IResolverRankerResult result) throws RemoteException {
            Runnable predictRunnable = new Runnable() {
                public void run() {
                    try {
                        ResolverRankerService.this.onPredictSharingProbabilities(targets);
                        ResolverRankerService.sendResult(targets, result);
                    } catch (Exception e) {
                        String str = ResolverRankerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onPredictSharingProbabilities failed; send null results: ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                        ResolverRankerService.sendResult(null, result);
                    }
                }
            };
            Handler h = ResolverRankerService.this.mHandler;
            if (h != null) {
                h.post(predictRunnable);
            }
        }

        public void train(final List<ResolverTarget> targets, final int selectedPosition) throws RemoteException {
            Runnable trainRunnable = new Runnable() {
                public void run() {
                    try {
                        ResolverRankerService.this.onTrainRankingModel(targets, selectedPosition);
                    } catch (Exception e) {
                        String str = ResolverRankerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onTrainRankingModel failed; skip train: ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                    }
                }
            };
            Handler h = ResolverRankerService.this.mHandler;
            if (h != null) {
                h.post(trainRunnable);
            }
        }
    }

    public void onPredictSharingProbabilities(List<ResolverTarget> list) {
    }

    public void onTrainRankingModel(List<ResolverTarget> list, int selectedPosition) {
    }

    public IBinder onBind(Intent intent) {
        if (!SERVICE_INTERFACE.equals(intent.getAction())) {
            return null;
        }
        if (this.mHandlerThread == null) {
            this.mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
            this.mHandlerThread.start();
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
        }
        if (this.mWrapper == null) {
            this.mWrapper = new ResolverRankerServiceWrapper();
        }
        return this.mWrapper;
    }

    public void onDestroy() {
        this.mHandler = null;
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
        }
        super.onDestroy();
    }

    private static void sendResult(List<ResolverTarget> targets, IResolverRankerResult result) {
        try {
            result.sendResult(targets);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("failed to send results: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }
}
