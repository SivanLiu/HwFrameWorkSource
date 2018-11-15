package com.android.server.pm;

import android.content.ComponentName;
import android.content.IntentFilter;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.PreferredComponent.Callbacks;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class PreferredActivity extends IntentFilter implements Callbacks {
    private static final boolean DEBUG_FILTERS = false;
    private static final String TAG = "PreferredActivity";
    final PreferredComponent mPref;

    PreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, boolean always) {
        super(filter);
        this.mPref = new PreferredComponent(this, match, set, activity, always);
    }

    PreferredActivity(XmlPullParser parser) throws XmlPullParserException, IOException {
        this.mPref = new PreferredComponent(this, parser);
    }

    public void writeToXml(XmlSerializer serializer, boolean full) throws IOException {
        this.mPref.writeToXml(serializer, full);
        serializer.startTag(null, "filter");
        super.writeToXml(serializer);
        serializer.endTag(null, "filter");
    }

    public boolean onReadTag(String tagName, XmlPullParser parser) throws XmlPullParserException, IOException {
        if (tagName.equals("filter")) {
            readFromXml(parser);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown element under <preferred-activities>: ");
            stringBuilder.append(parser.getName());
            PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
            XmlUtils.skipCurrentTag(parser);
        }
        return true;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PreferredActivity{0x");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.mPref.mComponent.flattenToShortString());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
