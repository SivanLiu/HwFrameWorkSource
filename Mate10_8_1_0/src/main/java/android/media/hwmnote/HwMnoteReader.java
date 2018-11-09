package android.media.hwmnote;

import java.io.IOException;
import java.io.InputStream;

class HwMnoteReader {
    private static final String TAG = "HwMnoteReader";
    private final HwMnoteInterfaceImpl mInterface;

    HwMnoteReader(HwMnoteInterfaceImpl iRef) {
        this.mInterface = iRef;
    }

    protected HwMnoteData read(InputStream inputStream) throws Exception, IOException {
        HwMnoteParser parser = HwMnoteParser.parse(inputStream, this.mInterface);
        HwMnoteData HwMnoteData = new HwMnoteData(parser.getByteOrder());
        for (int event = parser.next(); event != 3; event = parser.next()) {
            HwMnoteTag tag;
            switch (event) {
                case 0:
                    HwMnoteData.addIfdData(new HwMnoteIfdData(parser.getCurrentIfd()));
                    break;
                case 1:
                    tag = parser.getTag();
                    if (!tag.hasValue()) {
                        parser.registerForTagValue(tag);
                        break;
                    }
                    HwMnoteData.getIfdData(tag.getIfd()).setTag(tag);
                    break;
                case 2:
                    tag = parser.getTag();
                    if (tag.getDataType() == (short) 7) {
                        parser.readFullTagValue(tag);
                    }
                    HwMnoteData.getIfdData(tag.getIfd()).setTag(tag);
                    break;
                default:
                    break;
            }
        }
        return HwMnoteData;
    }
}
