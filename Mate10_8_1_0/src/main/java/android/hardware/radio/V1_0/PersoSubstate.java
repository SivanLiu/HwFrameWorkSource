package android.hardware.radio.V1_0;

public final class PersoSubstate {
    public static final int IN_PROGRESS = 1;
    public static final int READY = 2;
    public static final int RUIM_CORPORATE = 16;
    public static final int RUIM_CORPORATE_PUK = 22;
    public static final int RUIM_HRPD = 15;
    public static final int RUIM_HRPD_PUK = 21;
    public static final int RUIM_NETWORK1 = 13;
    public static final int RUIM_NETWORK1_PUK = 19;
    public static final int RUIM_NETWORK2 = 14;
    public static final int RUIM_NETWORK2_PUK = 20;
    public static final int RUIM_RUIM = 18;
    public static final int RUIM_RUIM_PUK = 24;
    public static final int RUIM_SERVICE_PROVIDER = 17;
    public static final int RUIM_SERVICE_PROVIDER_PUK = 23;
    public static final int SIM_CORPORATE = 5;
    public static final int SIM_CORPORATE_PUK = 10;
    public static final int SIM_NETWORK = 3;
    public static final int SIM_NETWORK_PUK = 8;
    public static final int SIM_NETWORK_SUBSET = 4;
    public static final int SIM_NETWORK_SUBSET_PUK = 9;
    public static final int SIM_SERVICE_PROVIDER = 6;
    public static final int SIM_SERVICE_PROVIDER_PUK = 11;
    public static final int SIM_SIM = 7;
    public static final int SIM_SIM_PUK = 12;
    public static final int UNKNOWN = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.PersoSubstate.dumpBitfield(int):java.lang.String
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.PersoSubstate.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "UNKNOWN";
        }
        if (o == 1) {
            return "IN_PROGRESS";
        }
        if (o == 2) {
            return "READY";
        }
        if (o == 3) {
            return "SIM_NETWORK";
        }
        if (o == 4) {
            return "SIM_NETWORK_SUBSET";
        }
        if (o == 5) {
            return "SIM_CORPORATE";
        }
        if (o == 6) {
            return "SIM_SERVICE_PROVIDER";
        }
        if (o == 7) {
            return "SIM_SIM";
        }
        if (o == 8) {
            return "SIM_NETWORK_PUK";
        }
        if (o == 9) {
            return "SIM_NETWORK_SUBSET_PUK";
        }
        if (o == 10) {
            return "SIM_CORPORATE_PUK";
        }
        if (o == 11) {
            return "SIM_SERVICE_PROVIDER_PUK";
        }
        if (o == 12) {
            return "SIM_SIM_PUK";
        }
        if (o == 13) {
            return "RUIM_NETWORK1";
        }
        if (o == 14) {
            return "RUIM_NETWORK2";
        }
        if (o == 15) {
            return "RUIM_HRPD";
        }
        if (o == 16) {
            return "RUIM_CORPORATE";
        }
        if (o == 17) {
            return "RUIM_SERVICE_PROVIDER";
        }
        if (o == 18) {
            return "RUIM_RUIM";
        }
        if (o == 19) {
            return "RUIM_NETWORK1_PUK";
        }
        if (o == 20) {
            return "RUIM_NETWORK2_PUK";
        }
        if (o == 21) {
            return "RUIM_HRPD_PUK";
        }
        if (o == 22) {
            return "RUIM_CORPORATE_PUK";
        }
        if (o == 23) {
            return "RUIM_SERVICE_PROVIDER_PUK";
        }
        if (o == 24) {
            return "RUIM_RUIM_PUK";
        }
        return "0x" + Integer.toHexString(o);
    }
}
