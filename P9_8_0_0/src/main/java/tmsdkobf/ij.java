package tmsdkobf;

import tmsdk.common.creator.ManagerCreatorC;

public class ij {
    private static volatile boolean ry = false;

    public static void reportChannelInfo() {
        if (!ry) {
            ry = true;
            final Object -l_0_R = new md("tms");
            if (!-l_0_R.getBoolean("reportlc", false)) {
                im.bJ().addTask(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(10000);
                        } catch (Object -l_1_R) {
                            -l_1_R.printStackTrace();
                        }
                        if (((ot) ManagerCreatorC.getManager(ot.class)).hw() == 0) {
                            -l_0_R.a("reportlc", true, true);
                        }
                        ij.ry = false;
                    }
                }, "nmct");
            }
        }
    }
}
