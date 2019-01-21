package android.speech.tts;

import android.util.Log;

class PlaybackSynthesisCallback extends AbstractSynthesisCallback {
    private static final boolean DBG = false;
    private static final int MIN_AUDIO_BUFFER_SIZE = 8192;
    private static final String TAG = "PlaybackSynthesisRequest";
    private final AudioOutputParams mAudioParams;
    private final AudioPlaybackHandler mAudioTrackHandler;
    private final Object mCallerIdentity;
    private final UtteranceProgressDispatcher mDispatcher;
    private volatile boolean mDone = false;
    private SynthesisPlaybackQueueItem mItem = null;
    private final AbstractEventLogger mLogger;
    private final Object mStateLock = new Object();
    protected int mStatusCode;

    PlaybackSynthesisCallback(AudioOutputParams audioParams, AudioPlaybackHandler audioTrackHandler, UtteranceProgressDispatcher dispatcher, Object callerIdentity, AbstractEventLogger logger, boolean clientIsUsingV2) {
        super(clientIsUsingV2);
        this.mAudioParams = audioParams;
        this.mAudioTrackHandler = audioTrackHandler;
        this.mDispatcher = dispatcher;
        this.mCallerIdentity = callerIdentity;
        this.mLogger = logger;
        this.mStatusCode = 0;
    }

    /* JADX WARNING: Missing block: B:14:0x001d, code skipped:
            if (r1 == null) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:15:0x001f, code skipped:
            r1.stop(-2);
     */
    /* JADX WARNING: Missing block: B:16:0x0023, code skipped:
            r3.mLogger.onCompleted(-2);
            r3.mDispatcher.dispatchOnStop();
     */
    /* JADX WARNING: Missing block: B:17:0x002d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void stop() {
        synchronized (this.mStateLock) {
            if (this.mDone) {
            } else if (this.mStatusCode == -2) {
                Log.w(TAG, "stop() called twice");
            } else {
                SynthesisPlaybackQueueItem item = this.mItem;
                this.mStatusCode = -2;
            }
        }
    }

    public int getMaxBufferSize() {
        return 8192;
    }

    public boolean hasStarted() {
        boolean z;
        synchronized (this.mStateLock) {
            z = this.mItem != null;
        }
        return z;
    }

    public boolean hasFinished() {
        boolean z;
        synchronized (this.mStateLock) {
            z = this.mDone;
        }
        return z;
    }

    public int start(int sampleRateInHz, int audioFormat, int channelCount) {
        if (!(audioFormat == 3 || audioFormat == 2 || audioFormat == 4)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Audio format encoding ");
            stringBuilder.append(audioFormat);
            stringBuilder.append(" not supported. Please use one of AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT or AudioFormat.ENCODING_PCM_FLOAT");
            Log.w(str, stringBuilder.toString());
        }
        this.mDispatcher.dispatchOnBeginSynthesis(sampleRateInHz, audioFormat, channelCount);
        int channelConfig = BlockingAudioTrack.getChannelConfig(channelCount);
        synchronized (this.mStateLock) {
            if (channelConfig == 0) {
                try {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unsupported number of channels :");
                    stringBuilder2.append(channelCount);
                    Log.e(str2, stringBuilder2.toString());
                    this.mStatusCode = -5;
                    return -1;
                } catch (Throwable th) {
                }
            } else if (this.mStatusCode == -2) {
                int errorCodeOnStop = errorCodeOnStop();
                return errorCodeOnStop;
            } else if (this.mStatusCode != 0) {
                return -1;
            } else if (this.mItem != null) {
                Log.e(TAG, "Start called twice");
                return -1;
            } else {
                SynthesisPlaybackQueueItem synthesisPlaybackQueueItem = new SynthesisPlaybackQueueItem(this.mAudioParams, sampleRateInHz, audioFormat, channelCount, this.mDispatcher, this.mCallerIdentity, this.mLogger);
                this.mAudioTrackHandler.enqueue(synthesisPlaybackQueueItem);
                this.mItem = synthesisPlaybackQueueItem;
                return 0;
            }
        }
    }

    public int audioAvailable(byte[] buffer, int offset, int length) {
        if (length > getMaxBufferSize() || length <= 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("buffer is too large or of zero length (");
            stringBuilder.append(length);
            stringBuilder.append(" bytes)");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        synchronized (this.mStateLock) {
            if (this.mItem == null) {
                this.mStatusCode = -5;
                return -1;
            } else if (this.mStatusCode != 0) {
                return -1;
            } else if (this.mStatusCode == -2) {
                int errorCodeOnStop = errorCodeOnStop();
                return errorCodeOnStop;
            } else {
                SynthesisPlaybackQueueItem item = this.mItem;
                byte[] bufferCopy = new byte[length];
                System.arraycopy(buffer, offset, bufferCopy, 0, length);
                this.mDispatcher.dispatchOnAudioAvailable(bufferCopy);
                try {
                    item.put(bufferCopy);
                    this.mLogger.onEngineDataReceived();
                    return 0;
                } catch (InterruptedException e) {
                    InterruptedException ie = e;
                    synchronized (this.mStateLock) {
                        this.mStatusCode = -5;
                        return -1;
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:24:0x004b, code skipped:
            if (r0 != 0) goto L_0x0051;
     */
    /* JADX WARNING: Missing block: B:25:0x004d, code skipped:
            r1.done();
     */
    /* JADX WARNING: Missing block: B:26:0x0051, code skipped:
            r1.stop(r0);
     */
    /* JADX WARNING: Missing block: B:27:0x0054, code skipped:
            r6.mLogger.onEngineComplete();
     */
    /* JADX WARNING: Missing block: B:28:0x005a, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int done() {
        synchronized (this.mStateLock) {
            if (this.mDone) {
                Log.w(TAG, "Duplicate call to done()");
                return -1;
            } else if (this.mStatusCode == -2) {
                int errorCodeOnStop = errorCodeOnStop();
                return errorCodeOnStop;
            } else {
                this.mDone = true;
                if (this.mItem == null) {
                    Log.w(TAG, "done() was called before start() call");
                    if (this.mStatusCode == 0) {
                        this.mDispatcher.dispatchOnSuccess();
                    } else {
                        this.mDispatcher.dispatchOnError(this.mStatusCode);
                    }
                    this.mLogger.onEngineComplete();
                    return -1;
                }
                SynthesisPlaybackQueueItem item = this.mItem;
                int statusCode = this.mStatusCode;
            }
        }
    }

    public void error() {
        error(-3);
    }

    public void error(int errorCode) {
        synchronized (this.mStateLock) {
            if (this.mDone) {
                return;
            }
            this.mStatusCode = errorCode;
        }
    }

    public void rangeStart(int markerInFrames, int start, int end) {
        if (this.mItem == null) {
            Log.e(TAG, "mItem is null");
        } else {
            this.mItem.rangeStart(markerInFrames, start, end);
        }
    }
}
