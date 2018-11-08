package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDeviceConnection;
import com.android.server.usb.descriptors.report.UsbStrings;

public final class UsbBinaryParser {
    private static final boolean LOGGING = false;
    private static final String TAG = "UsbBinaryParser";

    private void dumpDescriptor(ByteStream stream, int length, byte type, StringBuilder builder) {
        builder.append("<p>");
        builder.append("<b> l: ").append(length).append(" t:0x").append(Integer.toHexString(type)).append(" ").append(UsbStrings.getDescriptorName(type)).append("</b><br>");
        for (int index = 2; index < length; index++) {
            builder.append("0x").append(Integer.toHexString(stream.getByte() & 255)).append(" ");
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
