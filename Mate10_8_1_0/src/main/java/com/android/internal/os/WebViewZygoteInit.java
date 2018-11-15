package com.android.internal.os;

import android.app.ApplicationLoaders;
import android.net.LocalSocket;
import android.os.Build;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebViewFactory;
import android.webkit.WebViewFactoryProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

class WebViewZygoteInit {
    public static final String TAG = "WebViewZygoteInit";
    private static ZygoteServer sServer;

    private static class WebViewZygoteConnection extends ZygoteConnection {
        WebViewZygoteConnection(LocalSocket socket, String abiList) throws IOException {
            super(socket, abiList);
        }

        protected void preload() {
        }

        protected boolean isPreloadComplete() {
            return true;
        }

        protected void handlePreloadPackage(String packagePath, String libsPath, String cacheKey) {
            Log.i(WebViewZygoteInit.TAG, "Beginning package preload");
            ClassLoader loader = ApplicationLoaders.getDefault().createAndCacheWebViewClassLoader(packagePath, libsPath, cacheKey);
            for (String packageEntry : TextUtils.split(packagePath, File.pathSeparator)) {
                Zygote.nativeAllowFileAcrossFork(packageEntry);
            }
            boolean preloadSucceeded = false;
            try {
                Class<WebViewFactoryProvider> providerClass = WebViewFactory.getWebViewProviderClass(loader);
                Method preloadInZygote = providerClass.getMethod("preloadInZygote", new Class[0]);
                preloadInZygote.setAccessible(true);
                if (preloadInZygote.getReturnType() != Boolean.TYPE) {
                    Log.e(WebViewZygoteInit.TAG, "Unexpected return type: preloadInZygote must return boolean");
                } else {
                    preloadSucceeded = ((Boolean) providerClass.getMethod("preloadInZygote", new Class[0]).invoke(null, new Object[0])).booleanValue();
                    if (!preloadSucceeded) {
                        Log.e(WebViewZygoteInit.TAG, "preloadInZygote returned false");
                    }
                }
            } catch (ReflectiveOperationException e) {
                Log.e(WebViewZygoteInit.TAG, "Exception while preloading package", e);
            }
            try {
                getSocketOutputStream().writeInt(preloadSucceeded ? 1 : 0);
                Log.i(WebViewZygoteInit.TAG, "Package preload done");
            } catch (IOException ioe) {
                throw new IllegalStateException("Error writing to command socket", ioe);
            }
        }
    }

    private static class WebViewZygoteServer extends ZygoteServer {
        private WebViewZygoteServer() {
        }

        protected ZygoteConnection createNewConnection(LocalSocket socket, String abiList) throws IOException {
            return new WebViewZygoteConnection(socket, abiList);
        }
    }

    WebViewZygoteInit() {
    }

    public static void main(String[] argv) {
        sServer = new WebViewZygoteServer();
        try {
            Os.setpgid(0, 0);
            try {
                sServer.registerServerSocket("webview_zygote");
                Runnable caller = sServer.runSelectLoop(TextUtils.join((CharSequence) ",", Build.SUPPORTED_ABIS));
                sServer.closeServerSocket();
                if (caller != null) {
                    caller.run();
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Fatal exception:", e);
                throw e;
            } catch (Throwable th) {
                sServer.closeServerSocket();
            }
        } catch (ErrnoException ex) {
            throw new RuntimeException("Failed to setpgid(0,0)", ex);
        }
    }
}
