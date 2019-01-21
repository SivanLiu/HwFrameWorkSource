package android.nfc.cardemulation;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class AidGroup implements Parcelable {
    public static final Creator<AidGroup> CREATOR = new Creator<AidGroup>() {
        public AidGroup createFromParcel(Parcel source) {
            String category = source.readString();
            int listSize = source.readInt();
            List aidList = new ArrayList();
            if (listSize > 0) {
                source.readStringList(aidList);
            }
            return new AidGroup(aidList, category);
        }

        public AidGroup[] newArray(int size) {
            return new AidGroup[size];
        }
    };
    public static final int MAX_NUM_AIDS = 256;
    static final String TAG = "AidGroup";
    protected List<String> aids;
    protected String category;
    protected String description;

    public AidGroup(List<String> aids, String category) {
        if (aids == null || aids.size() == 0) {
            throw new IllegalArgumentException("No AIDS in AID group.");
        } else if (aids.size() <= 256) {
            for (String aid : aids) {
                if (!CardEmulation.isValidAid(aid)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AID ");
                    stringBuilder.append(aid);
                    stringBuilder.append(" is not a valid AID.");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (isValidCategory(category)) {
                this.category = category;
            } else {
                this.category = "other";
            }
            this.aids = new ArrayList(aids.size());
            for (String aid2 : aids) {
                this.aids.add(aid2.toUpperCase());
            }
            this.description = null;
        } else {
            throw new IllegalArgumentException("Too many AIDs in AID group.");
        }
    }

    AidGroup(String category, String description) {
        this.aids = new ArrayList();
        this.category = category;
        this.description = description;
    }

    public String getCategory() {
        return this.category;
    }

    public List<String> getAids() {
        return this.aids;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Category: ");
        stringBuilder.append(this.category);
        stringBuilder.append(", AIDs:");
        StringBuilder out = new StringBuilder(stringBuilder.toString());
        for (String aid : this.aids) {
            out.append(aid);
            out.append(", ");
        }
        return out.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.category);
        dest.writeInt(this.aids.size());
        if (this.aids.size() > 0) {
            dest.writeStringList(this.aids);
        }
    }

    public static AidGroup createFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        String category = null;
        List aids = new ArrayList();
        AidGroup group = null;
        boolean inGroup = false;
        int eventType = parser.getEventType();
        int minDepth = parser.getDepth();
        while (eventType != 1 && parser.getDepth() >= minDepth) {
            String tagName = parser.getName();
            String aid;
            if (eventType != 2) {
                if (eventType == 3 && tagName.equals("aid-group") && inGroup && aids.size() > 0) {
                    group = new AidGroup(aids, category);
                    break;
                }
            } else if (tagName.equals("aid")) {
                if (inGroup) {
                    aid = parser.getAttributeValue(null, "value");
                    if (aid != null) {
                        aids.add(aid.toUpperCase());
                    }
                } else {
                    Log.d(TAG, "Ignoring <aid> tag while not in group");
                }
            } else if (tagName.equals("aid-group")) {
                category = parser.getAttributeValue(null, "category");
                if (category == null) {
                    Log.e(TAG, "<aid-group> tag without valid category");
                    return null;
                }
                inGroup = true;
            } else {
                aid = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring unexpected tag: ");
                stringBuilder.append(tagName);
                Log.d(aid, stringBuilder.toString());
            }
            eventType = parser.next();
        }
        return group;
    }

    public void writeAsXml(XmlSerializer out) throws IOException {
        out.startTag(null, "aid-group");
        out.attribute(null, "category", this.category);
        for (String aid : this.aids) {
            out.startTag(null, "aid");
            out.attribute(null, "value", aid);
            out.endTag(null, "aid");
        }
        out.endTag(null, "aid-group");
    }

    static boolean isValidCategory(String category) {
        return CardEmulation.CATEGORY_PAYMENT.equals(category) || "other".equals(category);
    }
}
