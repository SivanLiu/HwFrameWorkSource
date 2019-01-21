package android.media;

import android.media.SoundPool.Builder;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;

public class MediaActionSound {
    public static final int FOCUS_COMPLETE = 1;
    private static final int NUM_MEDIA_SOUND_STREAMS = 1;
    public static final int SHUTTER_CLICK = 0;
    private static final String[] SOUND_DIRS = new String[]{"/product/media/audio/ui/", "/system/media/audio/ui/"};
    private static final String[] SOUND_FILES = new String[]{"camera_click.ogg", "camera_focus.ogg", "VideoRecord.ogg", "VideoStop.ogg"};
    public static final int START_VIDEO_RECORDING = 2;
    private static final int STATE_LOADED = 3;
    private static final int STATE_LOADING = 1;
    private static final int STATE_LOADING_PLAY_REQUESTED = 2;
    private static final int STATE_NOT_LOADED = 0;
    public static final int STOP_VIDEO_RECORDING = 3;
    private static final String TAG = "MediaActionSound";
    private OnLoadCompleteListener mLoadCompleteListener = new OnLoadCompleteListener() {
        /* JADX WARNING: Missing block: B:21:0x0074, code skipped:
            if (r0 == 0) goto L_0x0086;
     */
        /* JADX WARNING: Missing block: B:22:0x0076, code skipped:
            r13.play(r0, 1.0f, 1.0f, 0, 0, 1.0f);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            SoundState[] access$000 = MediaActionSound.this.mSounds;
            int length = access$000.length;
            int i = 0;
            while (i < length) {
                SoundState sound = access$000[i];
                if (sound.id != sampleId) {
                    i++;
                } else {
                    int playSoundId = 0;
                    synchronized (sound) {
                        String str;
                        StringBuilder stringBuilder;
                        if (status != 0) {
                            try {
                                sound.state = 0;
                                sound.id = 0;
                                str = MediaActionSound.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("OnLoadCompleteListener() error: ");
                                stringBuilder.append(status);
                                stringBuilder.append(" loading sound: ");
                                stringBuilder.append(sound.name);
                                Log.e(str, stringBuilder.toString());
                                return;
                            } catch (Throwable th) {
                                while (true) {
                                }
                            }
                        } else {
                            switch (sound.state) {
                                case 1:
                                    sound.state = 3;
                                    break;
                                case 2:
                                    playSoundId = sound.id;
                                    sound.state = 3;
                                    break;
                                default:
                                    str = MediaActionSound.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("OnLoadCompleteListener() called in wrong state: ");
                                    stringBuilder.append(sound.state);
                                    stringBuilder.append(" for sound: ");
                                    stringBuilder.append(sound.name);
                                    Log.e(str, stringBuilder.toString());
                                    break;
                            }
                        }
                    }
                }
            }
        }
    };
    private SoundPool mSoundPool = new Builder().setMaxStreams(1).setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setFlags(1).setContentType(4).build()).build();
    private SoundState[] mSounds;

    private class SoundState {
        public int id = 0;
        public final int name;
        public int state = 0;

        public SoundState(int name) {
            this.name = name;
        }
    }

    public MediaActionSound() {
        this.mSoundPool.setOnLoadCompleteListener(this.mLoadCompleteListener);
        this.mSounds = new SoundState[SOUND_FILES.length];
        for (int i = 0; i < this.mSounds.length; i++) {
            this.mSounds[i] = new SoundState(i);
        }
    }

    private int loadSound(SoundState sound) {
        String soundFileName = SOUND_FILES[sound.name];
        for (String soundDir : SOUND_DIRS) {
            int id = this.mSoundPool;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(soundDir);
            stringBuilder.append(soundFileName);
            id = id.load(stringBuilder.toString(), 1);
            if (id > 0) {
                sound.state = 1;
                sound.id = id;
                return id;
            }
        }
        return 0;
    }

    public void load(int soundName) {
        if (soundName < 0 || soundName >= SOUND_FILES.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown sound requested: ");
            stringBuilder.append(soundName);
            throw new RuntimeException(stringBuilder.toString());
        }
        SoundState sound = this.mSounds[soundName];
        synchronized (sound) {
            String str;
            StringBuilder stringBuilder2;
            if (sound.state != 0) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("load() called in wrong state: ");
                stringBuilder2.append(sound);
                stringBuilder2.append(" for sound: ");
                stringBuilder2.append(soundName);
                Log.e(str, stringBuilder2.toString());
            } else if (loadSound(sound) <= 0) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("load() error loading sound: ");
                stringBuilder2.append(soundName);
                Log.e(str, stringBuilder2.toString());
            }
        }
    }

    public void play(int soundName) {
        if (soundName < 0 || soundName >= SOUND_FILES.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown sound requested: ");
            stringBuilder.append(soundName);
            throw new RuntimeException(stringBuilder.toString());
        }
        SoundState sound = this.mSounds[soundName];
        synchronized (sound) {
            int i = sound.state;
            if (i != 3) {
                String str;
                StringBuilder stringBuilder2;
                switch (i) {
                    case 0:
                        loadSound(sound);
                        if (loadSound(sound) <= 0) {
                            str = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("play() error loading sound: ");
                            stringBuilder2.append(soundName);
                            Log.e(str, stringBuilder2.toString());
                            break;
                        }
                    case 1:
                        sound.state = 2;
                        break;
                    default:
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("play() called in wrong state: ");
                        stringBuilder2.append(sound.state);
                        stringBuilder2.append(" for sound: ");
                        stringBuilder2.append(soundName);
                        Log.e(str, stringBuilder2.toString());
                        break;
                }
            }
            this.mSoundPool.play(sound.id, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void release() {
        if (this.mSoundPool != null) {
            for (SoundState sound : this.mSounds) {
                synchronized (sound) {
                    sound.state = 0;
                    sound.id = 0;
                }
            }
            this.mSoundPool.release();
            this.mSoundPool = null;
        }
    }
}
