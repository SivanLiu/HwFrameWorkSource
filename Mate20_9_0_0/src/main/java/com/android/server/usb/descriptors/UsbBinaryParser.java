package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDeviceConnection;
import com.android.server.usb.descriptors.report.UsbStrings;

public final class UsbBinaryParser {
    private static final boolean LOGGING = false;
    private static final String TAG = "UsbBinaryParser";

    private void dumpDescriptor(ByteStream stream, int length, byte type, StringBuilder builder) {
        builder.append("<p>");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<b> l: ");
        stringBuilder.append(length);
        stringBuilder.append(" t:0x");
        stringBuilder.append(Integer.toHexString(type));
        stringBuilder.append(" ");
        stringBuilder.append(UsbStrings.getDescriptorName(type));
        stringBuilder.append("</b><br>");
        builder.append(stringBuilder.toString());
        for (int index = 2; index < length; index++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("0x");
            stringBuilder2.append(Integer.toHexString(stream.getByte() & 255));
            stringBuilder2.append(" ");
            builder.append(stringBuilder2.toString());
        }
        builder.append("</p>");
    }

    public void parseDescriptors(UsbDeviceConnection connection, byte[] descriptors, StringBuilder builder) {
        builder.append("<tt>");
        ByteStream stream = new ByteStream(descriptors);
        while (stream.available() > 0) {
            dumpDescriptor(stream, stream.getByte() & 255, stream.getByte(), builder);
        }
        builder.append("</tt>");
    }
}
