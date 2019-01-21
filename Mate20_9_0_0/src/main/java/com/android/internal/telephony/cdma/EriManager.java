package com.android.internal.telephony.cdma;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.util.Xml;
import com.android.internal.telephony.Phone;
import com.android.internal.util.XmlUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class EriManager {
    private static final boolean DBG = true;
    static final int ERI_FROM_FILE_SYSTEM = 1;
    static final int ERI_FROM_MODEM = 2;
    public static final int ERI_FROM_XML = 0;
    private static final boolean VDBG = false;
    private String LOG_TAG = "EriManager";
    private Context mContext;
    private EriFile mEriFile;
    private int mEriFileSource = 0;
    private boolean mIsEriFileLoaded;
    private final Phone mPhone;

    class EriDisplayInformation {
        int mEriIconIndex;
        int mEriIconMode;
        String mEriIconText;

        EriDisplayInformation(int eriIconIndex, int eriIconMode, String eriIconText) {
            this.mEriIconIndex = eriIconIndex;
            this.mEriIconMode = eriIconMode;
            this.mEriIconText = eriIconText;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EriDisplayInformation: { IconIndex: ");
            stringBuilder.append(this.mEriIconIndex);
            stringBuilder.append(" EriIconMode: ");
            stringBuilder.append(this.mEriIconMode);
            stringBuilder.append(" EriIconText: ");
            stringBuilder.append(this.mEriIconText);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    class EriFile {
        String[] mCallPromptId = new String[]{"", "", ""};
        int mEriFileType = -1;
        int mNumberOfEriEntries = 0;
        HashMap<Integer, EriInfo> mRoamIndTable = new HashMap();
        int mVersionNumber = -1;

        EriFile() {
        }
    }

    public EriManager(Phone phone, Context context, int eriFileSource) {
        this.mPhone = phone;
        this.mContext = context;
        this.mEriFileSource = eriFileSource;
        this.mEriFile = new EriFile();
        if (phone != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.LOG_TAG);
            stringBuilder.append("[SUB");
            stringBuilder.append(phone.getPhoneId());
            stringBuilder.append("]");
            this.LOG_TAG = stringBuilder.toString();
        }
    }

    public void dispose() {
        this.mEriFile = new EriFile();
        this.mIsEriFileLoaded = false;
    }

    public void loadEriFile() {
        switch (this.mEriFileSource) {
            case 1:
                loadEriFileFromFileSystem();
                return;
            case 2:
                loadEriFileFromModem();
                return;
            default:
                loadEriFileFromXml();
                return;
        }
    }

    private void loadEriFileFromModem() {
    }

    private void loadEriFileFromFileSystem() {
    }

    private void loadEriFileFromXml() {
        XmlPullParser parser;
        String str;
        FileInputStream stream = null;
        Resources r = this.mContext.getResources();
        String str2 = null;
        try {
            Rlog.d(this.LOG_TAG, "loadEriFileFromXml: check for alternate file");
            stream = new FileInputStream(r.getString(17039576));
            parser = Xml.newPullParser();
            parser.setInput(stream, null);
            Rlog.d(this.LOG_TAG, "loadEriFileFromXml: opened alternate file");
        } catch (FileNotFoundException e) {
            Rlog.d(this.LOG_TAG, "loadEriFileFromXml: no alternate file");
            parser = null;
        } catch (XmlPullParserException e2) {
            Rlog.d(this.LOG_TAG, "loadEriFileFromXml: no parser for alternate file");
            parser = null;
        }
        if (parser == null) {
            String eriFile = null;
            CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
            if (configManager != null) {
                PersistableBundle b = configManager.getConfigForSubId(this.mPhone.getSubId());
                if (b != null) {
                    eriFile = b.getString("carrier_eri_file_name_string");
                }
            }
            String eriFile2 = eriFile;
            eriFile = this.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("eriFile = ");
            stringBuilder.append(eriFile2);
            Rlog.d(eriFile, stringBuilder.toString());
            if (eriFile2 == null) {
                Rlog.e(this.LOG_TAG, "loadEriFileFromXml: Can't find ERI file to load");
                return;
            }
            try {
                parser = Xml.newPullParser();
                parser.setInput(this.mContext.getAssets().open(eriFile2), null);
            } catch (IOException | XmlPullParserException e3) {
                str = this.LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("loadEriFileFromXml: no parser for ");
                stringBuilder2.append(eriFile2);
                stringBuilder2.append(". Exception = ");
                stringBuilder2.append(e3.toString());
                Rlog.e(str, stringBuilder2.toString());
            }
        }
        try {
            XmlUtils.beginDocument(parser, "EriFile");
            this.mEriFile.mVersionNumber = Integer.parseInt(parser.getAttributeValue(null, "VersionNumber"));
            this.mEriFile.mNumberOfEriEntries = Integer.parseInt(parser.getAttributeValue(null, "NumberOfEriEntries"));
            this.mEriFile.mEriFileType = Integer.parseInt(parser.getAttributeValue(null, "EriFileType"));
            int parsedEriEntries = 0;
            while (true) {
                XmlUtils.nextElement(parser);
                String name = parser.getName();
                if (name == null) {
                    break;
                }
                int id;
                if (name.equals("CallPromptId")) {
                    id = Integer.parseInt(parser.getAttributeValue(str2, "Id"));
                    str = parser.getAttributeValue(str2, "CallPromptText");
                    if (id < 0 || id > 2) {
                        String str3 = this.LOG_TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Error Parsing ERI file: found");
                        stringBuilder3.append(id);
                        stringBuilder3.append(" CallPromptId");
                        Rlog.e(str3, stringBuilder3.toString());
                    } else {
                        this.mEriFile.mCallPromptId[id] = str;
                    }
                } else if (name.equals("EriInfo")) {
                    id = Integer.parseInt(parser.getAttributeValue(str2, "RoamingIndicator"));
                    int iconIndex = Integer.parseInt(parser.getAttributeValue(str2, "IconIndex"));
                    int iconMode = Integer.parseInt(parser.getAttributeValue(str2, "IconMode"));
                    String eriText = parser.getAttributeValue(str2, "EriText");
                    int callPromptId = Integer.parseInt(parser.getAttributeValue(str2, "CallPromptId"));
                    int alertId = Integer.parseInt(parser.getAttributeValue(str2, "AlertId"));
                    parsedEriEntries++;
                    HashMap hashMap = this.mEriFile.mRoamIndTable;
                    EriInfo eriInfo = r8;
                    Integer valueOf = Integer.valueOf(id);
                    EriInfo eriInfo2 = new EriInfo(id, iconIndex, iconMode, eriText, callPromptId, alertId);
                    hashMap.put(valueOf, eriInfo);
                }
                str2 = null;
            }
            if (parsedEriEntries != this.mEriFile.mNumberOfEriEntries) {
                str2 = this.LOG_TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Error Parsing ERI file: ");
                stringBuilder4.append(this.mEriFile.mNumberOfEriEntries);
                stringBuilder4.append(" defined, ");
                stringBuilder4.append(parsedEriEntries);
                stringBuilder4.append(" parsed!");
                Rlog.e(str2, stringBuilder4.toString());
            }
            str2 = this.LOG_TAG;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("loadEriFileFromXml: eri parsing successful, file loaded. ver = ");
            stringBuilder5.append(this.mEriFile.mVersionNumber);
            stringBuilder5.append(", # of entries = ");
            stringBuilder5.append(this.mEriFile.mNumberOfEriEntries);
            Rlog.d(str2, stringBuilder5.toString());
            this.mIsEriFileLoaded = true;
            if (parser instanceof XmlResourceParser) {
                ((XmlResourceParser) parser).close();
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e4) {
                }
            }
        } catch (Exception e32) {
            Rlog.e(this.LOG_TAG, "Got exception while loading ERI file.", e32);
            if (parser instanceof XmlResourceParser) {
                ((XmlResourceParser) parser).close();
            }
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            Throwable th2 = th;
            if (parser instanceof XmlResourceParser) {
                ((XmlResourceParser) parser).close();
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e5) {
                }
            }
        }
    }

    public int getEriFileVersion() {
        return this.mEriFile.mVersionNumber;
    }

    public int getEriNumberOfEntries() {
        return this.mEriFile.mNumberOfEriEntries;
    }

    public int getEriFileType() {
        return this.mEriFile.mEriFileType;
    }

    public boolean isEriFileLoaded() {
        return this.mIsEriFileLoaded;
    }

    private EriInfo getEriInfo(int roamingIndicator) {
        if (this.mEriFile.mRoamIndTable.containsKey(Integer.valueOf(roamingIndicator))) {
            return (EriInfo) this.mEriFile.mRoamIndTable.get(Integer.valueOf(roamingIndicator));
        }
        return null;
    }

    private EriDisplayInformation getEriDisplayInformation(int roamInd, int defRoamInd) {
        EriInfo eriInfo;
        EriDisplayInformation ret;
        if (this.mIsEriFileLoaded) {
            eriInfo = getEriInfo(roamInd);
            if (eriInfo != null) {
                return new EriDisplayInformation(eriInfo.iconIndex, eriInfo.iconMode, eriInfo.eriText);
            }
        }
        switch (roamInd) {
            case 0:
                ret = new EriDisplayInformation(0, 0, this.mContext.getText(17041020).toString());
                break;
            case 1:
                ret = new EriDisplayInformation(1, 0, this.mContext.getText(17041021).toString());
                break;
            case 2:
                ret = new EriDisplayInformation(2, 1, this.mContext.getText(17041025).toString());
                break;
            case 3:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041026).toString());
                break;
            case 4:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041027).toString());
                break;
            case 5:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041028).toString());
                break;
            case 6:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041029).toString());
                break;
            case 7:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041030).toString());
                break;
            case 8:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041031).toString());
                break;
            case 9:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041032).toString());
                break;
            case 10:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041022).toString());
                break;
            case 11:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041023).toString());
                break;
            case 12:
                ret = new EriDisplayInformation(roamInd, 0, this.mContext.getText(17041024).toString());
                break;
            default:
                if (!this.mIsEriFileLoaded) {
                    Rlog.d(this.LOG_TAG, "ERI File not loaded");
                    if (defRoamInd <= 2) {
                        switch (defRoamInd) {
                            case 0:
                                ret = new EriDisplayInformation(0, 0, this.mContext.getText(17041020).toString());
                                break;
                            case 1:
                                ret = new EriDisplayInformation(1, 0, this.mContext.getText(17041021).toString());
                                break;
                            case 2:
                                ret = new EriDisplayInformation(2, 1, this.mContext.getText(17041025).toString());
                                break;
                            default:
                                ret = new EriDisplayInformation(-1, -1, "ERI text");
                                break;
                        }
                    }
                    ret = new EriDisplayInformation(2, 1, this.mContext.getText(17041025).toString());
                    break;
                }
                EriDisplayInformation eriDisplayInformation;
                eriInfo = getEriInfo(roamInd);
                EriInfo defEriInfo = getEriInfo(defRoamInd);
                if (eriInfo != null) {
                    eriDisplayInformation = new EriDisplayInformation(eriInfo.iconIndex, eriInfo.iconMode, eriInfo.eriText);
                } else if (defEriInfo == null) {
                    String str = this.LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ERI defRoamInd ");
                    stringBuilder.append(defRoamInd);
                    stringBuilder.append(" not found in ERI file ...on");
                    Rlog.e(str, stringBuilder.toString());
                    eriDisplayInformation = new EriDisplayInformation(0, 0, this.mContext.getText(17041020).toString());
                } else {
                    eriDisplayInformation = new EriDisplayInformation(defEriInfo.iconIndex, defEriInfo.iconMode, defEriInfo.eriText);
                }
                ret = eriDisplayInformation;
                break;
        }
        return ret;
    }

    public int getCdmaEriIconIndex(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconIndex;
    }

    public int getCdmaEriIconMode(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconMode;
    }

    public String getCdmaEriText(int roamInd, int defRoamInd) {
        return getEriDisplayInformation(roamInd, defRoamInd).mEriIconText;
    }
}
