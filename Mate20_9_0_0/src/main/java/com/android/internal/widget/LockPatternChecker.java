package com.android.internal.widget;

import android.os.AsyncTask;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils.RequestThrottledException;
import com.android.internal.widget.LockPatternView.Cell;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LockPatternChecker {
    private static final String TAG = "LockPatternChecker";

    public interface OnCheckCallback {
        void onChecked(boolean z, int i);

        void onEarlyMatched() {
        }

        void onCancelled() {
        }
    }

    public interface OnVerifyCallback {
        void onVerified(byte[] bArr, int i);
    }

    public static AsyncTask<?, ?, ?> verifyPattern(LockPatternUtils utils, List<Cell> pattern, long challenge, int userId, OnVerifyCallback callback) {
        final List<Cell> list = pattern;
        final int i = userId;
        final LockPatternUtils lockPatternUtils = utils;
        final long j = challenge;
        final OnVerifyCallback onVerifyCallback = callback;
        AsyncTask task = new AsyncTask<Void, Void, byte[]>() {
            private int mThrottleTimeout;
            private List<Cell> patternCopy;

            protected void onPreExecute() {
                this.patternCopy = new ArrayList(list);
            }

            protected byte[] doInBackground(Void... args) {
                try {
                    String str = LockPatternChecker.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("verifyPattern userId : ");
                    stringBuilder.append(i);
                    Log.i(str, stringBuilder.toString());
                    return lockPatternUtils.verifyPattern(this.patternCopy, j, i);
                } catch (RequestThrottledException ex) {
                    this.mThrottleTimeout = ex.getTimeoutMs();
                    return null;
                }
            }

            protected void onPostExecute(byte[] result) {
                onVerifyCallback.onVerified(result, this.mThrottleTimeout);
            }
        };
        task.execute(new Void[0]);
        return task;
    }

    public static AsyncTask<?, ?, ?> checkPattern(final LockPatternUtils utils, final List<Cell> pattern, final int userId, final OnCheckCallback callback) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            private int mThrottleTimeout;
            private List<Cell> patternCopy;

            protected void onPreExecute() {
                this.patternCopy = new ArrayList(pattern);
            }

            protected Boolean doInBackground(Void... args) {
                try {
                    LockPatternUtils lockPatternUtils = utils;
                    List list = this.patternCopy;
                    int i = userId;
                    OnCheckCallback onCheckCallback = callback;
                    Objects.requireNonNull(onCheckCallback);
                    return Boolean.valueOf(lockPatternUtils.checkPattern(list, i, new -$$Lambda$TTC7hNz7BTsLwhNRb2L5kl-7mdU(onCheckCallback)));
                } catch (RequestThrottledException ex) {
                    this.mThrottleTimeout = ex.getTimeoutMs();
                    return Boolean.valueOf(false);
                }
            }

            protected void onPostExecute(Boolean result) {
                callback.onChecked(result.booleanValue(), this.mThrottleTimeout);
            }

            protected void onCancelled() {
                callback.onCancelled();
            }
        };
        task.execute(new Void[0]);
        return task;
    }

    public static AsyncTask<?, ?, ?> verifyPassword(LockPatternUtils utils, String password, long challenge, int userId, OnVerifyCallback callback) {
        final int i = userId;
        final LockPatternUtils lockPatternUtils = utils;
        final String str = password;
        final long j = challenge;
        final OnVerifyCallback onVerifyCallback = callback;
        AsyncTask task = new AsyncTask<Void, Void, byte[]>() {
            private int mThrottleTimeout;

            protected byte[] doInBackground(Void... args) {
                try {
                    String str = LockPatternChecker.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("verifyPassword userId : ");
                    stringBuilder.append(i);
                    Log.i(str, stringBuilder.toString());
                    return lockPatternUtils.verifyPassword(str, j, i);
                } catch (RequestThrottledException ex) {
                    this.mThrottleTimeout = ex.getTimeoutMs();
                    return null;
                }
            }

            protected void onPostExecute(byte[] result) {
                onVerifyCallback.onVerified(result, this.mThrottleTimeout);
            }
        };
        task.execute(new Void[0]);
        return task;
    }

    public static AsyncTask<?, ?, ?> verifyTiedProfileChallenge(LockPatternUtils utils, String password, boolean isPattern, long challenge, int userId, OnVerifyCallback callback) {
        final LockPatternUtils lockPatternUtils = utils;
        final String str = password;
        final boolean z = isPattern;
        final long j = challenge;
        final int i = userId;
        final OnVerifyCallback onVerifyCallback = callback;
        AsyncTask task = new AsyncTask<Void, Void, byte[]>() {
            private int mThrottleTimeout;

            protected byte[] doInBackground(Void... args) {
                try {
                    return lockPatternUtils.verifyTiedProfileChallenge(str, z, j, i);
                } catch (RequestThrottledException ex) {
                    this.mThrottleTimeout = ex.getTimeoutMs();
                    return null;
                }
            }

            protected void onPostExecute(byte[] result) {
                onVerifyCallback.onVerified(result, this.mThrottleTimeout);
            }
        };
        task.execute(new Void[0]);
        return task;
    }

    public static AsyncTask<?, ?, ?> checkPassword(final LockPatternUtils utils, final String password, final int userId, final OnCheckCallback callback) {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            private int mThrottleTimeout;

            protected Boolean doInBackground(Void... args) {
                try {
                    LockPatternUtils lockPatternUtils = utils;
                    String str = password;
                    int i = userId;
                    OnCheckCallback onCheckCallback = callback;
                    Objects.requireNonNull(onCheckCallback);
                    return Boolean.valueOf(lockPatternUtils.checkPassword(str, i, new -$$Lambda$TTC7hNz7BTsLwhNRb2L5kl-7mdU(onCheckCallback)));
                } catch (RequestThrottledException ex) {
                    this.mThrottleTimeout = ex.getTimeoutMs();
                    return Boolean.valueOf(false);
                }
            }

            protected void onPostExecute(Boolean result) {
                callback.onChecked(result.booleanValue(), this.mThrottleTimeout);
            }

            protected void onCancelled() {
                callback.onCancelled();
            }
        };
        task.execute(new Void[0]);
        return task;
    }
}
