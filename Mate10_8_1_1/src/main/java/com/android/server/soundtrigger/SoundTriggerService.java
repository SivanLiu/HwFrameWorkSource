package com.android.server.soundtrigger;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.PendingIntent.OnFinished;
import android.content.Context;
import android.content.Intent;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.IRecognitionStatusCallback.Stub;
import android.hardware.soundtrigger.SoundTrigger.GenericRecognitionEvent;
import android.hardware.soundtrigger.SoundTrigger.GenericSoundModel;
import android.hardware.soundtrigger.SoundTrigger.KeyphraseRecognitionEvent;
import android.hardware.soundtrigger.SoundTrigger.KeyphraseSoundModel;
import android.hardware.soundtrigger.SoundTrigger.ModuleProperties;
import android.hardware.soundtrigger.SoundTrigger.RecognitionConfig;
import android.hardware.soundtrigger.SoundTrigger.SoundModel;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.app.ISoundTriggerService;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.TreeMap;
import java.util.UUID;

public class SoundTriggerService extends SystemService {
    private static final boolean DEBUG = true;
    private static final String TAG = "SoundTriggerService";
    private OnFinished mCallbackCompletedHandler = new OnFinished() {
        public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            synchronized (SoundTriggerService.this.mLock) {
                SoundTriggerService.this.mWakelock.release();
            }
        }
    };
    final Context mContext;
    private SoundTriggerDbHelper mDbHelper;
    private final TreeMap<UUID, LocalSoundTriggerRecognitionStatusCallback> mIntentCallbacks;
    private final TreeMap<UUID, SoundModel> mLoadedModels;
    private final LocalSoundTriggerService mLocalSoundTriggerService;
    private Object mLock;
    private final SoundTriggerServiceStub mServiceStub;
    private SoundTriggerHelper mSoundTriggerHelper;
    private WakeLock mWakelock;

    private final class LocalSoundTriggerRecognitionStatusCallback extends Stub {
        private PendingIntent mCallbackIntent;
        private RecognitionConfig mRecognitionConfig;
        private UUID mUuid;

        public LocalSoundTriggerRecognitionStatusCallback(UUID modelUuid, PendingIntent callbackIntent, RecognitionConfig config) {
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
                Slog.w(SoundTriggerService.TAG, "Keyphrase sound trigger event: " + event);
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
                Slog.w(SoundTriggerService.TAG, "Generic sound trigger event: " + event);
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
                Slog.i(SoundTriggerService.TAG, "onError: " + status);
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
            synchronized (SoundTriggerService.this.mLock) {
                SoundTriggerService.this.mIntentCallbacks.remove(this.mUuid);
                if (releaseWakeLock) {
                    SoundTriggerService.this.mWakelock.release();
                }
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
            Slog.i(SoundTriggerService.TAG, "startRecognition(): Uuid : " + parcelUuid);
            GenericSoundModel model = getSoundModel(parcelUuid);
            if (model != null) {
                return SoundTriggerService.this.mSoundTriggerHelper.startGenericRecognition(parcelUuid.getUuid(), model, callback, config);
            }
            Slog.e(SoundTriggerService.TAG, "Null model in database for id: " + parcelUuid);
            return Integer.MIN_VALUE;
        }

        public int stopRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback callback) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "stopRecognition(): Uuid : " + parcelUuid);
            if (SoundTriggerService.this.isInitialized()) {
                return SoundTriggerService.this.mSoundTriggerHelper.stopGenericRecognition(parcelUuid.getUuid(), callback);
            }
            return Integer.MIN_VALUE;
        }

        public GenericSoundModel getSoundModel(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "getSoundModel(): id = " + soundModelId);
            return SoundTriggerService.this.mDbHelper.getGenericSoundModel(soundModelId.getUuid());
        }

        public void updateSoundModel(GenericSoundModel soundModel) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "updateSoundModel(): model = " + soundModel);
            SoundTriggerService.this.mDbHelper.updateGenericSoundModel(soundModel);
        }

        public void deleteSoundModel(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            Slog.i(SoundTriggerService.TAG, "deleteSoundModel(): id = " + soundModelId);
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
            Slog.i(SoundTriggerService.TAG, "loadGenericSoundModel(): id = " + soundModel.uuid);
            synchronized (SoundTriggerService.this.mLock) {
                SoundModel oldModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModel.uuid);
                if (!(oldModel == null || (oldModel.equals(soundModel) ^ 1) == 0)) {
                    SoundTriggerService.this.mSoundTriggerHelper.unloadGenericSoundModel(soundModel.uuid);
                    SoundTriggerService.this.mIntentCallbacks.remove(soundModel.uuid);
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
                Slog.i(SoundTriggerService.TAG, "loadKeyphraseSoundModel(): id = " + soundModel.uuid);
                synchronized (SoundTriggerService.this.mLock) {
                    SoundModel oldModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModel.uuid);
                    if (!(oldModel == null || (oldModel.equals(soundModel) ^ 1) == 0)) {
                        SoundTriggerService.this.mSoundTriggerHelper.unloadKeyphraseSoundModel(soundModel.keyphrases[0].id);
                        SoundTriggerService.this.mIntentCallbacks.remove(soundModel.uuid);
                    }
                    SoundTriggerService.this.mLoadedModels.put(soundModel.uuid, soundModel);
                }
                return 0;
            }
        }

        public int startRecognitionForIntent(ParcelUuid soundModelId, PendingIntent callbackIntent, RecognitionConfig config) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "startRecognition(): id = " + soundModelId);
            synchronized (SoundTriggerService.this.mLock) {
                SoundModel soundModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModelId.getUuid());
                if (soundModel == null) {
                    Slog.e(SoundTriggerService.TAG, soundModelId + " is not loaded");
                    return Integer.MIN_VALUE;
                } else if (((LocalSoundTriggerRecognitionStatusCallback) SoundTriggerService.this.mIntentCallbacks.get(soundModelId.getUuid())) != null) {
                    Slog.e(SoundTriggerService.TAG, soundModelId + " is already running");
                    return Integer.MIN_VALUE;
                } else {
                    int ret;
                    LocalSoundTriggerRecognitionStatusCallback callback = new LocalSoundTriggerRecognitionStatusCallback(soundModelId.getUuid(), callbackIntent, config);
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
                        Slog.e(SoundTriggerService.TAG, "Failed to start model: " + ret);
                        return ret;
                    }
                    SoundTriggerService.this.mIntentCallbacks.put(soundModelId.getUuid(), callback);
                    return 0;
                }
            }
        }

        public int stopRecognitionForIntent(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "stopRecognition(): id = " + soundModelId);
            synchronized (SoundTriggerService.this.mLock) {
                SoundModel soundModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModelId.getUuid());
                if (soundModel == null) {
                    Slog.e(SoundTriggerService.TAG, soundModelId + " is not loaded");
                    return Integer.MIN_VALUE;
                }
                LocalSoundTriggerRecognitionStatusCallback callback = (LocalSoundTriggerRecognitionStatusCallback) SoundTriggerService.this.mIntentCallbacks.get(soundModelId.getUuid());
                if (callback == null) {
                    Slog.e(SoundTriggerService.TAG, soundModelId + " is not running");
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
                    Slog.e(SoundTriggerService.TAG, "Failed to stop model: " + ret);
                    return ret;
                }
                SoundTriggerService.this.mIntentCallbacks.remove(soundModelId.getUuid());
                return 0;
            }
        }

        public int unloadSoundModel(ParcelUuid soundModelId) {
            SoundTriggerService.this.enforceCallingPermission("android.permission.MANAGE_SOUND_TRIGGER");
            if (!SoundTriggerService.this.isInitialized()) {
                return Integer.MIN_VALUE;
            }
            Slog.i(SoundTriggerService.TAG, "unloadSoundModel(): id = " + soundModelId);
            synchronized (SoundTriggerService.this.mLock) {
                SoundModel soundModel = (SoundModel) SoundTriggerService.this.mLoadedModels.get(soundModelId.getUuid());
                if (soundModel == null) {
                    Slog.e(SoundTriggerService.TAG, soundModelId + " is not loaded");
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
            synchronized (SoundTriggerService.this.mLock) {
                if (((LocalSoundTriggerRecognitionStatusCallback) SoundTriggerService.this.mIntentCallbacks.get(parcelUuid.getUuid())) == null) {
                    return false;
                }
                boolean isRecognitionRequested = SoundTriggerService.this.mSoundTriggerHelper.isRecognitionRequested(parcelUuid.getUuid());
                return isRecognitionRequested;
            }
        }
    }

    public SoundTriggerService(Context context) {
        super(context);
        this.mContext = context;
        this.mServiceStub = new SoundTriggerServiceStub();
        this.mLocalSoundTriggerService = new LocalSoundTriggerService(context);
        this.mLoadedModels = new TreeMap();
        this.mIntentCallbacks = new TreeMap();
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
        synchronized (this.mLock) {
            if (this.mWakelock == null) {
                this.mWakelock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, TAG);
            }
            this.mWakelock.acquire();
        }
    }

    private void enforceCallingPermission(String permission) {
        if (this.mContext.checkCallingOrSelfPermission(permission) != 0) {
            throw new SecurityException("Caller does not hold the permission " + permission);
        }
    }
}
