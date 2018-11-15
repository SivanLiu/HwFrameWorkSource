package com.android.systemui.shared.recents.model;

import java.util.concurrent.ConcurrentLinkedQueue;

class TaskResourceLoadQueue {
    private final ConcurrentLinkedQueue<Task> mQueue = new ConcurrentLinkedQueue();

    TaskResourceLoadQueue() {
    }

    void addTask(Task t) {
        if (!this.mQueue.contains(t)) {
            this.mQueue.add(t);
        }
        synchronized (this) {
            notifyAll();
        }
    }

    Task nextTask() {
        return (Task) this.mQueue.poll();
    }

    void removeTask(Task t) {
        this.mQueue.remove(t);
    }

    void clearTasks() {
        this.mQueue.clear();
    }

    boolean isEmpty() {
        return this.mQueue.isEmpty();
    }
}
