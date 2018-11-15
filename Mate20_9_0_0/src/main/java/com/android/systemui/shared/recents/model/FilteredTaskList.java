package com.android.systemui.shared.recents.model;

import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.systemui.shared.recents.model.Task.TaskKey;
import java.util.ArrayList;
import java.util.List;

class FilteredTaskList {
    private TaskFilter mFilter;
    private final ArrayMap<TaskKey, Integer> mFilteredTaskIndices = new ArrayMap();
    private final ArrayList<Task> mFilteredTasks = new ArrayList();
    private final ArrayList<Task> mTasks = new ArrayList();

    FilteredTaskList() {
    }

    boolean setFilter(TaskFilter filter) {
        ArrayList<Task> prevFilteredTasks = new ArrayList(this.mFilteredTasks);
        this.mFilter = filter;
        updateFilteredTasks();
        synchronized (this.mFilteredTasks) {
            if (prevFilteredTasks.equals(this.mFilteredTasks)) {
                return false;
            }
            return true;
        }
    }

    void add(Task t) {
        this.mTasks.add(t);
        updateFilteredTasks();
    }

    void set(List<Task> tasks) {
        this.mTasks.clear();
        this.mTasks.addAll(tasks);
        updateFilteredTasks();
    }

    boolean remove(Task t) {
        synchronized (this.mFilteredTasks) {
            if (this.mFilteredTasks.contains(t)) {
                boolean removed = this.mTasks.remove(t);
                updateFilteredTasks();
                return removed;
            }
            return false;
        }
    }

    int indexOf(Task t) {
        if (t == null || !this.mFilteredTaskIndices.containsKey(t.key)) {
            return -1;
        }
        return ((Integer) this.mFilteredTaskIndices.get(t.key)).intValue();
    }

    int size() {
        int size;
        synchronized (this.mFilteredTasks) {
            size = this.mFilteredTasks.size();
        }
        return size;
    }

    boolean contains(Task t) {
        return this.mFilteredTaskIndices.containsKey(t.key);
    }

    private void updateFilteredTasks() {
        ArrayList<Task> mFilteredTasksTemp = new ArrayList();
        if (this.mFilter != null) {
            SparseArray<Task> taskIdMap = new SparseArray();
            int taskCount = this.mTasks.size();
            int i = 0;
            for (int i2 = 0; i2 < taskCount; i2++) {
                Task t = (Task) this.mTasks.get(i2);
                taskIdMap.put(t.key.id, t);
            }
            while (i < taskCount) {
                Task t2 = (Task) this.mTasks.get(i);
                if (this.mFilter.acceptTask(taskIdMap, t2, i)) {
                    mFilteredTasksTemp.add(t2);
                }
                i++;
            }
        } else {
            mFilteredTasksTemp.addAll(this.mTasks);
        }
        synchronized (this.mFilteredTasks) {
            this.mFilteredTasks.clear();
            this.mFilteredTasks.addAll(mFilteredTasksTemp);
        }
        updateFilteredTaskIndices();
    }

    private void updateFilteredTaskIndices() {
        ArrayList<Task> mFilteredTasksTemp = new ArrayList();
        synchronized (this.mFilteredTasks) {
            mFilteredTasksTemp.addAll(this.mFilteredTasks);
        }
        int taskCount = mFilteredTasksTemp.size();
        this.mFilteredTaskIndices.clear();
        for (int i = 0; i < taskCount; i++) {
            this.mFilteredTaskIndices.put(((Task) mFilteredTasksTemp.get(i)).key, Integer.valueOf(i));
        }
    }

    ArrayList<Task> getTasks() {
        ArrayList<Task> arrayList;
        synchronized (this.mFilteredTasks) {
            arrayList = this.mFilteredTasks;
        }
        return arrayList;
    }
}
