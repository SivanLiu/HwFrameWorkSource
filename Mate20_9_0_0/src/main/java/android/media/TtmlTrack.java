package android.media;

import android.media.SubtitleTrack.Cue;
import android.util.Log;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import org.xmlpull.v1.XmlPullParserException;

/* compiled from: TtmlRenderer */
class TtmlTrack extends SubtitleTrack implements TtmlNodeListener {
    private static final String TAG = "TtmlTrack";
    private Long mCurrentRunID;
    private final TtmlParser mParser = new TtmlParser(this);
    private String mParsingData;
    private final TtmlRenderingWidget mRenderingWidget;
    private TtmlNode mRootNode;
    private final TreeSet<Long> mTimeEvents = new TreeSet();
    private final LinkedList<TtmlNode> mTtmlNodes = new LinkedList();

    TtmlTrack(TtmlRenderingWidget renderingWidget, MediaFormat format) {
        super(format);
        this.mRenderingWidget = renderingWidget;
        this.mParsingData = "";
    }

    public TtmlRenderingWidget getRenderingWidget() {
        return this.mRenderingWidget;
    }

    public void onData(byte[] data, boolean eos, long runID) {
        StringBuilder stringBuilder;
        try {
            String str = new String(data, "UTF-8");
            synchronized (this.mParser) {
                if (this.mCurrentRunID != null) {
                    if (runID != this.mCurrentRunID.longValue()) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Run #");
                        stringBuilder2.append(this.mCurrentRunID);
                        stringBuilder2.append(" in progress.  Cannot process run #");
                        stringBuilder2.append(runID);
                        throw new IllegalStateException(stringBuilder2.toString());
                    }
                }
                this.mCurrentRunID = Long.valueOf(runID);
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.mParsingData);
                stringBuilder.append(str);
                this.mParsingData = stringBuilder.toString();
                if (eos) {
                    try {
                        this.mParser.parse(this.mParsingData, this.mCurrentRunID.longValue());
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    finishedRun(runID);
                    this.mParsingData = "";
                    this.mCurrentRunID = null;
                }
            }
        } catch (UnsupportedEncodingException e3) {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("subtitle data is not UTF-8 encoded: ");
            stringBuilder.append(e3);
            Log.w(str2, stringBuilder.toString());
        }
    }

    public void onTtmlNodeParsed(TtmlNode node) {
        this.mTtmlNodes.addLast(node);
        addTimeEvents(node);
    }

    public void onRootNodeParsed(TtmlNode node) {
        this.mRootNode = node;
        TtmlCue cue = null;
        while (true) {
            TtmlCue nextResult = getNextResult();
            cue = nextResult;
            if (nextResult != null) {
                addCue(cue);
            } else {
                this.mRootNode = null;
                this.mTtmlNodes.clear();
                this.mTimeEvents.clear();
                return;
            }
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
            this.mRenderingWidget.setActiveCues(activeCues);
        }
    }

    public TtmlCue getNextResult() {
        while (this.mTimeEvents.size() >= 2) {
            long start = ((Long) this.mTimeEvents.pollFirst()).longValue();
            long end = ((Long) this.mTimeEvents.first()).longValue();
            if (!getActiveNodes(start, end).isEmpty()) {
                return new TtmlCue(start, end, TtmlUtils.applySpacePolicy(TtmlUtils.extractText(this.mRootNode, start, end), false), TtmlUtils.extractTtmlFragment(this.mRootNode, start, end));
            }
        }
        return null;
    }

    private void addTimeEvents(TtmlNode node) {
        this.mTimeEvents.add(Long.valueOf(node.mStartTimeMs));
        this.mTimeEvents.add(Long.valueOf(node.mEndTimeMs));
        for (int i = 0; i < node.mChildren.size(); i++) {
            addTimeEvents((TtmlNode) node.mChildren.get(i));
        }
    }

    private List<TtmlNode> getActiveNodes(long startTimeUs, long endTimeUs) {
        List<TtmlNode> activeNodes = new ArrayList();
        for (int i = 0; i < this.mTtmlNodes.size(); i++) {
            TtmlNode node = (TtmlNode) this.mTtmlNodes.get(i);
            if (node.isActive(startTimeUs, endTimeUs)) {
                activeNodes.add(node);
            }
        }
        return activeNodes;
    }
}
