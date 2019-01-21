package android.perf;

import android.os.Environment;
import android.os.SystemProperties;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;

public class HwOptPackageParserImpl implements HwOptPackageParser {
    private static final String TAG = "HwOptPackageParserImpl";
    private static final boolean mOptPackageListEnable = SystemProperties.getBoolean("persist.kirin.perfoptpackage_list", false);
    private Map<Integer, ArrayList<String>> mOptPackageMap = null;

    public void getOptPackages() {
        FileInputStream fis = null;
        String optType = null;
        ArrayList<String> packageList = null;
        String pkgName;
        IOException e;
        String str;
        StringBuilder stringBuilder;
        try {
            fis = new AtomicFile(new File(new File(Environment.getRootDirectory(), "/etc"), "packages-perfopt.xml")).openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, null);
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType != 0) {
                    switch (eventType) {
                        case 2:
                            if (!parser.getName().equals("opt_type")) {
                                if (parser.getName().equals("pkg")) {
                                    pkgName = parser.getAttributeValue(null, "name");
                                    if (!(pkgName == null || packageList == null)) {
                                        packageList.add(pkgName);
                                    }
                                    break;
                                }
                            }
                            optType = parser.getAttributeValue(null, "typeid");
                            packageList = new ArrayList();
                            break;
                            break;
                        case 3:
                            if (parser.getName().equals("opt_type")) {
                                addOptPackageList(optType, packageList);
                                optType = null;
                                packageList = null;
                                break;
                            }
                            break;
                        default:
                            break;
                    }
                }
                this.mOptPackageMap = new HashMap();
            }
            if (fis != null) {
                try {
                    fis.close();
                    return;
                } catch (IOException e2) {
                    e = e2;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            } else {
                return;
            }
            stringBuilder.append("Error close fis: ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
        } catch (Exception e3) {
            pkgName = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error parse packages-opt: ");
            stringBuilder2.append(e3.getMessage());
            Slog.e(pkgName, stringBuilder2.toString());
            this.mOptPackageMap = null;
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e4) {
                    e = e4;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e5) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error close fis: ");
                    stringBuilder.append(e5.getMessage());
                    Slog.e(TAG, stringBuilder.toString());
                }
            }
        }
    }

    public boolean isPerfOptEnable(String pkgName, int optTypeId) {
        if (!mOptPackageListEnable) {
            return true;
        }
        if (this.mOptPackageMap == null) {
            return false;
        }
        ArrayList<String> pkgList = (ArrayList) this.mOptPackageMap.get(Integer.valueOf(optTypeId));
        if (pkgList == null) {
            return false;
        }
        return pkgList.contains(pkgName);
    }

    private void addOptPackageList(String optType, ArrayList<String> packageList) {
        if (this.mOptPackageMap != null && optType != null && packageList != null) {
            int optTypeId;
            try {
                optTypeId = Integer.parseInt(optType);
            } catch (NumberFormatException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Errot opt type: ");
                stringBuilder.append(e.getMessage());
                Slog.e(str, stringBuilder.toString());
                optTypeId = 0;
            }
            if (optTypeId != 0) {
                this.mOptPackageMap.put(Integer.valueOf(optTypeId), packageList);
            }
        }
    }
}
