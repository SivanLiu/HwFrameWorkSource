package com.huawei.okhttp3.internal.connection;

import com.huawei.okhttp3.internal.platform.Platform;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

final class ConcurrentConnect {
    private final ArrayList<InetSocketAddress> addressList;
    private int attemptDelayMs;
    private volatile boolean cancelled = false;
    private final ArrayList<ChannelWrapper> channelList = new ArrayList();
    private final ArrayList<InetSocketAddress> failedAddressList = new ArrayList();
    private Selector selector;

    private final class ChannelWrapper {
        SocketChannel channel;
        long expiredTimeStamp;
        InetSocketAddress inetSocketAddress;

        private ChannelWrapper() {
        }

        void open(InetSocketAddress inetSocketAddress, long timeoutMs) throws IOException {
            this.inetSocketAddress = inetSocketAddress;
            this.expiredTimeStamp = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + timeoutMs;
            this.channel = SocketChannel.open();
            this.channel.configureBlocking(false);
            this.channel.connect(inetSocketAddress);
        }

        void close() {
            try {
                this.channel.close();
            } catch (IOException e) {
                ConcurrentConnect.this.logMessage("Socket channel close error", e);
            }
        }

        boolean isExpired() {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) >= this.expiredTimeStamp;
        }

        long getRemainingMs() {
            long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            if (now >= this.expiredTimeStamp) {
                return 0;
            }
            return this.expiredTimeStamp - now;
        }
    }

    public ConcurrentConnect(ArrayList<InetSocketAddress> addressList, int attemptDelayMs) {
        this.addressList = new ArrayList(addressList);
        this.attemptDelayMs = attemptDelayMs;
    }

    public Socket getConnectedSocket(long timeoutMs) {
        SocketChannel channel = null;
        try {
            channel = getConnectedSocketChannel(timeoutMs);
            if (channel != null) {
                try {
                    channel.configureBlocking(true);
                } catch (IOException e) {
                    try {
                        channel.close();
                    } catch (IOException ce) {
                        logMessage("Socket channel close error", ce);
                    }
                    channel = null;
                }
            }
        } catch (ClosedSelectorException e2) {
            logMessage("Selector is already closed", e2);
        }
        clearResource();
        if (channel != null) {
            return channel.socket();
        }
        return null;
    }

    public ArrayList<InetSocketAddress> failedAddressList() {
        return this.failedAddressList;
    }

    public void cancel() {
        if (this.selector != null) {
            try {
                this.cancelled = true;
                this.selector.close();
            } catch (IOException e) {
                logMessage("Selector close error", e);
            }
        }
    }

    private void logMessage(String message, Throwable e) {
        Platform.get().log(4, message, e);
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0083  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0076  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00a1  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x009f A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private SocketChannel getConnectedSocketChannel(long timeoutMs) {
        SocketChannel socketChannel;
        int addressListCount = this.addressList.size();
        long fixTimeoutMs = timeoutMs;
        if (addressListCount == 0) {
            return null;
        }
        int addressListCount2;
        try {
            SocketChannel selectedChannel;
            this.selector = Selector.open();
            long pendingAttemptDelayMs = (long) this.attemptDelayMs;
            long timeoutMs2 = timeoutMs - ((long) this.attemptDelayMs);
            long timeElapsed = 0;
            SocketChannel selectedChannel2 = null;
            SocketChannel selectedChannel3 = true;
            while (!this.cancelled) {
                long wait;
                long start;
                if (this.addressList.size() <= 0 || selectedChannel == null) {
                    socketChannel = selectedChannel2;
                    wait = this.channelList.size() <= 0 ? ((ChannelWrapper) this.channelList.get(0)).getRemainingMs() : timeoutMs2;
                    if (this.addressList.size() > 0 && wait > pendingAttemptDelayMs) {
                        wait = pendingAttemptDelayMs;
                    }
                    start = System.nanoTime();
                    SocketChannel socketChannel2;
                    try {
                        this.selector.select(wait);
                        if (this.cancelled) {
                            timeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                            selectedChannel = findConnectedChannel();
                            if (selectedChannel == null) {
                                checkForTimeout(timeElapsed);
                                if (this.failedAddressList.size() != addressListCount && timeElapsed < timeoutMs2) {
                                    timeoutMs2 -= timeElapsed;
                                    boolean toOpenChannel;
                                    if (this.addressList.size() <= 0) {
                                        toOpenChannel = selectedChannel3;
                                        addressListCount2 = addressListCount;
                                    } else if (timeElapsed >= pendingAttemptDelayMs) {
                                        toOpenChannel = selectedChannel3;
                                        addressListCount2 = addressListCount;
                                        pendingAttemptDelayMs = ((long) this.attemptDelayMs) - ((timeElapsed - pendingAttemptDelayMs) % ((long) this.attemptDelayMs));
                                        selectedChannel3 = true;
                                    } else {
                                        socketChannel2 = selectedChannel3;
                                        addressListCount2 = addressListCount;
                                        pendingAttemptDelayMs -= timeElapsed;
                                    }
                                    selectedChannel2 = selectedChannel;
                                    addressListCount = addressListCount2;
                                }
                            }
                            addressListCount2 = addressListCount;
                            break;
                        }
                        return null;
                    } catch (IOException e) {
                        socketChannel2 = selectedChannel3;
                        addressListCount2 = addressListCount;
                        return null;
                    }
                }
                InetSocketAddress inetSocketAddress = (InetSocketAddress) this.addressList.remove(0);
                try {
                    prepareSocketChannel(inetSocketAddress, fixTimeoutMs);
                    timeoutMs2 += (long) this.attemptDelayMs;
                    selectedChannel3 = null;
                    if (this.channelList.size() <= 0) {
                    }
                    wait = pendingAttemptDelayMs;
                    start = System.nanoTime();
                    this.selector.select(wait);
                    if (this.cancelled) {
                    }
                } catch (IOException e2) {
                    socketChannel = selectedChannel2;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to parepare socket channel for ");
                    stringBuilder.append(inetSocketAddress.toString());
                    logMessage(stringBuilder.toString(), e2);
                    this.failedAddressList.add(inetSocketAddress);
                    selectedChannel2 = socketChannel;
                }
            }
            selectedChannel = selectedChannel2;
            return selectedChannel;
        } catch (IOException e3) {
            addressListCount2 = addressListCount;
            return 0;
        }
    }

    private void prepareSocketChannel(InetSocketAddress inetSocketAddress, long fixTimeoutMs) throws IOException {
        ChannelWrapper channelWrapper = new ChannelWrapper();
        channelWrapper.open(inetSocketAddress, fixTimeoutMs);
        channelWrapper.channel.register(this.selector, 8).attach(channelWrapper);
        this.channelList.add(channelWrapper);
    }

    private void handleFailedChannel(ChannelWrapper channelWrapper) {
        this.failedAddressList.add(channelWrapper.inetSocketAddress);
        this.channelList.remove(channelWrapper);
        channelWrapper.close();
    }

    private void checkForTimeout(long timeElapsed) {
        while (this.channelList.size() > 0) {
            ChannelWrapper channelWrapper = (ChannelWrapper) this.channelList.get(0);
            if (channelWrapper.isExpired()) {
                handleFailedChannel(channelWrapper);
            } else {
                return;
            }
        }
    }

    private SocketChannel findConnectedChannel() {
        Iterator<SelectionKey> keyIterator = this.selector.selectedKeys().iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = (SelectionKey) keyIterator.next();
            keyIterator.remove();
            if (key.isConnectable()) {
                ChannelWrapper channelWrapper = (ChannelWrapper) key.attachment();
                try {
                    SocketChannel channel = channelWrapper.channel;
                    if (channel.finishConnect()) {
                        key.cancel();
                        SocketChannel selectedChannel = channel;
                        this.channelList.remove(channelWrapper);
                        return selectedChannel;
                    }
                } catch (IOException e) {
                    key.cancel();
                    handleFailedChannel(channelWrapper);
                }
            }
        }
        return null;
    }

    private void clearResource() {
        Iterator it = this.channelList.iterator();
        while (it.hasNext()) {
            ((ChannelWrapper) it.next()).close();
        }
        this.channelList.clear();
        if (this.selector != null) {
            try {
                this.selector.close();
                this.selector = null;
            } catch (IOException e) {
                logMessage("Selector close error", e);
            }
        }
    }
}
