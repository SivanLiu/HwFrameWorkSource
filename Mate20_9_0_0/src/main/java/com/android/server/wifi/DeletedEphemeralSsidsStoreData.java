package com.android.server.wifi;

import com.android.server.wifi.WifiConfigStore.StoreData;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DeletedEphemeralSsidsStoreData implements StoreData {
    private static final String XML_TAG_SECTION_HEADER_DELETED_EPHEMERAL_SSID_LIST = "DeletedEphemeralSSIDList";
    private static final String XML_TAG_SSID_LIST = "SSIDList";
    private Set<String> mSsidList;

    DeletedEphemeralSsidsStoreData() {
    }

    public void serializeData(XmlSerializer out, boolean shared) throws XmlPullParserException, IOException {
        if (shared) {
            throw new XmlPullParserException("Share data not supported");
        } else if (this.mSsidList != null) {
            XmlUtil.writeNextValue(out, XML_TAG_SSID_LIST, this.mSsidList);
        }
    }

    public void deserializeData(XmlPullParser in, int outerTagDepth, boolean shared) throws XmlPullParserException, IOException {
        if (in != null) {
            if (shared) {
                throw new XmlPullParserException("Share data not supported");
            }
            while (!XmlUtil.isNextSectionEnd(in, outerTagDepth)) {
                String[] valueName = new String[1];
                Object value = XmlUtil.readCurrentValue(in, valueName);
                if (valueName[0] != null) {
                    String str = valueName[0];
                    int i = -1;
                    if (str.hashCode() == 1427827385 && str.equals(XML_TAG_SSID_LIST)) {
                        i = 0;
                    }
                    if (i == 0) {
                        this.mSsidList = (Set) value;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown tag under DeletedEphemeralSSIDList: ");
                        stringBuilder.append(valueName[0]);
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                throw new XmlPullParserException("Missing value name");
            }
        }
    }

    public void resetData(boolean shared) {
        if (!shared) {
            this.mSsidList = null;
        }
    }

    public String getName() {
        return XML_TAG_SECTION_HEADER_DELETED_EPHEMERAL_SSID_LIST;
    }

    public boolean supportShareData() {
        return false;
    }

    public Set<String> getSsidList() {
        if (this.mSsidList == null) {
            return new HashSet();
        }
        return this.mSsidList;
    }

    public void setSsidList(Set<String> ssidList) {
        this.mSsidList = ssidList;
    }
}
