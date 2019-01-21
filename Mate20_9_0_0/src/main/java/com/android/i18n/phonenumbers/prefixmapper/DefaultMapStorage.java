package com.android.i18n.phonenumbers.prefixmapper;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.SortedMap;

class DefaultMapStorage extends PhonePrefixMapStorageStrategy {
    private String[] descriptions;
    private int[] phoneNumberPrefixes;

    public int getPrefix(int index) {
        return this.phoneNumberPrefixes[index];
    }

    public String getDescription(int index) {
        return this.descriptions[index];
    }

    public void readFromSortedMap(SortedMap<Integer, String> sortedPhonePrefixMap) {
        this.numOfEntries = sortedPhonePrefixMap.size();
        this.phoneNumberPrefixes = new int[this.numOfEntries];
        this.descriptions = new String[this.numOfEntries];
        int index = 0;
        for (Integer prefix : sortedPhonePrefixMap.keySet()) {
            int prefix2 = prefix.intValue();
            int index2 = index + 1;
            this.phoneNumberPrefixes[index] = prefix2;
            this.possibleLengths.add(Integer.valueOf(((int) Math.log10((double) prefix2)) + 1));
            index = index2;
        }
        sortedPhonePrefixMap.values().toArray(this.descriptions);
    }

    public void readExternal(ObjectInput objectInput) throws IOException {
        int i;
        this.numOfEntries = objectInput.readInt();
        if (this.phoneNumberPrefixes == null || this.phoneNumberPrefixes.length < this.numOfEntries) {
            this.phoneNumberPrefixes = new int[this.numOfEntries];
        }
        if (this.descriptions == null || this.descriptions.length < this.numOfEntries) {
            this.descriptions = new String[this.numOfEntries];
        }
        int i2 = 0;
        for (i = 0; i < this.numOfEntries; i++) {
            this.phoneNumberPrefixes[i] = objectInput.readInt();
            this.descriptions[i] = objectInput.readUTF();
        }
        i = objectInput.readInt();
        this.possibleLengths.clear();
        while (i2 < i) {
            this.possibleLengths.add(Integer.valueOf(objectInput.readInt()));
            i2++;
        }
    }

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeInt(this.numOfEntries);
        for (int i = 0; i < this.numOfEntries; i++) {
            objectOutput.writeInt(this.phoneNumberPrefixes[i]);
            objectOutput.writeUTF(this.descriptions[i]);
        }
        objectOutput.writeInt(this.possibleLengths.size());
        Iterator it = this.possibleLengths.iterator();
        while (it.hasNext()) {
            objectOutput.writeInt(((Integer) it.next()).intValue());
        }
    }
}
