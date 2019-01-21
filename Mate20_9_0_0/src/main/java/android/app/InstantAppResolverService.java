package android.app;

import android.annotation.SystemApi;
import android.app.IInstantAppResolver.Stub;
import android.content.Context;
import android.content.Intent;
import android.content.pm.InstantAppResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.SomeArgs;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SystemApi
public abstract class InstantAppResolverService extends Service {
    private static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    public static final String EXTRA_RESOLVE_INFO = "android.app.extra.RESOLVE_INFO";
    public static final String EXTRA_SEQUENCE = "android.app.extra.SEQUENCE";
    private static final String TAG = "PackageManager";
    Handler mHandler;

    public static final class InstantAppResolutionCallback {
        private final IRemoteCallback mCallback;
        private final int mSequence;

        InstantAppResolutionCallback(int sequence, IRemoteCallback callback) {
            this.mCallback = callback;
            this.mSequence = sequence;
        }

        public void onInstantAppResolveInfo(List<InstantAppResolveInfo> resolveInfo) {
            Bundle data = new Bundle();
            data.putParcelableList(InstantAppResolverService.EXTRA_RESOLVE_INFO, resolveInfo);
            data.putInt(InstantAppResolverService.EXTRA_SEQUENCE, this.mSequence);
            try {
                this.mCallback.sendResult(data);
            } catch (RemoteException e) {
                Log.e(InstantAppResolverService.TAG, "onInstantAppResolveInfo()");
            }
        }
    }

    private final class ServiceHandler extends Handler {
        public static final int MSG_GET_INSTANT_APP_INTENT_FILTER = 2;
        public static final int MSG_GET_INSTANT_APP_RESOLVE_INFO = 1;

        public ServiceHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message message) {
            int action = message.what;
            SomeArgs args;
            IRemoteCallback callback;
            int[] digestPrefix;
            String token;
            Intent intent;
            switch (action) {
                case 1:
                    args = (SomeArgs) message.obj;
                    callback = (IRemoteCallback) args.arg1;
                    digestPrefix = (int[]) args.arg2;
                    token = (String) args.arg3;
                    intent = (Intent) args.arg4;
                    int sequence = message.arg1;
                    if (InstantAppResolverService.DEBUG_INSTANT) {
                        String str = InstantAppResolverService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("[");
                        stringBuilder.append(token);
                        stringBuilder.append("] Phase1 request; prefix: ");
                        stringBuilder.append(Arrays.toString(digestPrefix));
                        Slog.d(str, stringBuilder.toString());
                    }
                    InstantAppResolverService.this.onGetInstantAppResolveInfo(intent, digestPrefix, token, new InstantAppResolutionCallback(sequence, callback));
                    return;
                case 2:
                    args = message.obj;
                    callback = args.arg1;
                    digestPrefix = args.arg2;
                    token = args.arg3;
                    intent = args.arg4;
                    if (InstantAppResolverService.DEBUG_INSTANT) {
                        String str2 = InstantAppResolverService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("[");
                        stringBuilder2.append(token);
                        stringBuilder2.append("] Phase2 request; prefix: ");
                        stringBuilder2.append(Arrays.toString(digestPrefix));
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    InstantAppResolverService.this.onGetInstantAppIntentFilter(intent, digestPrefix, token, new InstantAppResolutionCallback(-1, callback));
                    return;
                default:
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Unknown message: ");
                    stringBuilder3.append(action);
                    throw new IllegalArgumentException(stringBuilder3.toString());
            }
        }
    }

    @Deprecated
    public void onGetInstantAppResolveInfo(int[] digestPrefix, String token, InstantAppResolutionCallback callback) {
        throw new IllegalStateException("Must define onGetInstantAppResolveInfo");
    }

    @Deprecated
    public void onGetInstantAppIntentFilter(int[] digestPrefix, String token, InstantAppResolutionCallback callback) {
        throw new IllegalStateException("Must define onGetInstantAppIntentFilter");
    }

    public void onGetInstantAppResolveInfo(Intent sanitizedIntent, int[] hostDigestPrefix, String token, InstantAppResolutionCallback callback) {
        if (sanitizedIntent.isWebIntent()) {
            onGetInstantAppResolveInfo(hostDigestPrefix, token, callback);
        } else {
            callback.onInstantAppResolveInfo(Collections.emptyList());
        }
    }

    public void onGetInstantAppIntentFilter(Intent sanitizedIntent, int[] hostDigestPrefix, String token, InstantAppResolutionCallback callback) {
        Log.e(TAG, "New onGetInstantAppIntentFilter is not overridden");
        if (sanitizedIntent.isWebIntent()) {
            onGetInstantAppIntentFilter(hostDigestPrefix, token, callback);
        } else {
            callback.onInstantAppResolveInfo(Collections.emptyList());
        }
    }

    Looper getLooper() {
        return getBaseContext().getMainLooper();
    }

    public final void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        this.mHandler = new ServiceHandler(getLooper());
    }

    public final IBinder onBind(Intent intent) {
        return new Stub() {
            public void getInstantAppResolveInfoList(Intent sanitizedIntent, int[] digestPrefix, String token, int sequence, IRemoteCallback callback) {
                if (InstantAppResolverService.DEBUG_INSTANT) {
                    String str = InstantAppResolverService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[");
                    stringBuilder.append(token);
                    stringBuilder.append("] Phase1 called; posting");
                    Slog.v(str, stringBuilder.toString());
                }
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = callback;
                args.arg2 = digestPrefix;
                args.arg3 = token;
                args.arg4 = sanitizedIntent;
                InstantAppResolverService.this.mHandler.obtainMessage(1, sequence, 0, args).sendToTarget();
            }

            public void getInstantAppIntentFilterList(Intent sanitizedIntent, int[] digestPrefix, String token, IRemoteCallback callback) {
                if (InstantAppResolverService.DEBUG_INSTANT) {
                    String str = InstantAppResolverService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[");
                    stringBuilder.append(token);
                    stringBuilder.append("] Phase2 called; posting");
                    Slog.v(str, stringBuilder.toString());
                }
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = callback;
                args.arg2 = digestPrefix;
                args.arg3 = token;
                args.arg4 = sanitizedIntent;
                InstantAppResolverService.this.mHandler.obtainMessage(2, callback).sendToTarget();
            }
        };
    }
}
