package com.huawei.nb.utils.reporter.fault;

import android.util.SparseArray;
import com.huawei.android.util.IMonitorEx;
import com.huawei.android.util.IMonitorEx.EventStreamEx;

public abstract class Fault {
    protected static final short ACCESSOR = (short) 8;
    protected static final short ACCESSTYPE = (short) 10;
    protected static final short APPTYPE = (short) 17;
    protected static final short APPVERSION = (short) 16;
    protected static final short BUSINESS = (short) 11;
    protected static final short COMP = (short) 6;
    protected static final short DETAIL = (short) 15;
    protected static final short F1NAME = (short) 2;
    protected static final short FINGERPRINT = (short) 4;
    protected static final short INTTIMETYPE = (short) 3;
    protected static final short LENGTH = (short) 18;
    protected static final short NAME = (short) 0;
    private static final int ODMF_FAULT_ID = 901002012;
    protected static final short PNAME = (short) 1;
    private static final String PROCESS_NAME = "com.huawei.HwOPServer";
    protected static final short STATE = (short) 13;
    protected static final short TARGET = (short) 9;
    protected static final short TRIGGER = (short) 14;
    protected static final short TYPE = (short) 7;
    protected static final short URL = (short) 12;
    protected static final short VERSION = (short) 5;
    protected String keyMessage;
    protected SparseArray<String> parameters = new SparseArray();

    private SparseArray<String> getParameters() {
        return this.parameters;
    }

    public String getKeyMessage() {
        return this.keyMessage;
    }

    public EventStreamEx createEventStream() {
        EventStreamEx eventStreamEx = IMonitorEx.openEventStream(ODMF_FAULT_ID);
        if (eventStreamEx != null) {
            eventStreamEx.setParam(eventStreamEx, F1NAME, getFunctionInfo());
            eventStreamEx.setParam(eventStreamEx, PNAME, PROCESS_NAME);
            eventStreamEx.setParam(eventStreamEx, VERSION, "");
            SparseArray<String> parametersArray = getParameters();
            for (short i = NAME; i < APPVERSION; i = (short) (i + 1)) {
                if (parametersArray.get(i) != null) {
                    eventStreamEx.setParam(eventStreamEx, i, (String) parametersArray.get(i));
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
