package com.android.server.wifi;

import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.p2p.HwWifiP2pService;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class WifiRepeaterConfigStore extends StateMachine {
    private static final String REPEATER_CONFIG_FILE;
    private static final int REPEATER_CONFIG_FILE_VERSION = 2;
    private static final String TAG = "WifiRepeaterConfigStore";
    private State mActiveState = new ActiveState();
    private State mDefaultState = new DefaultState();
    private State mInactiveState = new InactiveState();
    private AsyncChannel mReplyChannel = new AsyncChannel();
    private WifiConfiguration mWifiRepeaterConfig = null;

    class ActiveState extends State {
        ActiveState() {
        }

        public void enter() {
            new Thread(new Runnable() {
                public void run() {
                    WifiRepeaterConfigStore.this.writeApConfiguration(WifiRepeaterConfigStore.this.mWifiRepeaterConfig);
                    WifiRepeaterConfigStore.this.sendMessage(HwWifiP2pService.CMD_SET_REPEATER_CONFIG_COMPLETED);
                }
            }).start();
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case HwWifiP2pService.CMD_SET_REPEATER_CONFIG /*143461*/:
                    WifiRepeaterConfigStore.this.deferMessage(message);
                    break;
                case HwWifiP2pService.CMD_SET_REPEATER_CONFIG_COMPLETED /*143462*/:
                    WifiRepeaterConfigStore.this.transitionTo(WifiRepeaterConfigStore.this.mInactiveState);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            String str;
            StringBuilder stringBuilder;
            switch (message.what) {
                case HwWifiP2pService.CMD_SET_REPEATER_CONFIG /*143461*/:
                case HwWifiP2pService.CMD_SET_REPEATER_CONFIG_COMPLETED /*143462*/:
                    str = WifiRepeaterConfigStore.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected message: ");
                    stringBuilder.append(message);
                    Log.e(str, stringBuilder.toString());
                    break;
                case HwWifiP2pService.CMD_REQUEST_REPEATER_CONFIG /*143463*/:
                    WifiRepeaterConfigStore.this.mReplyChannel.replyToMessage(message, HwWifiP2pService.CMD_RESPONSE_REPEATER_CONFIG, WifiRepeaterConfigStore.this.mWifiRepeaterConfig);
                    break;
                default:
                    str = WifiRepeaterConfigStore.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to handle ");
                    stringBuilder.append(message);
                    Log.e(str, stringBuilder.toString());
                    break;
            }
            return true;
        }
    }

    class InactiveState extends State {
        InactiveState() {
        }

        public boolean processMessage(Message message) {
            if (message.what != HwWifiP2pService.CMD_SET_REPEATER_CONFIG) {
                return false;
            }
            WifiConfiguration config = message.obj;
            if (config.SSID != null) {
                WifiRepeaterConfigStore.this.mWifiRepeaterConfig = config;
                WifiRepeaterConfigStore.this.transitionTo(WifiRepeaterConfigStore.this.mActiveState);
            } else {
                String str = WifiRepeaterConfigStore.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Try to setup AP config without SSID: ");
                stringBuilder.append(message);
                Log.e(str, stringBuilder.toString());
            }
            return true;
        }
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataDirectory());
        stringBuilder.append("/misc/wifi/wifirepeater.conf");
        REPEATER_CONFIG_FILE = stringBuilder.toString();
    }

    WifiRepeaterConfigStore(Handler target) {
        super(TAG, target.getLooper());
        addState(this.mDefaultState);
        addState(this.mInactiveState, this.mDefaultState);
        addState(this.mActiveState, this.mDefaultState);
        setInitialState(this.mInactiveState);
    }

    public static WifiRepeaterConfigStore makeWifiRepeaterConfigStore(Handler target) {
        WifiRepeaterConfigStore s = new WifiRepeaterConfigStore(target);
        s.start();
        return s;
    }

    /* JADX WARNING: Missing block: B:25:0x0067, code skipped:
            if (r0 == null) goto L_0x006a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void loadRepeaterConfiguration() {
        DataInputStream in = null;
        try {
            WifiConfiguration config = new WifiConfiguration();
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(REPEATER_CONFIG_FILE)));
            int version = in.readInt();
            if (version == 1 || version == 2) {
                config.SSID = in.readUTF();
                if (version >= 2) {
                    config.apBand = in.readInt();
                    config.apChannel = in.readInt();
                }
                int authType = in.readInt();
                config.allowedKeyManagement.set(authType);
                if (authType != 0) {
                    config.preSharedKey = in.readUTF();
                }
                this.mWifiRepeaterConfig = config;
                try {
                    in.close();
                } catch (IOException e) {
                }
                return;
            }
            Log.e(TAG, "Bad version on repeater configuration file, set defaults");
            setDefaultApConfiguration();
            try {
                in.close();
            } catch (IOException e2) {
            }
        } catch (IOException e3) {
            setDefaultApConfiguration();
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    public Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    private void writeApConfiguration(WifiConfiguration config) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(REPEATER_CONFIG_FILE)));
            out.writeInt(2);
            out.writeUTF(config.SSID);
            out.writeInt(config.apBand);
            out.writeInt(config.apChannel);
            int authType = config.getAuthType();
            out.writeInt(authType);
            if (!(authType == 0 || config.preSharedKey == null)) {
                out.writeUTF(config.preSharedKey);
            }
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error writing hotspot configuration");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            if (out == null) {
                return;
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e2) {
                }
            }
        }
        try {
            out.close();
        } catch (IOException e3) {
        }
    }

    private void setDefaultApConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedKeyManagement.set(4);
        String randomUUID = UUID.randomUUID().toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(randomUUID.substring(0, 8));
        stringBuilder.append(randomUUID.substring(9, 13));
        config.preSharedKey = stringBuilder.toString();
        config.SSID = HwWifiServiceFactory.getHwWifiServiceManager().getCustWifiApDefaultName(config);
        sendMessage(HwWifiP2pService.CMD_SET_REPEATER_CONFIG, config);
    }
}
