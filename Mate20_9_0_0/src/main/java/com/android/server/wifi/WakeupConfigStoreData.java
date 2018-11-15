package com.android.server.wifi;

import android.util.ArraySet;
import android.util.Log;
import com.android.server.wifi.WifiConfigStore.StoreData;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WakeupConfigStoreData implements StoreData {
    private static final String TAG = "WakeupConfigStoreData";
    private static final String XML_TAG_FEATURE_STATE_SECTION = "FeatureState";
    private static final String XML_TAG_IS_ACTIVE = "IsActive";
    private static final String XML_TAG_IS_ONBOARDED = "IsOnboarded";
    private static final String XML_TAG_NETWORK_SECTION = "Network";
    private static final String XML_TAG_NOTIFICATIONS_SHOWN = "NotificationsShown";
    private static final String XML_TAG_SECURITY = "Security";
    private static final String XML_TAG_SSID = "SSID";
    private boolean mHasBeenRead = false;
    private final DataSource<Boolean> mIsActiveDataSource;
    private final DataSource<Boolean> mIsOnboardedDataSource;
    private final DataSource<Set<ScanResultMatchInfo>> mNetworkDataSource;
    private final DataSource<Integer> mNotificationsDataSource;

    public interface DataSource<T> {
        T getData();

        void setData(T t);
    }

    public WakeupConfigStoreData(DataSource<Boolean> isActiveDataSource, DataSource<Boolean> isOnboardedDataSource, DataSource<Integer> notificationsDataSource, DataSource<Set<ScanResultMatchInfo>> networkDataSource) {
        this.mIsActiveDataSource = isActiveDataSource;
        this.mIsOnboardedDataSource = isOnboardedDataSource;
        this.mNotificationsDataSource = notificationsDataSource;
        this.mNetworkDataSource = networkDataSource;
    }

    public boolean hasBeenRead() {
        return this.mHasBeenRead;
    }

    public void serializeData(XmlSerializer out, boolean shared) throws XmlPullParserException, IOException {
        if (shared) {
            throw new XmlPullParserException("Share data not supported");
        }
        writeFeatureState(out);
        for (ScanResultMatchInfo scanResultMatchInfo : (Set) this.mNetworkDataSource.getData()) {
            writeNetwork(out, scanResultMatchInfo);
        }
    }

    private void writeFeatureState(XmlSerializer out) throws IOException, XmlPullParserException {
        XmlUtil.writeNextSectionStart(out, XML_TAG_FEATURE_STATE_SECTION);
        XmlUtil.writeNextValue(out, XML_TAG_IS_ACTIVE, this.mIsActiveDataSource.getData());
        XmlUtil.writeNextValue(out, XML_TAG_IS_ONBOARDED, this.mIsOnboardedDataSource.getData());
        XmlUtil.writeNextValue(out, XML_TAG_NOTIFICATIONS_SHOWN, this.mNotificationsDataSource.getData());
        XmlUtil.writeNextSectionEnd(out, XML_TAG_FEATURE_STATE_SECTION);
    }

    private void writeNetwork(XmlSerializer out, ScanResultMatchInfo scanResultMatchInfo) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(out, XML_TAG_NETWORK_SECTION);
        XmlUtil.writeNextValue(out, "SSID", scanResultMatchInfo.networkSsid);
        XmlUtil.writeNextValue(out, XML_TAG_SECURITY, Integer.valueOf(scanResultMatchInfo.networkType));
        XmlUtil.writeNextSectionEnd(out, XML_TAG_NETWORK_SECTION);
    }

    /* JADX WARNING: Removed duplicated region for block: B:32:0x005d A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0057  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x004d  */
    /* JADX WARNING: Missing block: B:16:0x003b, code:
            if (r4.equals(XML_TAG_FEATURE_STATE_SECTION) == false) goto L_0x0048;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void deserializeData(XmlPullParser in, int outerTagDepth, boolean shared) throws XmlPullParserException, IOException {
        if (!(shared || this.mHasBeenRead)) {
            Log.d(TAG, "WifiWake user data has been read");
            this.mHasBeenRead = true;
        }
        if (in != null) {
            if (shared) {
                throw new XmlPullParserException("Shared data not supported");
            }
            Set<ScanResultMatchInfo> networks = new ArraySet();
            String[] headerName = new String[1];
            while (XmlUtil.gotoNextSectionOrEnd(in, headerName, outerTagDepth)) {
                int i = 0;
                String str = headerName[0];
                int hashCode = str.hashCode();
                if (hashCode == -786828786) {
                    if (str.equals(XML_TAG_NETWORK_SECTION)) {
                        i = 1;
                        switch (i) {
                            case 0:
                                parseFeatureState(in, outerTagDepth + 1);
                                break;
                            case 1:
                                networks.add(parseNetwork(in, outerTagDepth + 1));
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1362433883) {
                }
                i = -1;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
            this.mNetworkDataSource.setData(networks);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:32:0x004f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0078  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0070  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0068  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x004f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0078  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0070  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0068  */
    /* JADX WARNING: Missing block: B:18:0x0048, code:
            if (r7.equals(XML_TAG_IS_ONBOARDED) != false) goto L_0x004c;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseFeatureState(XmlPullParser in, int outerTagDepth) throws IOException, XmlPullParserException {
        boolean isOnboarded = false;
        boolean isActive = false;
        int notificationsShown = 0;
        while (!XmlUtil.isNextSectionEnd(in, outerTagDepth)) {
            int i = 1;
            String[] valueName = new String[1];
            Object value = XmlUtil.readCurrentValue(in, valueName);
            if (valueName[0] != null) {
                String str = valueName[0];
                int hashCode = str.hashCode();
                if (hashCode != -1725092580) {
                    if (hashCode == -684272400) {
                        if (str.equals(XML_TAG_IS_ACTIVE)) {
                            i = 0;
                            switch (i) {
                                case 0:
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if (hashCode == 898665769 && str.equals(XML_TAG_NOTIFICATIONS_SHOWN)) {
                        i = 2;
                        switch (i) {
                            case 0:
                                isActive = ((Boolean) value).booleanValue();
                                break;
                            case 1:
                                isOnboarded = ((Boolean) value).booleanValue();
                                break;
                            case 2:
                                notificationsShown = ((Integer) value).intValue();
                                break;
                            default:
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown value found: ");
                                stringBuilder.append(valueName[0]);
                                throw new XmlPullParserException(stringBuilder.toString());
                        }
                    }
                }
                i = -1;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
            }
            throw new XmlPullParserException("Missing value name");
        }
        this.mIsActiveDataSource.setData(Boolean.valueOf(isActive));
        this.mIsOnboardedDataSource.setData(Boolean.valueOf(isOnboarded));
        this.mNotificationsDataSource.setData(Integer.valueOf(notificationsShown));
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0040 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0063  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0059  */
    /* JADX WARNING: Missing block: B:10:0x002f, code:
            if (r5.equals(XML_TAG_SECURITY) == false) goto L_0x003c;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ScanResultMatchInfo parseNetwork(XmlPullParser in, int outerTagDepth) throws IOException, XmlPullParserException {
        ScanResultMatchInfo scanResultMatchInfo = new ScanResultMatchInfo();
        while (!XmlUtil.isNextSectionEnd(in, outerTagDepth)) {
            int i = 1;
            String[] valueName = new String[1];
            Object value = XmlUtil.readCurrentValue(in, valueName);
            if (valueName[0] != null) {
                String str = valueName[0];
                int hashCode = str.hashCode();
                if (hashCode != 2554747) {
                    if (hashCode == 1013767008) {
                    }
                } else if (str.equals("SSID")) {
                    i = 0;
                    switch (i) {
                        case 0:
                            scanResultMatchInfo.networkSsid = (String) value;
                            break;
                        case 1:
                            scanResultMatchInfo.networkType = ((Integer) value).intValue();
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown tag under WakeupConfigStoreData: ");
                            stringBuilder.append(valueName[0]);
                            throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                i = -1;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
            throw new XmlPullParserException("Missing value name");
        }
        return scanResultMatchInfo;
    }

    public void resetData(boolean shared) {
        if (!shared) {
            this.mNetworkDataSource.setData(Collections.emptySet());
            this.mIsActiveDataSource.setData(Boolean.valueOf(false));
            this.mIsOnboardedDataSource.setData(Boolean.valueOf(false));
            this.mNotificationsDataSource.setData(Integer.valueOf(0));
        }
    }

    public String getName() {
        return TAG;
    }

    public boolean supportShareData() {
        return false;
    }
}
