package com.android.server.wm;

import android.util.ArrayMap;
import android.util.ArraySet;
import java.io.PrintWriter;
import java.util.ArrayList;

class AnimatingAppWindowTokenRegistry {
    private ArraySet<AppWindowToken> mAnimatingTokens = new ArraySet();
    private boolean mEndingDeferredFinish;
    private ArrayMap<AppWindowToken, Runnable> mFinishedTokens = new ArrayMap();
    private ArrayList<Runnable> mTmpRunnableList = new ArrayList();

    AnimatingAppWindowTokenRegistry() {
    }

    void notifyStarting(AppWindowToken token) {
        this.mAnimatingTokens.add(token);
    }

    void notifyFinished(AppWindowToken token) {
        this.mAnimatingTokens.remove(token);
        this.mFinishedTokens.remove(token);
        if (this.mAnimatingTokens.isEmpty()) {
            endDeferringFinished();
        }
    }

    boolean notifyAboutToFinish(AppWindowToken token, Runnable endDeferFinishCallback) {
        if (!this.mAnimatingTokens.remove(token)) {
            return false;
        }
        if (this.mAnimatingTokens.isEmpty()) {
            endDeferringFinished();
            return false;
        }
        this.mFinishedTokens.put(token, endDeferFinishCallback);
        return true;
    }

    private void endDeferringFinished() {
        if (!this.mEndingDeferredFinish) {
            try {
                int i;
                this.mEndingDeferredFinish = true;
                for (i = this.mFinishedTokens.size() - 1; i >= 0; i--) {
                    this.mTmpRunnableList.add((Runnable) this.mFinishedTokens.valueAt(i));
                }
                this.mFinishedTokens.clear();
                i = this.mTmpRunnableList.size() - 1;
                while (true) {
                    int i2 = i;
                    if (i2 < 0) {
                        break;
                    }
                    ((Runnable) this.mTmpRunnableList.get(i2)).run();
                    i = i2 - 1;
                }
                this.mTmpRunnableList.clear();
            } finally {
                this.mEndingDeferredFinish = false;
            }
        }
    }

    void dump(PrintWriter pw, String header, String prefix) {
        if (!this.mAnimatingTokens.isEmpty() || !this.mFinishedTokens.isEmpty()) {
            pw.print(prefix);
            pw.println(header);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            prefix = stringBuilder.toString();
            pw.print(prefix);
            pw.print("mAnimatingTokens=");
            pw.println(this.mAnimatingTokens);
            pw.print(prefix);
            pw.print("mFinishedTokens=");
            pw.println(this.mFinishedTokens);
        }
    }
}
