package com.android.server.hidata.hicure;

import android.util.Log;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DnsProbe {
    private static final String TAG = "DnsProbe";
    private int MAX_DOMAIN_CNT = 4;
    final CountDownLatch latch = new CountDownLatch(1);
    private DNSLookupThread[] mDnsLookup = new DNSLookupThread[this.MAX_DOMAIN_CNT];
    private int mdelaytime;
    private int mnetid;

    private class DNSLookupThread extends Thread {
        private InetAddress addr;
        private String hostname;

        public DNSLookupThread(String hostname) {
            this.hostname = hostname;
        }

        public void run() {
            try {
                String str = DnsProbe.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hostname = ");
                stringBuilder.append(this.hostname);
                Log.d(str, stringBuilder.toString());
                set(InetAddress.getByNameOnNet(this.hostname, DnsProbe.this.mnetid));
                String str2 = DnsProbe.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("hostname/addr:");
                stringBuilder2.append(getIP());
                Log.d(str2, stringBuilder2.toString());
                DnsProbe.this.latch.countDown();
            } catch (UnknownHostException e) {
                Log.d(DnsProbe.TAG, "UnknownHostException");
            }
        }

        private synchronized void set(InetAddress addr) {
            this.addr = addr;
        }

        public synchronized String getIP() {
            if (this.addr == null) {
                return null;
            }
            return this.addr.toString();
        }
    }

    public DnsProbe(int time, int netId) {
        this.mdelaytime = time;
        this.mnetid = netId;
    }

    public boolean isDnsAvailable(String Domain) {
        DNSLookupThread dnslookup = new DNSLookupThread(Domain);
        try {
            dnslookup.start();
            dnslookup.join((long) this.mdelaytime);
        } catch (IllegalThreadStateException e) {
            Log.e(TAG, "IllegalThreadStateException");
        } catch (InterruptedException e2) {
            Log.e(TAG, "InterruptedException");
        }
        if (dnslookup.getIP() != null) {
            return true;
        }
        return false;
    }

    public boolean isDnsAvailable(String[] Domains) {
        int domainCnt = Domains.length;
        int threadCnt = domainCnt > this.MAX_DOMAIN_CNT ? this.MAX_DOMAIN_CNT : domainCnt;
        int i = 0;
        while (i < threadCnt) {
            try {
                this.mDnsLookup[i] = new DNSLookupThread(Domains[i]);
                this.mDnsLookup[i].start();
                i++;
            } catch (IllegalThreadStateException e) {
                Log.e(TAG, "IllegalThreadStateException");
            } catch (InterruptedException e2) {
                Log.e(TAG, "InterruptedException");
            }
        }
        this.latch.await((long) this.mdelaytime, TimeUnit.MILLISECONDS);
        Log.d(TAG, "await over");
        for (i = 0; i < domainCnt; i++) {
            if (this.mDnsLookup[i].getIP() != null) {
                return true;
            }
        }
        return false;
    }
}
