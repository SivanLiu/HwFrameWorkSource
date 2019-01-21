package com.huawei.okhttp3;

import com.huawei.okhttp3.internal.connection.RealConnection;
import java.util.ArrayList;
import java.util.List;

public final class Http2Host {
    private final Address address;
    private final List<RealConnection> connections = new ArrayList();
    private int searchIndex = 0;

    public Http2Host(Address address) {
        this.address = address;
    }

    public Address address() {
        return this.address;
    }

    public void addConnection(RealConnection connection) {
        if (!this.connections.contains(connection)) {
            this.connections.add(connection);
        }
    }

    public void removeConnection(RealConnection connection) {
        this.connections.remove(connection);
    }

    public boolean isEmpty() {
        return this.connections.isEmpty();
    }

    public RealConnection getAvailableConnection() {
        return getConnectionWithLeastAllocation();
    }

    @Deprecated
    private RealConnection getConnectionRoundRobin() {
        if (this.connections.isEmpty()) {
            return null;
        }
        RealConnection connection;
        int i = 0;
        if (this.searchIndex >= this.connections.size()) {
            this.searchIndex = 0;
        }
        int connectionSize = this.connections.size();
        int i2 = this.searchIndex;
        while (i2 < connectionSize) {
            connection = (RealConnection) this.connections.get(i2);
            if (connection.allocations.size() >= connection.allocationLimit || connection.noNewStreams) {
                i2++;
            } else {
                this.searchIndex++;
                return connection;
            }
        }
        while (true) {
            i2 = i;
            if (i2 >= this.searchIndex) {
                return null;
            }
            connection = (RealConnection) this.connections.get(i2);
            if (connection.allocations.size() >= connection.allocationLimit || connection.noNewStreams) {
                i = i2 + 1;
            } else {
                this.searchIndex++;
                return connection;
            }
        }
    }

    private RealConnection getConnectionWithLeastAllocation() {
        RealConnection connection = null;
        int min_allocation_count = Integer.MAX_VALUE;
        for (RealConnection c : this.connections) {
            int count = c.allocations.size();
            if (count < c.allocationLimit && !c.noNewStreams && count < min_allocation_count) {
                connection = c;
                min_allocation_count = count;
            }
        }
        return connection;
    }
}
