package android.media;

public class MediaSyncEvent {
    public static final int SYNC_EVENT_NONE = 0;
    public static final int SYNC_EVENT_PRESENTATION_COMPLETE = 1;
    private int mAudioSession = 0;
    private final int mType;

    public static MediaSyncEvent createEvent(int eventType) throws IllegalArgumentException {
        if (isValidType(eventType)) {
            return new MediaSyncEvent(eventType);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(eventType);
        stringBuilder.append("is not a valid MediaSyncEvent type.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private MediaSyncEvent(int eventType) {
        this.mType = eventType;
    }

    public MediaSyncEvent setAudioSessionId(int audioSessionId) throws IllegalArgumentException {
        if (audioSessionId > 0) {
            this.mAudioSession = audioSessionId;
            return this;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(audioSessionId);
        stringBuilder.append(" is not a valid session ID.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int getType() {
        return this.mType;
    }

    public int getAudioSessionId() {
        return this.mAudioSession;
    }

    private static boolean isValidType(int type) {
        switch (type) {
            case 0:
            case 1:
                return true;
            default:
                return false;
        }
    }
}
