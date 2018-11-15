package com.android.commands.monkey;

import android.app.IActivityManager;
import android.os.RemoteException;
import android.view.IWindowManager;

public class MonkeyRotationEvent extends MonkeyEvent {
    private final boolean mPersist;
    private final int mRotationDegree;

    public MonkeyRotationEvent(int degree, boolean persist) {
        super(3);
        this.mRotationDegree = degree;
        this.mPersist = persist;
    }

    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (verbose > 0) {
            Logger logger = Logger.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(":Sending rotation degree=");
            stringBuilder.append(this.mRotationDegree);
            stringBuilder.append(", persist=");
            stringBuilder.append(this.mPersist);
            logger.println(stringBuilder.toString());
        }
        try {
            iwm.freezeRotation(this.mRotationDegree);
            if (!this.mPersist) {
                iwm.thawRotation();
            }
            return 1;
        } catch (RemoteException e) {
            return -1;
        }
    }
}
