package android.util;

import android.os.SystemClock;
import java.util.ArrayList;

public class TimingLogger {
    private boolean mDisabled;
    private String mLabel;
    ArrayList<String> mSplitLabels;
    ArrayList<Long> mSplits;
    private String mTag;

    public TimingLogger(String tag, String label) {
        reset(tag, label);
    }

    public void reset(String tag, String label) {
        this.mTag = tag;
        this.mLabel = label;
        reset();
    }

    public void reset() {
        this.mDisabled = Log.isLoggable(this.mTag, 2) ^ 1;
        if (!this.mDisabled) {
            if (this.mSplits == null) {
                this.mSplits = new ArrayList();
                this.mSplitLabels = new ArrayList();
            } else {
                this.mSplits.clear();
                this.mSplitLabels.clear();
            }
            addSplit(null);
        }
    }

    public void addSplit(String splitLabel) {
        if (!this.mDisabled) {
            this.mSplits.add(Long.valueOf(SystemClock.elapsedRealtime()));
            this.mSplitLabels.add(splitLabel);
        }
    }

    public void dumpToLog() {
        if (!this.mDisabled) {
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mLabel);
            stringBuilder.append(": begin");
            Log.d(str, stringBuilder.toString());
            long first = ((Long) this.mSplits.get(0)).longValue();
            long now = first;
            for (int i = 1; i < this.mSplits.size(); i++) {
                now = ((Long) this.mSplits.get(i)).longValue();
                String splitLabel = (String) this.mSplitLabels.get(i);
                long prev = ((Long) this.mSplits.get(i - 1)).longValue();
                String str2 = this.mTag;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mLabel);
                stringBuilder2.append(":      ");
                stringBuilder2.append(now - prev);
                stringBuilder2.append(" ms, ");
                stringBuilder2.append(splitLabel);
                Log.d(str2, stringBuilder2.toString());
            }
            String str3 = this.mTag;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(this.mLabel);
            stringBuilder3.append(": end, ");
            stringBuilder3.append(now - first);
            stringBuilder3.append(" ms");
            Log.d(str3, stringBuilder3.toString());
        }
    }
}
