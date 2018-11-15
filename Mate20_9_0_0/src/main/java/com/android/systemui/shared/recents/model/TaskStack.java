package com.android.systemui.shared.recents.model;

import android.content.ComponentName;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.shared.recents.model.Task.TaskKey;
import com.android.systemui.shared.recents.utilities.AnimationProps;
import com.android.systemui.shared.system.PackageManagerWrapper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TaskStack {
    private static final String TAG = "TaskStack";
    private TaskStackCallbacks mCb;
    private final ArrayList<Task> mRawTaskList = new ArrayList();
    private final FilteredTaskList mStackTaskList = new FilteredTaskList();

    public interface TaskStackCallbacks {
        void onStackTaskAdded(TaskStack taskStack, Task task);

        void onStackTaskRemoved(TaskStack taskStack, Task task, Task task2, AnimationProps animationProps, boolean z, boolean z2);

        void onStackTasksRemoved(TaskStack taskStack);

        void onStackTasksUpdated(TaskStack taskStack);
    }

    public TaskStack() {
        this.mStackTaskList.setFilter(-$$Lambda$TaskStack$gkuBLLtJ6FV7PDAxT-_KECDzTOI.INSTANCE);
    }

    public void setCallbacks(TaskStackCallbacks cb) {
        this.mCb = cb;
    }

    public void removeTask(Task t, AnimationProps animation, boolean fromDockGesture) {
        removeTask(t, animation, fromDockGesture, true);
    }

    public void removeTask(Task t, AnimationProps animation, boolean fromDockGesture, boolean dismissRecentsIfAllRemoved) {
        if (this.mStackTaskList.contains(t)) {
            this.mStackTaskList.remove(t);
            Task newFrontMostTask = getFrontMostTask();
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, t, newFrontMostTask, animation, fromDockGesture, dismissRecentsIfAllRemoved);
            }
        }
        this.mRawTaskList.remove(t);
    }

    public void removeAllTasks(boolean notifyStackChanges) {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task t = (Task) tasks.get(i);
            this.mStackTaskList.remove(t);
            this.mRawTaskList.remove(t);
        }
        if (this.mCb != null && notifyStackChanges) {
            this.mCb.onStackTasksRemoved(this);
        }
    }

    public void setTasks(TaskStack stack, boolean notifyStackChanges) {
        setTasks(stack.mRawTaskList, notifyStackChanges);
    }

    public void setTasks(List<Task> tasks, boolean notifyStackChanges) {
        int i;
        Task task;
        int i2;
        ArrayMap<TaskKey, Task> currentTasksMap = createTaskKeyMapFromList(this.mRawTaskList);
        ArrayMap<TaskKey, Task> newTasksMap = createTaskKeyMapFromList(tasks);
        ArrayList<Task> addedTasks = new ArrayList();
        ArrayList<Task> removedTasks = new ArrayList();
        ArrayList<Task> allTasks = new ArrayList();
        boolean notifyStackChanges2 = this.mCb == null ? false : notifyStackChanges;
        for (i = this.mRawTaskList.size() - 1; i >= 0; i--) {
            task = (Task) this.mRawTaskList.get(i);
            if (task == null) {
                Log.e(TAG, "setTasks,task is null here");
            } else if (!newTasksMap.containsKey(task.key) && notifyStackChanges2) {
                removedTasks.add(task);
            }
        }
        int taskCount = tasks.size();
        int i3 = 0;
        for (i2 = 0; i2 < taskCount; i2++) {
            Task newTask = (Task) tasks.get(i2);
            task = (Task) currentTasksMap.get(newTask.key);
            if (task == null && notifyStackChanges2) {
                addedTasks.add(newTask);
            } else if (task != null) {
                task.copyFrom(newTask);
                newTask = task;
            }
            allTasks.add(newTask);
        }
        List<Task> list = tasks;
        for (i2 = allTasks.size() - 1; i2 >= 0; i2--) {
            ((Task) allTasks.get(i2)).temporarySortIndexInStack = i2;
        }
        this.mStackTaskList.set(allTasks);
        this.mRawTaskList.clear();
        this.mRawTaskList.addAll(allTasks);
        int removedTaskCount = removedTasks.size();
        Task newFrontMostTask = getFrontMostTask();
        i2 = 0;
        while (true) {
            int i4 = i2;
            if (i4 >= removedTaskCount) {
                break;
            }
            int i5 = i4;
            int removedTaskCount2 = removedTaskCount;
            this.mCb.onStackTaskRemoved(this, (Task) removedTasks.get(i4), newFrontMostTask, AnimationProps.IMMEDIATE, false, true);
            i2 = i5 + 1;
            list = tasks;
            removedTaskCount = removedTaskCount2;
        }
        i2 = addedTasks.size();
        while (true) {
            i = i3;
            if (i >= i2) {
                break;
            }
            this.mCb.onStackTaskAdded(this, (Task) addedTasks.get(i));
            i3 = i + 1;
        }
        if (notifyStackChanges2) {
            this.mCb.onStackTasksUpdated(this);
        }
    }

    public Task getFrontMostTask() {
        ArrayList<Task> stackTasks = this.mStackTaskList.getTasks();
        if (stackTasks.isEmpty()) {
            return null;
        }
        return (Task) stackTasks.get(stackTasks.size() - 1);
    }

    public ArrayList<TaskKey> getTaskKeys() {
        ArrayList<TaskKey> taskKeys = new ArrayList();
        ArrayList<Task> tasks = computeAllTasksList();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            taskKeys.add(((Task) tasks.get(i)).key);
        }
        return taskKeys;
    }

    public ArrayList<Task> getTasks() {
        return this.mStackTaskList.getTasks();
    }

    public ArrayList<Task> computeAllTasksList() {
        ArrayList<Task> tasks = new ArrayList();
        tasks.addAll(this.mStackTaskList.getTasks());
        return tasks;
    }

    public int getTaskCount() {
        return this.mStackTaskList.size();
    }

    public Task getLaunchTarget() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = (Task) tasks.get(i);
            if (task != null && task.isLaunchTarget) {
                return task;
            }
        }
        return null;
    }

    public boolean isNextLaunchTargetPip(long lastPipTime) {
        Task launchTarget = getLaunchTarget();
        Task nextLaunchTarget = getNextLaunchTargetRaw();
        boolean z = false;
        if (nextLaunchTarget == null || lastPipTime <= 0) {
            return launchTarget != null && lastPipTime > 0 && getTaskCount() == 1;
        } else {
            if (lastPipTime > nextLaunchTarget.key.lastActiveTime) {
                z = true;
            }
            return z;
        }
    }

    public Task getNextLaunchTarget() {
        Task nextLaunchTarget = getNextLaunchTargetRaw();
        if (nextLaunchTarget != null) {
            return nextLaunchTarget;
        }
        return (Task) getTasks().get(getTaskCount() - 1);
    }

    private Task getNextLaunchTargetRaw() {
        if (getTaskCount() == 0) {
            return null;
        }
        int launchTaskIndex = indexOfTask(getLaunchTarget());
        if (launchTaskIndex == -1 || launchTaskIndex <= 0) {
            return null;
        }
        return (Task) getTasks().get(launchTaskIndex - 1);
    }

    public int indexOfTask(Task t) {
        return this.mStackTaskList.indexOf(t);
    }

    public Task findTaskWithId(int taskId) {
        ArrayList<Task> tasks = computeAllTasksList();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = (Task) tasks.get(i);
            if (task != null && task.key != null && task.key.id == taskId) {
                return task;
            }
        }
        return null;
    }

    public ArraySet<ComponentName> computeComponentsRemoved(String packageName, int userId) {
        ArraySet<ComponentName> existingComponents = new ArraySet();
        ArraySet<ComponentName> removedComponents = new ArraySet();
        ArrayList<TaskKey> taskKeys = getTaskKeys();
        int taskKeyCount = taskKeys.size();
        for (int i = 0; i < taskKeyCount; i++) {
            TaskKey t = (TaskKey) taskKeys.get(i);
            if (t.userId == userId) {
                ComponentName cn = t.getComponent();
                if (cn.getPackageName().equals(packageName) && !existingComponents.contains(cn)) {
                    if (PackageManagerWrapper.getInstance().getActivityInfo(cn, userId) != null) {
                        existingComponents.add(cn);
                    } else {
                        removedComponents.add(cn);
                    }
                }
            }
        }
        return removedComponents;
    }

    public String toString() {
        String str = new StringBuilder();
        str.append("Stack Tasks (");
        str.append(this.mStackTaskList.size());
        str.append("):\n");
        str = str.toString();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append("    ");
            stringBuilder.append(((Task) tasks.get(i)).toString());
            stringBuilder.append("\n");
            str = stringBuilder.toString();
        }
        return str;
    }

    private ArrayMap<TaskKey, Task> createTaskKeyMapFromList(List<Task> tasks) {
        ArrayMap<TaskKey, Task> map = new ArrayMap(tasks.size());
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = (Task) tasks.get(i);
            map.put(task.key, task);
        }
        return map;
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = new StringBuilder();
        innerPrefix.append(prefix);
        innerPrefix.append("  ");
        innerPrefix = innerPrefix.toString();
        writer.print(prefix);
        writer.print(TAG);
        writer.print(" numStackTasks=");
        writer.print(this.mStackTaskList.size());
        writer.println();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            ((Task) tasks.get(i)).dump(innerPrefix, writer);
        }
    }
}
