package sun.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaIndex {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static volatile Map<File, MetaIndex> jarMap;
    private String[] contents;
    private boolean isClassOnlyJar;

    public static MetaIndex forJar(File jar) {
        return (MetaIndex) getJarMap().get(jar);
    }

    public static synchronized void registerDirectory(File dir) {
        synchronized (MetaIndex.class) {
            File indexFile = new File(dir, "meta-index");
            if (indexFile.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(indexFile));
                    String curJarName = null;
                    boolean isCurJarContainClassOnly = false;
                    List<String> contents = new ArrayList();
                    Map<File, MetaIndex> map = getJarMap();
                    dir = dir.getCanonicalFile();
                    String line = reader.readLine();
                    if (line != null) {
                        if (line.equals("% VERSION 2")) {
                            while (true) {
                                String readLine = reader.readLine();
                                line = readLine;
                                if (readLine == null) {
                                    break;
                                }
                                char charAt = line.charAt(0);
                                if (!(charAt == '!' || charAt == '#')) {
                                    if (charAt != '%') {
                                        if (charAt != '@') {
                                            contents.add(line);
                                        }
                                    }
                                }
                                if (curJarName != null && contents.size() > 0) {
                                    map.put(new File(dir, curJarName), new MetaIndex(contents, isCurJarContainClassOnly));
                                    contents.clear();
                                }
                                curJarName = line.substring(2);
                                if (line.charAt(0) == '!') {
                                    isCurJarContainClassOnly = true;
                                } else if (isCurJarContainClassOnly) {
                                    isCurJarContainClassOnly = false;
                                }
                            }
                            if (curJarName != null && contents.size() > 0) {
                                map.put(new File(dir, curJarName), new MetaIndex(contents, isCurJarContainClassOnly));
                            }
                            reader.close();
                        }
                    }
                    reader.close();
                    return;
                } catch (IOException e) {
                }
            }
        }
    }

    public boolean mayContain(String entry) {
        if (this.isClassOnlyJar && !entry.endsWith(".class")) {
            return false;
        }
        String[] conts = this.contents;
        for (String startsWith : conts) {
            if (entry.startsWith(startsWith)) {
                return true;
            }
        }
        return false;
    }

    private MetaIndex(List<String> entries, boolean isClassOnlyJar) throws IllegalArgumentException {
        if (entries != null) {
            this.contents = (String[]) entries.toArray(new String[0]);
            this.isClassOnlyJar = isClassOnlyJar;
            return;
        }
        throw new IllegalArgumentException();
    }

    private static Map<File, MetaIndex> getJarMap() {
        if (jarMap == null) {
            synchronized (MetaIndex.class) {
                if (jarMap == null) {
                    jarMap = new HashMap();
                }
            }
        }
        return jarMap;
    }
}
