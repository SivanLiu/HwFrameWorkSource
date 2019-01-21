package android.hardware.camera2.utils;

import android.util.Log;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CloseableLock implements AutoCloseable {
    private static final boolean VERBOSE = false;
    private final String TAG;
    private volatile boolean mClosed;
    private final Condition mCondition;
    private boolean mExclusive;
    private final ReentrantLock mLock;
    private final ThreadLocal<Integer> mLockCount;
    private final String mName;
    private int mSharedLocks;

    public class ScopedLock implements AutoCloseable {
        /* synthetic */ ScopedLock(CloseableLock x0, AnonymousClass1 x1) {
            this();
        }

        private ScopedLock() {
        }

        public void close() {
            CloseableLock.this.releaseLock();
        }
    }

    public CloseableLock() {
        this.TAG = "CloseableLock";
        this.mClosed = false;
        this.mExclusive = false;
        this.mSharedLocks = 0;
        this.mLock = new ReentrantLock();
        this.mCondition = this.mLock.newCondition();
        this.mLockCount = new ThreadLocal<Integer>() {
            protected Integer initialValue() {
                return Integer.valueOf(0);
            }
        };
        this.mName = "";
    }

    public CloseableLock(String name) {
        this.TAG = "CloseableLock";
        this.mClosed = false;
        this.mExclusive = false;
        this.mSharedLocks = 0;
        this.mLock = new ReentrantLock();
        this.mCondition = this.mLock.newCondition();
        this.mLockCount = /* anonymous class already generated */;
        this.mName = name;
    }

    public void close() {
        if (!this.mClosed && acquireExclusiveLock() != null) {
            if (((Integer) this.mLockCount.get()).intValue() == 1) {
                try {
                    this.mLock.lock();
                    this.mClosed = true;
                    this.mExclusive = false;
                    this.mSharedLocks = 0;
                    this.mLockCount.remove();
                    this.mCondition.signalAll();
                } finally {
                    this.mLock.unlock();
                }
            } else {
                throw new IllegalStateException("Cannot close while one or more acquired locks are being held by this thread; release all other locks first");
            }
        }
    }

    public ScopedLock acquireLock() {
        ScopedLock scopedLock;
        try {
            this.mLock.lock();
            scopedLock = null;
            if (this.mClosed) {
                return scopedLock;
            }
            int ownedLocks = ((Integer) this.mLockCount.get()).intValue();
            if (this.mExclusive) {
                if (ownedLocks > 0) {
                    throw new IllegalStateException("Cannot acquire shared lock while holding exclusive lock");
                }
            }
            while (this.mExclusive) {
                this.mCondition.awaitUninterruptibly();
                if (this.mClosed) {
                    this.mLock.unlock();
                    return null;
                }
            }
            this.mSharedLocks++;
            this.mLockCount.set(Integer.valueOf(((Integer) this.mLockCount.get()).intValue() + 1));
            this.mLock.unlock();
            return new ScopedLock(this, null);
        } finally {
            scopedLock = this.mLock;
            scopedLock.unlock();
        }
    }

    public ScopedLock acquireExclusiveLock() {
        ScopedLock scopedLock;
        try {
            this.mLock.lock();
            scopedLock = null;
            if (this.mClosed) {
                return scopedLock;
            }
            int ownedLocks = ((Integer) this.mLockCount.get()).intValue();
            if (!this.mExclusive) {
                if (ownedLocks > 0) {
                    throw new IllegalStateException("Cannot acquire exclusive lock while holding shared lock");
                }
            }
            while (ownedLocks == 0 && (this.mExclusive || this.mSharedLocks > 0)) {
                this.mCondition.awaitUninterruptibly();
                if (this.mClosed) {
                    this.mLock.unlock();
                    return null;
                }
            }
            this.mExclusive = true;
            this.mLockCount.set(Integer.valueOf(((Integer) this.mLockCount.get()).intValue() + 1));
            this.mLock.unlock();
            return new ScopedLock(this, null);
        } finally {
            scopedLock = this.mLock;
            scopedLock.unlock();
        }
    }

    public void releaseLock() {
        if (((Integer) this.mLockCount.get()).intValue() > 0) {
            try {
                this.mLock.lock();
                if (this.mClosed) {
                    throw new IllegalStateException("Do not release after the lock has been closed");
                }
                if (!this.mExclusive) {
                    this.mSharedLocks--;
                } else if (this.mSharedLocks != 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Too many shared locks ");
                    stringBuilder.append(this.mSharedLocks);
                    throw new AssertionError(stringBuilder.toString());
                }
                int ownedLocks = ((Integer) this.mLockCount.get()).intValue() - 1;
                this.mLockCount.set(Integer.valueOf(ownedLocks));
                if (ownedLocks == 0 && this.mExclusive) {
                    this.mExclusive = false;
                    this.mCondition.signalAll();
                } else if (ownedLocks == 0 && this.mSharedLocks == 0) {
                    this.mCondition.signalAll();
                }
                this.mLock.unlock();
            } catch (Throwable th) {
                this.mLock.unlock();
            }
        } else {
            throw new IllegalStateException("Cannot release lock that was not acquired by this thread");
        }
    }

    private void log(String what) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CloseableLock[");
        stringBuilder.append(this.mName);
        stringBuilder.append("]");
        Log.v(stringBuilder.toString(), what);
    }
}
