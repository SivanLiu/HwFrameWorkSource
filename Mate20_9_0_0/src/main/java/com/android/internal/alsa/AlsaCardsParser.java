package com.android.internal.alsa;

import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class AlsaCardsParser {
    protected static final boolean DEBUG = false;
    public static final int SCANSTATUS_EMPTY = 2;
    public static final int SCANSTATUS_FAIL = 1;
    public static final int SCANSTATUS_NOTSCANNED = -1;
    public static final int SCANSTATUS_SUCCESS = 0;
    private static final String TAG = "AlsaCardsParser";
    private static final String kAlsaFolderPath = "/proc/asound";
    private static final String kCardsFilePath = "/proc/asound/cards";
    private static final String kDeviceAddressPrefix = "/dev/bus/usb/";
    private static LineTokenizer mTokenizer = new LineTokenizer(" :[]");
    private ArrayList<AlsaCardRecord> mCardRecords = new ArrayList();
    private int mScanStatus = -1;

    public class AlsaCardRecord {
        private static final String TAG = "AlsaCardRecord";
        private static final String kUsbCardKeyStr = "at usb-";
        String mCardDescription = "";
        String mCardName = "";
        int mCardNum = -1;
        String mField1 = "";
        private String mUsbDeviceAddress = null;

        public int getCardNum() {
            return this.mCardNum;
        }

        public String getCardName() {
            return this.mCardName;
        }

        public String getCardDescription() {
            return this.mCardDescription;
        }

        public void setDeviceAddress(String usbDeviceAddress) {
            this.mUsbDeviceAddress = usbDeviceAddress;
        }

        private boolean parse(String line, int lineIndex) {
            boolean isUsb = false;
            int tokenIndex;
            if (lineIndex == 0) {
                tokenIndex = AlsaCardsParser.mTokenizer.nextToken(line, 0);
                int delimIndex = AlsaCardsParser.mTokenizer.nextDelimiter(line, tokenIndex);
                try {
                    this.mCardNum = Integer.parseInt(line.substring(tokenIndex, delimIndex));
                    tokenIndex = AlsaCardsParser.mTokenizer.nextToken(line, delimIndex);
                    delimIndex = AlsaCardsParser.mTokenizer.nextDelimiter(line, tokenIndex);
                    this.mField1 = line.substring(tokenIndex, delimIndex);
                    this.mCardName = line.substring(AlsaCardsParser.mTokenizer.nextToken(line, delimIndex));
                } catch (NumberFormatException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to parse line ");
                    stringBuilder.append(lineIndex);
                    stringBuilder.append(" of ");
                    stringBuilder.append(AlsaCardsParser.kCardsFilePath);
                    stringBuilder.append(": ");
                    stringBuilder.append(line.substring(tokenIndex, delimIndex));
                    Slog.e(str, stringBuilder.toString());
                    return false;
                }
            } else if (lineIndex == 1) {
                tokenIndex = AlsaCardsParser.mTokenizer.nextToken(line, 0);
                if (tokenIndex != -1) {
                    int keyIndex = line.indexOf(kUsbCardKeyStr);
                    if (keyIndex != -1) {
                        isUsb = true;
                    }
                    if (isUsb) {
                        this.mCardDescription = line.substring(tokenIndex, keyIndex - 1);
                    }
                }
            }
            return true;
        }

        boolean isUsb() {
            return this.mUsbDeviceAddress != null;
        }

        public String textFormat() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mCardName);
            stringBuilder.append(" : ");
            stringBuilder.append(this.mCardDescription);
            stringBuilder.append(" [addr:");
            stringBuilder.append(this.mUsbDeviceAddress);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }

        public void log(int listIndex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(listIndex);
            stringBuilder.append(" [");
            stringBuilder.append(this.mCardNum);
            stringBuilder.append(" ");
            stringBuilder.append(this.mCardName);
            stringBuilder.append(" : ");
            stringBuilder.append(this.mCardDescription);
            stringBuilder.append(" usb:");
            stringBuilder.append(isUsb());
            Slog.d(str, stringBuilder.toString());
        }
    }

    public int scan() {
        this.mCardRecords = new ArrayList();
        try {
            FileReader reader = new FileReader(new File(kCardsFilePath));
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = "";
            while (true) {
                String readLine = bufferedReader.readLine();
                line = readLine;
                if (readLine == null) {
                    break;
                }
                AlsaCardRecord cardRecord = new AlsaCardRecord();
                cardRecord.parse(line, 0);
                line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                cardRecord.parse(line, 1);
                int cardNum = cardRecord.mCardNum;
                String cardFolderPath = new StringBuilder();
                cardFolderPath.append("/proc/asound/card");
                cardFolderPath.append(cardNum);
                cardFolderPath = cardFolderPath.toString();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(cardFolderPath);
                stringBuilder.append("/usbbus");
                File usbbusFile = new File(stringBuilder.toString());
                if (usbbusFile.exists()) {
                    FileReader usbbusReader = new FileReader(usbbusFile);
                    String deviceAddress = new BufferedReader(usbbusReader).readLine();
                    if (deviceAddress != null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(kDeviceAddressPrefix);
                        stringBuilder2.append(deviceAddress);
                        cardRecord.setDeviceAddress(stringBuilder2.toString());
                    }
                    usbbusReader.close();
                }
                this.mCardRecords.add(cardRecord);
            }
            reader.close();
            if (this.mCardRecords.size() > 0) {
                this.mScanStatus = 0;
            } else {
                this.mScanStatus = 2;
            }
        } catch (FileNotFoundException e) {
            this.mScanStatus = 1;
        } catch (IOException e2) {
            this.mScanStatus = 1;
        }
        return this.mScanStatus;
    }

    public int getScanStatus() {
        return this.mScanStatus;
    }

    public AlsaCardRecord findCardNumFor(String deviceAddress) {
        Iterator it = this.mCardRecords.iterator();
        while (it.hasNext()) {
            AlsaCardRecord cardRec = (AlsaCardRecord) it.next();
            if (cardRec.isUsb() && cardRec.mUsbDeviceAddress.equals(deviceAddress)) {
                return cardRec;
            }
        }
        return null;
    }

    private void Log(String heading) {
    }
}
