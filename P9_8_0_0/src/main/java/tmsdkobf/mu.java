package tmsdkobf;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import java.util.ArrayList;
import tmsdk.common.module.optimus.impl.bean.BsInput;
import tmsdk.common.module.optimus.impl.bean.BsNeighborCell;

public class mu {
    private PhoneStateListener Bm;
    private a Bn;
    private CellLocation Bo;
    private int Bp = -133;
    private int Bq = -1;
    private int Br = -1;
    private TelephonyManager mTelephonyManager;

    public interface a {
        void a(BsInput bsInput);
    }

    public mu(mt mtVar) {
    }

    public void a(a aVar) {
        this.Bn = aVar;
    }

    void fh() {
        if (this.Bn != null) {
            this.Bn.a(fi());
        }
    }

    public BsInput fi() {
        Object -l_2_R;
        Object -l_1_R = new BsInput();
        -l_1_R.timeInSeconds = (int) (System.currentTimeMillis() / 1000);
        -l_1_R.networkType = (short) ((short) this.Bq);
        -l_1_R.dataState = (short) ((short) this.Br);
        if (this.Bo == null) {
            try {
                -l_2_R = this.mTelephonyManager.getCellLocation();
                if (-l_2_R != null && (-l_2_R instanceof GsmCellLocation)) {
                    this.Bo = (GsmCellLocation) -l_2_R;
                }
            } catch (Object -l_2_R2) {
                -l_2_R2.printStackTrace();
            }
        }
        if (this.Bo != null) {
            if (this.Bo instanceof GsmCellLocation) {
                GsmCellLocation -l_2_R3 = (GsmCellLocation) this.Bo;
                -l_1_R.cid = -l_2_R3.getCid();
                -l_1_R.lac = -l_2_R3.getLac();
            } else if (this.Bo instanceof CdmaCellLocation) {
                CdmaCellLocation -l_2_R4 = (CdmaCellLocation) this.Bo;
                -l_1_R.cid = -l_2_R4.getBaseStationId();
                -l_1_R.lac = -l_2_R4.getNetworkId();
                -l_1_R.loc = (((long) -l_2_R4.getBaseStationLatitude()) << 32) | ((long) -l_2_R4.getBaseStationLongitude());
            }
        }
        -l_1_R.bsss = (short) ((short) this.Bp);
        try {
            -l_2_R2 = this.mTelephonyManager.getNetworkOperator();
            if (-l_2_R2 != null) {
                if (-l_2_R2.length() >= 4) {
                    -l_1_R.mcc = (short) ((short) Integer.parseInt(-l_2_R2.substring(0, 3)));
                    -l_1_R.mnc = (short) ((short) Integer.parseInt(-l_2_R2.substring(3)));
                }
            }
        } catch (Object -l_2_R22) {
            -l_2_R22.printStackTrace();
        }
        -l_2_R22 = new ArrayList();
        try {
            Object<NeighboringCellInfo> -l_3_R = this.mTelephonyManager.getNeighboringCellInfo();
            if (-l_3_R != null) {
                for (NeighboringCellInfo -l_5_R : -l_3_R) {
                    Object -l_6_R = new BsNeighborCell();
                    -l_6_R.cid = -l_5_R.getCid();
                    -l_6_R.lac = -l_5_R.getLac();
                    -l_6_R.bsss = (short) ((short) ((-l_5_R.getRssi() * 2) - 113));
                    -l_2_R22.add(-l_6_R);
                }
            }
        } catch (Object -l_4_R) {
            -l_4_R.printStackTrace();
        }
        -l_1_R.neighbors = -l_2_R22;
        return -l_1_R;
    }

    public void u(Context context) {
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.Bm = new PhoneStateListener(this) {
            final /* synthetic */ mu Bs;

            {
                this.Bs = r1;
            }

            private String aZ(int i) {
                Object -l_2_R = "null";
                switch (i) {
                    case 0:
                        -l_2_R = "DATA_DISCONNECTED";
                        break;
                    case 1:
                        -l_2_R = "DATA_CONNECTING";
                        break;
                    case 2:
                        -l_2_R = "DATA_CONNECTED";
                        break;
                    case 3:
                        -l_2_R = "DATA_SUSPENDED";
                        break;
                    default:
                        -l_2_R = "DATA_OTHER";
                        break;
                }
                return -l_2_R + "(" + i + ")";
            }

            private String ba(int i) {
                Object -l_2_R = "null";
                switch (i) {
                    case 0:
                        -l_2_R = "NETWORK_TYPE_UNKNOWN";
                        break;
                    case 1:
                        -l_2_R = "NETWORK_TYPE_GPRS";
                        break;
                    case 2:
                        -l_2_R = "NETWORK_TYPE_EDGE";
                        break;
                    case 3:
                        -l_2_R = "NETWORK_TYPE_UMTS";
                        break;
                    case 4:
                        -l_2_R = "NETWORK_TYPE_CDMA";
                        break;
                    case 5:
                        -l_2_R = "NETWORK_TYPE_EVDO_0";
                        break;
                    case 6:
                        -l_2_R = "NETWORK_TYPE_EVDO_A";
                        break;
                    case 7:
                        -l_2_R = "NETWORK_TYPE_1xRTT";
                        break;
                    case 8:
                        -l_2_R = "NETWORK_TYPE_HSDPA";
                        break;
                    case 9:
                        -l_2_R = "NETWORK_TYPE_HSUPA";
                        break;
                    case 10:
                        -l_2_R = "NETWORK_TYPE_HSPA";
                        break;
                    default:
                        -l_2_R = "NETWORK_TYPE_OTHER--" + i;
                        break;
                }
                return -l_2_R + "(" + i + ")";
            }

            public void onCellLocationChanged(CellLocation cellLocation) {
                super.onCellLocationChanged(cellLocation);
                if (cellLocation != null) {
                    this.Bs.Bo = cellLocation;
                    this.Bs.fh();
                }
            }

            public void onDataConnectionStateChanged(int i, int i2) {
                super.onDataConnectionStateChanged(i, i2);
                Object -l_3_R = aZ(i);
                Object -l_4_R = ba(i2);
                this.Bs.Bq = i2;
                this.Bs.Br = i;
                this.Bs.fh();
            }

            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                if (signalStrength != null) {
                    this.Bs.Bp = !signalStrength.isGsm() ? signalStrength.getCdmaDbm() : (signalStrength.getGsmSignalStrength() * 2) - 113;
                    if (this.Bs.Bp == 1) {
                        return;
                    }
                }
                this.Bs.fh();
            }
        };
        this.mTelephonyManager.listen(this.Bm, 336);
    }

    public void v(Context context) {
        this.mTelephonyManager.listen(this.Bm, 0);
    }
}
