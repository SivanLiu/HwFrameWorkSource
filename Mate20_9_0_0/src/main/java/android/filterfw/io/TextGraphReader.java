package android.filterfw.io;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterFactory;
import android.filterfw.core.FilterGraph;
import android.filterfw.core.KeyValueMap;
import android.filterfw.core.ProtocolException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

public class TextGraphReader extends GraphReader {
    private KeyValueMap mBoundReferences;
    private ArrayList<Command> mCommands = new ArrayList();
    private Filter mCurrentFilter;
    private FilterGraph mCurrentGraph;
    private FilterFactory mFactory;
    private KeyValueMap mSettings;

    private interface Command {
        void execute(TextGraphReader textGraphReader) throws GraphIOException;
    }

    private class AddLibraryCommand implements Command {
        private String mLibraryName;

        public AddLibraryCommand(String libraryName) {
            this.mLibraryName = libraryName;
        }

        public void execute(TextGraphReader reader) {
            reader.mFactory;
            FilterFactory.addFilterLibrary(this.mLibraryName);
        }
    }

    private class AllocateFilterCommand implements Command {
        private String mClassName;
        private String mFilterName;

        public AllocateFilterCommand(String className, String filterName) {
            this.mClassName = className;
            this.mFilterName = filterName;
        }

        public void execute(TextGraphReader reader) throws GraphIOException {
            try {
                reader.mCurrentFilter = reader.mFactory.createFilterByClassName(this.mClassName, this.mFilterName);
            } catch (IllegalArgumentException e) {
                throw new GraphIOException(e.getMessage());
            }
        }
    }

    private class ConnectCommand implements Command {
        private String mSourceFilter;
        private String mSourcePort;
        private String mTargetFilter;
        private String mTargetName;

        public ConnectCommand(String sourceFilter, String sourcePort, String targetFilter, String targetName) {
            this.mSourceFilter = sourceFilter;
            this.mSourcePort = sourcePort;
            this.mTargetFilter = targetFilter;
            this.mTargetName = targetName;
        }

        public void execute(TextGraphReader reader) {
            reader.mCurrentGraph.connect(this.mSourceFilter, this.mSourcePort, this.mTargetFilter, this.mTargetName);
        }
    }

    private class ImportPackageCommand implements Command {
        private String mPackageName;

        public ImportPackageCommand(String packageName) {
            this.mPackageName = packageName;
        }

        public void execute(TextGraphReader reader) throws GraphIOException {
            try {
                reader.mFactory.addPackage(this.mPackageName);
            } catch (IllegalArgumentException e) {
                throw new GraphIOException(e.getMessage());
            }
        }
    }

    private class InitFilterCommand implements Command {
        private KeyValueMap mParams;

        public InitFilterCommand(KeyValueMap params) {
            this.mParams = params;
        }

        public void execute(TextGraphReader reader) throws GraphIOException {
            try {
                reader.mCurrentFilter.initWithValueMap(this.mParams);
                reader.mCurrentGraph.addFilter(TextGraphReader.this.mCurrentFilter);
            } catch (ProtocolException e) {
                throw new GraphIOException(e.getMessage());
            }
        }
    }

    public FilterGraph readGraphString(String graphString) throws GraphIOException {
        FilterGraph result = new FilterGraph();
        reset();
        this.mCurrentGraph = result;
        parseString(graphString);
        applySettings();
        executeCommands();
        reset();
        return result;
    }

    private void reset() {
        this.mCurrentGraph = null;
        this.mCurrentFilter = null;
        this.mCommands.clear();
        this.mBoundReferences = new KeyValueMap();
        this.mSettings = new KeyValueMap();
        this.mFactory = new FilterFactory();
    }

    /* JADX WARNING: Missing block: B:10:0x00d2, code skipped:
            r0 = r1;
            r2 = r7;
            r41 = r10;
            r1 = r11;
            r42 = r15;
            r35 = r37;
            r10 = r38;
            r7 = r3;
     */
    /* JADX WARNING: Missing block: B:11:0x00de, code skipped:
            r11 = r5;
     */
    /* JADX WARNING: Missing block: B:23:0x0223, code skipped:
            r0 = r1;
     */
    /* JADX WARNING: Missing block: B:24:0x0224, code skipped:
            r35 = r37;
     */
    /* JADX WARNING: Missing block: B:25:0x0226, code skipped:
            r2 = r44;
            r1 = r45;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseString(String graphString) throws GraphIOException {
        Pattern commandPattern = Pattern.compile("@[a-zA-Z]+");
        Pattern curlyClosePattern = Pattern.compile("\\}");
        Pattern curlyOpenPattern = Pattern.compile("\\{");
        Pattern ignorePattern = Pattern.compile("(\\s+|//[^\\n]*\\n)+");
        Pattern packageNamePattern = Pattern.compile("[a-zA-Z\\.]+");
        Pattern libraryNamePattern = Pattern.compile("[a-zA-Z\\./:]+");
        Pattern portPattern = Pattern.compile("\\[[a-zA-Z0-9\\-_]+\\]");
        Pattern rightArrowPattern = Pattern.compile("=>");
        Pattern semicolonPattern = Pattern.compile(";");
        Pattern wordPattern = Pattern.compile("[a-zA-Z0-9\\-_]+");
        int state = 0;
        PatternScanner scanner = new PatternScanner(graphString, ignorePattern);
        String curClassName = null;
        String curTargetPortName = null;
        String curTargetFilterName = null;
        String curSourcePortName = null;
        String curSourceFilterName = null;
        while (true) {
            Pattern ignorePattern2;
            Pattern semicolonPattern2;
            String curClassName2;
            PatternScanner scanner2;
            Pattern packageNamePattern2;
            int state2 = state;
            String curTargetPortName2 = curTargetPortName;
            String str;
            int i;
            int state3;
            if (!scanner.atEnd()) {
                str = curTargetPortName2;
                Pattern commandPattern2;
                Pattern packageNamePattern3;
                String curClassName3;
                switch (state2) {
                    case 0:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern2 = packageNamePattern;
                        packageNamePattern = wordPattern;
                        state2 = commandPattern2;
                        curTargetPortName2 = scanner2.eat(state2, "<command>");
                        if (curTargetPortName2.equals("@import")) {
                            state3 = 1;
                        } else if (curTargetPortName2.equals("@library")) {
                            state3 = 2;
                        } else if (curTargetPortName2.equals("@filter")) {
                            state3 = 3;
                        } else if (curTargetPortName2.equals("@connect")) {
                            state3 = 8;
                        } else if (curTargetPortName2.equals("@set")) {
                            state3 = 13;
                        } else if (curTargetPortName2.equals("@external")) {
                            state3 = 14;
                        } else if (curTargetPortName2.equals("@setting")) {
                            state3 = 15;
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown command '");
                            stringBuilder.append(curTargetPortName2);
                            stringBuilder.append("'!");
                            throw new GraphIOException(stringBuilder.toString());
                        }
                        state = state3;
                        curTargetPortName = str;
                        break;
                    case 1:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        packageNamePattern2 = packageNamePattern3;
                        this.mCommands.add(new ImportPackageCommand(scanner2.eat(packageNamePattern2, "<package-name>")));
                        state = 16;
                        curTargetPortName = str;
                        state2 = commandPattern2;
                        break;
                    case 2:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        this.mCommands.add(new AddLibraryCommand(scanner2.eat(libraryNamePattern, "<library-name>")));
                        packageNamePattern2 = 16;
                        break;
                    case 3:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        packageNamePattern2 = 4;
                        curClassName2 = scanner2.eat(packageNamePattern, "<class-name>");
                        break;
                    case 4:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        this.mCommands.add(new AllocateFilterCommand(curClassName2, scanner2.eat(packageNamePattern, "<filter-name>")));
                        packageNamePattern2 = 5;
                        break;
                    case 5:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        scanner2.eat(curlyOpenPattern, "{");
                        state = 6;
                        break;
                    case 6:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        this.mCommands.add(new InitFilterCommand(readKeyValueAssignments(scanner2, curlyClosePattern)));
                        packageNamePattern2 = 7;
                        break;
                    case 7:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        scanner2.eat(curlyClosePattern, "}");
                        state = 0;
                        break;
                    case 8:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        curSourceFilterName = scanner2.eat(packageNamePattern, "<source-filter-name>");
                        state = 9;
                        break;
                    case 9:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        curClassName = scanner2.eat(portPattern, "[<source-port-name>]");
                        curSourcePortName = curClassName.substring(1, curClassName.length() - 1);
                        state = 10;
                        break;
                    case 10:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        scanner2.eat(rightArrowPattern, "=>");
                        state = 11;
                        break;
                    case 11:
                        i = state2;
                        commandPattern2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        packageNamePattern3 = packageNamePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern = wordPattern;
                        curTargetFilterName = scanner2.eat(packageNamePattern, "<target-filter-name>");
                        state = 12;
                        break;
                    case 12:
                        curClassName3 = curClassName;
                        curClassName = scanner.eat(portPattern, "[<target-port-name>]");
                        int state4 = state2;
                        curTargetPortName = curClassName.substring(1, curClassName.length() - 1);
                        String str2 = curTargetPortName2;
                        str = curClassName;
                        ignorePattern2 = ignorePattern;
                        curClassName2 = curClassName3;
                        semicolonPattern2 = semicolonPattern;
                        i = state4;
                        ArrayList arrayList = this.mCommands;
                        commandPattern2 = commandPattern;
                        scanner2 = scanner;
                        packageNamePattern3 = packageNamePattern;
                        packageNamePattern = wordPattern;
                        curTargetPortName2 = new ConnectCommand(curSourceFilterName, curSourcePortName, curTargetFilterName, curTargetPortName);
                        arrayList.add(curTargetPortName2);
                        state = 16;
                        break;
                    case 13:
                        curClassName3 = curClassName;
                        this.mBoundReferences.putAll(readKeyValueAssignments(scanner, semicolonPattern));
                        curClassName = 16;
                        break;
                    case 14:
                        curClassName3 = curClassName;
                        bindExternal(scanner.eat(wordPattern, "<external-identifier>"));
                        curClassName = 16;
                        break;
                    case 15:
                        curClassName3 = curClassName;
                        this.mSettings.putAll(readKeyValueAssignments(scanner, semicolonPattern));
                        curClassName = 16;
                        break;
                    case 16:
                        scanner.eat(semicolonPattern, ";");
                        state = 0;
                        state2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        semicolonPattern2 = semicolonPattern;
                        curTargetPortName = str;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern2 = packageNamePattern;
                        break;
                    default:
                        i = state2;
                        state2 = commandPattern;
                        ignorePattern2 = ignorePattern;
                        semicolonPattern2 = semicolonPattern;
                        curClassName2 = curClassName;
                        scanner2 = scanner;
                        packageNamePattern2 = packageNamePattern;
                        packageNamePattern = wordPattern;
                        curTargetPortName = str;
                        state = i;
                        break;
                }
            }
            str = curTargetPortName2;
            i = state2;
            ignorePattern2 = ignorePattern;
            semicolonPattern2 = semicolonPattern;
            state3 = i;
            if (state3 != 16 && state3 != 0) {
                throw new GraphIOException("Unexpected end of input!");
            }
            return;
            String str3 = graphString;
            scanner = scanner2;
            wordPattern = packageNamePattern;
            semicolonPattern = semicolonPattern2;
            packageNamePattern = packageNamePattern2;
            commandPattern = state2;
            curClassName = curClassName2;
            ignorePattern = ignorePattern2;
        }
    }

    public KeyValueMap readKeyValueAssignments(String assignments) throws GraphIOException {
        return readKeyValueAssignments(new PatternScanner(assignments, Pattern.compile("\\s+")), null);
    }

    private KeyValueMap readKeyValueAssignments(PatternScanner scanner, Pattern endPattern) throws GraphIOException {
        int STATE_POST_VALUE;
        Pattern semicolonPattern;
        TextGraphReader textGraphReader = this;
        PatternScanner patternScanner = scanner;
        int STATE_VALUE = 2;
        int STATE_POST_VALUE2 = 3;
        Pattern equalsPattern = Pattern.compile("=");
        Pattern semicolonPattern2 = Pattern.compile(";");
        Pattern wordPattern = Pattern.compile("[a-zA-Z]+[a-zA-Z0-9]*");
        Pattern stringPattern = Pattern.compile("'[^']*'|\\\"[^\\\"]*\\\"");
        Pattern intPattern = Pattern.compile("[0-9]+");
        Pattern floatPattern = Pattern.compile("[0-9]*\\.[0-9]+f?");
        Pattern referencePattern = Pattern.compile("\\$[a-zA-Z]+[a-zA-Z0-9]");
        Pattern booleanPattern = Pattern.compile("true|false");
        KeyValueMap newVals = new KeyValueMap();
        int STATE_IDENTIFIER = 0;
        int STATE_EQUALS = 1;
        int state = 0;
        Object curKey = null;
        String curValue = null;
        while (!scanner.atEnd()) {
            int STATE_VALUE2;
            if (endPattern == null || !scanner.peek(endPattern)) {
                int state2;
                switch (state) {
                    case 0:
                        STATE_VALUE2 = STATE_VALUE;
                        STATE_POST_VALUE = STATE_POST_VALUE2;
                        semicolonPattern = semicolonPattern2;
                        state = 1;
                        curKey = patternScanner.eat(wordPattern, "<identifier>");
                        continue;
                    case 1:
                        STATE_VALUE2 = STATE_VALUE;
                        STATE_POST_VALUE = STATE_POST_VALUE2;
                        semicolonPattern = semicolonPattern2;
                        patternScanner.eat(equalsPattern, "=");
                        state2 = 2;
                        break;
                    case 2:
                        STATE_VALUE2 = STATE_VALUE;
                        STATE_VALUE = patternScanner.tryEat(stringPattern);
                        curValue = STATE_VALUE;
                        STATE_POST_VALUE = STATE_POST_VALUE2;
                        if (STATE_VALUE == 0) {
                            STATE_VALUE = patternScanner.tryEat(referencePattern);
                            curValue = STATE_VALUE;
                            if (STATE_VALUE == 0) {
                                semicolonPattern = semicolonPattern2;
                                String tryEat = patternScanner.tryEat(booleanPattern);
                                curValue = tryEat;
                                if (tryEat != null) {
                                    newVals.put(curKey, Boolean.valueOf(Boolean.parseBoolean(curValue)));
                                } else {
                                    tryEat = patternScanner.tryEat(floatPattern);
                                    curValue = tryEat;
                                    if (tryEat != null) {
                                        newVals.put(curKey, Float.valueOf(Float.parseFloat(curValue)));
                                    } else {
                                        tryEat = patternScanner.tryEat(intPattern);
                                        curValue = tryEat;
                                        if (tryEat != null) {
                                            newVals.put(curKey, Integer.valueOf(Integer.parseInt(curValue)));
                                        } else {
                                            throw new GraphIOException(patternScanner.unexpectedTokenMessage("<value>"));
                                        }
                                    }
                                }
                                state2 = 3;
                                break;
                            }
                            Object referencedObject;
                            STATE_VALUE = curValue.substring(1, curValue.length());
                            if (textGraphReader.mBoundReferences != null) {
                                referencedObject = textGraphReader.mBoundReferences.get(STATE_VALUE);
                            } else {
                                referencedObject = null;
                            }
                            if (referencedObject != null) {
                                newVals.put(curKey, referencedObject);
                            } else {
                                STATE_POST_VALUE2 = new StringBuilder();
                                semicolonPattern = semicolonPattern2;
                                STATE_POST_VALUE2.append("Unknown object reference to '");
                                STATE_POST_VALUE2.append(STATE_VALUE);
                                STATE_POST_VALUE2.append("'!");
                                throw new GraphIOException(STATE_POST_VALUE2.toString());
                            }
                        }
                        newVals.put(curKey, curValue.substring(1, curValue.length() - 1));
                        semicolonPattern = semicolonPattern2;
                        state2 = 3;
                    case 3:
                        STATE_VALUE2 = STATE_VALUE;
                        patternScanner.eat(semicolonPattern2, ";");
                        state = 0;
                        STATE_POST_VALUE = STATE_POST_VALUE2;
                        semicolonPattern = semicolonPattern2;
                        continue;
                    default:
                        STATE_VALUE2 = STATE_VALUE;
                        STATE_POST_VALUE = STATE_POST_VALUE2;
                        semicolonPattern = semicolonPattern2;
                        continue;
                }
                state = state2;
                STATE_VALUE = STATE_VALUE2;
                STATE_POST_VALUE2 = STATE_POST_VALUE;
                semicolonPattern2 = semicolonPattern;
                textGraphReader = this;
            } else {
                STATE_VALUE2 = STATE_VALUE;
                STATE_POST_VALUE = STATE_POST_VALUE2;
                semicolonPattern = semicolonPattern2;
                if (state != 0 || state == 3) {
                    return newVals;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected end of assignments on line ");
                stringBuilder.append(scanner.lineNo());
                stringBuilder.append("!");
                throw new GraphIOException(stringBuilder.toString());
            }
        }
        STATE_POST_VALUE = STATE_POST_VALUE2;
        semicolonPattern = semicolonPattern2;
        if (state != 0) {
        }
        return newVals;
    }

    private void bindExternal(String name) throws GraphIOException {
        if (this.mReferences.containsKey(name)) {
            this.mBoundReferences.put(name, this.mReferences.get(name));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown external variable '");
        stringBuilder.append(name);
        stringBuilder.append("'! You must add a reference to this external in the host program using addReference(...)!");
        throw new GraphIOException(stringBuilder.toString());
    }

    private void checkReferences() throws GraphIOException {
        for (String reference : this.mReferences.keySet()) {
            if (!this.mBoundReferences.containsKey(reference)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Host program specifies reference to '");
                stringBuilder.append(reference);
                stringBuilder.append("', which is not declared @external in graph file!");
                throw new GraphIOException(stringBuilder.toString());
            }
        }
    }

    private void applySettings() throws GraphIOException {
        for (String setting : this.mSettings.keySet()) {
            Object value = this.mSettings.get(setting);
            StringBuilder stringBuilder;
            if (setting.equals("autoBranch")) {
                expectSettingClass(setting, value, String.class);
                if (value.equals("synced")) {
                    this.mCurrentGraph.setAutoBranchMode(1);
                } else if (value.equals("unsynced")) {
                    this.mCurrentGraph.setAutoBranchMode(2);
                } else if (value.equals("off")) {
                    this.mCurrentGraph.setAutoBranchMode(0);
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown autobranch setting: ");
                    stringBuilder.append(value);
                    stringBuilder.append("!");
                    throw new GraphIOException(stringBuilder.toString());
                }
            } else if (setting.equals("discardUnconnectedOutputs")) {
                expectSettingClass(setting, value, Boolean.class);
                this.mCurrentGraph.setDiscardUnconnectedOutputs(((Boolean) value).booleanValue());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown @setting '");
                stringBuilder.append(setting);
                stringBuilder.append("'!");
                throw new GraphIOException(stringBuilder.toString());
            }
        }
    }

    private void expectSettingClass(String setting, Object value, Class expectedClass) throws GraphIOException {
        if (value.getClass() != expectedClass) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting '");
            stringBuilder.append(setting);
            stringBuilder.append("' must have a value of type ");
            stringBuilder.append(expectedClass.getSimpleName());
            stringBuilder.append(", but found a value of type ");
            stringBuilder.append(value.getClass().getSimpleName());
            stringBuilder.append("!");
            throw new GraphIOException(stringBuilder.toString());
        }
    }

    private void executeCommands() throws GraphIOException {
        Iterator it = this.mCommands.iterator();
        while (it.hasNext()) {
            ((Command) it.next()).execute(this);
        }
    }
}
