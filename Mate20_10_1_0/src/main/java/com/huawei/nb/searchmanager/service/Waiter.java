package com.huawei.nb.searchmanager.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Waiter {
    private int awaitCount = 0;
    private boolean completed = false;
    private Condition condition = this.lock.newCondition();
    private boolean interrupted = false;
    private final ReentrantLock lock = new ReentrantLock();

    public boolean await(long timeout) {
        long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
        this.lock.lock();
        try {
            this.awaitCount++;
            while (!this.interrupted) {
                if (this.completed) {
                    int i = this.awaitCount - 1;
                    this.awaitCount = i;
                    if (i == 0) {
                        this.completed = false;
                        this.interrupted = false;
                    }
                    this.lock.unlock();
                    return true;
                } else if (nanos <= 0) {
                    int i2 = this.awaitCount - 1;
                    this.awaitCount = i2;
                    if (i2 == 0) {
                        this.completed = false;
                        this.interrupted = false;
                    }
                    this.lock.unlock();
                    return false;
                } else {
                    nanos = this.condition.awaitNanos(nanos);
                }
            }
            int i3 = this.awaitCount - 1;
            this.awaitCount = i3;
            if (i3 == 0) {
                this.completed = false;
                this.interrupted = false;
            }
            this.lock.unlock();
            return false;
        } catch (InterruptedException e) {
            int i4 = this.awaitCount - 1;
            this.awaitCount = i4;
            if (i4 == 0) {
                this.completed = false;
                this.interrupted = false;
            }
            this.lock.unlock();
            return false;
        } catch (Throwable th) {
            int i5 = this.awaitCount - 1;
            this.awaitCount = i5;
            if (i5 == 0) {
                this.completed = false;
                this.interrupted = false;
            }
            this.lock.unlock();
            throw th;
        }
    }

    public void interrupt() {
        this.lock.lock();
        try {
            this.interrupted = true;
            this.condition.signal();
        } finally {
            this.lock.unlock();
        }
    }

    public void signal() {
        this.lock.lock();
        try {
            this.completed = true;
            this.condition.signal();
        } finally {
            this.lock.unlock();
        }
    }

    public void signalAll() {
        this.lock.lock();
        try {
            this.completed = true;
            this.condition.signalAll();
        } finally {
            this.lock.unlock();
        }
    }
}
