package huawei.android.security.facerecognition.base;

import huawei.android.security.facerecognition.utils.LogUtil;
import java.util.ArrayList;

public class HwSecurityTaskThread extends Thread {
    public static final int PRIORITY_HIGH = 0;
    public static final int PRIORITY_LOW = 2;
    public static final int PRIORITY_NORMAL = 1;
    private static final String TAG = HwSecurityTaskThread.class.getSimpleName();
    private static HwSecurityTaskThread gInstance = null;
    private static Object mInstanceLock = new Object();
    private ArrayList<HwSecurityTaskBase> mHighPriorityTasks = new ArrayList();
    private ArrayList<HwSecurityTaskBase> mLowPriorityTasks = new ArrayList();
    private ArrayList<HwSecurityTaskBase> mNormalPriorityTasks = new ArrayList();
    private boolean mNotified = false;
    private Object mSignal = new Object();
    private boolean mStop = false;
    private Object mTaskLock = new Object();

    private HwSecurityTaskThread() {
    }

    public void startThread() {
        synchronized (this.mSignal) {
            this.mStop = false;
            this.mNotified = false;
            start();
        }
    }

    public void stopThread() {
        synchronized (this.mSignal) {
            this.mStop = true;
            this.mNotified = true;
            this.mSignal.notifyAll();
        }
        try {
            join(0);
        } catch (InterruptedException e) {
        }
    }

    public void notifyThread() {
        synchronized (this.mSignal) {
            this.mNotified = true;
            this.mSignal.notifyAll();
        }
    }

    public void waitThread() {
        try {
            synchronized (this.mSignal) {
                while (!this.mNotified) {
                    this.mSignal.wait();
                }
                this.mNotified = false;
            }
        } catch (InterruptedException e) {
            LogUtil.e(TAG, "security taskthread wait failed.");
        }
    }

    public boolean checkQuit() {
        boolean z;
        synchronized (this.mSignal) {
            z = this.mStop;
        }
        return z;
    }

    public void run() {
        while (!checkQuit()) {
            HwSecurityTaskBase task = getNextTask();
            if (task != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("run: ");
                stringBuilder.append(task.getClass().getSimpleName());
                stringBuilder.append(", status: ");
                stringBuilder.append(task.getTaskStatus());
                LogUtil.d(str, stringBuilder.toString());
                if (task.getTaskStatus() == 0) {
                    task.execute();
                }
            } else {
                waitThread();
            }
        }
    }

    public boolean pushTask(HwSecurityTaskBase task, int priority) {
        boolean pushSucceed;
        synchronized (this.mTaskLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pushTask: ");
            stringBuilder.append(task.getClass().getSimpleName());
            LogUtil.d(str, stringBuilder.toString());
            if (priority == 0) {
                pushSucceed = this.mHighPriorityTasks.add(task);
            } else if (priority != 2) {
                pushSucceed = this.mNormalPriorityTasks.add(task);
            } else {
                pushSucceed = this.mLowPriorityTasks.add(task);
            }
            if (pushSucceed) {
                task.onStart();
                notifyThread();
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pushTask: ");
                stringBuilder2.append(task.getClass().getSimpleName());
                stringBuilder2.append(" failed, priority: ");
                stringBuilder2.append(priority);
                LogUtil.w(str2, stringBuilder2.toString());
            }
        }
        return pushSucceed;
    }

    protected HwSecurityTaskBase getNextTask() {
        synchronized (this.mTaskLock) {
            HwSecurityTaskBase hwSecurityTaskBase;
            if (this.mHighPriorityTasks != null && !this.mHighPriorityTasks.isEmpty()) {
                hwSecurityTaskBase = (HwSecurityTaskBase) this.mHighPriorityTasks.remove(0);
                return hwSecurityTaskBase;
            } else if (this.mNormalPriorityTasks != null && !this.mNormalPriorityTasks.isEmpty()) {
                hwSecurityTaskBase = (HwSecurityTaskBase) this.mNormalPriorityTasks.remove(0);
                return hwSecurityTaskBase;
            } else if (this.mLowPriorityTasks == null || this.mLowPriorityTasks.isEmpty()) {
                return null;
            } else {
                hwSecurityTaskBase = (HwSecurityTaskBase) this.mLowPriorityTasks.remove(0);
                return hwSecurityTaskBase;
            }
        }
    }

    public static void staticPushTask(HwSecurityTaskBase task, int priority) {
        HwSecurityTaskThread gThread = getInstance();
        if (gThread != null) {
            gThread.pushTask(task, priority);
        }
    }

    public static void createInstance() {
        synchronized (mInstanceLock) {
            if (gInstance == null) {
                gInstance = new HwSecurityTaskThread();
            }
        }
    }

    public static HwSecurityTaskThread getInstance() {
        HwSecurityTaskThread hwSecurityTaskThread;
        synchronized (mInstanceLock) {
            hwSecurityTaskThread = gInstance;
        }
        return hwSecurityTaskThread;
    }

    public static void destroyInstance() {
        synchronized (mInstanceLock) {
            if (gInstance != null) {
                gInstance = null;
            }
        }
    }
}
