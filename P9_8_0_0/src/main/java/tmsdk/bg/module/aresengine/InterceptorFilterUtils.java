package tmsdk.bg.module.aresengine;

import tmsdk.bg.creator.ManagerCreatorB;

public final class InterceptorFilterUtils {
    public static final int INTERCEPTOR_MODE_ACCEPTED_ONLY_WHITELIST = 2;
    public static final int INTERCEPTOR_MODE_REJECTED_ONLY_BLACKLIST = 1;
    public static final int INTERCEPTOR_MODE_STANDARD = 0;

    private InterceptorFilterUtils() {
    }

    private static void cP() {
        Object -l_2_R;
        Object -l_3_R;
        Object -l_4_R;
        Object -l_5_R;
        Object -l_6_R;
        AresEngineManager -l_0_R = (AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class);
        Object -l_1_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_INCOMING_SMS);
        if (-l_1_R != null) {
            -l_2_R = -l_1_R.dataFilter();
            -l_3_R = -l_2_R.getConfig();
            -l_4_R = -l_2_R.defalutFilterConfig();
            if (-l_3_R != null) {
                -l_4_R.set(1, -l_3_R.get(1));
            }
            -l_2_R.setConfig(-l_4_R);
        }
        -l_2_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_OUTGOING_SMS);
        if (-l_2_R != null) {
            -l_3_R = -l_2_R.dataFilter();
            -l_4_R = -l_3_R.getConfig();
            -l_5_R = -l_3_R.defalutFilterConfig();
            if (-l_4_R != null) {
                -l_5_R.set(1, -l_4_R.get(1));
            }
            -l_3_R.setConfig(-l_5_R);
        }
        -l_3_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_INCOMING_CALL);
        if (-l_3_R != null) {
            -l_4_R = -l_3_R.dataFilter();
            -l_5_R = -l_4_R.getConfig();
            -l_6_R = -l_4_R.defalutFilterConfig();
            if (-l_5_R != null) {
                -l_6_R.set(1, -l_5_R.get(1));
            }
            -l_4_R.setConfig(-l_6_R);
        }
        -l_4_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_SYSTEM_CALL);
        if (-l_4_R != null) {
            -l_5_R = -l_4_R.dataFilter();
            -l_6_R = -l_5_R.getConfig();
            Object -l_7_R = -l_5_R.defalutFilterConfig();
            if (-l_6_R != null) {
                -l_7_R.set(1, -l_6_R.get(1));
            }
            -l_5_R.setConfig(-l_7_R);
        }
    }

    private static void cQ() {
        Object -l_2_R;
        Object -l_3_R;
        Object -l_4_R;
        Object -l_5_R;
        Object -l_6_R;
        AresEngineManager -l_0_R = (AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class);
        Object -l_1_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_INCOMING_SMS);
        if (-l_1_R != null) {
            -l_2_R = -l_1_R.dataFilter();
            -l_3_R = -l_2_R.getConfig();
            -l_4_R = -l_2_R.defalutFilterConfig();
            if (-l_3_R != null) {
                -l_4_R.set(1, -l_3_R.get(1));
            }
            -l_4_R.set(2, 3);
            -l_4_R.set(4, 1);
            -l_4_R.set(8, 3);
            -l_4_R.set(16, 3);
            -l_4_R.set(32, 2);
            -l_4_R.set(64, 3);
            -l_4_R.set(128, 3);
            -l_2_R.setConfig(-l_4_R);
        }
        -l_2_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_OUTGOING_SMS);
        if (-l_2_R != null) {
            -l_3_R = -l_2_R.dataFilter();
            -l_4_R = -l_3_R.getConfig();
            -l_5_R = -l_3_R.defalutFilterConfig();
            if (-l_4_R != null) {
                -l_5_R.set(1, -l_4_R.get(1));
            }
            -l_3_R.setConfig(-l_5_R);
        }
        -l_3_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_INCOMING_CALL);
        if (-l_3_R != null) {
            -l_4_R = -l_3_R.dataFilter();
            -l_5_R = -l_4_R.getConfig();
            -l_6_R = -l_4_R.defalutFilterConfig();
            if (-l_5_R != null) {
                -l_6_R.set(1, -l_5_R.get(1));
            }
            -l_6_R.set(2, 3);
            -l_6_R.set(4, 1);
            -l_6_R.set(8, 3);
            -l_6_R.set(16, 3);
            -l_6_R.set(32, 3);
            -l_6_R.set(64, 0);
            -l_4_R.setConfig(-l_6_R);
        }
        -l_4_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_SYSTEM_CALL);
        if (-l_4_R != null) {
            -l_5_R = -l_4_R.dataFilter();
            -l_6_R = -l_5_R.getConfig();
            Object -l_7_R = -l_5_R.defalutFilterConfig();
            if (-l_6_R != null) {
                -l_7_R.set(1, -l_6_R.get(1));
            }
            -l_7_R.set(2, 3);
            -l_7_R.set(4, 1);
            -l_7_R.set(8, 3);
            -l_7_R.set(16, 3);
            -l_7_R.set(32, 0);
            -l_7_R.set(64, 3);
            -l_7_R.set(128, 3);
            -l_7_R.set(256, 2);
            -l_5_R.setConfig(-l_7_R);
        }
    }

    private static void cR() {
        Object -l_2_R;
        Object -l_3_R;
        Object -l_4_R;
        Object -l_5_R;
        Object -l_6_R;
        AresEngineManager -l_0_R = (AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class);
        Object -l_1_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_INCOMING_SMS);
        if (-l_1_R != null) {
            -l_2_R = -l_1_R.dataFilter();
            -l_3_R = -l_2_R.getConfig();
            -l_4_R = -l_2_R.defalutFilterConfig();
            if (-l_3_R != null) {
                -l_4_R.set(1, -l_3_R.get(1));
            }
            -l_4_R.set(2, 0);
            -l_4_R.set(4, 3);
            -l_4_R.set(8, 3);
            -l_4_R.set(16, 3);
            -l_4_R.set(32, 3);
            -l_4_R.set(64, 3);
            -l_4_R.set(128, 1);
            -l_2_R.setConfig(-l_4_R);
        }
        -l_2_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_OUTGOING_SMS);
        if (-l_2_R != null) {
            -l_3_R = -l_2_R.dataFilter();
            -l_4_R = -l_3_R.getConfig();
            -l_5_R = -l_3_R.defalutFilterConfig();
            if (-l_4_R != null) {
                -l_5_R.set(1, -l_4_R.get(1));
            }
            -l_3_R.setConfig(-l_5_R);
        }
        -l_3_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_INCOMING_CALL);
        if (-l_3_R != null) {
            -l_4_R = -l_3_R.dataFilter();
            -l_5_R = -l_4_R.getConfig();
            -l_6_R = -l_4_R.defalutFilterConfig();
            if (-l_5_R != null) {
                -l_6_R.set(1, -l_5_R.get(1));
            }
            -l_6_R.set(64, 0);
            -l_6_R.set(2, 0);
            -l_6_R.set(4, 3);
            -l_6_R.set(8, 3);
            -l_6_R.set(16, 3);
            -l_6_R.set(32, 1);
            -l_4_R.setConfig(-l_6_R);
        }
        -l_4_R = -l_0_R.findInterceptor(DataInterceptorBuilder.TYPE_SYSTEM_CALL);
        if (-l_4_R != null) {
            -l_5_R = -l_4_R.dataFilter();
            -l_6_R = -l_5_R.getConfig();
            Object -l_7_R = -l_5_R.defalutFilterConfig();
            if (-l_6_R != null) {
                -l_7_R.set(1, -l_6_R.get(1));
            }
            -l_7_R.set(2, 0);
            -l_7_R.set(4, 3);
            -l_7_R.set(8, 3);
            -l_7_R.set(16, 3);
            -l_7_R.set(32, 1);
            -l_7_R.set(64, 3);
            -l_7_R.set(128, 3);
            -l_7_R.set(256, 2);
            -l_5_R.setConfig(-l_7_R);
        }
    }

    public static void setInterceptorMode(int i) {
        switch (i) {
            case 0:
                cP();
                return;
            case 1:
                cQ();
                return;
            case 2:
                cR();
                return;
            default:
                return;
        }
    }
}
