package com.android.internal.telephony;

import android.os.Handler;
import android.os.Looper;

public class ExponentialBackoff {
    private long mCurrentDelayMs;
    private Handler mHandler;
    private long mMaximumDelayMs;
    private int mMultiplier;
    private int mRetryCounter;
    private Runnable mRunnable;
    private long mStartDelayMs;

    public ExponentialBackoff(long initialDelayMs, long maximumDelayMs, int multiplier, Looper looper, Runnable runnable) {
        this(initialDelayMs, maximumDelayMs, multiplier, new Handler(looper), runnable);
    }

    public ExponentialBackoff(long initialDelayMs, long maximumDelayMs, int multiplier, Handler handler, Runnable runnable) {
        this.mRetryCounter = 0;
        this.mStartDelayMs = initialDelayMs;
        this.mMaximumDelayMs = maximumDelayMs;
        this.mMultiplier = multiplier;
        this.mHandler = handler;
        this.mRunnable = runnable;
    }

    public void start() {
        this.mRetryCounter = 0;
        this.mCurrentDelayMs = this.mStartDelayMs;
        this.mHandler.removeCallbacks(this.mRunnable);
        this.mHandler.postDelayed(this.mRunnable, this.mCurrentDelayMs);
    }

    public void stop() {
        this.mRetryCounter = 0;
        this.mHandler.removeCallbacks(this.mRunnable);
    }

    public void notifyFailed() {
        this.mRetryCounter++;
        this.mCurrentDelayMs = (long) (((Math.random() + 1.0d) / 2.0d) * ((double) Math.min(this.mMaximumDelayMs, (long) (((double) this.mStartDelayMs) * Math.pow((double) this.mMultiplier, (double) this.mRetryCounter)))));
        this.mHandler.removeCallbacks(this.mRunnable);
        this.mHandler.postDelayed(this.mRunnable, this.mCurrentDelayMs);
    }

    public long getCurrentDelay() {
        return this.mCurrentDelayMs;
    }
}
