package android.os;

import com.google.android.collect.Maps;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class SystemService {
    private static Object sPropertyLock = new Object();
    private static HashMap<String, State> sStates = Maps.newHashMap();

    public enum State {
        RUNNING("running"),
        STOPPING("stopping"),
        STOPPED("stopped"),
        RESTARTING("restarting");

        private State(String state) {
            SystemService.sStates.put(state, this);
        }
    }

    static {
        SystemProperties.addChangeCallback(new Runnable() {
            public void run() {
                synchronized (SystemService.sPropertyLock) {
                    SystemService.sPropertyLock.notifyAll();
                }
            }
        });
    }

    public static void start(String name) {
        SystemProperties.set("ctl.start", name);
    }

    public static void stop(String name) {
        SystemProperties.set("ctl.stop", name);
    }

    public static void restart(String name) {
        SystemProperties.set("ctl.restart", name);
    }

    public static State getState(String service) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("init.svc.");
        stringBuilder.append(service);
        State state = (State) sStates.get(SystemProperties.get(stringBuilder.toString()));
        if (state != null) {
            return state;
        }
        return State.STOPPED;
    }

    public static boolean isStopped(String service) {
        return State.STOPPED.equals(getState(service));
    }

    public static boolean isRunning(String service) {
        return State.RUNNING.equals(getState(service));
    }

    public static void waitForState(String service, State state, long timeoutMillis) throws TimeoutException {
        long endMillis = SystemClock.elapsedRealtime() + timeoutMillis;
        while (true) {
            synchronized (sPropertyLock) {
                State currentState = getState(service);
                if (state.equals(currentState)) {
                    return;
                } else if (SystemClock.elapsedRealtime() < endMillis) {
                    try {
                        sPropertyLock.wait(timeoutMillis);
                    } catch (InterruptedException e) {
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Service ");
                    stringBuilder.append(service);
                    stringBuilder.append(" currently ");
                    stringBuilder.append(currentState);
                    stringBuilder.append("; waited ");
                    stringBuilder.append(timeoutMillis);
                    stringBuilder.append("ms for ");
                    stringBuilder.append(state);
                    throw new TimeoutException(stringBuilder.toString());
                }
            }
        }
    }

    public static void waitForAnyStopped(String... services) {
        while (true) {
            synchronized (sPropertyLock) {
                for (String service : services) {
                    if (State.STOPPED.equals(getState(service))) {
                        return;
                    }
                }
                try {
                    sPropertyLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
