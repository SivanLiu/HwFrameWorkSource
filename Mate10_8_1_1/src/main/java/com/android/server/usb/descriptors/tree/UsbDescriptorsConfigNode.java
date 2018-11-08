package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbConfigDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;

public final class UsbDescriptorsConfigNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsConfigNode";
    private final UsbConfigDescriptor mConfigDescriptor;
    private final ArrayList<UsbDescriptorsInterfaceNode> mInterfaceNodes = new ArrayList();

    public UsbDescriptorsConfigNode(UsbConfigDescriptor configDescriptor) {
        this.mConfigDescriptor = configDescriptor;
    }

    public void addInterfaceNode(UsbDescriptorsInterfaceNode interfaceNode) {
        this.mInterfaceNodes.add(interfaceNode);
    }

    public void report(ReportCanvas canvas) {
        this.mConfigDescriptor.report(canvas);
        canvas.openList();
        for (UsbDescriptorsInterfaceNode node : this.mInterfaceNodes) {
            node.report(canvas);
        }
        canvas.closeList();
    }
}
