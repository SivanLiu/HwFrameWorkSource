package com.android.internal.http.multipart;

public abstract class PartBase extends Part {
    private String charSet;
    private String contentType;
    private String name;
    private String transferEncoding;

    public PartBase(String name, String contentType, String charSet, String transferEncoding) {
        if (name != null) {
            this.name = name;
            this.contentType = contentType;
            this.charSet = charSet;
            this.transferEncoding = transferEncoding;
            return;
        }
        throw new IllegalArgumentException("Name must not be null");
    }

    public String getName() {
        return this.name;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getCharSet() {
        return this.charSet;
    }

    public String getTransferEncoding() {
        return this.transferEncoding;
    }

    public void setCharSet(String charSet) {
        this.charSet = charSet;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setName(String name) {
        if (name != null) {
            this.name = name;
            return;
        }
        throw new IllegalArgumentException("Name must not be null");
    }

    public void setTransferEncoding(String transferEncoding) {
        this.transferEncoding = transferEncoding;
    }
}
