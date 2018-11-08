package tmsdk.bg.module.network;

import java.io.File;
import tmsdkobf.lu;

final class e extends d {

    private static final class a implements NetDataEntityFactory {
        private f vb;
        private String vc = g.dc();

        public a(f fVar) {
            this.vb = fVar;
        }

        public NetDataEntity getNetDataEntity() {
            Object -l_1_R = new NetDataEntity();
            Object -l_2_R = new long[]{0, 0, 0, 0};
            Object -l_3_R = lu.e(new File(this.vc));
            if (-l_3_R != null) {
                try {
                    for (String trim : -l_3_R) {
                        Object -l_6_R = trim.trim().split("[:\\s]+");
                        if (this.vb.bc(-l_6_R[0].trim().toLowerCase())) {
                            -l_2_R[0] = -l_2_R[0] + Long.parseLong(-l_6_R[1]);
                            -l_2_R[1] = -l_2_R[1] + Long.parseLong(-l_6_R[2]);
                            -l_2_R[2] = -l_2_R[2] + Long.parseLong(-l_6_R[9]);
                            -l_2_R[3] = -l_2_R[3] + Long.parseLong(-l_6_R[10]);
                        }
                    }
                } catch (Exception e) {
                }
            }
            -l_1_R.mReceiver = -l_2_R[0];
            -l_1_R.mReceiverPks = -l_2_R[1];
            -l_1_R.mTranslate = -l_2_R[2];
            -l_1_R.mTranslatePks = -l_2_R[3];
            return -l_1_R;
        }

        public void networkConnectivityChangeNotify() {
        }
    }

    public e(INetworkInfoDao iNetworkInfoDao, f fVar) {
        super(new a(fVar), iNetworkInfoDao);
    }
}
