package sun.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.AccessController;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sun.security.action.GetPropertyAction;

public class JarIndex {
    public static final String INDEX_NAME = "META-INF/INDEX.LIST";
    private static final boolean metaInfFilenames = "true".equals(AccessController.doPrivileged(new GetPropertyAction("sun.misc.JarIndex.metaInfFilenames")));
    private HashMap<String, LinkedList<String>> indexMap;
    private String[] jarFiles;
    private HashMap<String, LinkedList<String>> jarMap;

    public JarIndex() {
        this.indexMap = new HashMap();
        this.jarMap = new HashMap();
    }

    public JarIndex(InputStream is) throws IOException {
        this();
        read(is);
    }

    public JarIndex(String[] files) throws IOException {
        this();
        this.jarFiles = files;
        parseJars(files);
    }

    public static JarIndex getJarIndex(JarFile jar) throws IOException {
        return getJarIndex(jar, null);
    }

    public static JarIndex getJarIndex(JarFile jar, MetaIndex metaIndex) throws IOException {
        JarIndex index = null;
        if (metaIndex != null && !metaIndex.mayContain(INDEX_NAME)) {
            return null;
        }
        JarEntry e = jar.getJarEntry(INDEX_NAME);
        if (e != null) {
            index = new JarIndex(jar.getInputStream(e));
        }
        return index;
    }

    public String[] getJarFiles() {
        return this.jarFiles;
    }

    private void addToList(String key, String value, HashMap<String, LinkedList<String>> t) {
        LinkedList<String> list = (LinkedList) t.get(key);
        if (list == null) {
            list = new LinkedList();
            list.add(value);
            t.put(key, list);
        } else if (!list.contains(value)) {
            list.add(value);
        }
    }

    public LinkedList<String> get(String fileName) {
        LinkedList<String> linkedList = (LinkedList) this.indexMap.get(fileName);
        LinkedList<String> jarFiles = linkedList;
        if (linkedList != null) {
            return jarFiles;
        }
        int lastIndexOf = fileName.lastIndexOf("/");
        int pos = lastIndexOf;
        if (lastIndexOf != -1) {
            return (LinkedList) this.indexMap.get(fileName.substring(0, pos));
        }
        return jarFiles;
    }

    public void add(String fileName, String jarName) {
        String packageName;
        int lastIndexOf = fileName.lastIndexOf("/");
        int pos = lastIndexOf;
        if (lastIndexOf != -1) {
            packageName = fileName.substring(null, pos);
        } else {
            packageName = fileName;
        }
        addMapping(packageName, jarName);
    }

    private void addMapping(String jarItem, String jarName) {
        addToList(jarItem, jarName, this.indexMap);
        addToList(jarName, jarItem, this.jarMap);
    }

    private void parseJars(String[] files) throws IOException {
        if (files != null) {
            for (String currentJar : files) {
                ZipFile zrf = new ZipFile(currentJar.replace('/', File.separatorChar));
                Enumeration<? extends ZipEntry> entries = zrf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    String fileName = entry.getName();
                    if (!(fileName.equals("META-INF/") || fileName.equals(INDEX_NAME))) {
                        if (!fileName.equals(JarFile.MANIFEST_NAME)) {
                            if (!metaInfFilenames || !fileName.startsWith("META-INF/")) {
                                add(fileName, currentJar);
                            } else if (!entry.isDirectory()) {
                                addMapping(fileName, currentJar);
                            }
                        }
                    }
                }
                zrf.close();
            }
        }
    }

    public void write(OutputStream out) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "UTF8"));
        bw.write("JarIndex-Version: 1.0\n\n");
        if (this.jarFiles != null) {
            for (String jar : this.jarFiles) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(jar);
                stringBuilder.append("\n");
                bw.write(stringBuilder.toString());
                LinkedList<String> jarlist = (LinkedList) this.jarMap.get(jar);
                if (jarlist != null) {
                    Iterator<String> listitr = jarlist.iterator();
                    while (listitr.hasNext()) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append((String) listitr.next());
                        stringBuilder2.append("\n");
                        bw.write(stringBuilder2.toString());
                    }
                }
                bw.write("\n");
            }
            bw.flush();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:7:0x0025  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void read(InputStream is) throws IOException {
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF8"));
        String currentJar = null;
        Vector<String> jars = new Vector();
        while (true) {
            String readLine = br.readLine();
            line = readLine;
            if (readLine == null || line.endsWith(".jar")) {
                while (line != null) {
                    if (line.length() != 0) {
                        if (line.endsWith(".jar")) {
                            currentJar = line;
                            jars.add(currentJar);
                        } else {
                            addMapping(line, currentJar);
                        }
                    }
                    line = br.readLine();
                }
            }
        }
        while (line != null) {
        }
        this.jarFiles = (String[]) jars.toArray(new String[jars.size()]);
    }

    public void merge(JarIndex toIndex, String path) {
        for (Entry<String, LinkedList<String>> e : this.indexMap.entrySet()) {
            String packageName = (String) e.getKey();
            Iterator<String> listItr = ((LinkedList) e.getValue()).iterator();
            while (listItr.hasNext()) {
                String jarName = (String) listItr.next();
                if (path != null) {
                    jarName = path.concat(jarName);
                }
                toIndex.addMapping(packageName, jarName);
            }
        }
    }
}
