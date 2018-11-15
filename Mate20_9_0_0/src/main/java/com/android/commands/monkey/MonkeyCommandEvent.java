package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;

public class MonkeyCommandEvent extends MonkeyEvent {
    private String mCmd;

    public MonkeyCommandEvent(String cmd) {
        super(4);
        this.mCmd = cmd;
    }

    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (this.mCmd != null) {
            try {
                int status = Runtime.getRuntime().exec(this.mCmd).waitFor();
                Logger logger = Logger.err;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("// Shell command ");
                stringBuilder.append(this.mCmd);
                stringBuilder.append(" status was ");
                stringBuilder.append(status);
                logger.println(stringBuilder.toString());
            } catch (Exception e) {
                Logger logger2 = Logger.err;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("// Exception from ");
                stringBuilder2.append(this.mCmd);
                stringBuilder2.append(":");
                logger2.println(stringBuilder2.toString());
                Logger.err.println(e.toString());
            }
        }
        return 1;
    }
}
