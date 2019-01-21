package android.media;

import android.media.AudioManager.OnAudioPortUpdateListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

class AudioPortEventHandler {
    private static final int AUDIOPORT_EVENT_NEW_LISTENER = 4;
    private static final int AUDIOPORT_EVENT_PATCH_LIST_UPDATED = 2;
    private static final int AUDIOPORT_EVENT_PORT_LIST_UPDATED = 1;
    private static final int AUDIOPORT_EVENT_SERVICE_DIED = 3;
    private static final long RESCHEDULE_MESSAGE_DELAY_MS = 100;
    private static final String TAG = "AudioPortEventHandler";
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private long mJniCallback;
    private final ArrayList<OnAudioPortUpdateListener> mListeners = new ArrayList();

    private native void native_finalize();

    private native void native_setup(Object obj);

    AudioPortEventHandler() {
    }

    /* JADX WARNING: Missing block: B:11:0x0037, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void init() {
        synchronized (this) {
            if (this.mHandler != null) {
                return;
            }
            this.mHandlerThread = new HandlerThread(TAG);
            this.mHandlerThread.start();
            if (this.mHandlerThread.getLooper() != null) {
                this.mHandler = new Handler(this.mHandlerThread.getLooper()) {
                    /* JADX WARNING: Missing block: B:28:0x0071, code skipped:
            if (r2 >= r0.size()) goto L_0x00bd;
     */
                    /* JADX WARNING: Missing block: B:35:0x009d, code skipped:
            if (r9.what == 1) goto L_0x00bd;
     */
                    /* Code decompiled incorrectly, please refer to instructions dump. */
                    public void handleMessage(Message msg) {
                        ArrayList<OnAudioPortUpdateListener> listeners;
                        synchronized (this) {
                            if (msg.what == 4) {
                                listeners = new ArrayList();
                                if (AudioPortEventHandler.this.mListeners.contains(msg.obj)) {
                                    listeners.add((OnAudioPortUpdateListener) msg.obj);
                                }
                            } else {
                                listeners = AudioPortEventHandler.this.mListeners;
                            }
                        }
                        if (msg.what == 1 || msg.what == 2 || msg.what == 3) {
                            AudioManager.resetAudioPortGeneration();
                        }
                        if (!listeners.isEmpty()) {
                            ArrayList<AudioPort> ports = new ArrayList();
                            ArrayList<AudioPatch> patches = new ArrayList();
                            if (msg.what == 3 || AudioManager.updateAudioPortCache(ports, patches, 0) == 0) {
                                int i = 0;
                                switch (msg.what) {
                                    case 1:
                                    case 4:
                                        AudioPort[] portList = (AudioPort[]) ports.toArray(new AudioPort[0]);
                                        for (int i2 = 0; i2 < listeners.size(); i2++) {
                                            ((OnAudioPortUpdateListener) listeners.get(i2)).onAudioPortListUpdate(portList);
                                        }
                                        break;
                                    case 2:
                                        AudioPatch[] patchList = (AudioPatch[]) patches.toArray(new AudioPatch[0]);
                                        while (true) {
                                            int i3 = i;
                                            if (i3 >= listeners.size()) {
                                                break;
                                            }
                                            ((OnAudioPortUpdateListener) listeners.get(i3)).onAudioPatchListUpdate(patchList);
                                            i = i3 + 1;
                                        }
                                    case 3:
                                        while (true) {
                                            int i4 = i;
                                            ((OnAudioPortUpdateListener) listeners.get(i4)).onServiceDied();
                                            i = i4 + 1;
                                            break;
                                        }
                                }
                                return;
                            }
                            sendMessageDelayed(obtainMessage(msg.what, msg.obj), AudioPortEventHandler.RESCHEDULE_MESSAGE_DELAY_MS);
                        }
                    }
                };
                native_setup(new WeakReference(this));
            } else {
                this.mHandler = null;
            }
        }
    }

    protected void finalize() {
        native_finalize();
        if (this.mHandlerThread.isAlive()) {
            this.mHandlerThread.quit();
        }
    }

    void registerListener(OnAudioPortUpdateListener l) {
        synchronized (this) {
            this.mListeners.add(l);
        }
        if (this.mHandler != null) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(4, 0, 0, l));
        }
    }

    void unregisterListener(OnAudioPortUpdateListener l) {
        synchronized (this) {
            this.mListeners.remove(l);
        }
    }

    Handler handler() {
        return this.mHandler;
    }

    private static void postEventFromNative(Object module_ref, int what, int arg1, int arg2, Object obj) {
        AudioPortEventHandler eventHandler = (AudioPortEventHandler) ((WeakReference) module_ref).get();
        if (!(eventHandler == null || eventHandler == null)) {
            Handler handler = eventHandler.handler();
            if (handler != null) {
                Message m = handler.obtainMessage(what, arg1, arg2, obj);
                if (what != 4) {
                    handler.removeMessages(what);
                }
                handler.sendMessage(m);
            }
        }
    }
}
