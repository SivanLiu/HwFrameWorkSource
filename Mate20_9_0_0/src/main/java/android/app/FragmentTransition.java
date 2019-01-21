package android.app;

import android.graphics.Rect;
import android.transition.Transition;
import android.transition.Transition.EpicenterCallback;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.view.OneShotPreDrawListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class FragmentTransition {
    private static final int[] INVERSE_OPS = new int[]{0, 3, 0, 1, 5, 4, 7, 6, 9, 8};

    public static class FragmentContainerTransition {
        public Fragment firstOut;
        public boolean firstOutIsPop;
        public BackStackRecord firstOutTransaction;
        public Fragment lastIn;
        public boolean lastInIsPop;
        public BackStackRecord lastInTransaction;
    }

    FragmentTransition() {
    }

    static void startTransitions(FragmentManagerImpl fragmentManager, ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex, boolean isReordered) {
        if (fragmentManager.mCurState >= 1) {
            SparseArray<FragmentContainerTransition> transitioningFragments = new SparseArray();
            for (int i = startIndex; i < endIndex; i++) {
                BackStackRecord record = (BackStackRecord) records.get(i);
                if (((Boolean) isRecordPop.get(i)).booleanValue()) {
                    calculatePopFragments(record, transitioningFragments, isReordered);
                } else {
                    calculateFragments(record, transitioningFragments, isReordered);
                }
            }
            if (transitioningFragments.size() != 0) {
                View nonExistentView = new View(fragmentManager.mHost.getContext());
                int numContainers = transitioningFragments.size();
                for (int i2 = 0; i2 < numContainers; i2++) {
                    int containerId = transitioningFragments.keyAt(i2);
                    ArrayMap<String, String> nameOverrides = calculateNameOverrides(containerId, records, isRecordPop, startIndex, endIndex);
                    FragmentContainerTransition containerTransition = (FragmentContainerTransition) transitioningFragments.valueAt(i2);
                    if (isReordered) {
                        configureTransitionsReordered(fragmentManager, containerId, containerTransition, nonExistentView, nameOverrides);
                    } else {
                        configureTransitionsOrdered(fragmentManager, containerId, containerTransition, nonExistentView, nameOverrides);
                    }
                }
            }
        }
    }

    private static ArrayMap<String, String> calculateNameOverrides(int containerId, ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        ArrayMap<String, String> nameOverrides = new ArrayMap();
        for (int recordNum = endIndex - 1; recordNum >= startIndex; recordNum--) {
            BackStackRecord record = (BackStackRecord) records.get(recordNum);
            if (record.interactsWith(containerId)) {
                boolean isPop = ((Boolean) isRecordPop.get(recordNum)).booleanValue();
                if (record.mSharedElementSourceNames != null) {
                    ArrayList<String> targets;
                    ArrayList<String> sources;
                    int numSharedElements = record.mSharedElementSourceNames.size();
                    if (isPop) {
                        targets = record.mSharedElementSourceNames;
                        sources = record.mSharedElementTargetNames;
                    } else {
                        sources = record.mSharedElementSourceNames;
                        targets = record.mSharedElementTargetNames;
                    }
                    for (int i = 0; i < numSharedElements; i++) {
                        String sourceName = (String) sources.get(i);
                        String targetName = (String) targets.get(i);
                        String previousTarget = (String) nameOverrides.remove(targetName);
                        if (previousTarget != null) {
                            nameOverrides.put(sourceName, previousTarget);
                        } else {
                            nameOverrides.put(sourceName, targetName);
                        }
                    }
                }
            }
        }
        return nameOverrides;
    }

    private static void configureTransitionsReordered(FragmentManagerImpl fragmentManager, int containerId, FragmentContainerTransition fragments, View nonExistentView, ArrayMap<String, String> nameOverrides) {
        FragmentManagerImpl fragmentManagerImpl = fragmentManager;
        FragmentContainerTransition fragmentContainerTransition = fragments;
        View view = nonExistentView;
        ViewGroup sceneRoot = null;
        if (fragmentManagerImpl.mContainer.onHasView()) {
            sceneRoot = (ViewGroup) fragmentManagerImpl.mContainer.onFindViewById(containerId);
        } else {
            int i = containerId;
        }
        ViewGroup sceneRoot2 = sceneRoot;
        if (sceneRoot2 != null) {
            Transition exitTransition;
            Fragment inFragment = fragmentContainerTransition.lastIn;
            Fragment outFragment = fragmentContainerTransition.firstOut;
            boolean inIsPop = fragmentContainerTransition.lastInIsPop;
            boolean outIsPop = fragmentContainerTransition.firstOutIsPop;
            ArrayList<View> sharedElementsIn = new ArrayList();
            ArrayList<View> sharedElementsOut = new ArrayList();
            Transition enterTransition = getEnterTransition(inFragment, inIsPop);
            Transition exitTransition2 = getExitTransition(outFragment, outIsPop);
            Transition enterTransition2 = enterTransition;
            ArrayList<View> sharedElementsOut2 = sharedElementsOut;
            ArrayList<View> sharedElementsIn2 = sharedElementsIn;
            TransitionSet sharedElementTransition = configureSharedElementsReordered(sceneRoot2, view, nameOverrides, fragmentContainerTransition, sharedElementsOut, sharedElementsIn, enterTransition2, exitTransition2);
            Transition enterTransition3 = enterTransition2;
            if (enterTransition3 == null && sharedElementTransition == null) {
                exitTransition = exitTransition2;
                if (exitTransition == null) {
                    return;
                }
            }
            exitTransition = exitTransition2;
            ArrayList<View> exitingViews = configureEnteringExitingViews(exitTransition, outFragment, sharedElementsOut2, view);
            ArrayList<View> enteringViews = configureEnteringExitingViews(enterTransition3, inFragment, sharedElementsIn2, view);
            setViewVisibility(enteringViews, 4);
            Transition transition = mergeTransitions(enterTransition3, exitTransition, sharedElementTransition, inFragment, inIsPop);
            if (transition != null) {
                replaceHide(exitTransition, outFragment, exitingViews);
                transition.setNameOverrides(nameOverrides);
                scheduleRemoveTargets(transition, enterTransition3, enteringViews, exitTransition, exitingViews, sharedElementTransition, sharedElementsIn2);
                TransitionManager.beginDelayedTransition(sceneRoot2, transition);
                setViewVisibility(enteringViews, 0);
                if (sharedElementTransition != null) {
                    sharedElementTransition.getTargets().clear();
                    sharedElementTransition.getTargets().addAll(sharedElementsIn2);
                    replaceTargets(sharedElementTransition, sharedElementsOut2, sharedElementsIn2);
                }
            } else {
                ArrayMap<String, String> arrayMap = nameOverrides;
            }
        }
    }

    private static void configureTransitionsOrdered(FragmentManagerImpl fragmentManager, int containerId, FragmentContainerTransition fragments, View nonExistentView, ArrayMap<String, String> nameOverrides) {
        FragmentManagerImpl fragmentManagerImpl = fragmentManager;
        FragmentContainerTransition fragmentContainerTransition = fragments;
        View view = nonExistentView;
        ViewGroup sceneRoot = null;
        if (fragmentManagerImpl.mContainer.onHasView()) {
            sceneRoot = (ViewGroup) fragmentManagerImpl.mContainer.onFindViewById(containerId);
        } else {
            int i = containerId;
        }
        ViewGroup sceneRoot2 = sceneRoot;
        if (sceneRoot2 != null) {
            Transition exitTransition;
            Fragment inFragment = fragmentContainerTransition.lastIn;
            Fragment outFragment = fragmentContainerTransition.firstOut;
            boolean inIsPop = fragmentContainerTransition.lastInIsPop;
            boolean outIsPop = fragmentContainerTransition.firstOutIsPop;
            Transition enterTransition = getEnterTransition(inFragment, inIsPop);
            Transition exitTransition2 = getExitTransition(outFragment, outIsPop);
            ArrayList<View> sharedElementsOut = new ArrayList();
            ArrayList<View> sharedElementsIn = new ArrayList();
            ArrayList<View> sharedElementsOut2 = sharedElementsOut;
            Transition exitTransition3 = exitTransition2;
            Transition enterTransition2 = enterTransition;
            TransitionSet sharedElementTransition = configureSharedElementsOrdered(sceneRoot2, view, nameOverrides, fragmentContainerTransition, sharedElementsOut2, sharedElementsIn, enterTransition, exitTransition3);
            Transition enterTransition3 = enterTransition2;
            if (enterTransition3 == null && sharedElementTransition == null) {
                exitTransition = exitTransition3;
                if (exitTransition == null) {
                    return;
                }
            }
            exitTransition = exitTransition3;
            ArrayList<View> exitingViews = configureEnteringExitingViews(exitTransition, outFragment, sharedElementsOut2, view);
            if (exitingViews == null || exitingViews.isEmpty()) {
                exitTransition = null;
            }
            if (enterTransition3 != null) {
                enterTransition3.addTarget(view);
            }
            enterTransition = mergeTransitions(enterTransition3, exitTransition, sharedElementTransition, inFragment, fragmentContainerTransition.lastInIsPop);
            ViewGroup sceneRoot3;
            if (enterTransition != null) {
                enterTransition.setNameOverrides(nameOverrides);
                ArrayList<View> enteringViews = new ArrayList();
                scheduleRemoveTargets(enterTransition, enterTransition3, enteringViews, exitTransition, exitingViews, sharedElementTransition, sharedElementsIn);
                sceneRoot3 = sceneRoot2;
                scheduleTargetChange(sceneRoot2, inFragment, view, sharedElementsIn, enterTransition3, enteringViews, exitTransition, exitingViews);
                TransitionManager.beginDelayedTransition(sceneRoot3, enterTransition);
            } else {
                ArrayMap<String, String> arrayMap = nameOverrides;
                boolean z = inIsPop;
                Fragment fragment = outFragment;
                Fragment fragment2 = inFragment;
                sceneRoot3 = sceneRoot2;
            }
        }
    }

    private static void replaceHide(Transition exitTransition, Fragment exitingFragment, final ArrayList<View> exitingViews) {
        if (exitingFragment != null && exitTransition != null && exitingFragment.mAdded && exitingFragment.mHidden && exitingFragment.mHiddenChanged) {
            exitingFragment.setHideReplaced(true);
            final View fragmentView = exitingFragment.getView();
            OneShotPreDrawListener.add(exitingFragment.mContainer, new -$$Lambda$FragmentTransition$PZ32bJ_FSMpbzYzBl8x73NJPidQ(exitingViews));
            exitTransition.addListener(new TransitionListenerAdapter() {
                public void onTransitionEnd(Transition transition) {
                    transition.removeListener(this);
                    fragmentView.setVisibility(8);
                    FragmentTransition.setViewVisibility(exitingViews, 0);
                }
            });
        }
    }

    private static void scheduleTargetChange(ViewGroup sceneRoot, Fragment inFragment, View nonExistentView, ArrayList<View> sharedElementsIn, Transition enterTransition, ArrayList<View> enteringViews, Transition exitTransition, ArrayList<View> exitingViews) {
        OneShotPreDrawListener.add(sceneRoot, new -$$Lambda$FragmentTransition$8Ei4ls5jlZcfRvuLcweFAxtFBFs(enterTransition, nonExistentView, inFragment, sharedElementsIn, enteringViews, exitingViews, exitTransition));
    }

    static /* synthetic */ void lambda$scheduleTargetChange$1(Transition enterTransition, View nonExistentView, Fragment inFragment, ArrayList sharedElementsIn, ArrayList enteringViews, ArrayList exitingViews, Transition exitTransition) {
        if (enterTransition != null) {
            enterTransition.removeTarget(nonExistentView);
            enteringViews.addAll(configureEnteringExitingViews(enterTransition, inFragment, sharedElementsIn, nonExistentView));
        }
        if (exitingViews != null) {
            if (exitTransition != null) {
                ArrayList<View> tempExiting = new ArrayList();
                tempExiting.add(nonExistentView);
                replaceTargets(exitTransition, exitingViews, tempExiting);
            }
            exitingViews.clear();
            exitingViews.add(nonExistentView);
        }
    }

    private static TransitionSet getSharedElementTransition(Fragment inFragment, Fragment outFragment, boolean isPop) {
        if (inFragment == null || outFragment == null) {
            return null;
        }
        Transition transition;
        if (isPop) {
            transition = outFragment.getSharedElementReturnTransition();
        } else {
            transition = inFragment.getSharedElementEnterTransition();
        }
        transition = cloneTransition(transition);
        if (transition == null) {
            return null;
        }
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(transition);
        return transitionSet;
    }

    private static Transition getEnterTransition(Fragment inFragment, boolean isPop) {
        if (inFragment == null) {
            return null;
        }
        Transition reenterTransition;
        if (isPop) {
            reenterTransition = inFragment.getReenterTransition();
        } else {
            reenterTransition = inFragment.getEnterTransition();
        }
        return cloneTransition(reenterTransition);
    }

    private static Transition getExitTransition(Fragment outFragment, boolean isPop) {
        if (outFragment == null) {
            return null;
        }
        Transition returnTransition;
        if (isPop) {
            returnTransition = outFragment.getReturnTransition();
        } else {
            returnTransition = outFragment.getExitTransition();
        }
        return cloneTransition(returnTransition);
    }

    private static Transition cloneTransition(Transition transition) {
        if (transition != null) {
            return transition.clone();
        }
        return transition;
    }

    private static TransitionSet configureSharedElementsReordered(ViewGroup sceneRoot, View nonExistentView, ArrayMap<String, String> nameOverrides, FragmentContainerTransition fragments, ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn, Transition enterTransition, Transition exitTransition) {
        View view = nonExistentView;
        ArrayMap<String, String> arrayMap = nameOverrides;
        FragmentContainerTransition fragmentContainerTransition = fragments;
        ArrayList<View> arrayList = sharedElementsOut;
        ArrayList<View> arrayList2 = sharedElementsIn;
        Transition transition = enterTransition;
        Transition transition2 = exitTransition;
        Fragment inFragment = fragmentContainerTransition.lastIn;
        Fragment outFragment = fragmentContainerTransition.firstOut;
        if (!(inFragment == null || inFragment.getView() == null)) {
            inFragment.getView().setVisibility(0);
        }
        if (inFragment == null || outFragment == null) {
            ViewGroup viewGroup = sceneRoot;
            return null;
        }
        boolean inIsPop = fragmentContainerTransition.lastInIsPop;
        TransitionSet sharedElementTransition = nameOverrides.isEmpty() ? null : getSharedElementTransition(inFragment, outFragment, inIsPop);
        ArrayMap<String, View> outSharedElements = captureOutSharedElements(arrayMap, sharedElementTransition, fragmentContainerTransition);
        ArrayMap<String, View> inSharedElements = captureInSharedElements(arrayMap, sharedElementTransition, fragmentContainerTransition);
        if (nameOverrides.isEmpty()) {
            sharedElementTransition = null;
            if (outSharedElements != null) {
                outSharedElements.clear();
            }
            if (inSharedElements != null) {
                inSharedElements.clear();
            }
        } else {
            addSharedElementsWithMatchingNames(arrayList, outSharedElements, nameOverrides.keySet());
            addSharedElementsWithMatchingNames(arrayList2, inSharedElements, nameOverrides.values());
        }
        TransitionSet sharedElementTransition2 = sharedElementTransition;
        if (transition == null && transition2 == null && sharedElementTransition2 == null) {
            return null;
        }
        Rect epicenter;
        View epicenterView;
        callSharedElementStartEnd(inFragment, outFragment, inIsPop, outSharedElements, true);
        if (sharedElementTransition2 != null) {
            arrayList2.add(view);
            setSharedElementTargets(sharedElementTransition2, view, arrayList);
            setOutEpicenter(sharedElementTransition2, transition2, outSharedElements, fragmentContainerTransition.firstOutIsPop, fragmentContainerTransition.firstOutTransaction);
            final Rect epicenter2 = new Rect();
            View epicenterView2 = getInEpicenterView(inSharedElements, fragmentContainerTransition, transition, inIsPop);
            if (epicenterView2 != null) {
                transition.setEpicenterCallback(new EpicenterCallback() {
                    public Rect onGetEpicenter(Transition transition) {
                        return epicenter2;
                    }
                });
            }
            epicenter = epicenter2;
            epicenterView = epicenterView2;
        } else {
            epicenter = null;
            epicenterView = null;
        }
        -$$Lambda$FragmentTransition$jurn0WXuKw3bRQ_2d5zCWdeZWuI -__lambda_fragmenttransition_jurn0wxukw3brq_2d5zcwdezwui = r7;
        TransitionSet sharedElementTransition3 = sharedElementTransition2;
        -$$Lambda$FragmentTransition$jurn0WXuKw3bRQ_2d5zCWdeZWuI -__lambda_fragmenttransition_jurn0wxukw3brq_2d5zcwdezwui2 = new -$$Lambda$FragmentTransition$jurn0WXuKw3bRQ_2d5zCWdeZWuI(inFragment, outFragment, inIsPop, inSharedElements, epicenterView, epicenter);
        OneShotPreDrawListener.add(sceneRoot, -__lambda_fragmenttransition_jurn0wxukw3brq_2d5zcwdezwui);
        return sharedElementTransition3;
    }

    static /* synthetic */ void lambda$configureSharedElementsReordered$2(Fragment inFragment, Fragment outFragment, boolean inIsPop, ArrayMap inSharedElements, View epicenterView, Rect epicenter) {
        callSharedElementStartEnd(inFragment, outFragment, inIsPop, inSharedElements, false);
        if (epicenterView != null) {
            epicenterView.getBoundsOnScreen(epicenter);
        }
    }

    private static void addSharedElementsWithMatchingNames(ArrayList<View> views, ArrayMap<String, View> sharedElements, Collection<String> nameOverridesSet) {
        for (int i = sharedElements.size() - 1; i >= 0; i--) {
            View view = (View) sharedElements.valueAt(i);
            if (view != null && nameOverridesSet.contains(view.getTransitionName())) {
                views.add(view);
            }
        }
    }

    private static TransitionSet configureSharedElementsOrdered(ViewGroup sceneRoot, View nonExistentView, ArrayMap<String, String> nameOverrides, FragmentContainerTransition fragments, ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn, Transition enterTransition, Transition exitTransition) {
        FragmentContainerTransition fragmentContainerTransition = fragments;
        ArrayList<View> arrayList = sharedElementsOut;
        Transition transition = enterTransition;
        Transition transition2 = exitTransition;
        Fragment inFragment = fragmentContainerTransition.lastIn;
        Fragment outFragment = fragmentContainerTransition.firstOut;
        Rect rect = null;
        ViewGroup viewGroup;
        Fragment fragment;
        Fragment fragment2;
        if (inFragment == null) {
            viewGroup = sceneRoot;
            fragment = outFragment;
            fragment2 = inFragment;
        } else if (outFragment == null) {
            viewGroup = sceneRoot;
            fragment = outFragment;
            fragment2 = inFragment;
        } else {
            boolean z = fragmentContainerTransition.lastInIsPop;
            TransitionSet sharedElementTransition = nameOverrides.isEmpty() ? null : getSharedElementTransition(inFragment, outFragment, z);
            ArrayMap<String, String> arrayMap = nameOverrides;
            ArrayMap<String, View> outSharedElements = captureOutSharedElements(arrayMap, sharedElementTransition, fragmentContainerTransition);
            if (nameOverrides.isEmpty()) {
                sharedElementTransition = null;
            } else {
                arrayList.addAll(outSharedElements.values());
            }
            TransitionSet sharedElementTransition2 = sharedElementTransition;
            if (transition == null && transition2 == null && sharedElementTransition2 == null) {
                return null;
            }
            callSharedElementStartEnd(inFragment, outFragment, z, outSharedElements, true);
            if (sharedElementTransition2 != null) {
                rect = new Rect();
                setSharedElementTargets(sharedElementTransition2, nonExistentView, arrayList);
                setOutEpicenter(sharedElementTransition2, transition2, outSharedElements, fragmentContainerTransition.firstOutIsPop, fragmentContainerTransition.firstOutTransaction);
                if (transition != null) {
                    transition.setEpicenterCallback(new EpicenterCallback() {
                        public Rect onGetEpicenter(Transition transition) {
                            if (rect.isEmpty()) {
                                return null;
                            }
                            return rect;
                        }
                    });
                }
            } else {
                View view = nonExistentView;
            }
            -$$Lambda$FragmentTransition$Ip0LktADPhG_3ouNBXgzufWpFfY -__lambda_fragmenttransition_ip0lktadphg_3ounbxgzufwpffy = r0;
            TransitionSet sharedElementTransition3 = sharedElementTransition2;
            boolean inIsPop = z;
            -$$Lambda$FragmentTransition$Ip0LktADPhG_3ouNBXgzufWpFfY -__lambda_fragmenttransition_ip0lktadphg_3ounbxgzufwpffy2 = new -$$Lambda$FragmentTransition$Ip0LktADPhG_3ouNBXgzufWpFfY(arrayMap, sharedElementTransition2, fragmentContainerTransition, sharedElementsIn, nonExistentView, inFragment, outFragment, z, arrayList, transition, rect);
            OneShotPreDrawListener.add(sceneRoot, -__lambda_fragmenttransition_ip0lktadphg_3ounbxgzufwpffy);
            return sharedElementTransition3;
        }
        return null;
    }

    static /* synthetic */ void lambda$configureSharedElementsOrdered$3(ArrayMap nameOverrides, TransitionSet finalSharedElementTransition, FragmentContainerTransition fragments, ArrayList sharedElementsIn, View nonExistentView, Fragment inFragment, Fragment outFragment, boolean inIsPop, ArrayList sharedElementsOut, Transition enterTransition, Rect inEpicenter) {
        ArrayMap<String, View> inSharedElements = captureInSharedElements(nameOverrides, finalSharedElementTransition, fragments);
        if (inSharedElements != null) {
            sharedElementsIn.addAll(inSharedElements.values());
            sharedElementsIn.add(nonExistentView);
        }
        callSharedElementStartEnd(inFragment, outFragment, inIsPop, inSharedElements, false);
        if (finalSharedElementTransition != null) {
            finalSharedElementTransition.getTargets().clear();
            finalSharedElementTransition.getTargets().addAll(sharedElementsIn);
            replaceTargets(finalSharedElementTransition, sharedElementsOut, sharedElementsIn);
            View inEpicenterView = getInEpicenterView(inSharedElements, fragments, enterTransition, inIsPop);
            if (inEpicenterView != null) {
                inEpicenterView.getBoundsOnScreen(inEpicenter);
            }
        }
    }

    private static ArrayMap<String, View> captureOutSharedElements(ArrayMap<String, String> nameOverrides, TransitionSet sharedElementTransition, FragmentContainerTransition fragments) {
        if (nameOverrides.isEmpty() || sharedElementTransition == null) {
            nameOverrides.clear();
            return null;
        }
        SharedElementCallback sharedElementCallback;
        ArrayList<String> names;
        Fragment outFragment = fragments.firstOut;
        ArrayMap<String, View> outSharedElements = new ArrayMap();
        outFragment.getView().findNamedViews(outSharedElements);
        BackStackRecord outTransaction = fragments.firstOutTransaction;
        if (fragments.firstOutIsPop) {
            sharedElementCallback = outFragment.getEnterTransitionCallback();
            names = outTransaction.mSharedElementTargetNames;
        } else {
            sharedElementCallback = outFragment.getExitTransitionCallback();
            names = outTransaction.mSharedElementSourceNames;
        }
        outSharedElements.retainAll(names);
        if (sharedElementCallback != null) {
            sharedElementCallback.onMapSharedElements(names, outSharedElements);
            for (int i = names.size() - 1; i >= 0; i--) {
                String name = (String) names.get(i);
                View view = (View) outSharedElements.get(name);
                if (view == null) {
                    nameOverrides.remove(name);
                } else if (!name.equals(view.getTransitionName())) {
                    nameOverrides.put(view.getTransitionName(), (String) nameOverrides.remove(name));
                }
            }
        } else {
            nameOverrides.retainAll(outSharedElements.keySet());
        }
        return outSharedElements;
    }

    private static ArrayMap<String, View> captureInSharedElements(ArrayMap<String, String> nameOverrides, TransitionSet sharedElementTransition, FragmentContainerTransition fragments) {
        Fragment inFragment = fragments.lastIn;
        View fragmentView = inFragment.getView();
        if (nameOverrides.isEmpty() || sharedElementTransition == null || fragmentView == null) {
            nameOverrides.clear();
            return null;
        }
        SharedElementCallback sharedElementCallback;
        ArrayList<String> names;
        ArrayMap<String, View> inSharedElements = new ArrayMap();
        fragmentView.findNamedViews(inSharedElements);
        BackStackRecord inTransaction = fragments.lastInTransaction;
        if (fragments.lastInIsPop) {
            sharedElementCallback = inFragment.getExitTransitionCallback();
            names = inTransaction.mSharedElementSourceNames;
        } else {
            sharedElementCallback = inFragment.getEnterTransitionCallback();
            names = inTransaction.mSharedElementTargetNames;
        }
        if (names != null) {
            inSharedElements.retainAll(names);
        }
        if (names == null || sharedElementCallback == null) {
            retainValues(nameOverrides, inSharedElements);
        } else {
            sharedElementCallback.onMapSharedElements(names, inSharedElements);
            for (int i = names.size() - 1; i >= 0; i--) {
                String name = (String) names.get(i);
                View view = (View) inSharedElements.get(name);
                String key;
                if (view == null) {
                    key = findKeyForValue(nameOverrides, name);
                    if (key != null) {
                        nameOverrides.remove(key);
                    }
                } else if (!name.equals(view.getTransitionName())) {
                    key = findKeyForValue(nameOverrides, name);
                    if (key != null) {
                        nameOverrides.put(key, view.getTransitionName());
                    }
                }
            }
        }
        return inSharedElements;
    }

    private static String findKeyForValue(ArrayMap<String, String> map, String value) {
        int numElements = map.size();
        for (int i = 0; i < numElements; i++) {
            if (value.equals(map.valueAt(i))) {
                return (String) map.keyAt(i);
            }
        }
        return null;
    }

    private static View getInEpicenterView(ArrayMap<String, View> inSharedElements, FragmentContainerTransition fragments, Transition enterTransition, boolean inIsPop) {
        BackStackRecord inTransaction = fragments.lastInTransaction;
        if (enterTransition == null || inSharedElements == null || inTransaction.mSharedElementSourceNames == null || inTransaction.mSharedElementSourceNames.isEmpty()) {
            return null;
        }
        String targetName;
        if (inIsPop) {
            targetName = (String) inTransaction.mSharedElementSourceNames.get(0);
        } else {
            targetName = (String) inTransaction.mSharedElementTargetNames.get(0);
        }
        return (View) inSharedElements.get(targetName);
    }

    private static void setOutEpicenter(TransitionSet sharedElementTransition, Transition exitTransition, ArrayMap<String, View> outSharedElements, boolean outIsPop, BackStackRecord outTransaction) {
        if (outTransaction.mSharedElementSourceNames != null && !outTransaction.mSharedElementSourceNames.isEmpty()) {
            String sourceName;
            if (outIsPop) {
                sourceName = (String) outTransaction.mSharedElementTargetNames.get(0);
            } else {
                sourceName = (String) outTransaction.mSharedElementSourceNames.get(0);
            }
            View outEpicenterView = (View) outSharedElements.get(sourceName);
            setEpicenter(sharedElementTransition, outEpicenterView);
            if (exitTransition != null) {
                setEpicenter(exitTransition, outEpicenterView);
            }
        }
    }

    private static void setEpicenter(Transition transition, View view) {
        if (view != null) {
            final Rect epicenter = new Rect();
            view.getBoundsOnScreen(epicenter);
            transition.setEpicenterCallback(new EpicenterCallback() {
                public Rect onGetEpicenter(Transition transition) {
                    return epicenter;
                }
            });
        }
    }

    private static void retainValues(ArrayMap<String, String> nameOverrides, ArrayMap<String, View> namedViews) {
        for (int i = nameOverrides.size() - 1; i >= 0; i--) {
            if (!namedViews.containsKey((String) nameOverrides.valueAt(i))) {
                nameOverrides.removeAt(i);
            }
        }
    }

    private static void callSharedElementStartEnd(Fragment inFragment, Fragment outFragment, boolean isPop, ArrayMap<String, View> sharedElements, boolean isStart) {
        SharedElementCallback sharedElementCallback;
        if (isPop) {
            sharedElementCallback = outFragment.getEnterTransitionCallback();
        } else {
            sharedElementCallback = inFragment.getEnterTransitionCallback();
        }
        if (sharedElementCallback != null) {
            ArrayList<View> views = new ArrayList();
            ArrayList<String> names = new ArrayList();
            int i = 0;
            int count = sharedElements == null ? 0 : sharedElements.size();
            while (i < count) {
                names.add((String) sharedElements.keyAt(i));
                views.add((View) sharedElements.valueAt(i));
                i++;
            }
            if (isStart) {
                sharedElementCallback.onSharedElementStart(names, views, null);
            } else {
                sharedElementCallback.onSharedElementEnd(names, views, null);
            }
        }
    }

    private static void setSharedElementTargets(TransitionSet transition, View nonExistentView, ArrayList<View> sharedViews) {
        List<View> views = transition.getTargets();
        views.clear();
        int count = sharedViews.size();
        for (int i = 0; i < count; i++) {
            bfsAddViewChildren(views, (View) sharedViews.get(i));
        }
        views.add(nonExistentView);
        sharedViews.add(nonExistentView);
        addTargets(transition, sharedViews);
    }

    private static void bfsAddViewChildren(List<View> views, View startView) {
        int startIndex = views.size();
        if (!containedBeforeIndex(views, startView, startIndex)) {
            views.add(startView);
            for (int index = startIndex; index < views.size(); index++) {
                View view = (View) views.get(index);
                if (view instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) view;
                    int childCount = viewGroup.getChildCount();
                    for (int childIndex = 0; childIndex < childCount; childIndex++) {
                        View child = viewGroup.getChildAt(childIndex);
                        if (!containedBeforeIndex(views, child, startIndex)) {
                            views.add(child);
                        }
                    }
                }
            }
        }
    }

    private static boolean containedBeforeIndex(List<View> views, View view, int maxIndex) {
        for (int i = 0; i < maxIndex; i++) {
            if (views.get(i) == view) {
                return true;
            }
        }
        return false;
    }

    private static void scheduleRemoveTargets(Transition overalTransition, Transition enterTransition, ArrayList<View> enteringViews, Transition exitTransition, ArrayList<View> exitingViews, TransitionSet sharedElementTransition, ArrayList<View> sharedElementsIn) {
        final Transition transition = enterTransition;
        final ArrayList<View> arrayList = enteringViews;
        final Transition transition2 = exitTransition;
        final ArrayList<View> arrayList2 = exitingViews;
        final TransitionSet transitionSet = sharedElementTransition;
        final ArrayList<View> arrayList3 = sharedElementsIn;
        overalTransition.addListener(new TransitionListenerAdapter() {
            public void onTransitionStart(Transition transition) {
                if (transition != null) {
                    FragmentTransition.replaceTargets(transition, arrayList, null);
                }
                if (transition2 != null) {
                    FragmentTransition.replaceTargets(transition2, arrayList2, null);
                }
                if (transitionSet != null) {
                    FragmentTransition.replaceTargets(transitionSet, arrayList3, null);
                }
            }
        });
    }

    public static void replaceTargets(Transition transition, ArrayList<View> oldTargets, ArrayList<View> newTargets) {
        int i = 0;
        int numTransitions;
        if (transition instanceof TransitionSet) {
            TransitionSet set = (TransitionSet) transition;
            numTransitions = set.getTransitionCount();
            while (i < numTransitions) {
                replaceTargets(set.getTransitionAt(i), oldTargets, newTargets);
                i++;
            }
        } else if (!hasSimpleTarget(transition)) {
            List<View> targets = transition.getTargets();
            if (targets != null && targets.size() == oldTargets.size() && targets.containsAll(oldTargets)) {
                numTransitions = newTargets == null ? 0 : newTargets.size();
                while (i < numTransitions) {
                    transition.addTarget((View) newTargets.get(i));
                    i++;
                }
                for (i = oldTargets.size() - 1; i >= 0; i--) {
                    transition.removeTarget((View) oldTargets.get(i));
                }
            }
        }
    }

    public static void addTargets(Transition transition, ArrayList<View> views) {
        if (transition != null) {
            int i = 0;
            int numTransitions;
            if (transition instanceof TransitionSet) {
                TransitionSet set = (TransitionSet) transition;
                numTransitions = set.getTransitionCount();
                while (i < numTransitions) {
                    addTargets(set.getTransitionAt(i), views);
                    i++;
                }
            } else if (!hasSimpleTarget(transition) && isNullOrEmpty(transition.getTargets())) {
                numTransitions = views.size();
                while (i < numTransitions) {
                    transition.addTarget((View) views.get(i));
                    i++;
                }
            }
        }
    }

    private static boolean hasSimpleTarget(Transition transition) {
        return (isNullOrEmpty(transition.getTargetIds()) && isNullOrEmpty(transition.getTargetNames()) && isNullOrEmpty(transition.getTargetTypes())) ? false : true;
    }

    private static boolean isNullOrEmpty(List list) {
        return list == null || list.isEmpty();
    }

    private static ArrayList<View> configureEnteringExitingViews(Transition transition, Fragment fragment, ArrayList<View> sharedElements, View nonExistentView) {
        ArrayList<View> viewList = null;
        if (transition != null) {
            viewList = new ArrayList();
            View root = fragment.getView();
            if (root != null) {
                root.captureTransitioningViews(viewList);
            }
            if (sharedElements != null) {
                viewList.removeAll(sharedElements);
            }
            if (!viewList.isEmpty()) {
                viewList.add(nonExistentView);
                addTargets(transition, viewList);
            }
        }
        return viewList;
    }

    private static void setViewVisibility(ArrayList<View> views, int visibility) {
        if (views != null) {
            for (int i = views.size() - 1; i >= 0; i--) {
                ((View) views.get(i)).setVisibility(visibility);
            }
        }
    }

    private static Transition mergeTransitions(Transition enterTransition, Transition exitTransition, Transition sharedElementTransition, Fragment inFragment, boolean isPop) {
        boolean overlap = true;
        if (!(enterTransition == null || exitTransition == null || inFragment == null)) {
            boolean allowReturnTransitionOverlap;
            if (isPop) {
                allowReturnTransitionOverlap = inFragment.getAllowReturnTransitionOverlap();
            } else {
                allowReturnTransitionOverlap = inFragment.getAllowEnterTransitionOverlap();
            }
            overlap = allowReturnTransitionOverlap;
        }
        Transition transition;
        if (overlap) {
            transition = new TransitionSet();
            if (enterTransition != null) {
                transition.addTransition(enterTransition);
            }
            if (exitTransition != null) {
                transition.addTransition(exitTransition);
            }
            if (sharedElementTransition == null) {
                return transition;
            }
            transition.addTransition(sharedElementTransition);
            return transition;
        }
        transition = null;
        if (exitTransition != null && enterTransition != null) {
            transition = new TransitionSet().addTransition(exitTransition).addTransition(enterTransition).setOrdering(1);
        } else if (exitTransition != null) {
            transition = exitTransition;
        } else if (enterTransition != null) {
            transition = enterTransition;
        }
        if (sharedElementTransition == null) {
            return transition;
        }
        Transition transition2 = new TransitionSet();
        if (transition != null) {
            transition2.addTransition(transition);
        }
        transition2.addTransition(sharedElementTransition);
        return transition2;
    }

    public static void calculateFragments(BackStackRecord transaction, SparseArray<FragmentContainerTransition> transitioningFragments, boolean isReordered) {
        int numOps = transaction.mOps.size();
        for (int opNum = 0; opNum < numOps; opNum++) {
            addToFirstInLastOut(transaction, (Op) transaction.mOps.get(opNum), transitioningFragments, false, isReordered);
        }
    }

    public static void calculatePopFragments(BackStackRecord transaction, SparseArray<FragmentContainerTransition> transitioningFragments, boolean isReordered) {
        if (transaction.mManager.mContainer.onHasView()) {
            for (int opNum = transaction.mOps.size() - 1; opNum >= 0; opNum--) {
                addToFirstInLastOut(transaction, (Op) transaction.mOps.get(opNum), transitioningFragments, true, isReordered);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:93:0x011a  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0106  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void addToFirstInLastOut(BackStackRecord transaction, Op op, SparseArray<FragmentContainerTransition> transitioningFragments, boolean isPop, boolean isReorderedTransaction) {
        BackStackRecord backStackRecord = transaction;
        Op op2 = op;
        SparseArray<FragmentContainerTransition> sparseArray = transitioningFragments;
        boolean z = isPop;
        Fragment fragment = op2.fragment;
        if (fragment != null) {
            int containerId = fragment.mContainerId;
            if (containerId != 0) {
                FragmentContainerTransition containerTransition;
                Fragment fragment2;
                int command = z ? INVERSE_OPS[op2.cmd] : op2.cmd;
                boolean setLastIn = false;
                boolean wasRemoved = false;
                boolean setFirstOut = false;
                boolean wasAdded = false;
                boolean z2 = false;
                if (command != 1) {
                    switch (command) {
                        case 3:
                        case 6:
                            if (isReorderedTransaction) {
                                if (!fragment.mAdded && fragment.mView != null && fragment.mView.getVisibility() == 0 && fragment.mView.getTransitionAlpha() > 0.0f) {
                                    z2 = true;
                                }
                                setFirstOut = z2;
                            } else {
                                if (fragment.mAdded && !fragment.mHidden) {
                                    z2 = true;
                                }
                                setFirstOut = z2;
                            }
                            wasRemoved = true;
                            break;
                        case 4:
                            if (isReorderedTransaction) {
                                if (fragment.mHiddenChanged && fragment.mAdded && fragment.mHidden) {
                                    z2 = true;
                                }
                                setFirstOut = z2;
                            } else {
                                if (fragment.mAdded && !fragment.mHidden) {
                                    z2 = true;
                                }
                                setFirstOut = z2;
                            }
                            wasRemoved = true;
                            break;
                        case 5:
                            if (isReorderedTransaction) {
                                if (fragment.mHiddenChanged && !fragment.mHidden && fragment.mAdded) {
                                    z2 = true;
                                }
                                setLastIn = z2;
                            } else {
                                setLastIn = fragment.mHidden;
                            }
                            wasAdded = true;
                            break;
                        case 7:
                            break;
                    }
                }
                if (isReorderedTransaction) {
                    setLastIn = fragment.mIsNewlyAdded;
                } else {
                    if (!(fragment.mAdded || fragment.mHidden)) {
                        z2 = true;
                    }
                    setLastIn = z2;
                }
                wasAdded = true;
                boolean setLastIn2 = setLastIn;
                boolean wasRemoved2 = wasRemoved;
                boolean setFirstOut2 = setFirstOut;
                boolean wasAdded2 = wasAdded;
                FragmentContainerTransition containerTransition2 = (FragmentContainerTransition) sparseArray.get(containerId);
                if (setLastIn2) {
                    containerTransition2 = ensureContainer(containerTransition2, sparseArray, containerId);
                    containerTransition2.lastIn = fragment;
                    containerTransition2.lastInIsPop = z;
                    containerTransition2.lastInTransaction = backStackRecord;
                }
                FragmentContainerTransition containerTransition3 = containerTransition2;
                if (!isReorderedTransaction && wasAdded2) {
                    if (containerTransition3 != null && containerTransition3.firstOut == fragment) {
                        containerTransition3.firstOut = null;
                    }
                    FragmentManagerImpl manager = backStackRecord.mManager;
                    if (fragment.mState < 1 && manager.mCurState >= 1 && manager.mHost.getContext().getApplicationInfo().targetSdkVersion >= 24 && !backStackRecord.mReorderingAllowed) {
                        manager.makeActive(fragment);
                        containerTransition = containerTransition3;
                        fragment2 = null;
                        manager.moveToState(fragment, 1, 0, 0, false);
                        if (setFirstOut2) {
                            containerTransition2 = containerTransition;
                        } else {
                            containerTransition2 = containerTransition;
                            if (containerTransition2 == null || containerTransition2.firstOut == null) {
                                containerTransition3 = ensureContainer(containerTransition2, sparseArray, containerId);
                                containerTransition3.firstOut = fragment;
                                containerTransition3.firstOutIsPop = z;
                                containerTransition3.firstOutTransaction = backStackRecord;
                                if (!isReorderedTransaction && wasRemoved2 && containerTransition3 != null && containerTransition3.lastIn == fragment) {
                                    containerTransition3.lastIn = fragment2;
                                }
                            }
                        }
                        containerTransition3 = containerTransition2;
                        containerTransition3.lastIn = fragment2;
                    }
                }
                fragment2 = null;
                containerTransition = containerTransition3;
                if (setFirstOut2) {
                }
                containerTransition3 = containerTransition2;
                containerTransition3.lastIn = fragment2;
            }
        }
    }

    private static FragmentContainerTransition ensureContainer(FragmentContainerTransition containerTransition, SparseArray<FragmentContainerTransition> transitioningFragments, int containerId) {
        if (containerTransition != null) {
            return containerTransition;
        }
        containerTransition = new FragmentContainerTransition();
        transitioningFragments.put(containerId, containerTransition);
        return containerTransition;
    }
}
