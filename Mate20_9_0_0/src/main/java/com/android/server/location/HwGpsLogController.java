package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class HwGpsLogController {
    private static final String BCM_47531 = "47531";
    private static final String BCM_4774 = "4774";
    private static final long CHECK_GPS_LOG_INTERVEL = 30000;
    private static final String CHIP_TYPE = "ro.connectivity.chiptype";
    private static boolean DBG = true;
    private static final String GPS_CONFIG_PATH = "data/gps/gpsconfig.xml";
    private static final String GPS_LOG_ENABLE = "gps_log_enable";
    private static final String GPS_LOG_STATUS_CLOSE = "com.android.huawei.log.GPS_LOG_STATUS_CLOSE";
    private static final String GPS_LOG_STATUS_OPEN = "com.android.huawei.log.GPS_LOG_STATUS_OPEN";
    private static final String GPS_PATH_IC_TYPE = "/proc/device-tree/gps_power/broadcom_config,ic_type";
    private static final int MSG_CHECK_GPS_LOG_STATUS = 1103;
    private static final int MSG_GPS_LOG_STATUS_CLOSE = 1102;
    private static final int MSG_GPS_LOG_STATUS_OPEN = 1101;
    private static final String TAG = "GpsLogController";
    private static HwGpsLogController mGpsLogController;
    private String mBcmChipType;
    private Context mContext;
    private Handler mHandler;

    private HwGpsLogController(Context context) {
        if (DBG) {
            Log.d(TAG, "HwGpsLogController");
        }
        this.mContext = context;
        this.mHandler = new Handler(new Callback() {
            public boolean handleMessage(Message msg) {
                if (HwGpsLogController.DBG) {
                    String str = HwGpsLogController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleMessage: ");
                    stringBuilder.append(msg.what);
                    Log.d(str, stringBuilder.toString());
                }
                switch (msg.what) {
                    case HwGpsLogController.MSG_GPS_LOG_STATUS_OPEN /*1101*/:
                        Global.putString(HwGpsLogController.this.mContext.getContentResolver(), HwGpsLogController.GPS_LOG_ENABLE, Boolean.toString(true));
                        HwGpsLogController.this.operatorLog(true);
                        return true;
                    case HwGpsLogController.MSG_GPS_LOG_STATUS_CLOSE /*1102*/:
                        Global.putString(HwGpsLogController.this.mContext.getContentResolver(), HwGpsLogController.GPS_LOG_ENABLE, Boolean.toString(false));
                        HwGpsLogController.this.operatorLog(false);
                        return true;
                    case HwGpsLogController.MSG_CHECK_GPS_LOG_STATUS /*1103*/:
                        HwGpsLogController.this.checkGpsLogStatus();
                        return true;
                    default:
                        return false;
                }
            }
        });
        this.mHandler.sendEmptyMessageDelayed(MSG_CHECK_GPS_LOG_STATUS, 30000);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GPS_LOG_STATUS_OPEN);
        intentFilter.addAction(GPS_LOG_STATUS_CLOSE);
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (HwGpsLogController.GPS_LOG_STATUS_OPEN.equals(action)) {
                    HwGpsLogController.this.mHandler.sendEmptyMessage(HwGpsLogController.MSG_GPS_LOG_STATUS_OPEN);
                    HwGpsLogController.this.mHandler.removeMessages(HwGpsLogController.MSG_CHECK_GPS_LOG_STATUS);
                } else if (HwGpsLogController.GPS_LOG_STATUS_CLOSE.equals(action)) {
                    HwGpsLogController.this.mHandler.sendEmptyMessage(HwGpsLogController.MSG_GPS_LOG_STATUS_CLOSE);
                    HwGpsLogController.this.mHandler.removeMessages(HwGpsLogController.MSG_CHECK_GPS_LOG_STATUS);
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
        getBcmGpsChipType();
    }

    private String getBcmGpsChipType() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(GPS_PATH_IC_TYPE), "UTF-8"));
            int line = 1;
            while (true) {
                String readLine = reader.readLine();
                String tempString = readLine;
                if (readLine == null) {
                    break;
                }
                if (DBG) {
                    readLine = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("line ");
                    stringBuilder.append(line);
                    stringBuilder.append(": ");
                    stringBuilder.append(tempString);
                    Log.d(readLine, stringBuilder.toString());
                }
                line++;
                if (tempString.contains(BCM_47531)) {
                    this.mBcmChipType = BCM_47531;
                } else if (tempString.contains(BCM_4774)) {
                    this.mBcmChipType = BCM_4774;
                }
            }
            reader.close();
            try {
                reader.close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            e2.printStackTrace();
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                }
            }
        }
        return null;
    }

    public static synchronized HwGpsLogController create(Context context) {
        HwGpsLogController hwGpsLogController;
        synchronized (HwGpsLogController.class) {
            if (mGpsLogController == null) {
                mGpsLogController = new HwGpsLogController(context);
            }
            hwGpsLogController = mGpsLogController;
        }
        return hwGpsLogController;
    }

    private void checkGpsLogStatus() {
        try {
            String result = Global.getString(this.mContext.getContentResolver(), GPS_LOG_ENABLE);
            if (result != null) {
                if (!"".equals(result)) {
                    operatorLog(Boolean.parseBoolean(result));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void operatorLog(boolean enable) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("operatorLog: ");
            stringBuilder.append(enable);
            Log.d(str, stringBuilder.toString());
        }
        try {
            if (SystemProperties.get(CHIP_TYPE, "").contains("bcm") && SystemProperties.getInt("persist.sys.huawei.debug.on", 0) == 1 && this.mBcmChipType != null) {
                xmlModify(enable);
                killGpsProcess();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void killGpsProcess() {
        BufferedReader bufferedReader = null;
        BufferedReader inputStream = bufferedReader;
        try {
            Process proc = Runtime.getRuntime().exec("ps");
            InputStream inputStream2 = proc.getInputStream();
            proc.waitFor();
            if (inputStream2 != null) {
                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream2, "UTF-8"));
                    while (true) {
                        String readLine = bufferedReader.readLine();
                        String line = readLine;
                        if (readLine == null) {
                            try {
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (line.contains("glgps")) {
                            int pid = getPid(line);
                            if (-1 != pid) {
                                if (DBG) {
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("kill ");
                                    stringBuilder.append(pid);
                                    Log.i(str, stringBuilder.toString());
                                }
                                Process.killProcessQuiet(pid);
                            }
                        }
                    }
                    inputStream2.close();
                    try {
                        bufferedReader.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                } catch (IOException e22) {
                    e22.printStackTrace();
                    try {
                        inputStream2.close();
                    } catch (IOException e222) {
                        e222.printStackTrace();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (RuntimeException rte) {
                    rte.printStackTrace();
                    try {
                        inputStream2.close();
                    } catch (IOException e2222) {
                        e2222.printStackTrace();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    try {
                        inputStream2.close();
                    } catch (IOException e22222) {
                        e22222.printStackTrace();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (Throwable th) {
                    try {
                        inputStream2.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e32) {
                            e32.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e4) {
            e4.printStackTrace();
        }
    }

    private int getPid(String ps) {
        String[] results = ps.split(" +");
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPid ");
            stringBuilder.append(results[1]);
            Log.i(str, stringBuilder.toString());
        }
        return Integer.parseInt(results[1]);
    }

    private void xmlModify(boolean enable) {
        try {
            File file = new File(GPS_CONFIG_PATH);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            if (doc != null) {
                Node gllNodes = doc.getElementsByTagName("gll").item(0);
                if (gllNodes != null && BCM_47531.equals(this.mBcmChipType)) {
                    Node logFacMaskNode = gllNodes.getAttributes().getNamedItem("LogFacMask");
                    if (logFacMaskNode != null) {
                        logFacMaskNode.setNodeValue(enable ? "LOG_DEFAULT" : "LOG_GLLAPI | LOG_NMEA");
                        if (DBG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("modify:");
                            stringBuilder.append(logFacMaskNode.getNodeName());
                            stringBuilder.append("=");
                            stringBuilder.append(logFacMaskNode.getNodeValue());
                            Log.d(str, stringBuilder.toString());
                        }
                    }
                }
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty("indent", "yes");
                transformer.transform(new DOMSource(doc), new StreamResult(file));
            }
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
        } catch (SAXException e2) {
            e2.printStackTrace();
        } catch (TransformerConfigurationException e3) {
            e3.printStackTrace();
        } catch (TransformerException e4) {
            e4.printStackTrace();
        } catch (IOException e5) {
            e5.printStackTrace();
        } catch (RuntimeException e52) {
            e52.printStackTrace();
        }
    }
}
