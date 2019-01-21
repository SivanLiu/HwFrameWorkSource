package com.android.server.os;

import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class UDIDModelWhiteConfig {
    private static final String TAG = "UDIDModelWhiteConfig";
    private static final int UDID_MODEL_TYPE = 0;
    private static final String UDID_MODEL_WHITE_LIST = "udid_model_whitelist.xml";
    private static final String XML_ATTRIBUTE_NAME = "name";
    private static final String XML_UDID_MODEL = "udid_model";
    private static UDIDModelWhiteConfig udidModelWhiteConfig;
    private List<String> whiteModelInfos = new ArrayList();

    public static UDIDModelWhiteConfig getInstance() {
        if (udidModelWhiteConfig == null) {
            udidModelWhiteConfig = new UDIDModelWhiteConfig();
        }
        return udidModelWhiteConfig;
    }

    private UDIDModelWhiteConfig() {
        loadWhiteModelWhiteList();
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x005f A:{Catch:{ FileNotFoundException -> 0x0052, XmlPullParserException -> 0x004f, IOException -> 0x004d, all -> 0x004a }} */
    /* JADX WARNING: Removed duplicated region for block: B:66:? A:{SYNTHETIC, RETURN, ORIG_RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0090 A:{SYNTHETIC, Splitter:B:33:0x0090} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadWhiteModelWhiteList() {
        String str;
        StringBuilder stringBuilder;
        IOException e;
        InputStream inputStream = null;
        File udidModeFile = null;
        try {
            udidModeFile = HwCfgFilePolicy.getCfgFile("xml/udid_model_whitelist.xml", 0);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("udidModeFile exits? ");
            stringBuilder.append(udidModeFile.exists());
            Slog.w(str, stringBuilder.toString());
        } catch (NoClassDefFoundError e2) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        } catch (Exception e3) {
            Log.d(TAG, "HwCfgFilePolicy get udid_model_whitelist exception");
        }
        if (udidModeFile != null) {
            try {
                if (udidModeFile.exists()) {
                    inputStream = new FileInputStream(udidModeFile);
                    if (inputStream != null) {
                        XmlPullParser xmlParser = Xml.newPullParser();
                        xmlParser.setInput(inputStream, null);
                        for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                            if (xmlEventType == 2 && XML_UDID_MODEL.equals(xmlParser.getName())) {
                                addUDIDModelInfo(xmlParser.getAttributeValue(null, "name"));
                            }
                        }
                    }
                    if (inputStream == null) {
                        try {
                            inputStream.close();
                            return;
                        } catch (IOException e4) {
                            e = e4;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    } else {
                        return;
                    }
                }
            } catch (FileNotFoundException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("load udid model config: ");
                stringBuilder.append(e5.getMessage());
                Log.e(str, stringBuilder.toString());
                if (inputStream != null) {
                    try {
                        inputStream.close();
                        return;
                    } catch (IOException e6) {
                        e = e6;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } else {
                    return;
                }
            } catch (XmlPullParserException e7) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("load udid model config: ");
                stringBuilder.append(e7.getMessage());
                Log.e(str, stringBuilder.toString());
                if (inputStream != null) {
                    try {
                        inputStream.close();
                        return;
                    } catch (IOException e8) {
                        e = e8;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } else {
                    return;
                }
            } catch (IOException e9) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("load udid model config: ");
                stringBuilder.append(e9.getMessage());
                Log.e(str, stringBuilder.toString());
                if (inputStream != null) {
                    try {
                        inputStream.close();
                        return;
                    } catch (IOException e10) {
                        e9 = e10;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                } else {
                    return;
                }
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e11) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("load udid model config: IO Exception while closing stream: ");
                        stringBuilder.append(e11.getMessage());
                        Log.e(TAG, stringBuilder.toString());
                    }
                }
            }
        }
        Slog.w(TAG, "udid_model_whitelist.xml is not exist");
        if (inputStream != null) {
        }
        if (inputStream == null) {
        }
        stringBuilder.append("load udid model config: IO Exception while closing stream: ");
        stringBuilder.append(e9.getMessage());
        Log.e(str, stringBuilder.toString());
    }

    public boolean isWhiteModelForUDID(String model) {
        int size = this.whiteModelInfos.size();
        if (size <= 0) {
            loadWhiteModelWhiteList();
        }
        for (int i = 0; i < size; i++) {
            String modelInWhiteList = (String) this.whiteModelInfos.get(i);
            if (model != null && modelInWhiteList != null && model.indexOf(modelInWhiteList) == 0) {
                return true;
            }
        }
        return false;
    }

    private void addUDIDModelInfo(String model) {
        if (!this.whiteModelInfos.contains(model)) {
            this.whiteModelInfos.add(model);
        }
    }
}
