package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DSDigest extends AManagedObject {
    public static final Creator<DSDigest> CREATOR = new Creator<DSDigest>() {
        public DSDigest createFromParcel(Parcel in) {
            return new DSDigest(in);
        }

        public DSDigest[] newArray(int size) {
            return new DSDigest[size];
        }
    };
    private String comeFrom;
    private String createdtime;
    private Integer deleted;
    private String excerpt;
    private String extra;
    private String htmlDigest;
    private String htmlPath;
    private Integer id;
    private Integer isDownload = Integer.valueOf(0);
    private Integer isImgLoaded = Integer.valueOf(0);
    private String isLoaded;
    private String isMhtHastitle;
    private Integer isUpload = Integer.valueOf(0);
    private String localUrl;
    private String mhtUtl;
    private String pageUrl;
    private String params;
    private String pkgName;
    private String reserved0;
    private String reserved1;
    private String reserved2;
    private String reserved3;
    private String reserved4;
    private String reserved5;
    private String serverId;
    private String source;
    private String sourceTime;
    private Integer syncCount = Integer.valueOf(0);
    private String thumbnail;
    private String thumbnailUrl;
    private String title;
    private String uniqueFlag;

    public DSDigest(Cursor cursor) {
        Integer num;
        Integer num2 = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        if (cursor.isNull(1)) {
            num = null;
        } else {
            num = Integer.valueOf(cursor.getInt(1));
        }
        this.id = num;
        this.title = cursor.getString(2);
        this.pageUrl = cursor.getString(3);
        this.serverId = cursor.getString(4);
        this.mhtUtl = cursor.getString(5);
        this.thumbnail = cursor.getString(6);
        this.htmlPath = cursor.getString(7);
        this.createdtime = cursor.getString(8);
        this.comeFrom = cursor.getString(9);
        this.localUrl = cursor.getString(10);
        this.excerpt = cursor.getString(11);
        this.uniqueFlag = cursor.getString(12);
        this.isLoaded = cursor.getString(13);
        this.deleted = cursor.isNull(14) ? null : Integer.valueOf(cursor.getInt(14));
        this.thumbnailUrl = cursor.getString(15);
        this.isImgLoaded = cursor.isNull(16) ? null : Integer.valueOf(cursor.getInt(16));
        this.isUpload = cursor.isNull(17) ? null : Integer.valueOf(cursor.getInt(17));
        this.isDownload = cursor.isNull(18) ? null : Integer.valueOf(cursor.getInt(18));
        if (!cursor.isNull(19)) {
            num2 = Integer.valueOf(cursor.getInt(19));
        }
        this.syncCount = num2;
        this.extra = cursor.getString(20);
        this.pkgName = cursor.getString(21);
        this.source = cursor.getString(22);
        this.sourceTime = cursor.getString(23);
        this.params = cursor.getString(24);
        this.isMhtHastitle = cursor.getString(25);
        this.htmlDigest = cursor.getString(26);
        this.reserved0 = cursor.getString(27);
        this.reserved1 = cursor.getString(28);
        this.reserved2 = cursor.getString(29);
        this.reserved3 = cursor.getString(30);
        this.reserved4 = cursor.getString(31);
        this.reserved5 = cursor.getString(32);
    }

    public DSDigest(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.title = in.readByte() == (byte) 0 ? null : in.readString();
        this.pageUrl = in.readByte() == (byte) 0 ? null : in.readString();
        this.serverId = in.readByte() == (byte) 0 ? null : in.readString();
        this.mhtUtl = in.readByte() == (byte) 0 ? null : in.readString();
        this.thumbnail = in.readByte() == (byte) 0 ? null : in.readString();
        this.htmlPath = in.readByte() == (byte) 0 ? null : in.readString();
        this.createdtime = in.readByte() == (byte) 0 ? null : in.readString();
        this.comeFrom = in.readByte() == (byte) 0 ? null : in.readString();
        this.localUrl = in.readByte() == (byte) 0 ? null : in.readString();
        this.excerpt = in.readByte() == (byte) 0 ? null : in.readString();
        this.uniqueFlag = in.readByte() == (byte) 0 ? null : in.readString();
        this.isLoaded = in.readByte() == (byte) 0 ? null : in.readString();
        this.deleted = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.thumbnailUrl = in.readByte() == (byte) 0 ? null : in.readString();
        this.isImgLoaded = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.isUpload = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.isDownload = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.syncCount = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.extra = in.readByte() == (byte) 0 ? null : in.readString();
        this.pkgName = in.readByte() == (byte) 0 ? null : in.readString();
        this.source = in.readByte() == (byte) 0 ? null : in.readString();
        this.sourceTime = in.readByte() == (byte) 0 ? null : in.readString();
        this.params = in.readByte() == (byte) 0 ? null : in.readString();
        this.isMhtHastitle = in.readByte() == (byte) 0 ? null : in.readString();
        this.htmlDigest = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved0 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved3 = in.readByte() == (byte) 0 ? null : in.readString();
        this.reserved4 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved5 = str;
    }

    private DSDigest(Integer id, String title, String pageUrl, String serverId, String mhtUtl, String thumbnail, String htmlPath, String createdtime, String comeFrom, String localUrl, String excerpt, String uniqueFlag, String isLoaded, Integer deleted, String thumbnailUrl, Integer isImgLoaded, Integer isUpload, Integer isDownload, Integer syncCount, String extra, String pkgName, String source, String sourceTime, String params, String isMhtHastitle, String htmlDigest, String reserved0, String reserved1, String reserved2, String reserved3, String reserved4, String reserved5) {
        this.id = id;
        this.title = title;
        this.pageUrl = pageUrl;
        this.serverId = serverId;
        this.mhtUtl = mhtUtl;
        this.thumbnail = thumbnail;
        this.htmlPath = htmlPath;
        this.createdtime = createdtime;
        this.comeFrom = comeFrom;
        this.localUrl = localUrl;
        this.excerpt = excerpt;
        this.uniqueFlag = uniqueFlag;
        this.isLoaded = isLoaded;
        this.deleted = deleted;
        this.thumbnailUrl = thumbnailUrl;
        this.isImgLoaded = isImgLoaded;
        this.isUpload = isUpload;
        this.isDownload = isDownload;
        this.syncCount = syncCount;
        this.extra = extra;
        this.pkgName = pkgName;
        this.source = source;
        this.sourceTime = sourceTime;
        this.params = params;
        this.isMhtHastitle = isMhtHastitle;
        this.htmlDigest = htmlDigest;
        this.reserved0 = reserved0;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.reserved3 = reserved3;
        this.reserved4 = reserved4;
        this.reserved5 = reserved5;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
        setValue();
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
        setValue();
    }

    public String getPageUrl() {
        return this.pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
        setValue();
    }

    public String getServerId() {
        return this.serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
        setValue();
    }

    public String getMhtUtl() {
        return this.mhtUtl;
    }

    public void setMhtUtl(String mhtUtl) {
        this.mhtUtl = mhtUtl;
        setValue();
    }

    public String getThumbnail() {
        return this.thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
        setValue();
    }

    public String getHtmlPath() {
        return this.htmlPath;
    }

    public void setHtmlPath(String htmlPath) {
        this.htmlPath = htmlPath;
        setValue();
    }

    public String getCreatedtime() {
        return this.createdtime;
    }

    public void setCreatedtime(String createdtime) {
        this.createdtime = createdtime;
        setValue();
    }

    public String getComeFrom() {
        return this.comeFrom;
    }

    public void setComeFrom(String comeFrom) {
        this.comeFrom = comeFrom;
        setValue();
    }

    public String getLocalUrl() {
        return this.localUrl;
    }

    public void setLocalUrl(String localUrl) {
        this.localUrl = localUrl;
        setValue();
    }

    public String getExcerpt() {
        return this.excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
        setValue();
    }

    public String getUniqueFlag() {
        return this.uniqueFlag;
    }

    public void setUniqueFlag(String uniqueFlag) {
        this.uniqueFlag = uniqueFlag;
        setValue();
    }

    public String getIsLoaded() {
        return this.isLoaded;
    }

    public void setIsLoaded(String isLoaded) {
        this.isLoaded = isLoaded;
        setValue();
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
        setValue();
    }

    public String getThumbnailUrl() {
        return this.thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        setValue();
    }

    public Integer getIsImgLoaded() {
        return this.isImgLoaded;
    }

    public void setIsImgLoaded(Integer isImgLoaded) {
        this.isImgLoaded = isImgLoaded;
        setValue();
    }

    public Integer getIsUpload() {
        return this.isUpload;
    }

    public void setIsUpload(Integer isUpload) {
        this.isUpload = isUpload;
        setValue();
    }

    public Integer getIsDownload() {
        return this.isDownload;
    }

    public void setIsDownload(Integer isDownload) {
        this.isDownload = isDownload;
        setValue();
    }

    public Integer getSyncCount() {
        return this.syncCount;
    }

    public void setSyncCount(Integer syncCount) {
        this.syncCount = syncCount;
        setValue();
    }

    public String getExtra() {
        return this.extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
        setValue();
    }

    public String getPkgName() {
        return this.pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
        setValue();
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
        setValue();
    }

    public String getSourceTime() {
        return this.sourceTime;
    }

    public void setSourceTime(String sourceTime) {
        this.sourceTime = sourceTime;
        setValue();
    }

    public String getParams() {
        return this.params;
    }

    public void setParams(String params) {
        this.params = params;
        setValue();
    }

    public String getIsMhtHastitle() {
        return this.isMhtHastitle;
    }

    public void setIsMhtHastitle(String isMhtHastitle) {
        this.isMhtHastitle = isMhtHastitle;
        setValue();
    }

    public String getHtmlDigest() {
        return this.htmlDigest;
    }

    public void setHtmlDigest(String htmlDigest) {
        this.htmlDigest = htmlDigest;
        setValue();
    }

    public String getReserved0() {
        return this.reserved0;
    }

    public void setReserved0(String reserved0) {
        this.reserved0 = reserved0;
        setValue();
    }

    public String getReserved1() {
        return this.reserved1;
    }

    public void setReserved1(String reserved1) {
        this.reserved1 = reserved1;
        setValue();
    }

    public String getReserved2() {
        return this.reserved2;
    }

    public void setReserved2(String reserved2) {
        this.reserved2 = reserved2;
        setValue();
    }

    public String getReserved3() {
        return this.reserved3;
    }

    public void setReserved3(String reserved3) {
        this.reserved3 = reserved3;
        setValue();
    }

    public String getReserved4() {
        return this.reserved4;
    }

    public void setReserved4(String reserved4) {
        this.reserved4 = reserved4;
        setValue();
    }

    public String getReserved5() {
        return this.reserved5;
    }

    public void setReserved5(String reserved5) {
        this.reserved5 = reserved5;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.title != null) {
            out.writeByte((byte) 1);
            out.writeString(this.title);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.pageUrl != null) {
            out.writeByte((byte) 1);
            out.writeString(this.pageUrl);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.serverId != null) {
            out.writeByte((byte) 1);
            out.writeString(this.serverId);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mhtUtl != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mhtUtl);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.thumbnail != null) {
            out.writeByte((byte) 1);
            out.writeString(this.thumbnail);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.htmlPath != null) {
            out.writeByte((byte) 1);
            out.writeString(this.htmlPath);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.createdtime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.createdtime);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.comeFrom != null) {
            out.writeByte((byte) 1);
            out.writeString(this.comeFrom);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.localUrl != null) {
            out.writeByte((byte) 1);
            out.writeString(this.localUrl);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.excerpt != null) {
            out.writeByte((byte) 1);
            out.writeString(this.excerpt);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.uniqueFlag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.uniqueFlag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isLoaded != null) {
            out.writeByte((byte) 1);
            out.writeString(this.isLoaded);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.deleted != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.deleted.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.thumbnailUrl != null) {
            out.writeByte((byte) 1);
            out.writeString(this.thumbnailUrl);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isImgLoaded != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.isImgLoaded.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isUpload != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.isUpload.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isDownload != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.isDownload.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.syncCount != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.syncCount.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.extra != null) {
            out.writeByte((byte) 1);
            out.writeString(this.extra);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.pkgName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.pkgName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.source != null) {
            out.writeByte((byte) 1);
            out.writeString(this.source);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.sourceTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.sourceTime);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.params != null) {
            out.writeByte((byte) 1);
            out.writeString(this.params);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isMhtHastitle != null) {
            out.writeByte((byte) 1);
            out.writeString(this.isMhtHastitle);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.htmlDigest != null) {
            out.writeByte((byte) 1);
            out.writeString(this.htmlDigest);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved0 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved0);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved3 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved3);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved4 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved4);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved5 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved5);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<DSDigest> getHelper() {
        return DSDigestHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.DSDigest";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DSDigest { id: ").append(this.id);
        sb.append(", title: ").append(this.title);
        sb.append(", pageUrl: ").append(this.pageUrl);
        sb.append(", serverId: ").append(this.serverId);
        sb.append(", mhtUtl: ").append(this.mhtUtl);
        sb.append(", thumbnail: ").append(this.thumbnail);
        sb.append(", htmlPath: ").append(this.htmlPath);
        sb.append(", createdtime: ").append(this.createdtime);
        sb.append(", comeFrom: ").append(this.comeFrom);
        sb.append(", localUrl: ").append(this.localUrl);
        sb.append(", excerpt: ").append(this.excerpt);
        sb.append(", uniqueFlag: ").append(this.uniqueFlag);
        sb.append(", isLoaded: ").append(this.isLoaded);
        sb.append(", deleted: ").append(this.deleted);
        sb.append(", thumbnailUrl: ").append(this.thumbnailUrl);
        sb.append(", isImgLoaded: ").append(this.isImgLoaded);
        sb.append(", isUpload: ").append(this.isUpload);
        sb.append(", isDownload: ").append(this.isDownload);
        sb.append(", syncCount: ").append(this.syncCount);
        sb.append(", extra: ").append(this.extra);
        sb.append(", pkgName: ").append(this.pkgName);
        sb.append(", source: ").append(this.source);
        sb.append(", sourceTime: ").append(this.sourceTime);
        sb.append(", params: ").append(this.params);
        sb.append(", isMhtHastitle: ").append(this.isMhtHastitle);
        sb.append(", htmlDigest: ").append(this.htmlDigest);
        sb.append(", reserved0: ").append(this.reserved0);
        sb.append(", reserved1: ").append(this.reserved1);
        sb.append(", reserved2: ").append(this.reserved2);
        sb.append(", reserved3: ").append(this.reserved3);
        sb.append(", reserved4: ").append(this.reserved4);
        sb.append(", reserved5: ").append(this.reserved5);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.10";
    }

    public int getDatabaseVersionCode() {
        return 10;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
