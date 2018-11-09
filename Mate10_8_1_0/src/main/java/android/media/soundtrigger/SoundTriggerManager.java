package android.media.soundtrigger;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.soundtrigger.SoundTrigger.GenericSoundModel;
import android.hardware.soundtrigger.SoundTrigger.KeyphraseSoundModel;
import android.hardware.soundtrigger.SoundTrigger.RecognitionConfig;
import android.hardware.soundtrigger.SoundTrigger.SoundModel;
import android.media.soundtrigger.SoundTriggerDetector.Callback;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.app.ISoundTriggerService;
import java.util.HashMap;
import java.util.UUID;

public final class SoundTriggerManager {
    private static final boolean DBG = false;
    public static final String EXTRA_MESSAGE_TYPE = "android.media.soundtrigger.MESSAGE_TYPE";
    public static final String EXTRA_RECOGNITION_EVENT = "android.media.soundtrigger.RECOGNITION_EVENT";
    public static final String EXTRA_STATUS = "android.media.soundtrigger.STATUS";
    public static final int FLAG_MESSAGE_TYPE_RECOGNITION_ERROR = 1;
    public static final int FLAG_MESSAGE_TYPE_RECOGNITION_EVENT = 0;
    public static final int FLAG_MESSAGE_TYPE_RECOGNITION_PAUSED = 2;
    public static final int FLAG_MESSAGE_TYPE_RECOGNITION_RESUMED = 3;
    public static final int FLAG_MESSAGE_TYPE_UNKNOWN = -1;
    private static final String TAG = "SoundTriggerManager";
    private final Context mContext;
    private final HashMap<UUID, SoundTriggerDetector> mReceiverInstanceMap = new HashMap();
    private final ISoundTriggerService mSoundTriggerService;

    public static class Model {
        private GenericSoundModel mGenericSoundModel;

        Model(GenericSoundModel soundTriggerModel) {
            this.mGenericSoundModel = soundTriggerModel;
        }

        public static Model create(UUID modelUuid, UUID vendorUuid, byte[] data) {
            return new Model(new GenericSoundModel(modelUuid, vendorUuid, data));
        }

        public UUID getModelUuid() {
            return this.mGenericSoundModel.uuid;
        }

        public UUID getVendorUuid() {
            return this.mGenericSoundModel.vendorUuid;
        }

        public byte[] getModelData() {
            return this.mGenericSoundModel.data;
        }

        GenericSoundModel getGenericSoundModel() {
            return this.mGenericSoundModel;
        }
    }

    public SoundTriggerManager(Context context, ISoundTriggerService soundTriggerService) {
        this.mSoundTriggerService = soundTriggerService;
        this.mContext = context;
    }

    public void updateModel(Model model) {
        try {
            this.mSoundTriggerService.updateSoundModel(model.getGenericSoundModel());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Model getModel(UUID soundModelId) {
        try {
            return new Model(this.mSoundTriggerService.getSoundModel(new ParcelUuid(soundModelId)));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void deleteModel(UUID soundModelId) {
        try {
            this.mSoundTriggerService.deleteSoundModel(new ParcelUuid(soundModelId));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public SoundTriggerDetector createSoundTriggerDetector(UUID soundModelId, Callback callback, Handler handler) {
        if (soundModelId == null) {
            return null;
        }
        SoundTriggerDetector oldInstance = (SoundTriggerDetector) this.mReceiverInstanceMap.get(soundModelId);
        SoundTriggerDetector newInstance = new SoundTriggerDetector(this.mSoundTriggerService, soundModelId, callback, handler);
        this.mReceiverInstanceMap.put(soundModelId, newInstance);
        return newInstance;
    }

    public int loadSoundModel(SoundModel soundModel) {
        if (soundModel == null) {
            return Integer.MIN_VALUE;
        }
        try {
            switch (soundModel.type) {
                case 0:
                    return this.mSoundTriggerService.loadKeyphraseSoundModel((KeyphraseSoundModel) soundModel);
                case 1:
                    return this.mSoundTriggerService.loadGenericSoundModel((GenericSoundModel) soundModel);
                default:
                    Slog.e(TAG, "Unkown model type");
                    return Integer.MIN_VALUE;
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int startRecognition(UUID soundModelId, PendingIntent callbackIntent, RecognitionConfig config) {
        if (soundModelId == null || callbackIntent == null || config == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return this.mSoundTriggerService.startRecognitionForIntent(new ParcelUuid(soundModelId), callbackIntent, config);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int stopRecognition(UUID soundModelId) {
        if (soundModelId == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return this.mSoundTriggerService.stopRecognitionForIntent(new ParcelUuid(soundModelId));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int unloadSoundModel(UUID soundModelId) {
        if (soundModelId == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return this.mSoundTriggerService.unloadSoundModel(new ParcelUuid(soundModelId));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isRecognitionActive(UUID soundModelId) {
        if (soundModelId == null) {
            return false;
        }
        try {
            return this.mSoundTriggerService.isRecognitionActive(new ParcelUuid(soundModelId));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
