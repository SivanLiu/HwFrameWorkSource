package com.android.server.security.securitycenter.parsexml;

import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Slog;
import android.util.Xml;
import com.android.server.pm.auth.DevicePublicKeyLoader;
import com.android.server.security.securitycenter.cache.ModuleData;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

public class ConfigFileManager {
    private static final int MARGIN_SPACE_NUM_CONFIG = 0;
    private static final int MARGIN_SPACE_NUM_ITEM = 6;
    private static final int MARGIN_SPACE_NUM_METHOD = 4;
    private static final int MARGIN_SPACE_NUM_MODULE = 2;
    private static final int METHOD_ARGS_INIT_SIZE = 5;
    private static final String PARSE_RESULT_ERROR = "";
    private static final String TAG = "ConfigFileManager";

    private ConfigFileManager() {
        Slog.d(TAG, "ConfigFileManager constructor");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0057, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0058, code lost:
        $closeResource(r1, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x005b, code lost:
        throw r5;
     */
    public static String parseConfigData(ConcurrentHashMap<String, ModuleData> moduleDataMap) {
        Slog.i(TAG, "start parse config data");
        File configFile = openConfigFile("securitycenter_config.xml", true);
        if (configFile == null) {
            Slog.e(TAG, "open file error, parse end!");
            return "";
        }
        try {
            FileInputStream inputStream = new FileInputStream(configFile);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            Element rootElement = factory.newDocumentBuilder().parse(inputStream).getDocumentElement();
            if (rootElement != null) {
                String feature = parseRootElement(rootElement, moduleDataMap);
                Slog.i(TAG, "end parse config data");
                $closeResource(null, inputStream);
                return feature;
            }
            $closeResource(null, inputStream);
            return "";
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "parse config file FileNotFoundException");
        } catch (IOException e2) {
            Slog.e(TAG, "parse config file IOException");
        } catch (SAXException e3) {
            Slog.e(TAG, "parse config file SAXException " + e3.getMessage());
        } catch (ParserConfigurationException e4) {
            Slog.e(TAG, "parse config file ParserConfigurationException " + e4.getMessage());
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
            } catch (Throwable th) {
                x0.addSuppressed(th);
            }
        } else {
            x1.close();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x007d, code lost:
        r2 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x007e, code lost:
        $closeResource(r0, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0081, code lost:
        throw r2;
     */
    public static void writeConfigData(HashMap<String, ModuleData> moduleDataMap, String feature) {
        if (moduleDataMap == null || moduleDataMap.isEmpty()) {
            Slog.e(TAG, "module list is empty");
            return;
        }
        Slog.i(TAG, "start write config data");
        File configFile = openConfigFile("securitycenter_config.xml", false);
        if (configFile == null) {
            Slog.e(TAG, "open file error, writeConfigData end!");
            return;
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(configFile, false);
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
            serializer.startDocument(StandardCharsets.UTF_8.name(), true);
            addLineSeparatorWithMargin(serializer, 0);
            serializer.startTag(null, "config");
            serializer.attribute(null, "feature", feature);
            for (ModuleData moduleData : moduleDataMap.values()) {
                writeModule(serializer, moduleData);
            }
            addLineSeparatorWithMargin(serializer, 0);
            serializer.endTag(null, "config");
            serializer.endDocument();
            Slog.i(TAG, "end write config data");
            $closeResource(null, outputStream);
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "writeConfigData FileNotFoundException");
        } catch (IOException e2) {
            Slog.e(TAG, "writeConfigData IOException");
        } catch (IllegalArgumentException e3) {
            Slog.e(TAG, "writeConfigData IllegalArgumentException" + e3.getMessage());
        } catch (IllegalStateException e4) {
            Slog.e(TAG, "writeConfigData IllegalStateException" + e4.getMessage());
        }
    }

    private static void writeModule(XmlSerializer serializer, ModuleData moduleData) throws IllegalStateException, IllegalArgumentException, IOException {
        addLineSeparatorWithMargin(serializer, 2);
        serializer.startTag(null, "module");
        serializer.attribute(null, "name", moduleData.getModuleName());
        serializer.attribute(null, "package", moduleData.getPackageName());
        for (String method : moduleData.getAllMethods()) {
            addLineSeparatorWithMargin(serializer, 4);
            serializer.startTag(null, "method");
            serializer.attribute(null, "name", method);
            Bundle argsMap = moduleData.getMethodBundleArgs(method);
            if (argsMap != null) {
                if (!argsMap.isEmpty()) {
                    for (String keyStr : argsMap.keySet()) {
                        addLineSeparatorWithMargin(serializer, 6);
                        serializer.startTag(null, "item");
                        serializer.attribute(null, DevicePublicKeyLoader.KEY, keyStr);
                        serializer.attribute(null, "value", argsMap.getString(keyStr));
                        serializer.endTag(null, "item");
                    }
                    addLineSeparatorWithMargin(serializer, 4);
                    serializer.endTag(null, "method");
                }
            }
        }
        addLineSeparatorWithMargin(serializer, 2);
        serializer.endTag(null, "module");
    }

    private static String parseRootElement(Element rootElement, ConcurrentHashMap<String, ModuleData> moduleDataMap) {
        int moduleSize;
        NodeList moduleList;
        Element moduleElement;
        int moduleSize2;
        NodeList moduleList2;
        String feature = parseFeature(rootElement);
        NodeList moduleList3 = rootElement.getElementsByTagName("module");
        int moduleSize3 = moduleList3.getLength();
        int i = 0;
        while (i < moduleSize3) {
            Element moduleElement2 = (Element) moduleList3.item(i);
            String moduleName = moduleElement2.getAttribute("name");
            String packageName = moduleElement2.getAttribute("package");
            if (isStringTrimmedEmpty(moduleName)) {
                moduleList = moduleList3;
                moduleSize = moduleSize3;
            } else if (isStringTrimmedEmpty(packageName)) {
                moduleList = moduleList3;
                moduleSize = moduleSize3;
            } else {
                ModuleData moduleData = new ModuleData(moduleName, packageName);
                NodeList methodList = moduleElement2.getElementsByTagName("method");
                int methodSize = methodList.getLength();
                int j = 0;
                while (j < methodSize) {
                    Element methodElement = (Element) methodList.item(j);
                    String methodName = methodElement.getAttribute("name");
                    if (isStringTrimmedEmpty(methodName)) {
                        moduleList2 = moduleList3;
                        moduleSize2 = moduleSize3;
                        moduleElement = moduleElement2;
                    } else {
                        Bundle methodArgs = new Bundle();
                        moduleList2 = moduleList3;
                        NodeList itemList = methodElement.getElementsByTagName("item");
                        int itemSize = itemList.getLength();
                        moduleSize2 = moduleSize3;
                        int z = 0;
                        while (z < itemSize) {
                            Element itemElement = (Element) itemList.item(z);
                            methodArgs.putString(itemElement.getAttribute(DevicePublicKeyLoader.KEY), itemElement.getAttribute("value"));
                            z++;
                            itemSize = itemSize;
                            itemList = itemList;
                            moduleElement2 = moduleElement2;
                        }
                        moduleElement = moduleElement2;
                        moduleData.addMethod(methodName, methodArgs);
                    }
                    j++;
                    moduleList3 = moduleList2;
                    moduleSize3 = moduleSize2;
                    moduleElement2 = moduleElement;
                }
                moduleList = moduleList3;
                moduleSize = moduleSize3;
                moduleDataMap.put(moduleName, moduleData);
            }
            i++;
            moduleList3 = moduleList;
            moduleSize3 = moduleSize;
        }
        return feature;
    }

    private static String parseFeature(Element configElement) {
        return configElement.getAttribute("feature");
    }

    private static File openConfigFile(String fileName, boolean isRead) {
        File configFile = Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", fileName});
        if (configFile == null) {
            Slog.e(TAG, "openConfigFile:open file error!");
            return null;
        }
        if (!configFile.exists()) {
            if (isRead) {
                Slog.e(TAG, "openConfigFile:file is not exists!");
                return null;
            }
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                Slog.e(TAG, "openConfigFile:createNewFile error!");
                return null;
            }
        }
        return configFile;
    }

    private static void addLineSeparatorWithMargin(XmlSerializer serializer, int marginSpaceNum) throws IOException, IllegalArgumentException, IllegalStateException {
        String separator = System.lineSeparator();
        StringBuilder stringBuilder = new StringBuilder(marginSpaceNum);
        for (int i = 0; i < marginSpaceNum; i++) {
            stringBuilder.append(" ");
        }
        serializer.text(separator);
        serializer.text(stringBuilder.toString());
    }

    private static boolean isStringTrimmedEmpty(String string) {
        return string == null || TextUtils.isEmpty(string.trim());
    }
}
