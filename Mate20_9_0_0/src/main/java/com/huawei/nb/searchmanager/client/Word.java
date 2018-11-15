package com.huawei.nb.searchmanager.client;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class Word implements Parcelable {
    public static final Creator<Word> CREATOR = new Creator<Word>() {
        public Word createFromParcel(Parcel in) {
            return new Word(in);
        }

        public Word[] newArray(int size) {
            return new Word[size];
        }
    };
    private int endOffset;
    private int startOffset;
    private String wordText;
    private String wordType;

    public Word() {
        this.wordText = "";
        this.startOffset = 0;
        this.endOffset = 0;
        this.wordType = "word";
    }

    protected Word(Parcel in) {
        this.wordText = in.readString();
        this.startOffset = in.readInt();
        this.endOffset = in.readInt();
        this.wordType = in.readString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.wordText);
        dest.writeInt(this.startOffset);
        dest.writeInt(this.endOffset);
        dest.writeString(this.wordType);
    }

    public String getWordText() {
        return this.wordText;
    }

    public void setWordText(String wordText) {
        this.wordText = wordText;
    }

    public int getStartOffset() {
        return this.startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return this.endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public String getWordType() {
        return this.wordType;
    }

    public void setWordType(String wordType) {
        this.wordType = wordType;
    }

    public int describeContents() {
        return 0;
    }
}
