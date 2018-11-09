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
    private static final String REPEATER_CONFIG_FILE = (Environment.getDataDirectory() + "/misc/wifi/wifirepeater.conf");
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
            switch (message.what) {
                case HwWifiP2pService.CMD_SET_REPEATER_CONFIG /*143461*/:
                case HwWifiP2pService.CMD_SET_REPEATER_CONFIG_COMPLETED /*143462*/:
                    Log.e(WifiRepeaterConfigStore.TAG, "Unexpected message: " + message);
                    break;
                case HwWifiP2pService.CMD_REQUEST_REPEATER_CONFIG /*143463*/:
                    WifiRepeaterConfigStore.this.mReplyChannel.replyToMessage(message, HwWifiP2pService.CMD_RESPONSE_REPEATER_CONFIG, WifiRepeaterConfigStore.this.mWifiRepeaterConfig);
                    break;
                default:
                    Log.e(WifiRepeaterConfigStore.TAG, "Failed to handle " + message);
                    break;
            }
            return true;
        }
    }

    class InactiveState extends State {
        InactiveState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case HwWifiP2pService.CMD_SET_REPEATER_CONFIG /*143461*/:
                    WifiConfiguration config = message.obj;
                    if (config.SSID != null) {
                        WifiRepeaterConfigStore.this.mWifiRepeaterConfig = config;
                        WifiRepeaterConfigStore.this.transitionTo(WifiRepeaterConfigStore.this.mActiveState);
                    } else {
                        Log.e(WifiRepeaterConfigStore.TAG, "Try to setup AP config without SSID: " + message);
                    }
                    return true;
                default:
                    return false;
            }
        }
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

    public void loadRepeaterConfiguration() {
        Throwable th;
        DataInputStream dataInputStream = null;
        try {
            WifiConfiguration config = new WifiConfiguration();
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(REPEATER_CONFIG_FILE)));
            try {
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
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                        }
                    }
                    dataInputStream = in;
                }
                Log.e(TAG, "Bad version on repeater configuration file, set defaults");
                setDefaultApConfiguration();
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException e3) {
                dataInputStream = in;
                try {
                    setDefaultApConfiguration();
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        } catch (IOException e4) {
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                dataInputStream = in;
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                throw th;
            }
        } catch (IOException e6) {
            setDefaultApConfiguration();
            if (dataInputStream != null) {
                dataInputStream.close();
            }
        }
    }

    public Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    private void writeApConfiguration(WifiConfiguration config) {
        IOException e;
        Throwable th;
        DataOutputStream dataOutputStream = null;
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(REPEATER_CONFIG_FILE)));
            try {
                out.writeInt(2);
                out.writeUTF(config.SSID);
                out.writeInt(config.apBand);
                out.writeInt(config.apChannel);
                int authType = config.getAuthType();
                out.writeInt(authType);
                if (!(authType == 0 || config.preSharedKey == null)) {
                    out.writeUTF(config.preSharedKey);
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e2) {
                    }
                }
                dataOutputStream = out;
            } catch (IOException e3) {
                e = e3;
                dataOutputStream = out;
                try {
                    Log.e(TAG, "Error writing hotspot configuration" + e);
                    if (dataOutputStream != null) {
                        try {
                            dataOutputStream.close();
                        } catch (IOException e4) {
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (dataOutputStream != null) {
                        try {
                            dataOutputStream.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                dataOutputStream = out;
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                throw th;
            }
        } catch (IOException e6) {
            e = e6;
            Log.e(TAG, "Error writing hotspot configuration" + e);
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
        }
    }

    private void setDefaultApConfiguration() {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedKeyManagement.set(4);
        String randomUUID = UUID.randomUUID().toString();
        config.preSharedKey = randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
        config.SSID = HwWifiServiceFactory.getHwWifiServiceManager().getCustWifiApDefaultName(config);
        sendMessage(HwWifiP2pService.CMD_SET_REPEATER_CONFIG, config);
    }
}
