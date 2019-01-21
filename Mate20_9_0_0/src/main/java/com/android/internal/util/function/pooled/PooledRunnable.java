package com.android.internal.util.function.pooled;

import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

public interface PooledRunnable extends PooledLambda, Runnable, ThrowingRunnable {
    PooledRunnable recycleOnUse();
}
