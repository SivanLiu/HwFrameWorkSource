package com.android.server.wifi;

import android.text.TextUtils;
import com.android.server.wifi.WifiConfigStore.StoreData;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SsidSetStoreData implements StoreData {
    private static final String XML_TAG_SECTION_HEADER_SUFFIX = "ConfigData";
    private static final String XML_TAG_SSID_SET = "SSIDSet";
    private final DataSource mDataSource;
    private final String mTagName;

    public interface DataSource {
        Set<String> getSsids();

        void setSsids(Set<String> set);
    }

    SsidSetStoreData(String name, DataSource dataSource) {
        this.mTagName = name + XML_TAG_SECTION_HEADER_SUFFIX;
        this.mDataSource = dataSource;
    }

    public void serializeData(XmlSerializer out, boolean shared) throws XmlPullParserException, IOException {
        if (shared) {
            throw new XmlPullParserException("Share data not supported");
        }
        Set<String> ssidSet = this.mDataSource.getSsids();
        if (ssidSet != null && (ssidSet.isEmpty() ^ 1) != 0) {
            XmlUtil.writeNextValue(out, XML_TAG_SSID_SET, this.mDataSource.getSsids());
        }
    }

    public void deserializeData(XmlPullParser in, int outerTagDepth, boolean shared) throws XmlPullParserException, IOException {
        if (shared) {
            throw new XmlPullParserException("Share data not supported");
        }
        while (!XmlUtil.isNextSectionEnd(in, outerTagDepth)) {
            String[] valueName = new String[1];
            Object readCurrentValue = XmlUtil.readCurrentValue(in, valueName);
            if (TextUtils.isEmpty(valueName[0])) {
                throw new XmlPullParserException("Missing value name");
            } else if (valueName[0].equals(XML_TAG_SSID_SET)) {
                this.mDataSource.setSsids((Set) readCurrentValue);
            } else {
                throw new XmlPullParserException("Unknown tag under " + this.mTagName + ": " + valueName[0]);
            }
        }
    }

    public void resetData(boolean shared) {
        if (!shared) {
            this.mDataSource.setSsids(new HashSet());
        }
    }

    public String getName() {
        return this.mTagName;
    }

    public boolean supportShareData() {
        return false;
    }
}
