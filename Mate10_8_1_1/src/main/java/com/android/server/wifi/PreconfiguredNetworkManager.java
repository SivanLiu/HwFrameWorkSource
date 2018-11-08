package com.android.server.wifi;

import android.os.SystemProperties;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PreconfiguredNetworkManager {
    private static String[] CFG_DIRS = new String[]{"/cust_spec", "/hw_oem"};
    private static final int EAP_AKA = 5;
    private static final String EAP_AKA_METHOD = "EAP_AKA";
    private static final int EAP_AKA_PRIME = 6;
    private static final String EAP_AKA_PRIME_METHOD = "EAP_AKA_PRIME";
    private static final int EAP_SIM = 4;
    private static final String EAP_SIM_METHOD = "EAP_SIM";
    public static final boolean IS_R1;
    private static final String PRECONFIGUREDNETWORKLIST_NODE_ROOT = "PreconfiguredNetworkList";
    private static final String PRECONFIGUREDNETWORK_EAPMETHOD = "eapMethod";
    private static final String PRECONFIGUREDNETWORK_NODE = "PreconfiguredNetwork";
    private static final String PRECONFIGUREDNETWORK_SSID = "ssid";
    private static final String STORE_FILE_NAME = "PreconfiguredNetwork.xml";
    private static final String TAG = "PreconfiguredNetwork";
    private static PreconfiguredNetworkManager instance = new PreconfiguredNetworkManager();
    private List<PreconfiguredNetwork> preconfiguredNetworks = new ArrayList();

    static {
        boolean z = false;
        if (SystemProperties.get("ro.config.hw_opta", "0").equals("389")) {
            z = SystemProperties.get("ro.config.hw_optb", "0").equals("840");
        }
        IS_R1 = z;
    }

    private PreconfiguredNetworkManager() {
        pasreConfigFile();
    }

    public static PreconfiguredNetworkManager getInstance() {
        return instance;
    }

    public boolean isPreconfiguredNetwork(String ssid) {
        int list_size = this.preconfiguredNetworks.size();
        for (int i = 0; i < list_size; i++) {
            PreconfiguredNetwork preconfiguredNetwork = (PreconfiguredNetwork) this.preconfiguredNetworks.get(i);
            if (ssid.equals(preconfiguredNetwork.getSsid()) || ssid.equals("\"" + preconfiguredNetwork.getSsid() + "\"")) {
                return true;
            }
        }
        return false;
    }

    public PreconfiguredNetwork match(String ssid) {
        int list_size = this.preconfiguredNetworks.size();
        for (int i = 0; i < list_size; i++) {
            PreconfiguredNetwork preconfiguredNetwork = (PreconfiguredNetwork) this.preconfiguredNetworks.get(i);
            if (preconfiguredNetwork.getSsid().equals(ssid)) {
                return preconfiguredNetwork;
            }
        }
        return null;
    }

    private File[] searchFile(File folder) {
        File[] subFolders = folder.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isDirectory() || (pathname.isFile() && pathname.getName().equals(PreconfiguredNetworkManager.STORE_FILE_NAME))) {
                    return true;
                }
                return false;
            }
        });
        List<File> result = new ArrayList();
        if (subFolders != null) {
            for (File subFile : subFolders) {
                if (subFile.isFile()) {
                    result.add(subFile);
                } else {
                    for (File file : searchFile(subFile)) {
                        result.add(file);
                    }
                }
            }
        }
        return (File[]) result.toArray(new File[0]);
    }

    private void pasreConfigFile() {
        Throwable th;
        InputStream inputStream = null;
        File configFile = null;
        for (String dir : CFG_DIRS) {
            File[] result = searchFile(new File(dir));
            if (result.length > 0) {
                Log.i("PreconfiguredNetwork", dir);
                configFile = result[0];
                break;
            }
        }
        if (configFile == null || (configFile.exists() ^ 1) != 0) {
            Log.e("PreconfiguredNetwork", "file not find");
            return;
        }
        try {
            InputStream inputStream2 = new FileInputStream(configFile);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(inputStream2, "UTF-8");
                XmlUtils.beginDocument(parser, PRECONFIGUREDNETWORKLIST_NODE_ROOT);
                while (true) {
                    XmlUtils.nextElement(parser);
                    if (!"PreconfiguredNetwork".equalsIgnoreCase(parser.getName())) {
                        break;
                    }
                    this.preconfiguredNetworks.add(new PreconfiguredNetwork(parser.getAttributeValue(null, "ssid"), getEapMethod(parser.getAttributeValue(null, PRECONFIGUREDNETWORK_EAPMETHOD))));
                }
                if (inputStream2 != null) {
                    try {
                        inputStream2.close();
                    } catch (IOException e) {
                        Log.e("PreconfiguredNetwork", "Close inputStream error.");
                    }
                }
            } catch (FileNotFoundException e2) {
                inputStream = inputStream2;
                try {
                    Log.e("PreconfiguredNetwork", "Exception in preconfiguredNetwork parse.");
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e3) {
                            Log.e("PreconfiguredNetwork", "Close inputStream error.");
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e4) {
                            Log.e("PreconfiguredNetwork", "Close inputStream error.");
                        }
                    }
                    throw th;
                }
            } catch (XmlPullParserException e5) {
                inputStream = inputStream2;
                Log.e("PreconfiguredNetwork", "Exception in preconfiguredNetwork parse.");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e6) {
                        Log.e("PreconfiguredNetwork", "Close inputStream error.");
                    }
                }
            } catch (IOException e7) {
                inputStream = inputStream2;
                Log.e("PreconfiguredNetwork", "Exception in preconfiguredNetwork parse.");
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e8) {
                        Log.e("PreconfiguredNetwork", "Close inputStream error.");
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                inputStream = inputStream2;
                if (inputStream != null) {
                    inputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e9) {
            Log.e("PreconfiguredNetwork", "Exception in preconfiguredNetwork parse.");
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (XmlPullParserException e10) {
            Log.e("PreconfiguredNetwork", "Exception in preconfiguredNetwork parse.");
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e11) {
            Log.e("PreconfiguredNetwork", "Exception in preconfiguredNetwork parse.");
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private int getEapMethod(String eap_mode) {
        if (eap_mode.equals(EAP_AKA_METHOD)) {
            return 5;
        }
        if (eap_mode.equals(EAP_SIM_METHOD)) {
            return 4;
        }
        if (eap_mode.equals(EAP_AKA_PRIME_METHOD)) {
            return 6;
        }
        return 5;
    }
}
