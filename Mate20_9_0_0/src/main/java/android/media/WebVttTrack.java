package android.media;

import android.media.SubtitleTrack.Cue;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/* compiled from: WebVttRenderer */
class WebVttTrack extends SubtitleTrack implements WebVttCueListener {
    private static final String TAG = "WebVttTrack";
    private Long mCurrentRunID;
    private final UnstyledTextExtractor mExtractor = new UnstyledTextExtractor();
    private final WebVttParser mParser = new WebVttParser(this);
    private final Map<String, TextTrackRegion> mRegions = new HashMap();
    private final WebVttRenderingWidget mRenderingWidget;
    private final Vector<Long> mTimestamps = new Vector();
    private final Tokenizer mTokenizer = new Tokenizer(this.mExtractor);

    WebVttTrack(WebVttRenderingWidget renderingWidget, MediaFormat format) {
        super(format);
        this.mRenderingWidget = renderingWidget;
    }

    public WebVttRenderingWidget getRenderingWidget() {
        return this.mRenderingWidget;
    }

    public void onData(byte[] data, boolean eos, long runID) {
        try {
            String str = new String(data, "UTF-8");
            synchronized (this.mParser) {
                if (this.mCurrentRunID != null) {
                    if (runID != this.mCurrentRunID.longValue()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Run #");
                        stringBuilder.append(this.mCurrentRunID);
                        stringBuilder.append(" in progress.  Cannot process run #");
                        stringBuilder.append(runID);
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                }
                this.mCurrentRunID = Long.valueOf(runID);
                this.mParser.parse(str);
                if (eos) {
                    finishedRun(runID);
                    this.mParser.eos();
                    this.mRegions.clear();
                    this.mCurrentRunID = null;
                }
            }
        } catch (UnsupportedEncodingException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("subtitle data is not UTF-8 encoded: ");
            stringBuilder2.append(e);
            Log.w(str2, stringBuilder2.toString());
        }
    }

    public void onCueParsed(TextTrackCue cue) {
        synchronized (this.mParser) {
            String str;
            StringBuilder stringBuilder;
            if (cue.mRegionId.length() != 0) {
                cue.mRegion = (TextTrackRegion) this.mRegions.get(cue.mRegionId);
            }
            if (this.DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("adding cue ");
                stringBuilder.append(cue);
                Log.v(str, stringBuilder.toString());
            }
            this.mTokenizer.reset();
            int ix = 0;
            for (String s : cue.mStrings) {
                this.mTokenizer.tokenize(s);
            }
            cue.mLines = this.mExtractor.getText();
            if (this.DEBUG) {
                str = TAG;
                stringBuilder = cue.appendStringsToBuilder(new StringBuilder());
                stringBuilder.append(" simplified to: ");
                Log.v(str, cue.appendLinesToBuilder(stringBuilder).toString());
            }
            for (TextTrackCueSpan[] line : cue.mLines) {
                for (TextTrackCueSpan span : r1[r4]) {
                    if (span.mTimestampMs > cue.mStartTimeMs && span.mTimestampMs < cue.mEndTimeMs && !this.mTimestamps.contains(Long.valueOf(span.mTimestampMs))) {
                        this.mTimestamps.add(Long.valueOf(span.mTimestampMs));
                    }
                }
            }
            if (this.mTimestamps.size() > 0) {
                cue.mInnerTimesMs = new long[this.mTimestamps.size()];
                while (true) {
                    int ix2 = ix;
                    if (ix2 >= this.mTimestamps.size()) {
                        break;
                    }
                    cue.mInnerTimesMs[ix2] = ((Long) this.mTimestamps.get(ix2)).longValue();
                    ix = ix2 + 1;
                }
                this.mTimestamps.clear();
            } else {
                cue.mInnerTimesMs = null;
            }
            cue.mRunID = this.mCurrentRunID.longValue();
        }
        addCue(cue);
    }

    public void onRegionParsed(TextTrackRegion region) {
        synchronized (this.mParser) {
            this.mRegions.put(region.mId, region);
        }
    }

    public void updateView(Vector<Cue> activeCues) {
        if (this.mVisible) {
            if (this.DEBUG && this.mTimeProvider != null) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("at ");
                    stringBuilder.append(this.mTimeProvider.getCurrentTimeUs(false, true) / 1000);
                    stringBuilder.append(" ms the active cues are:");
                    Log.d(str, stringBuilder.toString());
                } catch (IllegalStateException e) {
                    Log.d(TAG, "at (illegal state) the active cues are:");
                }
            }
            if (this.mRenderingWidget != null) {
                this.mRenderingWidget.setActiveCues(activeCues);
            }
        }
    }
}
