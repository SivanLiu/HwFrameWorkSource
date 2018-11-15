package com.android.server.security.securityprofile;

import android.content.Context;
import android.util.Slog;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import huawei.android.security.securityprofile.ApkDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PolicyEngine {
    private static final String TAG = "SecurityProfileService";
    Map<String, Action> mActions = new HashMap();
    private PolicyDatabase mPolicyDatabase = null;
    Map<String, State> mStates = new HashMap();
    private Map<String, BBDNode> objectSubsystemOperationBBDNodes = new HashMap();
    private Map<String, Set<String>> subjectAndObjectLabels = new HashMap();
    private Map<String, BBDNode> subsystemOperationBBDNodes = new HashMap();

    public interface Action {
        void execute(int i);
    }

    abstract class BBDNode {
        public abstract void dump(String str);

        abstract boolean evaluate(Set<String> set, Set<String> set2, boolean z, int i);

        abstract BBDNode optimize(Map<String, Boolean> map, Map<String, Boolean> map2, Map<State, Boolean> map3);

        BBDNode() {
        }

        protected Map bindValue(Map map, Object key, Boolean value) {
            Map result = new HashMap(map);
            result.put(key, value);
            return result;
        }
    }

    interface RuleToFallthrough {
        BBDNode getFallthrough(JSONObject jSONObject) throws JSONException;
    }

    interface RuleToKey {
        String getKey(JSONObject jSONObject) throws JSONException;
    }

    public interface State {
        boolean evaluate();
    }

    class ActionNode extends BBDNode {
        Action action;
        BBDNode afterNode;

        public ActionNode(Action a, BBDNode an) {
            super();
            this.action = a;
            this.afterNode = an;
        }

        boolean evaluate(Set<String> subjectLabels, Set<String> objectLabels, boolean sideEffectsAllowed, int timeout) {
            if (sideEffectsAllowed) {
                this.action.execute(timeout);
            }
            return this.afterNode.evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
        }

        BBDNode optimize(Map<String, Boolean> subjectLabelBindings, Map<String, Boolean> objectLabelBindings, Map<State, Boolean> map) {
            return new ActionNode(this.action, this.afterNode.optimize(subjectLabelBindings, objectLabelBindings, new HashMap()));
        }

        public void dump(String indent) {
            String str = PolicyEngine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(String.valueOf(this.action.getClass()));
            Slog.d(str, stringBuilder.toString());
            BBDNode bBDNode = this.afterNode;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(" ");
            bBDNode.dump(stringBuilder.toString());
        }

        public boolean equals(Object o) {
            return (o instanceof ActionNode) && ((ActionNode) o).action.equals(this.action) && ((ActionNode) o).afterNode.equals(this.afterNode);
        }
    }

    class BooleanNode extends BBDNode {
        boolean value;

        public BooleanNode(boolean v) {
            super();
            this.value = v;
        }

        boolean evaluate(Set<String> set, Set<String> set2, boolean sideEffectsAllowed, int timeout) {
            return this.value;
        }

        BBDNode optimize(Map<String, Boolean> map, Map<String, Boolean> map2, Map<State, Boolean> map3) {
            return this;
        }

        public void dump(String indent) {
            String str = PolicyEngine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(String.valueOf(this.value));
            Slog.d(str, stringBuilder.toString());
        }

        public boolean equals(Object o) {
            return (o instanceof BooleanNode) && ((BooleanNode) o).value == this.value;
        }
    }

    class ObjectNode extends BBDNode {
        BBDNode falseNode;
        String label;
        BBDNode trueNode;

        public ObjectNode(String l, BBDNode tn, BBDNode fn) {
            super();
            this.label = l;
            this.trueNode = tn;
            this.falseNode = fn;
        }

        boolean evaluate(Set<String> subjectLabels, Set<String> objectLabels, boolean sideEffectsAllowed, int timeout) {
            if (objectLabels.contains(this.label)) {
                return this.trueNode.evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
            }
            return this.falseNode.evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
        }

        BBDNode optimize(Map<String, Boolean> subjectLabelBindings, Map<String, Boolean> objectLabelBindings, Map<State, Boolean> stateBindings) {
            if (!objectLabelBindings.containsKey(this.label)) {
                BBDNode t = this.trueNode.optimize(subjectLabelBindings, bindValue(objectLabelBindings, this.label, Boolean.valueOf(true)), stateBindings);
                BBDNode f = this.falseNode.optimize(subjectLabelBindings, bindValue(objectLabelBindings, this.label, Boolean.valueOf(false)), stateBindings);
                if (t.equals(f)) {
                    return t;
                }
                return new ObjectNode(this.label, t, f);
            } else if (((Boolean) objectLabelBindings.get(this.label)).booleanValue()) {
                return this.trueNode.optimize(subjectLabelBindings, objectLabelBindings, stateBindings);
            } else {
                return this.falseNode.optimize(subjectLabelBindings, objectLabelBindings, stateBindings);
            }
        }

        public void dump(String indent) {
            String str = PolicyEngine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(String.valueOf(this.label));
            Slog.d(str, stringBuilder.toString());
            BBDNode bBDNode = this.trueNode;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(" ");
            bBDNode.dump(stringBuilder.toString());
            bBDNode = this.falseNode;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(" ");
            bBDNode.dump(stringBuilder.toString());
        }

        public boolean equals(Object o) {
            return (o instanceof ObjectNode) && ((ObjectNode) o).label.equals(this.label) && ((ObjectNode) o).trueNode.equals(this.trueNode) && ((ObjectNode) o).falseNode.equals(this.falseNode);
        }
    }

    class StateNode extends BBDNode {
        BBDNode falseNode;
        State state;
        BBDNode trueNode;

        public StateNode(State s, BBDNode tn, BBDNode fn) {
            super();
            this.state = s;
            this.trueNode = tn;
            this.falseNode = fn;
        }

        boolean evaluate(Set<String> subjectLabels, Set<String> objectLabels, boolean sideEffectsAllowed, int timeout) {
            if (this.state.evaluate()) {
                return this.trueNode.evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
            }
            return this.falseNode.evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
        }

        BBDNode optimize(Map<String, Boolean> subjectLabelBindings, Map<String, Boolean> objectLabelBindings, Map<State, Boolean> stateBindings) {
            if (!stateBindings.containsKey(this.state)) {
                BBDNode t = this.trueNode.optimize(subjectLabelBindings, objectLabelBindings, bindValue(stateBindings, this.state, Boolean.valueOf(true)));
                BBDNode f = this.falseNode.optimize(subjectLabelBindings, objectLabelBindings, bindValue(stateBindings, this.state, Boolean.valueOf(false)));
                if (t.equals(f)) {
                    return t;
                }
                return new StateNode(this.state, t, f);
            } else if (((Boolean) stateBindings.get(this.state)).booleanValue()) {
                return this.trueNode.optimize(subjectLabelBindings, objectLabelBindings, stateBindings);
            } else {
                return this.falseNode.optimize(subjectLabelBindings, objectLabelBindings, stateBindings);
            }
        }

        public void dump(String indent) {
            String str = PolicyEngine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(String.valueOf(this.state.getClass()));
            Slog.d(str, stringBuilder.toString());
            BBDNode bBDNode = this.trueNode;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(" ");
            bBDNode.dump(stringBuilder.toString());
            bBDNode = this.falseNode;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(" ");
            bBDNode.dump(stringBuilder.toString());
        }

        public boolean equals(Object o) {
            return (o instanceof StateNode) && ((StateNode) o).state.equals(this.state) && ((StateNode) o).trueNode.equals(this.trueNode) && ((StateNode) o).falseNode.equals(this.falseNode);
        }
    }

    class SubjectNode extends BBDNode {
        BBDNode falseNode;
        String label;
        BBDNode trueNode;

        public SubjectNode(String l, BBDNode tn, BBDNode fn) {
            super();
            this.label = l;
            this.trueNode = tn;
            this.falseNode = fn;
        }

        boolean evaluate(Set<String> subjectLabels, Set<String> objectLabels, boolean sideEffectsAllowed, int timeout) {
            if (subjectLabels.contains(this.label)) {
                return this.trueNode.evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
            }
            return this.falseNode.evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
        }

        BBDNode optimize(Map<String, Boolean> subjectLabelBindings, Map<String, Boolean> objectLabelBindings, Map<State, Boolean> stateBindings) {
            if (!subjectLabelBindings.containsKey(this.label)) {
                BBDNode t = this.trueNode.optimize(bindValue(subjectLabelBindings, this.label, Boolean.valueOf(true)), objectLabelBindings, stateBindings);
                BBDNode f = this.falseNode.optimize(bindValue(subjectLabelBindings, this.label, Boolean.valueOf(false)), objectLabelBindings, stateBindings);
                if (t.equals(f)) {
                    return t;
                }
                return new SubjectNode(this.label, t, f);
            } else if (((Boolean) subjectLabelBindings.get(this.label)).booleanValue()) {
                return this.trueNode.optimize(subjectLabelBindings, objectLabelBindings, stateBindings);
            } else {
                return this.falseNode.optimize(subjectLabelBindings, objectLabelBindings, stateBindings);
            }
        }

        public void dump(String indent) {
            String str = PolicyEngine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(String.valueOf(this.label));
            Slog.d(str, stringBuilder.toString());
            BBDNode bBDNode = this.trueNode;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(" ");
            bBDNode.dump(stringBuilder.toString());
            bBDNode = this.falseNode;
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append(" ");
            bBDNode.dump(stringBuilder.toString());
        }

        public boolean equals(Object o) {
            return (o instanceof SubjectNode) && ((SubjectNode) o).label.equals(this.label) && ((SubjectNode) o).trueNode.equals(this.trueNode) && ((SubjectNode) o).falseNode.equals(this.falseNode);
        }
    }

    private void addLabeltoLookup(String subjectOrObject, String label) {
        if (!this.subjectAndObjectLabels.containsKey(subjectOrObject)) {
            this.subjectAndObjectLabels.put(subjectOrObject, new HashSet());
        }
        ((Set) this.subjectAndObjectLabels.get(subjectOrObject)).add(label);
    }

    private void addRuleSubjectAndObjectToLabelLookup(JSONObject rule) throws JSONException {
        String subject = rule.getString("subject");
        String object = rule.getString("object");
        addLabeltoLookup(subject, subject);
        addLabeltoLookup(object, object);
    }

    private void addRulesToLookup(JSONArray rules, RuleToFallthrough ruleToFallthrough, Map<String, BBDNode> lookup, RuleToKey ruleToKey) {
        JSONArray jSONArray = rules;
        Map<String, BBDNode> map = lookup;
        RuleToKey ruleToKey2 = ruleToKey;
        int i = rules.length() - 1;
        while (true) {
            int i2 = i;
            if (i2 >= 0) {
                try {
                    String key = ruleToKey2.getKey(jSONArray.getJSONObject(i2));
                    if (!map.containsKey(key)) {
                        BBDNode fallThrough = ruleToFallthrough.getFallthrough(jSONArray.getJSONObject(i2));
                        int j = i2;
                        while (j >= 0) {
                            JSONObject rule = jSONArray.getJSONObject(j);
                            addRuleSubjectAndObjectToLabelLookup(rule);
                            String decision = rule.getString("decision");
                            String ruleSubject = rule.getString("subject");
                            String ruleObject = rule.getString("object");
                            if (key.equals(ruleToKey2.getKey(rule))) {
                                BBDNode match;
                                if (decision.equals("deny")) {
                                    match = new BooleanNode(false);
                                } else {
                                    match = new BooleanNode(true);
                                }
                                if (decision.equals("allowafter") || decision.equals("allowif")) {
                                    State state = (State) this.mStates.get(rule.getJSONObject("conditions").getString("state"));
                                    if (state == null) {
                                        String str = TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Missing state handler for ");
                                        stringBuilder.append(rule.getJSONObject("conditions").getString("state"));
                                        Slog.e(str, stringBuilder.toString());
                                    } else {
                                        match = new StateNode(state, match, fallThrough);
                                    }
                                }
                                if (decision.equals("allowafter")) {
                                    Action action = (Action) this.mActions.get(rule.getJSONObject("conditions").getJSONObject(PreciseIgnore.RECEIVER_ACTION_ELEMENT_KEY).getString("name"));
                                    if (action == null) {
                                        String str2 = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Missing state handler for ");
                                        stringBuilder2.append(rule.getJSONObject("conditions").getJSONObject(PreciseIgnore.RECEIVER_ACTION_ELEMENT_KEY).getString("name"));
                                        Slog.e(str2, stringBuilder2.toString());
                                    } else {
                                        match = new ActionNode(action, match);
                                    }
                                }
                                if (!ruleObject.equals("ANY")) {
                                    match = new ObjectNode(ruleObject, match, fallThrough);
                                }
                                if (!ruleSubject.equals("ANY")) {
                                    match = new SubjectNode(ruleSubject, match, fallThrough);
                                }
                                fallThrough = match;
                            }
                            j--;
                            jSONArray = rules;
                            ruleToKey2 = ruleToKey;
                            RuleToFallthrough ruleToFallthrough2 = ruleToFallthrough;
                        }
                        map.put(key, fallThrough.optimize(new HashMap(), new HashMap(), new HashMap()));
                    }
                } catch (JSONException e) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("addRulesToLookup err:");
                    stringBuilder3.append(e.getMessage());
                    Slog.e(str3, stringBuilder3.toString());
                }
                i = i2 - 1;
                jSONArray = rules;
                ruleToKey2 = ruleToKey;
            } else {
                return;
            }
        }
    }

    public void createLookup() {
        StringBuilder stringBuilder;
        synchronized (this.mPolicyDatabase) {
            this.subsystemOperationBBDNodes = new HashMap();
            this.objectSubsystemOperationBBDNodes = new HashMap();
            this.subjectAndObjectLabels = new HashMap();
            JSONObject activePolicy = this.mPolicyDatabase.getPolicy();
            String packageName;
            try {
                addRulesToLookup(activePolicy.getJSONArray("rules"), new -$$Lambda$PolicyEngine$Ev97TXuQjn_50nSgWsrnm2ZJAmk(this), this.subsystemOperationBBDNodes, -$$Lambda$PolicyEngine$y8ZWBmdZk6eAk13PmjJk7plltsI.INSTANCE);
                try {
                    Iterator<String> keys = activePolicy.getJSONObject("domains").keys();
                    while (keys.hasNext()) {
                        packageName = (String) keys.next();
                        try {
                            JSONArray labels = activePolicy.getJSONObject("domains").getJSONObject(packageName).getJSONArray("labels");
                            for (int i = 0; i < labels.length(); i++) {
                                addLabeltoLookup(packageName, labels.getString(i));
                            }
                            JSONArray rules = activePolicy.getJSONObject("domains").getJSONObject(packageName).optJSONArray("rules");
                            if (rules != null) {
                                addRulesToLookup(rules, new -$$Lambda$PolicyEngine$x44nI3x7YQL8Bb-A3B4e32udp1g(this), this.objectSubsystemOperationBBDNodes, -$$Lambda$PolicyEngine$5_CQ6157GdeIWUC0jG5xZWuUJZM.INSTANCE);
                            }
                        } catch (JSONException e) {
                            String str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("packageName:");
                            stringBuilder2.append(packageName);
                            stringBuilder2.append(",err:");
                            stringBuilder2.append(e.getMessage());
                            Slog.e(str, stringBuilder2.toString());
                        }
                    }
                } catch (JSONException e2) {
                    packageName = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("rules err:");
                    stringBuilder.append(e2.getMessage());
                    Slog.e(packageName, stringBuilder.toString());
                }
            } catch (Exception e3) {
                packageName = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("createLookup err:");
                stringBuilder.append(e3.getMessage());
                Slog.e(packageName, stringBuilder.toString());
            }
        }
    }

    static /* synthetic */ String lambda$createLookup$1(JSONObject rule) throws JSONException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(rule.getString("subsystem"));
        stringBuilder.append(rule.getString("operation"));
        return stringBuilder.toString();
    }

    public static /* synthetic */ BBDNode lambda$createLookup$2(PolicyEngine policyEngine, JSONObject rule) throws JSONException {
        Map map = policyEngine.subsystemOperationBBDNodes;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(rule.getString("subsystem"));
        stringBuilder.append(rule.getString("operation"));
        return (BBDNode) map.getOrDefault(stringBuilder.toString(), new BooleanNode(false));
    }

    static /* synthetic */ String lambda$createLookup$3(JSONObject rule) throws JSONException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(rule.getString("object"));
        stringBuilder.append(rule.getString("subsystem"));
        stringBuilder.append(rule.getString("operation"));
        return stringBuilder.toString();
    }

    public PolicyEngine(Context context) {
        this.mPolicyDatabase = new PolicyDatabase(context);
    }

    public void start() {
        createLookup();
    }

    public void addPolicy(JSONObject policy) {
        synchronized (this.mPolicyDatabase) {
            this.mPolicyDatabase.addPolicy(policy);
            createLookup();
        }
    }

    public void addBlackApp(List<String> packageList) {
        if (packageList != null && this.mPolicyDatabase != null) {
            synchronized (this.mPolicyDatabase) {
                this.mPolicyDatabase.addLabel(packageList, "Black");
                createLookup();
            }
        }
    }

    public void removeBlackApp(List<String> packageList) {
        if (packageList != null && this.mPolicyDatabase != null) {
            synchronized (this.mPolicyDatabase) {
                this.mPolicyDatabase.removeLabel(packageList, "Black");
                createLookup();
            }
        }
    }

    public void updateBlackApp(List<String> packageList) {
        if (packageList != null && this.mPolicyDatabase != null) {
            synchronized (this.mPolicyDatabase) {
                this.mPolicyDatabase.removeLabel("Black");
                this.mPolicyDatabase.addLabel(packageList, "Black");
                createLookup();
            }
        }
    }

    public void updatePackageInformation(String packageName) {
        synchronized (this.mPolicyDatabase) {
            this.mPolicyDatabase.updatePackageInformation(packageName);
            createLookup();
        }
    }

    protected void setPackageSigned(String packageName, boolean isPackageSigned) {
        this.mPolicyDatabase.setPackageSigned(packageName, isPackageSigned);
    }

    private boolean hasLabel(String packageName, String label) {
        return ((Set) this.subjectAndObjectLabels.getOrDefault(packageName, new HashSet())).contains(label);
    }

    public void addState(String name, State state) {
        this.mStates.put(name, state);
    }

    public void addAction(String name, Action action) {
        this.mActions.put(name, action);
    }

    private boolean findRulesAndEvaluate(String subject, String object, String extraObjectLabel, String subsystem, String operation, boolean sideEffectsAllowed, int timeout) {
        try {
            Set<String> subjectLabels = (Set) this.subjectAndObjectLabels.getOrDefault(subject, new HashSet());
            Set<String> objectLabels = (Set) this.subjectAndObjectLabels.getOrDefault(object, new HashSet());
            if (extraObjectLabel != null) {
                objectLabels.add(extraObjectLabel);
            }
            Map map = this.objectSubsystemOperationBBDNodes;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(object);
            stringBuilder.append(subsystem);
            stringBuilder.append(operation);
            if (map.containsKey(stringBuilder.toString())) {
                map = this.objectSubsystemOperationBBDNodes;
                stringBuilder = new StringBuilder();
                stringBuilder.append(object);
                stringBuilder.append(subsystem);
                stringBuilder.append(operation);
                return ((BBDNode) map.get(stringBuilder.toString())).evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
            }
            map = this.subsystemOperationBBDNodes;
            stringBuilder = new StringBuilder();
            stringBuilder.append(subsystem);
            stringBuilder.append(operation);
            if (map.containsKey(stringBuilder.toString())) {
                map = this.subsystemOperationBBDNodes;
                stringBuilder = new StringBuilder();
                stringBuilder.append(subsystem);
                stringBuilder.append(operation);
                return ((BBDNode) map.get(stringBuilder.toString())).evaluate(subjectLabels, objectLabels, sideEffectsAllowed, timeout);
            }
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(subsystem);
            stringBuilder.append(operation);
            Slog.e(str, stringBuilder.toString());
            return true;
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
            return true;
        }
    }

    public boolean checkAccess(String subject, String object, String subsystem, String operation) {
        return findRulesAndEvaluate(subject, object, null, subsystem, operation, false, 0);
    }

    public boolean requestAccessWithExtraLabel(String subject, String object, String extraObjectLabel, String subsystem, String operation, int timeout) {
        return findRulesAndEvaluate(subject, object, extraObjectLabel, subsystem, operation, true, timeout);
    }

    public boolean requestAccess(String subject, String object, String subsystem, String operation, int timeout) {
        return requestAccessWithExtraLabel(subject, object, null, subsystem, operation, timeout);
    }

    public List<String> getLabels(String packageName, ApkDigest apkDigest) {
        return this.mPolicyDatabase.getLabels(packageName, apkDigest);
    }

    public boolean isBlackApp(String packageName) {
        return hasLabel(packageName, "Black");
    }

    public boolean isNewVersionFirstBoot() {
        return this.mPolicyDatabase.isNewVersionFirstBoot();
    }

    public boolean isPackageSigned(String packageName) {
        return this.mPolicyDatabase.isPackageSigned(packageName);
    }
}
