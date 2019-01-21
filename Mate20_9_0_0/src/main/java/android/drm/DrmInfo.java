package android.drm;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class DrmInfo {
    private final HashMap<String, Object> mAttributes = new HashMap();
    private byte[] mData;
    private final int mInfoType;
    private final String mMimeType;

    public DrmInfo(int infoType, byte[] data, String mimeType) {
        this.mInfoType = infoType;
        this.mMimeType = mimeType;
        this.mData = data;
        if (!isValid()) {
            String msg = new StringBuilder();
            msg.append("infoType: ");
            msg.append(infoType);
            msg.append(",mimeType: ");
            msg.append(mimeType);
            msg.append(",data: ");
            msg.append(Arrays.toString(data));
            throw new IllegalArgumentException(msg.toString());
        }
    }

    public DrmInfo(int infoType, String path, String mimeType) {
        this.mInfoType = infoType;
        this.mMimeType = mimeType;
        try {
            this.mData = DrmUtils.readBytes(path);
        } catch (IOException e) {
            this.mData = null;
        }
        if (!isValid()) {
            String msg = new StringBuilder();
            msg.append("infoType: ");
            msg.append(infoType);
            msg.append(",mimeType: ");
            msg.append(mimeType);
            msg.append(",data: ");
            msg.append(Arrays.toString(this.mData));
            msg = msg.toString();
            throw new IllegalArgumentException();
        }
    }

    public void put(String key, Object value) {
        this.mAttributes.put(key, value);
    }

    public Object get(String key) {
        return this.mAttributes.get(key);
    }

    public Iterator<String> keyIterator() {
        return this.mAttributes.keySet().iterator();
    }

    public Iterator<Object> iterator() {
        return this.mAttributes.values().iterator();
    }

    public byte[] getData() {
        return this.mData;
    }

    public String getMimeType() {
        return this.mMimeType;
    }

    public int getInfoType() {
        return this.mInfoType;
    }

    boolean isValid() {
        return (this.mMimeType == null || this.mMimeType.equals("") || this.mData == null || this.mData.length <= 0 || !DrmInfoRequest.isValidType(this.mInfoType)) ? false : true;
    }
}
