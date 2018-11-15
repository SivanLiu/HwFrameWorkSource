package com.android.server.hidata.hicure;

import android.util.Log;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DnsProbe {
    private static final String TAG = "DnsProbe";
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
                InetAddress add = InetAddress.getByNameOnNet(this.hostname, DnsProbe.this.mnetid);
                String str = DnsProbe.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hostname = ");
                stringBuilder.append(this.hostname);
                Log.d(str, stringBuilder.toString());
                set(add);
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
        for (String domain : Domains) {
            if (isDnsAvailable(domain)) {
                return true;
            }
        }
        return false;
    }
}
