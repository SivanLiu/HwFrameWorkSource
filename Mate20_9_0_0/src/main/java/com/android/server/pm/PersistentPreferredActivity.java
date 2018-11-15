package com.android.server.pm;

import android.content.ComponentName;
import android.content.IntentFilter;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class PersistentPreferredActivity extends IntentFilter {
    private static final String ATTR_FILTER = "filter";
    private static final String ATTR_NAME = "name";
    private static final boolean DEBUG_FILTERS = false;
    private static final String TAG = "PersistentPreferredActivity";
    final ComponentName mComponent;

    PersistentPreferredActivity(IntentFilter filter, ComponentName activity) {
        super(filter);
        this.mComponent = activity;
    }

    PersistentPreferredActivity(XmlPullParser parser) throws XmlPullParserException, IOException {
        StringBuilder stringBuilder;
        String shortComponent = parser.getAttributeValue(null, "name");
        this.mComponent = ComponentName.unflattenFromString(shortComponent);
        if (this.mComponent == null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error in package manager settings: Bad activity name ");
            stringBuilder2.append(shortComponent);
            stringBuilder2.append(" at ");
            stringBuilder2.append(parser.getPositionDescription());
            PackageManagerService.reportSettingsProblem(5, stringBuilder2.toString());
        }
        int outerDepth = parser.getDepth();
        String tagName = parser.getName();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            tagName = parser.getName();
            if (type != 3) {
                if (type != 4) {
                    if (type != 2) {
                        continue;
                    } else if (tagName.equals(ATTR_FILTER)) {
                        break;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element: ");
                        stringBuilder.append(tagName);
                        stringBuilder.append(" at ");
                        stringBuilder.append(parser.getPositionDescription());
                        PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
        if (tagName.equals(ATTR_FILTER)) {
            readFromXml(parser);
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Missing element filter at ");
        stringBuilder.append(parser.getPositionDescription());
        PackageManagerService.reportSettingsProblem(5, stringBuilder.toString());
        XmlUtils.skipCurrentTag(parser);
    }

    public void writeToXml(XmlSerializer serializer) throws IOException {
        serializer.attribute(null, "name", this.mComponent.flattenToShortString());
        serializer.startTag(null, ATTR_FILTER);
        super.writeToXml(serializer);
        serializer.endTag(null, ATTR_FILTER);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PersistentPreferredActivity{0x");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.mComponent.flattenToShortString());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
