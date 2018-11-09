package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbInterfaceDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;

public final class UsbDescriptorsInterfaceNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsInterfaceNode";
    private final ArrayList<UsbDescriptorsACInterfaceNode> mACInterfaceNodes = new ArrayList();
    private final ArrayList<UsbDescriptorsEndpointNode> mEndpointNodes = new ArrayList();
    private final UsbInterfaceDescriptor mInterfaceDescriptor;

    public UsbDescriptorsInterfaceNode(UsbInterfaceDescriptor interfaceDescriptor) {
        this.mInterfaceDescriptor = interfaceDescriptor;
    }

    public void addEndpointNode(UsbDescriptorsEndpointNode endpointNode) {
        this.mEndpointNodes.add(endpointNode);
    }

    public void addACInterfaceNode(UsbDescriptorsACInterfaceNode acInterfaceNode) {
        this.mACInterfaceNodes.add(acInterfaceNode);
    }

    public void report(ReportCanvas canvas) {
        this.mInterfaceDescriptor.report(canvas);
        if (this.mACInterfaceNodes.size() > 0) {
            canvas.writeParagraph("Audio Class Interfaces", false);
            canvas.openList();
            for (UsbDescriptorsACInterfaceNode node : this.mACInterfaceNodes) {
                node.report(canvas);
            }
            canvas.closeList();
        }
        if (this.mEndpointNodes.size() > 0) {
            canvas.writeParagraph("Endpoints", false);
            canvas.openList();
            for (UsbDescriptorsEndpointNode node2 : this.mEndpointNodes) {
                node2.report(canvas);
            }
            canvas.closeList();
        }
    }
}
