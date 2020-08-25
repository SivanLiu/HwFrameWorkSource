package com.huawei.nb.utils.reporter.fault;

import android.util.SparseArray;
import com.huawei.android.util.IMonitorEx;

public abstract class Fault {
    protected static final short ACCESSOR = 8;
    protected static final short ACCESS_TYPE = 10;
    protected static final short APP_TYPE = 17;
    protected static final short APP_VERSION = 16;
    protected static final short BUSINESS = 11;
    protected static final short COMP = 6;
    protected static final short DETAIL = 15;
    protected static final short F1NAME = 2;
    protected static final short FINGERPRINT = 4;
    protected static final short INT_TIME_TYPE = 3;
    protected static final short LENGTH = 18;
    protected static final short NAME = 0;
    private static final int ODMF_FAULT_ID = 901002012;
    protected static final short PROCESS_ID = 1;
    private static final String PROCESS_NAME = "com.huawei.HwOPServer";
    protected static final short STATE = 13;
    protected static final short TARGET = 9;
    protected static final short TRIGGER = 14;
    protected static final short TYPE = 7;
    protected static final short URL = 12;
    protected static final short VERSION = 5;
    private static String appVersion = "";
    protected String keyMessage;
    protected SparseArray<String> parameters = new SparseArray<>();

    private SparseArray<String> getParameters() {
        return this.parameters;
    }

    public String getKeyMessage() {
        return this.keyMessage;
    }

    public static void setAppVersion(String version) {
        if (version != null) {
            appVersion = version;
        }
    }

    public IMonitorEx.EventStreamEx createEventStream() {
        IMonitorEx.EventStreamEx eventStreamEx = IMonitorEx.openEventStream((int) ODMF_FAULT_ID);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, (short) F1NAME, getFunctionInfo());
            eventStreamEx.setParam(eventStreamEx, (short) PROCESS_ID, PROCESS_NAME);
            eventStreamEx.setParam(eventStreamEx, (short) VERSION, appVersion);
            SparseArray<String> parametersArray = getParameters();
            for (short i = NAME; i < 16; i = (short) (i + PROCESS_ID)) {
                if (parametersArray.get(i) != null) {
                    eventStreamEx.setParam(eventStreamEx, i, parametersArray.get(i));
                }
            }
        }
        return eventStreamEx;
    }

    private String getFunctionInfo() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();
        if (sts == null) {
            return null;
        }
        for (StackTraceElement st : sts) {
            if (!st.isNativeMethod() && !st.getClassName().equals(Thread.class.getName()) && !st.getClassName().equals(Fault.class.getName()) && !st.getMethodName().equals("k") && !st.getMethodName().equals("a") && !st.getMethodName().equals("report") && !st.getMethodName().equals("f")) {
                return st.getMethodName();
            }
        }
        return "";
    }
}
