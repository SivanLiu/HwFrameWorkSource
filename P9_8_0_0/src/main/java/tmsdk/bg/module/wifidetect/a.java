package tmsdk.bg.module.wifidetect;

import android.net.LocalServerSocket;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdk.common.utils.ScriptHelper;
import tmsdk.common.utils.f;

class a {
    static final a wP = new a();
    LocalServerSocket wN;
    boolean wO = false;

    private a() {
    }

    public static a dw() {
        return wP;
    }

    public void bp(String str) {
        f.h("WifiDetectManager", "[Beg]checkArp-binaryPath:[" + str + "]");
        if (new File(str).exists()) {
            List -l_3_R = new ArrayList();
            -l_3_R.add("chmod 0755 " + str + "\n");
            -l_3_R.add(str + " " + 10);
            f.h("WifiDetectManager", "ScriptHelper.runScript-cmds:[" + -l_3_R + "]");
            String str2 = "WifiDetectManager";
            f.h(str2, "[End]checkArp-runScript-ret:[" + ScriptHelper.runScript(-1, -l_3_R) + "]");
            return;
        }
        f.h("WifiDetectManager", "binaryFile not exist");
    }

    public int dx() {
        f.h("WifiDetectManager", "startServerAutoStop");
        int -l_1_I = 261;
        this.wN = new LocalServerSocket("tms_socket_server_path");
        this.wO = false;
        while (!this.wO) {
            Object -l_3_R;
            f.h("WifiDetectManager", "[Beg]Server.accept");
            Object -l_2_R = this.wN.accept();
            f.h("WifiDetectManager", "[End]Server.accept:[" + -l_2_R + "]");
            if (-l_2_R != null) {
                if (!this.wO) {
                    -l_3_R = -l_2_R.getInputStream();
                    Object -l_4_R = new StringBuilder();
                    Object -l_5_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
                    while (true) {
                        int -l_6_I = -l_3_R.read(-l_5_R);
                        if (-l_6_I == -1) {
                            break;
                        }
                        -l_4_R.append(new String(-l_5_R, 0, -l_6_I));
                    }
                    Object -l_7_R = -l_4_R.toString();
                    f.h("WifiDetectManager", "received from binary:[" + -l_7_R + "]");
                    if ("found danger".equals(-l_7_R)) {
                        -l_1_I = 262;
                    }
                    this.wO = true;
                }
            }
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_3_R2) {
                    try {
                        f.h("WifiDetectManager", "close local socket exception: " + -l_3_R2.getMessage());
                    } catch (Object -l_2_R2) {
                        f.e("WifiDetectManager", "startServer:[" + -l_2_R2 + "]");
                        return 263;
                    }
                }
            }
        }
        f.h("WifiDetectManager", "server has been stop, close the server");
        this.wN.close();
        this.wN = null;
        return -l_1_I;
    }
}
