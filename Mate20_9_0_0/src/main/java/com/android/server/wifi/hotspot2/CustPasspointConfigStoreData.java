package com.android.server.wifi.hotspot2;

import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.hotspot2.PasspointConfigStoreData.DataSource;
import com.android.server.wifi.util.XmlUtil;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class CustPasspointConfigStoreData extends PasspointConfigStoreData {
    CustPasspointConfigStoreData(WifiKeyStore keyStore, SIMAccessor simAccessor, DataSource dataSource) {
        super(keyStore, simAccessor, dataSource);
    }

    public void deserializeCustData(XmlPullParser in, int outerTagDepth) throws XmlPullParserException, IOException {
        String[] headerName = new String[1];
        while (XmlUtil.gotoNextSectionOrEnd(in, headerName, outerTagDepth)) {
            String str = headerName[0];
            int i = -1;
            if (str.hashCode() == -254992817 && str.equals("ProviderList")) {
                i = 0;
            }
            if (i == 0) {
                PasspointManager.addProviders(deserializeProviderList(in, outerTagDepth + 1));
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown Passpoint user store data ");
                stringBuilder.append(headerName[0]);
                throw new XmlPullParserException(stringBuilder.toString());
            }
        }
    }
}
