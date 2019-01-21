package android.net.wifi.p2p;

import android.net.wifi.WifiEnterpriseConfig;

public class WifiP2pProvDiscEvent {
    public static final int ENTER_PIN = 3;
    public static final int PBC_REQ = 1;
    public static final int PBC_RSP = 2;
    public static final int SHOW_PIN = 4;
    private static final String TAG = "WifiP2pProvDiscEvent";
    public WifiP2pDevice device;
    public int event;
    public String pin;

    public WifiP2pProvDiscEvent() {
        this.device = new WifiP2pDevice();
    }

    public WifiP2pProvDiscEvent(String string) throws IllegalArgumentException {
        String[] tokens = string.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        StringBuilder stringBuilder;
        if (tokens.length >= 2) {
            if (tokens[0].endsWith("PBC-REQ")) {
                this.event = 1;
            } else if (tokens[0].endsWith("PBC-RESP")) {
                this.event = 2;
            } else if (tokens[0].endsWith("ENTER-PIN")) {
                this.event = 3;
            } else if (tokens[0].endsWith("SHOW-PIN")) {
                this.event = 4;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Malformed event ");
                stringBuilder.append(string);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.device = new WifiP2pDevice();
            this.device.deviceAddress = tokens[1];
            if (this.event == 4) {
                this.pin = tokens[2];
                return;
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Malformed event ");
        stringBuilder.append(string);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(this.device);
        sbuf.append("\n event: ");
        sbuf.append(this.event);
        sbuf.append("\n pin: ");
        sbuf.append(this.pin);
        return sbuf.toString();
    }
}
