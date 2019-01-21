package org.apache.harmony.xml.dom;

import android.icu.text.PluralRules;
import java.util.Map;
import java.util.TreeMap;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class DOMConfigurationImpl implements DOMConfiguration {
    private static final Map<String, Parameter> PARAMETERS = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    private boolean cdataSections = true;
    private boolean comments = true;
    private boolean datatypeNormalization = false;
    private boolean entities = true;
    private DOMErrorHandler errorHandler;
    private boolean namespaces = true;
    private String schemaLocation;
    private String schemaType;
    private boolean splitCdataSections = true;
    private boolean validate = false;
    private boolean wellFormed = true;

    interface Parameter {
        boolean canSet(DOMConfigurationImpl dOMConfigurationImpl, Object obj);

        Object get(DOMConfigurationImpl dOMConfigurationImpl);

        void set(DOMConfigurationImpl dOMConfigurationImpl, Object obj);
    }

    static abstract class BooleanParameter implements Parameter {
        BooleanParameter() {
        }

        public boolean canSet(DOMConfigurationImpl config, Object value) {
            return value instanceof Boolean;
        }
    }

    static class FixedParameter implements Parameter {
        final Object onlyValue;

        FixedParameter(Object onlyValue) {
            this.onlyValue = onlyValue;
        }

        public Object get(DOMConfigurationImpl config) {
            return this.onlyValue;
        }

        public void set(DOMConfigurationImpl config, Object value) {
            if (!this.onlyValue.equals(value)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported value: ");
                stringBuilder.append(value);
                throw new DOMException((short) 9, stringBuilder.toString());
            }
        }

        public boolean canSet(DOMConfigurationImpl config, Object value) {
            return this.onlyValue.equals(value);
        }
    }

    static {
        PARAMETERS.put("canonical-form", new FixedParameter(Boolean.valueOf(false)));
        PARAMETERS.put("cdata-sections", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.cdataSections);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.cdataSections = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("check-character-normalization", new FixedParameter(Boolean.valueOf(false)));
        PARAMETERS.put("comments", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.comments);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.comments = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("datatype-normalization", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.datatypeNormalization);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                if (((Boolean) value).booleanValue()) {
                    config.datatypeNormalization = true;
                    config.validate = true;
                    return;
                }
                config.datatypeNormalization = false;
            }
        });
        PARAMETERS.put("element-content-whitespace", new FixedParameter(Boolean.valueOf(true)));
        PARAMETERS.put("entities", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.entities);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.entities = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("error-handler", new Parameter() {
            public Object get(DOMConfigurationImpl config) {
                return config.errorHandler;
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.errorHandler = (DOMErrorHandler) value;
            }

            public boolean canSet(DOMConfigurationImpl config, Object value) {
                return value == null || (value instanceof DOMErrorHandler);
            }
        });
        PARAMETERS.put("infoset", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                boolean z = !config.entities && !config.datatypeNormalization && !config.cdataSections && config.wellFormed && config.comments && config.namespaces;
                return Boolean.valueOf(z);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                if (((Boolean) value).booleanValue()) {
                    config.entities = false;
                    config.datatypeNormalization = false;
                    config.cdataSections = false;
                    config.wellFormed = true;
                    config.comments = true;
                    config.namespaces = true;
                }
            }
        });
        PARAMETERS.put("namespaces", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.namespaces);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.namespaces = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("namespace-declarations", new FixedParameter(Boolean.valueOf(true)));
        PARAMETERS.put("normalize-characters", new FixedParameter(Boolean.valueOf(false)));
        PARAMETERS.put("schema-location", new Parameter() {
            public Object get(DOMConfigurationImpl config) {
                return config.schemaLocation;
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.schemaLocation = (String) value;
            }

            public boolean canSet(DOMConfigurationImpl config, Object value) {
                return value == null || (value instanceof String);
            }
        });
        PARAMETERS.put("schema-type", new Parameter() {
            public Object get(DOMConfigurationImpl config) {
                return config.schemaType;
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.schemaType = (String) value;
            }

            public boolean canSet(DOMConfigurationImpl config, Object value) {
                return value == null || (value instanceof String);
            }
        });
        PARAMETERS.put("split-cdata-sections", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.splitCdataSections);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.splitCdataSections = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("validate", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.validate);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.validate = ((Boolean) value).booleanValue();
            }
        });
        PARAMETERS.put("validate-if-schema", new FixedParameter(Boolean.valueOf(false)));
        PARAMETERS.put("well-formed", new BooleanParameter() {
            public Object get(DOMConfigurationImpl config) {
                return Boolean.valueOf(config.wellFormed);
            }

            public void set(DOMConfigurationImpl config, Object value) {
                config.wellFormed = ((Boolean) value).booleanValue();
            }
        });
    }

    public boolean canSetParameter(String name, Object value) {
        Parameter parameter = (Parameter) PARAMETERS.get(name);
        return parameter != null && parameter.canSet(this, value);
    }

    public void setParameter(String name, Object value) throws DOMException {
        StringBuilder stringBuilder;
        Parameter parameter = (Parameter) PARAMETERS.get(name);
        if (parameter != null) {
            try {
                parameter.set(this, value);
                return;
            } catch (NullPointerException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Null not allowed for ");
                stringBuilder.append(name);
                throw new DOMException((short) 17, stringBuilder.toString());
            } catch (ClassCastException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid type for ");
                stringBuilder.append(name);
                stringBuilder.append(PluralRules.KEYWORD_RULE_SEPARATOR);
                stringBuilder.append(value.getClass());
                throw new DOMException((short) 17, stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("No such parameter: ");
        stringBuilder2.append(name);
        throw new DOMException((short) 8, stringBuilder2.toString());
    }

    public Object getParameter(String name) throws DOMException {
        Parameter parameter = (Parameter) PARAMETERS.get(name);
        if (parameter != null) {
            return parameter.get(this);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No such parameter: ");
        stringBuilder.append(name);
        throw new DOMException((short) 8, stringBuilder.toString());
    }

    public DOMStringList getParameterNames() {
        return internalGetParameterNames();
    }

    private static DOMStringList internalGetParameterNames() {
        final String[] result = (String[]) PARAMETERS.keySet().toArray(new String[PARAMETERS.size()]);
        return new DOMStringList() {
            public String item(int index) {
                return index < result.length ? result[index] : null;
            }

            public int getLength() {
                return result.length;
            }

            public boolean contains(String str) {
                return DOMConfigurationImpl.PARAMETERS.containsKey(str);
            }
        };
    }

    /* JADX WARNING: Missing block: B:22:0x007f, code skipped:
            r0 = ((org.apache.harmony.xml.dom.TextImpl) r5).minimize();
     */
    /* JADX WARNING: Missing block: B:23:0x0086, code skipped:
            if (r0 == null) goto L_?;
     */
    /* JADX WARNING: Missing block: B:24:0x0088, code skipped:
            checkTextValidity(r0.buffer);
     */
    /* JADX WARNING: Missing block: B:30:0x00b1, code skipped:
            r0 = r5.getFirstChild();
     */
    /* JADX WARNING: Missing block: B:31:0x00b5, code skipped:
            if (r0 == null) goto L_0x00c1;
     */
    /* JADX WARNING: Missing block: B:32:0x00b7, code skipped:
            r1 = r0.getNextSibling();
            normalize(r0);
            r0 = r1;
     */
    /* JADX WARNING: Missing block: B:39:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:40:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:43:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void normalize(Node node) {
        switch (node.getNodeType()) {
            case (short) 1:
                NamedNodeMap attributes = ((ElementImpl) node).getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    normalize(attributes.item(i));
                }
                break;
            case (short) 2:
                checkTextValidity(((AttrImpl) node).getValue());
                return;
            case (short) 3:
                break;
            case (short) 4:
                CDATASectionImpl cdata = (CDATASectionImpl) node;
                if (!this.cdataSections) {
                    node = cdata.replaceWithText();
                    break;
                }
                if (cdata.needsSplitting()) {
                    if (this.splitCdataSections) {
                        cdata.split();
                        report((short) 1, "cdata-sections-splitted");
                    } else {
                        report((short) 2, "wf-invalid-character");
                    }
                }
                checkTextValidity(cdata.buffer);
                return;
            case (short) 5:
            case (short) 6:
            case (short) 10:
            case (short) 12:
                return;
            case (short) 7:
                checkTextValidity(((ProcessingInstructionImpl) node).getData());
                return;
            case (short) 8:
                CommentImpl comment = (CommentImpl) node;
                if (this.comments) {
                    if (comment.containsDashDash()) {
                        report((short) 2, "wf-invalid-character");
                    }
                    checkTextValidity(comment.buffer);
                    return;
                }
                comment.getParentNode().removeChild(comment);
                return;
            case (short) 9:
            case (short) 11:
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported node type ");
                stringBuilder.append(node.getNodeType());
                throw new DOMException((short) 9, stringBuilder.toString());
        }
    }

    private void checkTextValidity(CharSequence s) {
        if (this.wellFormed && !isValid(s)) {
            report((short) 2, "wf-invalid-character");
        }
    }

    private boolean isValid(CharSequence text) {
        int i = 0;
        while (true) {
            boolean valid = true;
            if (i >= text.length()) {
                return true;
            }
            char c = text.charAt(i);
            if (!(c == 9 || c == 10 || c == 13 || ((c >= ' ' && c <= 55295) || (c >= 57344 && c <= 65533)))) {
                valid = false;
            }
            if (!valid) {
                return false;
            }
            i++;
        }
    }

    private void report(short severity, String type) {
        if (this.errorHandler != null) {
            this.errorHandler.handleError(new DOMErrorImpl(severity, type));
        }
    }
}
