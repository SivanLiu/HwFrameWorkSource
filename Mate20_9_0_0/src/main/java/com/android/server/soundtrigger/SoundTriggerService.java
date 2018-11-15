package com.android.server.soundtrigger;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.PendingIntent.OnFinished;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.IRecognitionStatusCallback.Stub;
import android.hardware.soundtrigger.SoundTrigger.GenericRecognitionEvent;
import android.hardware.soundtrigger.SoundTrigger.GenericSoundModel;
import android.hardware.soundtrigger.SoundTrigger.KeyphraseRecognitionEvent;
import android.hardware.soundtrigger.SoundTrigger.KeyphraseSoundModel;
import android.hardware.soundtrigger.SoundTrigger.ModuleProperties;
import android.hardware.soundtrigger.SoundTrigger.RecognitionConfig;
import android.hardware.soundtrigger.SoundTrigger.SoundModel;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.soundtrigger.ISoundTriggerDetectionService;
import android.media.soundtrigger.ISoundTriggerDetectionServiceClient;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.ISoundTriggerService;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.os.HwBootFail;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SoundTriggerService extends SystemService {
    private static final boolean DEBUG = true;
    private static final String TAG = "SoundTriggerService";
    private OnFinished mCallbackCompletedHandler = new OnFinished() {
        public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            synchronized (SoundTriggerService.this.mCallbacksLock) {
                SoundTriggerService.this.mWakelock.release();
            }
        }
    };
    private final TreeMap<UUID, IRecognitionStatusCallback> mCallbacks;
    private Object mCallbacksLock;
    final Context mContext;
    private SoundTriggerDbHelper mDbHelper;
    private final TreeMap<UUID, SoundModel> mLoadedModels;
    private final LocalSoundTriggerService mLocalSoundTriggerService;
    private Object mLock;
    @GuardedBy("mLock")
    private final ArrayMap<String, NumOps> mNumOpsPerPackage = new ArrayMap();
    private final SoundTriggerServiceStub mServiceStub;
    private SoundTriggerHelper mSoundTriggerHelper;
    private WakeLock mWakelock;

    private final class LocalSoundTriggerRecognitionStatusIntentCallback extends Stub {
        private PendingIntent mCallbackIntent;
        private RecognitionConfig mRecognitionConfig;
        private UUID mUuid;

        public LocalSoundTriggerRecognitionStatusIntentCallback(UUID modelUuid, PendingIntent callbackIntent, RecognitionConfig config) {
            this.mUuid = modelUuid;
            this.mCallbackIntent = callbackIntent;
            this.mRecognitionConfig = config;
        }

        public boolean pingBinder() {
            return this.mCallbackIntent != null;
        }

        public void onKeyphraseDetected(KeyphraseRecognitionEvent event) {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                String str = SoundTriggerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Keyphrase sound trigger event: ");
                stringBuilder.append(event);
                Slog.w(str, stringBuilder.toString());
                Intent extras = new Intent();
                extras.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 0);
                extras.putExtra("android.media.soundtrigger.RECOGNITION_EVENT", event);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, extras, SoundTriggerService.this.mCallbackCompletedHandler, null);
                    if (!this.mRecognitionConfig.allowMultipleTriggers) {
                        removeCallback(false);
                    }
                } catch (CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        public void onGenericSoundTriggerDetected(GenericRecognitionEvent event) {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                String str = SoundTriggerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Generic sound trigger event: ");
                stringBuilder.append(event);
                Slog.w(str, stringBuilder.toString());
                Intent extras = new Intent();
                extras.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 0);
                extras.putExtra("android.media.soundtrigger.RECOGNITION_EVENT", event);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, extras, SoundTriggerService.this.mCallbackCompletedHandler, null);
                    if (!this.mRecognitionConfig.allowMultipleTriggers) {
                        removeCallback(false);
                    }
                } catch (CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        public void onError(int status) {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                String str = SoundTriggerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onError: ");
                stringBuilder.append(status);
                Slog.i(str, stringBuilder.toString());
                Intent extras = new Intent();
                extras.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 1);
                extras.putExtra("android.media.soundtrigger.STATUS", status);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, extras, SoundTriggerService.this.mCallbackCompletedHandler, null);
                    removeCallback(false);
                } catch (CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        public void onRecognitionPaused() {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                Slog.i(SoundTriggerService.TAG, "onRecognitionPaused");
                Intent extras = new Intent();
                extras.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 2);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, extras, SoundTriggerService.this.mCallbackCompletedHandler, null);
                } catch (CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        public void onRecognitionResumed() {
            if (this.mCallbackIntent != null) {
                SoundTriggerService.this.grabWakeLock();
                Slog.i(SoundTriggerService.TAG, "onRecognitionResumed");
                Intent extras = new Intent();
                extras.putExtra("android.media.soundtrigger.MESSAGE_TYPE", 3);
                try {
                    this.mCallbackIntent.send(SoundTriggerService.this.mContext, 0, extras, SoundTriggerService.this.mCallbackCompletedHandler, null);
                } catch (CanceledException e) {
                    removeCallback(true);
                }
            }
        }

        private void removeCallback(boolean releaseWakeLock) {
            this.mCallbackIntent = null;
            synchronized (SoundTriggerService.this.mCallbacksLock) {
                SoundTriggerService.this.mCallbacks.remove(this.mUuid);
                if (releaseWakeLock) {
                    SoundTriggerService.this.mWakelock.release();
                }
            }
        }
    }

    private static class NumOps {
        @GuardedBy("mLock")
        private long mLastOpsHourSinceBoot;
        private final Object mLock;
        @GuardedBy("mLock")
        private int[] mNumOps;

        private NumOps() {
            this.mLock = new Object();
            this.mNumOps = new int[24];
        }

        /* synthetic */ NumOps(AnonymousClass1 x0) {
            this();
        }

        void clearOldOps(long currentTime) {
            synchronized (this.mLock) {
                long numHoursSinceBoot = TimeUnit.HOURS.convert(currentTime, TimeUnit.NANOSECONDS);
                if (this.mLastOpsHourSinceBoot != 0) {
                    long hour = this.mLastOpsHourSinceBoot;
                    while (true) {
                        hour++;
                        if (hour > numHoursSinceBoot) {
                            break;
                        }
                        this.mNumOps[(int) (hour % 24)] = 0;
                    }
                }
            }
        }

        void addOp(long currentTime) {
            synchronized (this.mLock) {
                long numHoursSinceBoot = TimeUnit.HOURS.convert(currentTime, TimeUnit.NANOSECONDS);
                int[] iArr = this.mNumOps;
                int i = (int) (numHoursSinceBoot % 24);
                iArr[i] = iArr[i] + 1;
                this.mLastOpsHourSinceBoot = numHoursSinceBoot;
            }
        }

        int getOpsAdded() {
            int totalOperationsInLastDay;
            synchronized (this.mLock) {
                totalOperationsInLastDay = 0;
                for (int i = 0; i < 24; i++) {
                    totalOperationsInLastDay += this.mNumOps[i];
                }
            }
            return totalOperationsInLastDay;
        }
    }

    private static class Operation {
        private final Runnable mDropOp;
        private final ExecuteOp mExecuteOp;
        private final Runnable mSetupOp;

        private interface ExecuteOp {
            void run(int i, ISoundTriggerDetectionService iSoundTriggerDetectionService) throws RemoteException;
        }

        /* synthetic */ Operation(Runnable x0, ExecuteOp x1, Runnable x2, AnonymousClass1 x3) {
            this(x0, x1, x2);
        }

        private Operation(Runnable setupOp, ExecuteOp executeOp, Runnable cancelOp) {
            this.mSetupOp = setupOp;
            this.mExecuteOp = executeOp;
            this.mDropOp = cancelOp;
        }

        private void setup() {
            if (this.mSetupOp != null) {
                this.mSetupOp.run();
            }
        }

        void run(int opId, ISoundTriggerDetectionService service) throws RemoteException {
            setup();
            this.mExecuteOp.run(opId, service);
        }

        void drop() {
            setup();
            if (this.mDropOp != null) {
                this.mDropOp.run();
            }
        }
    }

    private class RemoteSoundTriggerDetectionService extends Stub implements ServiceConnection {
        private static final int MSG_STOP_ALL_PENDING_OPERATIONS = 1;
        private final ISoundTriggerDetectionServiceClient mClient;
        @GuardedBy("mRemoteServiceLock")
        private boolean mDestroyOnceRunningOpsDone;
        private final Handler mHandler;
        @GuardedBy("mRemoteServiceLock")
        private boolean mIsBound;
        @GuardedBy("mRemoteServiceLock")
        private boolean mIsDestroyed;
        private final NumOps mNumOps;
        @GuardedBy("mRemoteServiceLock")
        private int mNumTotalOpsPerformed;
        private final Bundle mParams;
        @GuardedBy("mRemoteServiceLock")
        private final ArrayList<Operation> mPendingOps = new ArrayList();
        private final ParcelUuid mPuuid;
        private final RecognitionConfig mRecognitionConfig;
        private final Object mRemoteServiceLock = new Object();
        private final WakeLock mRemoteServiceWakeLock;
        @GuardedBy("mRemoteServiceLock")
        private final ArraySet<Integer> mRunningOpIds = new ArraySet();
        @GuardedBy("mRemoteServiceLock")
        private ISoundTriggerDetectionService mService;
        private final ComponentName mServiceName;
        private final UserHandle mUser;

        public RemoteSoundTriggerDetectionService(UUID modelUuid, Bundle params, ComponentName serviceName, UserHandle user, RecognitionConfig config) {
            this.mPuuid = new ParcelUuid(modelUuid);
            this.mParams = params;
            this.mServiceName = serviceName;
            this.mUser = user;
            this.mRecognitionConfig = config;
            this.mHandler = new Handler(Looper.getMainLooper());
            PowerManager pm = (PowerManager) SoundTriggerService.this.mContext.getSystemService("power");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteSoundTriggerDetectionService ");
            stringBuilder.append(this.mServiceName.getPackageName());
            stringBuilder.append(":");
            stringBuilder.append(this.mServiceName.getClassName());
            this.mRemoteServiceWakeLock = pm.newWakeLock(1, stringBuilder.toString());
            synchronized (SoundTriggerService.this.mLock) {
                NumOps numOps = (NumOps) SoundTriggerService.this.mNumOpsPerPackage.get(this.mServiceName.getPackageName());
                if (numOps == null) {
                    numOps = new NumOps();
                    SoundTriggerService.this.mNumOpsPerPackage.put(this.mServiceName.getPackageName(), numOps);
                }
                this.mNumOps = numOps;
            }
            this.mClient = new ISoundTriggerDetectionServiceClient.Stub(SoundTriggerService.this) {
                public void onOpFinished(int opId) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        synchronized (RemoteSoundTriggerDetectionService.this.mRemoteServiceLock) {
                            RemoteSoundTriggerDetectionService.this.mRunningOpIds.remove(Integer.valueOf(opId));
                            if (RemoteSoundTriggerDetectionService.this.mRunningOpIds.isEmpty() && RemoteSoundTriggerDetectionService.this.mPendingOps.isEmpty()) {
                                if (RemoteSoundTriggerDetectionService.this.mDestroyOnceRunningOpsDone) {
                                    RemoteSoundTriggerDetectionService.this.destroy();
                                } else {
                                    RemoteSoundTriggerDetectionService.this.disconnectLocked();
                                }
                            }
                        }
                        Binder.restoreCallingIdentity(token);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(token);
                    }
                }
            };
        }

        public boolean pingBinder() {
            return (this.mIsDestroyed || this.mDestroyOnceRunningOpsDone) ? false : true;
        }

        private void disconnectLocked() {
            if (this.mService != null) {
                try {
                    this.mService.removeClient(this.mPuuid);
                } catch (Exception e) {
                    String str = SoundTriggerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mPuuid);
                    stringBuilder.append(": Cannot remove client");
                    Slog.e(str, stringBuilder.toString(), e);
                }
                this.mService = null;
            }
            if (this.mIsBound) {
                SoundTriggerService.this.mContext.unbindService(this);
                this.mIsBound = false;
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    this.mRemoteServiceWakeLock.release();
                }
            }
        }

        private void destroy() {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append(": destroy");
            Slog.v(str, stringBuilder.toString());
            synchronized (this.mRemoteServiceLock) {
                disconnectLocked();
                this.mIsDestroyed = true;
            }
            if (!this.mDestroyOnceRunningOpsDone) {
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    SoundTriggerService.this.mCallbacks.remove(this.mPuuid.getUuid());
                }
            }
        }

        private void stopAllPendingOperations() {
            synchronized (this.mRemoteServiceLock) {
                if (this.mIsDestroyed) {
                    return;
                }
                if (this.mService != null) {
                    int numOps = this.mRunningOpIds.size();
                    for (int i = 0; i < numOps; i++) {
                        try {
                            this.mService.onStopOperation(this.mPuuid, ((Integer) this.mRunningOpIds.valueAt(i)).intValue());
                        } catch (Exception e) {
                            String str = SoundTriggerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(this.mPuuid);
                            stringBuilder.append(": Could not stop operation ");
                            stringBuilder.append(this.mRunningOpIds.valueAt(i));
                            Slog.e(str, stringBuilder.toString(), e);
                        }
                    }
                    this.mRunningOpIds.clear();
                }
                disconnectLocked();
            }
        }

        private void bind() {
            long token = Binder.clearCallingIdentity();
            try {
                Intent i = new Intent();
                i.setComponent(this.mServiceName);
                ResolveInfo ri = SoundTriggerService.this.mContext.getPackageManager().resolveServiceAsUser(i, 268435588, this.mUser.getIdentifier());
                String str;
                StringBuilder stringBuilder;
                if (ri == null) {
                    str = SoundTriggerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mPuuid);
                    stringBuilder.append(": ");
                    stringBuilder.append(this.mServiceName);
                    stringBuilder.append(" not found");
                    Slog.w(str, stringBuilder.toString());
                } else if ("android.permission.BIND_SOUND_TRIGGER_DETECTION_SERVICE".equals(ri.serviceInfo.permission)) {
                    this.mIsBound = SoundTriggerService.this.mContext.bindServiceAsUser(i, this, 67108865, this.mUser);
                    if (this.mIsBound) {
                        this.mRemoteServiceWakeLock.acquire();
                    } else {
                        str = SoundTriggerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(this.mPuuid);
                        stringBuilder.append(": Could not bind to ");
                        stringBuilder.append(this.mServiceName);
                        Slog.w(str, stringBuilder.toString());
                    }
                    Binder.restoreCallingIdentity(token);
                } else {
                    str = SoundTriggerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mPuuid);
                    stringBuilder.append(": ");
                    stringBuilder.append(this.mServiceName);
                    stringBuilder.append(" does not require ");
                    stringBuilder.append("android.permission.BIND_SOUND_TRIGGER_DETECTION_SERVICE");
                    Slog.w(str, stringBuilder.toString());
                    Binder.restoreCallingIdentity(token);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX WARNING: Missing block: B:30:0x00e4, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void runOrAddOperation(Operation op) {
            synchronized (this.mRemoteServiceLock) {
                if (this.mIsDestroyed || this.mDestroyOnceRunningOpsDone) {
                    String str = SoundTriggerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mPuuid);
                    stringBuilder.append(": Dropped operation as already destroyed or marked for destruction");
                    Slog.w(str, stringBuilder.toString());
                    op.drop();
                } else if (this.mService == null) {
                    this.mPendingOps.add(op);
                    if (!this.mIsBound) {
                        bind();
                    }
                } else {
                    long currentTime = System.nanoTime();
                    this.mNumOps.clearOldOps(currentTime);
                    int opsAllowed = Global.getInt(SoundTriggerService.this.mContext.getContentResolver(), "max_sound_trigger_detection_service_ops_per_day", HwBootFail.STAGE_BOOT_SUCCESS);
                    int opsAdded = this.mNumOps.getOpsAdded();
                    this.mNumOps.addOp(currentTime);
                    int opId = this.mNumTotalOpsPerformed;
                    do {
                        this.mNumTotalOpsPerformed++;
                    } while (this.mRunningOpIds.contains(Integer.valueOf(opId)));
                    try {
                        String str2 = SoundTriggerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.mPuuid);
                        stringBuilder2.append(": runOp ");
                        stringBuilder2.append(opId);
                        Slog.v(str2, stringBuilder2.toString());
                        op.run(opId, this.mService);
                        this.mRunningOpIds.add(Integer.valueOf(opId));
                    } catch (Exception e) {
                        String str3 = SoundTriggerService.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(this.mPuuid);
                        stringBuilder3.append(": Could not run operation ");
                        stringBuilder3.append(opId);
                        Slog.e(str3, stringBuilder3.toString(), e);
                    }
                    if (!this.mPendingOps.isEmpty() || !this.mRunningOpIds.isEmpty()) {
                        this.mHandler.removeMessages(1);
                        this.mHandler.sendMessageDelayed(PooledLambda.obtainMessage(-$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$wfDlqQ7aPvu9qZCZ24jJu4tfUMY.INSTANCE, this).setWhat(1), Global.getLong(SoundTriggerService.this.mContext.getContentResolver(), "sound_trigger_detection_service_op_timeout", JobStatus.NO_LATEST_RUNTIME));
                    } else if (this.mDestroyOnceRunningOpsDone) {
                        destroy();
                    } else {
                        disconnectLocked();
                    }
                }
            }
        }

        public void onKeyphraseDetected(KeyphraseRecognitionEvent event) {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append("->");
            stringBuilder.append(this.mServiceName);
            stringBuilder.append(": IGNORED onKeyphraseDetected(");
            stringBuilder.append(event);
            stringBuilder.append(")");
            Slog.w(str, stringBuilder.toString());
        }

        private AudioRecord createAudioRecordForEvent(GenericRecognitionEvent event) {
            int bufferSize;
            int i;
            Builder attributesBuilder = new Builder();
            attributesBuilder.setInternalCapturePreset(1999);
            AudioAttributes attributes = attributesBuilder.build();
            AudioFormat originalFormat = event.getCaptureFormat();
            AudioFormat captureFormat = new AudioFormat.Builder().setChannelMask(originalFormat.getChannelMask()).setEncoding(originalFormat.getEncoding()).setSampleRate(originalFormat.getSampleRate()).build();
            if (captureFormat.getSampleRate() == 0) {
                bufferSize = 192000;
            } else {
                bufferSize = captureFormat.getSampleRate();
            }
            if (captureFormat.getChannelCount() == 2) {
                i = 12;
            } else {
                i = 16;
            }
            return new AudioRecord(attributes, captureFormat, AudioRecord.getMinBufferSize(bufferSize, i, captureFormat.getEncoding()), event.getCaptureSession());
        }

        public void onGenericSoundTriggerDetected(GenericRecognitionEvent event) {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append(": Generic sound trigger event: ");
            stringBuilder.append(event);
            Slog.v(str, stringBuilder.toString());
            runOrAddOperation(new Operation(new -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$yqLMvkOmrO13yWrggtSaVrLgsWo(this), new -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$F-iA254xzDfAHrQW86c2oSqXfwI(this, event), new -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$pFqiq_C9KJsoa_HQOdj7lmMixsI(this, event), null));
        }

        public static /* synthetic */ void lambda$onGenericSoundTriggerDetected$0(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService) {
            if (!remoteSoundTriggerDetectionService.mRecognitionConfig.allowMultipleTriggers) {
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    SoundTriggerService.this.mCallbacks.remove(remoteSoundTriggerDetectionService.mPuuid.getUuid());
                }
                remoteSoundTriggerDetectionService.mDestroyOnceRunningOpsDone = true;
            }
        }

        public static /* synthetic */ void lambda$onGenericSoundTriggerDetected$2(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService, GenericRecognitionEvent event) {
            if (event.isCaptureAvailable()) {
                AudioRecord capturedData = remoteSoundTriggerDetectionService.createAudioRecordForEvent(event);
                capturedData.startRecording();
                capturedData.release();
            }
        }

        public void onError(int status) {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append(": onError: ");
            stringBuilder.append(status);
            Slog.v(str, stringBuilder.toString());
            runOrAddOperation(new Operation(new -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$t5mBYXswwLAAdm47WS10stLjYng(this), new -$$Lambda$SoundTriggerService$RemoteSoundTriggerDetectionService$crQZgbDmIG6q92Mrkm49T2yqrs0(this, status), null, null));
        }

        public static /* synthetic */ void lambda$onError$3(RemoteSoundTriggerDetectionService remoteSoundTriggerDetectionService) {
            synchronized (SoundTriggerService.this.mCallbacksLock) {
                SoundTriggerService.this.mCallbacks.remove(remoteSoundTriggerDetectionService.mPuuid.getUuid());
            }
            remoteSoundTriggerDetectionService.mDestroyOnceRunningOpsDone = true;
        }

        public void onRecognitionPaused() {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append("->");
            stringBuilder.append(this.mServiceName);
            stringBuilder.append(": IGNORED onRecognitionPaused");
            Slog.i(str, stringBuilder.toString());
        }

        public void onRecognitionResumed() {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append("->");
            stringBuilder.append(this.mServiceName);
            stringBuilder.append(": IGNORED onRecognitionResumed");
            Slog.i(str, stringBuilder.toString());
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append(": onServiceConnected(");
            stringBuilder.append(service);
            stringBuilder.append(")");
            Slog.v(str, stringBuilder.toString());
            synchronized (this.mRemoteServiceLock) {
                this.mService = ISoundTriggerDetectionService.Stub.asInterface(service);
                try {
                    this.mService.setClient(this.mPuuid, this.mParams, this.mClient);
                    while (!this.mPendingOps.isEmpty()) {
                        runOrAddOperation((Operation) this.mPendingOps.remove(0));
                    }
                } catch (Exception e) {
                    String str2 = SoundTriggerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(this.mPuuid);
                    stringBuilder2.append(": Could not init ");
                    stringBuilder2.append(this.mServiceName);
                    Slog.e(str2, stringBuilder2.toString(), e);
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append(": onServiceDisconnected");
            Slog.v(str, stringBuilder.toString());
            synchronized (this.mRemoteServiceLock) {
                this.mService = null;
            }
        }

        public void onBindingDied(ComponentName name) {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPuuid);
            stringBuilder.append(": onBindingDied");
            Slog.v(str, stringBuilder.toString());
            synchronized (this.mRemoteServiceLock) {
                destroy();
            }
        }

        public void onNullBinding(ComponentName name) {
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append(" for model ");
            stringBuilder.append(this.mPuuid);
            stringBuilder.append(" returned a null binding");
            Slog.w(str, stringBuilder.toString());
            synchronized (this.mRemoteServiceLock) {
                disconnectLocked();
            }
        }
    }

    class SoundTriggerServiceStub extends ISoundTriggerService.Stub {
        SoundTriggerServiceStub() {
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf(SoundTriggerService.TAG, "SoundTriggerService Crash", e);
                }
                throw e;
            }
        }

        public int startRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback callback, RecognitionConfig config) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startRecognition(): Uuid : ");
            stringBuilder.append(parcelUuid);
            Slog.i(str, stringBuilder.toString());
            GenericSoundModel model = getSoundModel(parcelUuid);
            if (model != null) {
                return SoundTriggerService.this.mSoundTriggerHelper.startGenericRecognition(parcelUuid.getUuid(), model, callback, config);
            }
            String str2 = SoundTriggerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Null model in database for id: ");
            stringBuilder2.append(parcelUuid);
            Slog.e(str2, stringBuilder2.toString());
            return Integer.MIN_VALUE;
        }

        public int stopRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback callback) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopRecognition(): Uuid : ");
            stringBuilder.append(parcelUuid);
            Slog.i(str, stringBuilder.toString());
            if (SoundTriggerService.this.isInitialized()) {
                return SoundTriggerService.this.mSoundTriggerHelper.stopGenericRecognition(parcelUuid.getUuid(), callback);
            }
            return Integer.MIN_VALUE;
        }

        public GenericSoundModel getSoundModel(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSoundModel(): id = ");
            stringBuilder.append(soundModelId);
            Slog.i(str, stringBuilder.toString());
            return SoundTriggerService.this.mDbHelper.getGenericSoundModel(soundModelId.getUuid());
        }

        public void updateSoundModel(GenericSoundModel soundModel) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateSoundModel(): model = ");
            stringBuilder.append(soundModel);
            Slog.i(str, stringBuilder.toString());
            SoundTriggerService.this.mDbHelper.updateGenericSoundModel(soundModel);
        }

        public void deleteSoundModel(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deleteSoundModel(): id = ");
            stringBuilder.append(soundModelId);
            Slog.i(str, stringBuilder.toString());
            SoundTriggerService.this.mSoundTriggerHelper.unloadGenericSoundModel(soundModelId.getUuid());
            SoundTriggerService.this.mDbHelper.deleteGenericSoundModel(soundModelId.getUuid());
        }

        public int loadGenericSoundModel(GenericSoundModel soundModel) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            if (soundModel == null || soundModel.uuid == null) {
                Slog.e(SoundTriggerService.TAG, "Invalid sound model");
                return Integer.MIN_VALUE;
            }
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadGenericSoundModel(): id = ");
            stringBuilder.append(soundModel.uuid);
            Slog.i(str, stringBuilder.toString());
            synchronized (SoundTriggerService.this.mLock) {
                SoundModel oldModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModel.uuid);
                if (!(oldModel == null || oldModel.equals(soundModel))) {
                    SoundTriggerService.this.mSoundTriggerHelper.unloadGenericSoundModel(soundModel.uuid);
                    synchronized (SoundTriggerService.this.mCallbacksLock) {
                        SoundTriggerService.this.mCallbacks.remove(soundModel.uuid);
                    }
                }
                SoundTriggerService.this.mLoadedModels.put(soundModel.uuid, soundModel);
            }
            return 0;
        }

        public int loadKeyphraseSoundModel(KeyphraseSoundModel soundModel) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            if (soundModel == null || soundModel.uuid == null) {
                Slog.e(SoundTriggerService.TAG, "Invalid sound model");
                return Integer.MIN_VALUE;
            } else if (soundModel.keyphrases == null || soundModel.keyphrases.length != 1) {
                Slog.e(SoundTriggerService.TAG, "Only one keyphrase per model is currently supported.");
                return Integer.MIN_VALUE;
            } else {
                String str = SoundTriggerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("loadKeyphraseSoundModel(): id = ");
                stringBuilder.append(soundModel.uuid);
                Slog.i(str, stringBuilder.toString());
                synchronized (SoundTriggerService.this.mLock) {
                    SoundModel oldModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModel.uuid);
                    if (!(oldModel == null || oldModel.equals(soundModel))) {
                        SoundTriggerService.this.mSoundTriggerHelper.unloadKeyphraseSoundModel(soundModel.keyphrases[0].id);
                        synchronized (SoundTriggerService.this.mCallbacksLock) {
                            SoundTriggerService.this.mCallbacks.remove(soundModel.uuid);
                        }
                    }
                    SoundTriggerService.this.mLoadedModels.put(soundModel.uuid, soundModel);
                }
                return 0;
            }
        }

        public int startRecognitionForService(ParcelUuid soundModelId, Bundle params, ComponentName detectionService, RecognitionConfig config) {
            Preconditions.checkNotNull(soundModelId);
            Preconditions.checkNotNull(detectionService);
            Preconditions.checkNotNull(config);
            return startRecognitionForInt(soundModelId, new RemoteSoundTriggerDetectionService(soundModelId.getUuid(), params, detectionService, Binder.getCallingUserHandle(), config), config);
        }

        public int startRecognitionForIntent(ParcelUuid soundModelId, PendingIntent callbackIntent, RecognitionConfig config) {
            return startRecognitionForInt(soundModelId, new LocalSoundTriggerRecognitionStatusIntentCallback(soundModelId.getUuid(), callbackIntent, config), config);
        }

        private int startRecognitionForInt(ParcelUuid soundModelId, IRecognitionStatusCallback callback, RecognitionConfig config) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startRecognition(): id = ");
            stringBuilder.append(soundModelId);
            Slog.i(str, stringBuilder.toString());
            synchronized (SoundTriggerService.this.mLock) {
                SoundModel soundModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModelId.getUuid());
                if (soundModel == null) {
                    String str2 = SoundTriggerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(soundModelId);
                    stringBuilder2.append(" is not loaded");
                    Slog.e(str2, stringBuilder2.toString());
                    return Integer.MIN_VALUE;
                }
                IRecognitionStatusCallback existingCallback;
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    existingCallback = (IRecognitionStatusCallback) SoundTriggerService.this.mCallbacks.get(soundModelId.getUuid());
                }
                String str3;
                StringBuilder stringBuilder3;
                if (existingCallback != null) {
                    str3 = SoundTriggerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(soundModelId);
                    stringBuilder3.append(" is already running");
                    Slog.e(str3, stringBuilder3.toString());
                    return Integer.MIN_VALUE;
                }
                int ret;
                switch (soundModel.type) {
                    case 0:
                        KeyphraseSoundModel keyphraseSoundModel = (KeyphraseSoundModel) soundModel;
                        ret = SoundTriggerService.this.mSoundTriggerHelper.startKeyphraseRecognition(keyphraseSoundModel.keyphrases[0].id, keyphraseSoundModel, callback, config);
                        break;
                    case 1:
                        ret = SoundTriggerService.this.mSoundTriggerHelper.startGenericRecognition(soundModel.uuid, (GenericSoundModel) soundModel, callback, config);
                        break;
                    default:
                        Slog.e(SoundTriggerService.TAG, "Unknown model type");
                        return Integer.MIN_VALUE;
                }
                if (ret != 0) {
                    str3 = SoundTriggerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Failed to start model: ");
                    stringBuilder3.append(ret);
                    Slog.e(str3, stringBuilder3.toString());
                    return ret;
                }
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    SoundTriggerService.this.mCallbacks.put(soundModelId.getUuid(), callback);
                }
                return 0;
            }
        }

        public int stopRecognitionForIntent(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopRecognition(): id = ");
            stringBuilder.append(soundModelId);
            Slog.i(str, stringBuilder.toString());
            synchronized (SoundTriggerService.this.mLock) {
                SoundModel soundModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModelId.getUuid());
                if (soundModel == null) {
                    String str2 = SoundTriggerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(soundModelId);
                    stringBuilder2.append(" is not loaded");
                    Slog.e(str2, stringBuilder2.toString());
                    return Integer.MIN_VALUE;
                }
                IRecognitionStatusCallback callback;
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    callback = (IRecognitionStatusCallback) SoundTriggerService.this.mCallbacks.get(soundModelId.getUuid());
                }
                String str3;
                StringBuilder stringBuilder3;
                if (callback == null) {
                    str3 = SoundTriggerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(soundModelId);
                    stringBuilder3.append(" is not running");
                    Slog.e(str3, stringBuilder3.toString());
                    return Integer.MIN_VALUE;
                }
                int ret;
                switch (soundModel.type) {
                    case 0:
                        ret = SoundTriggerService.this.mSoundTriggerHelper.stopKeyphraseRecognition(((KeyphraseSoundModel) soundModel).keyphrases[0].id, callback);
                        break;
                    case 1:
                        ret = SoundTriggerService.this.mSoundTriggerHelper.stopGenericRecognition(soundModel.uuid, callback);
                        break;
                    default:
                        Slog.e(SoundTriggerService.TAG, "Unknown model type");
                        return Integer.MIN_VALUE;
                }
                if (ret != 0) {
                    str3 = SoundTriggerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Failed to stop model: ");
                    stringBuilder3.append(ret);
                    Slog.e(str3, stringBuilder3.toString());
                    return ret;
                }
                synchronized (SoundTriggerService.this.mCallbacksLock) {
                    SoundTriggerService.this.mCallbacks.remove(soundModelId.getUuid());
                }
                return 0;
            }
        }

        public int unloadSoundModel(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            String str = SoundTriggerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unloadSoundModel(): id = ");
            stringBuilder.append(soundModelId);
            Slog.i(str, stringBuilder.toString());
            synchronized (SoundTriggerService.this.mLock) {
                SoundModel soundModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModelId.getUuid());
                if (soundModel == null) {
                    String str2 = SoundTriggerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(soundModelId);
                    stringBuilder2.append(" is not loaded");
                    Slog.e(str2, stringBuilder2.toString());
                    return Integer.MIN_VALUE;
                }
                int ret;
                switch (soundModel.type) {
                    case 0:
                        ret = SoundTriggerService.this.mSoundTriggerHelper.unloadKeyphraseSoundModel(((KeyphraseSoundModel) soundModel).keyphrases[0].id);
                        break;
                    case 1:
                        ret = SoundTriggerService.this.mSoundTriggerHelper.unloadGenericSoundModel(soundModel.uuid);
                        break;
                    default:
                        Slog.e(SoundTriggerService.TAG, "Unknown model type");
                        return Integer.MIN_VALUE;
                }
                if (ret != 0) {
                    Slog.e(SoundTriggerService.TAG, "Failed to unload model");
                    return ret;
                }
                SoundTriggerService.this.mLoadedModels.remove(soundModelId.getUuid());
                return 0;
            }
        }

        public boolean isRecognitionActive(ParcelUuid parcelUuid) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return false;
            }
            synchronized (SoundTriggerService.this.mCallbacksLock) {
                if (((IRecognitionStatusCallback) SoundTriggerService.this.mCallbacks.get(parcelUuid.getUuid())) == null) {
                    return false;
                }
                return SoundTriggerService.this.mSoundTriggerHelper.isRecognitionRequested(parcelUuid.getUuid());
            }
        }
    }

    public final class LocalSoundTriggerService extends SoundTriggerInternal {
        private final Context mContext;
        private SoundTriggerHelper mSoundTriggerHelper;

        LocalSoundTriggerService(Context context) {
            this.mContext = context;
        }

        synchronized void setSoundTriggerHelper(SoundTriggerHelper helper) {
            this.mSoundTriggerHelper = helper;
        }

        public int startRecognition(int keyphraseId, KeyphraseSoundModel soundModel, IRecognitionStatusCallback listener, RecognitionConfig recognitionConfig) {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.startKeyphraseRecognition(keyphraseId, soundModel, listener, recognitionConfig);
            }
            return Integer.MIN_VALUE;
        }

        public synchronized int stopRecognition(int keyphraseId, IRecognitionStatusCallback listener) {
            if (!isInitialized()) {
                return Integer.MIN_VALUE;
            }
            return this.mSoundTriggerHelper.stopKeyphraseRecognition(keyphraseId, listener);
        }

        public ModuleProperties getModuleProperties() {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.getModuleProperties();
            }
            return null;
        }

        public int unloadKeyphraseModel(int keyphraseId) {
            if (isInitialized()) {
                return this.mSoundTriggerHelper.unloadKeyphraseSoundModel(keyphraseId);
            }
            return Integer.MIN_VALUE;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (isInitialized()) {
                this.mSoundTriggerHelper.dump(fd, pw, args);
            }
        }

        private synchronized boolean isInitialized() {
            if (this.mSoundTriggerHelper != null) {
                return true;
            }
            Slog.e(SoundTriggerService.TAG, "SoundTriggerHelper not initialized.");
            return false;
        }
    }

    public SoundTriggerService(Context context) {
        super(context);
        this.mContext = context;
        this.mServiceStub = new SoundTriggerServiceStub();
        this.mLocalSoundTriggerService = new LocalSoundTriggerService(context);
        this.mLoadedModels = new TreeMap();
        this.mCallbacksLock = new Object();
        this.mCallbacks = new TreeMap();
        this.mLock = new Object();
    }

    public void onStart() {
        publishBinderService("soundtrigger", this.mServiceStub);
        publishLocalService(SoundTriggerInternal.class, this.mLocalSoundTriggerService);
    }

    public void onBootPhase(int phase) {
        if (500 == phase) {
            initSoundTriggerHelper();
            this.mLocalSoundTriggerService.setSoundTriggerHelper(this.mSoundTriggerHelper);
        } else if (600 == phase) {
            this.mDbHelper = new SoundTriggerDbHelper(this.mContext);
        }
    }

    public void onStartUser(int userHandle) {
    }

    public void onSwitchUser(int userHandle) {
    }

    private synchronized void initSoundTriggerHelper() {
        if (this.mSoundTriggerHelper == null) {
            this.mSoundTriggerHelper = new SoundTriggerHelper(this.mContext);
        }
    }

    private synchronized boolean isInitialized() {
        if (this.mSoundTriggerHelper != null) {
            return true;
        }
        Slog.e(TAG, "SoundTriggerHelper not initialized.");
        return false;
    }

    private void grabWakeLock() {
        synchronized (this.mCallbacksLock) {
            if (this.mWakelock == null) {
                this.mWakelock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, TAG);
            }
            this.mWakelock.acquire();
        }
    }

    private void enforceCallingPermission(String permission) {
        if (this.mContext.checkCallingOrSelfPermission(permission) != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Caller does not hold the permission ");
            stringBuilder.append(permission);
            throw new SecurityException(stringBuilder.toString());
        }
    }
}
