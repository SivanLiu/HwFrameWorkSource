package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;

public class MonkeyWaitEvent extends MonkeyEvent {
    private long mWaitTime;

    public MonkeyWaitEvent(long waitTime) {
        super(6);
        this.mWaitTime = waitTime;
    }

    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (verbose > 1) {
            Logger logger = Logger.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Wait Event for ");
            stringBuilder.append(this.mWaitTime);
            stringBuilder.append(" milliseconds");
            logger.println(stringBuilder.toString());
        }
        try {
            Thread.sleep(this.mWaitTime);
            return 1;
        } catch (InterruptedException e) {
            Logger.out.println("** Monkey interrupted in sleep.");
            return 0;
        }
    }
}
