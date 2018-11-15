package android.support.v4.app;

import android.graphics.Rect;
import android.os.Build.VERSION;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewCompat;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class FragmentTransition {
    private static final int[] INVERSE_OPS = new int[]{0, 3, 0, 1, 5, 4, 7, 6, 9, 8};
    private static final FragmentTransitionImpl PLATFORM_IMPL = (VERSION.SDK_INT >= 21 ? new FragmentTransitionCompat21() : null);
    private static final FragmentTransitionImpl SUPPORT_IMPL = resolveSupportImpl();

    static class FragmentContainerTransition {
        public Fragment firstOut;
        public boolean firstOutIsPop;
        public BackStackRecord firstOutTransaction;
        public Fragment lastIn;
        public boolean lastInIsPop;
        public BackStackRecord lastInTransaction;

        FragmentContainerTransition() {
        }
    }

    private static FragmentTransitionImpl resolveSupportImpl() {
        try {
            return (FragmentTransitionImpl) Class.forName("android.support.transition.FragmentTransitionSupport").getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            return null;
        }
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
        View sceneRoot = null;
        if (fragmentManagerImpl.mContainer.onHasView()) {
            sceneRoot = (ViewGroup) fragmentManagerImpl.mContainer.onFindViewById(containerId);
        } else {
            int i = containerId;
        }
        View sceneRoot2 = sceneRoot;
        if (sceneRoot2 != null) {
            Fragment inFragment = fragmentContainerTransition.lastIn;
            Fragment outFragment = fragmentContainerTransition.firstOut;
            FragmentTransitionImpl impl = chooseImpl(outFragment, inFragment);
            if (impl != null) {
                Object exitTransition;
                boolean inIsPop = fragmentContainerTransition.lastInIsPop;
                boolean outIsPop = fragmentContainerTransition.firstOutIsPop;
                ArrayList<View> sharedElementsIn = new ArrayList();
                ArrayList<View> sharedElementsOut = new ArrayList();
                Object enterTransition = getEnterTransition(impl, inFragment, inIsPop);
                Object exitTransition2 = getExitTransition(impl, outFragment, outIsPop);
                ArrayList<View> sharedElementsOut2 = sharedElementsOut;
                ArrayList<View> sharedElementsIn2 = sharedElementsIn;
                boolean inIsPop2 = inIsPop;
                Object enterTransition2 = enterTransition;
                FragmentTransitionImpl impl2 = impl;
                Object sharedElementTransition = configureSharedElementsReordered(impl, sceneRoot2, view, nameOverrides, fragmentContainerTransition, sharedElementsOut2, sharedElementsIn2, enterTransition2, exitTransition2);
                if (enterTransition2 == null && sharedElementTransition == null) {
                    exitTransition = exitTransition2;
                    if (exitTransition == null) {
                        return;
                    }
                }
                exitTransition = exitTransition2;
                sharedElementsIn = sharedElementsOut2;
                sharedElementsOut = configureEnteringExitingViews(impl2, exitTransition, outFragment, sharedElementsIn, view);
                ArrayList<View> sharedElementsIn3 = sharedElementsIn2;
                ArrayList<View> enteringViews = configureEnteringExitingViews(impl2, enterTransition2, inFragment, sharedElementsIn3, view);
                setViewVisibility(enteringViews, 4);
                ArrayList<View> enteringViews2 = enteringViews;
                ArrayList<View> sharedElementsIn4 = sharedElementsIn3;
                ArrayList<View> exitingViews = sharedElementsOut;
                ArrayList<View> sharedElementsOut3 = sharedElementsIn;
                Object transition = mergeTransitions(impl2, enterTransition2, exitTransition, sharedElementTransition, inFragment, inIsPop2);
                if (transition != null) {
                    replaceHide(impl2, exitTransition, outFragment, exitingViews);
                    ArrayList<String> inNames = impl2.prepareSetNameOverridesReordered(sharedElementsIn4);
                    Object transition2 = transition;
                    impl2.scheduleRemoveTargets(transition, enterTransition2, enteringViews2, exitTransition, exitingViews, sharedElementTransition, sharedElementsIn4);
                    impl2.beginDelayedTransition(sceneRoot2, transition2);
                    impl2.setNameOverridesReordered(sceneRoot2, sharedElementsOut3, sharedElementsIn4, inNames, nameOverrides);
                    setViewVisibility(enteringViews2, 0);
                    impl2.swapSharedElementTargets(sharedElementTransition, sharedElementsOut3, sharedElementsIn4);
                } else {
                    Object obj = exitTransition;
                    Object obj2 = enterTransition2;
                    ArrayList<View> arrayList = enteringViews2;
                    ArrayList<View> arrayList2 = sharedElementsOut3;
                }
            }
        }
    }

    private static void replaceHide(FragmentTransitionImpl impl, Object exitTransition, Fragment exitingFragment, final ArrayList<View> exitingViews) {
        if (exitingFragment != null && exitTransition != null && exitingFragment.mAdded && exitingFragment.mHidden && exitingFragment.mHiddenChanged) {
            exitingFragment.setHideReplaced(true);
            impl.scheduleHideFragmentView(exitTransition, exitingFragment.getView(), exitingViews);
            OneShotPreDrawListener.add(exitingFragment.mContainer, new Runnable() {
                public void run() {
                    FragmentTransition.setViewVisibility(exitingViews, 4);
                }
            });
        }
    }

    private static void configureTransitionsOrdered(FragmentManagerImpl fragmentManager, int containerId, FragmentContainerTransition fragments, View nonExistentView, ArrayMap<String, String> nameOverrides) {
        FragmentManagerImpl fragmentManagerImpl = fragmentManager;
        FragmentContainerTransition fragmentContainerTransition = fragments;
        View view = nonExistentView;
        ArrayMap<String, String> arrayMap = nameOverrides;
        ViewGroup sceneRoot = null;
        if (fragmentManagerImpl.mContainer.onHasView()) {
            sceneRoot = (ViewGroup) fragmentManagerImpl.mContainer.onFindViewById(containerId);
        } else {
            int i = containerId;
        }
        ViewGroup sceneRoot2 = sceneRoot;
        if (sceneRoot2 != null) {
            Fragment inFragment = fragmentContainerTransition.lastIn;
            Fragment outFragment = fragmentContainerTransition.firstOut;
            FragmentTransitionImpl impl = chooseImpl(outFragment, inFragment);
            if (impl != null) {
                Object exitTransition;
                boolean inIsPop = fragmentContainerTransition.lastInIsPop;
                boolean outIsPop = fragmentContainerTransition.firstOutIsPop;
                Object enterTransition = getEnterTransition(impl, inFragment, inIsPop);
                Fragment exitTransition2 = getExitTransition(impl, outFragment, outIsPop);
                ArrayList<View> sharedElementsOut = new ArrayList();
                ArrayList<View> sharedElementsIn = new ArrayList();
                ArrayList<View> sharedElementsOut2 = sharedElementsOut;
                Fragment exitTransition3 = exitTransition2;
                Object enterTransition2 = enterTransition;
                FragmentTransitionImpl impl2 = impl;
                Fragment outFragment2 = outFragment;
                Object sharedElementTransition = configureSharedElementsOrdered(impl, sceneRoot2, view, arrayMap, fragmentContainerTransition, sharedElementsOut2, sharedElementsIn, enterTransition2, exitTransition3);
                Object enterTransition3 = enterTransition2;
                if (enterTransition3 == null && sharedElementTransition == null) {
                    exitTransition = exitTransition3;
                    if (exitTransition == null) {
                        return;
                    }
                }
                exitTransition = exitTransition3;
                ArrayList<View> sharedElementsOut3 = sharedElementsOut2;
                ArrayList<View> exitingViews = configureEnteringExitingViews(impl2, exitTransition, outFragment2, sharedElementsOut3, view);
                if (exitingViews == null || exitingViews.isEmpty()) {
                    exitTransition = null;
                }
                Object exitTransition4 = exitTransition;
                impl2.addTarget(enterTransition3, view);
                Object transition = mergeTransitions(impl2, enterTransition3, exitTransition4, sharedElementTransition, inFragment, fragmentContainerTransition.lastInIsPop);
                ArrayList<View> sharedElementsIn2;
                if (transition != null) {
                    ArrayList<View> enteringViews = new ArrayList();
                    impl2.scheduleRemoveTargets(transition, enterTransition3, enteringViews, exitTransition4, exitingViews, sharedElementTransition, sharedElementsIn);
                    Object transition2 = transition;
                    scheduleTargetChange(impl2, sceneRoot2, inFragment, view, sharedElementsIn, enterTransition3, enteringViews, exitTransition4, exitingViews);
                    sharedElementsIn2 = sharedElementsIn;
                    impl2.setNameOverridesOrdered(sceneRoot2, sharedElementsIn2, arrayMap);
                    impl2.beginDelayedTransition(sceneRoot2, transition2);
                    impl2.scheduleNameReset(sceneRoot2, sharedElementsIn2, arrayMap);
                } else {
                    ArrayList<View> arrayList = exitingViews;
                    ArrayList<View> arrayList2 = sharedElementsOut3;
                    Object obj = enterTransition3;
                    sharedElementsIn2 = sharedElementsIn;
                }
            }
        }
    }

    private static void scheduleTargetChange(FragmentTransitionImpl impl, ViewGroup sceneRoot, Fragment inFragment, View nonExistentView, ArrayList<View> sharedElementsIn, Object enterTransition, ArrayList<View> enteringViews, Object exitTransition, ArrayList<View> exitingViews) {
        final Object obj = enterTransition;
        final FragmentTransitionImpl fragmentTransitionImpl = impl;
        final View view = nonExistentView;
        final Fragment fragment = inFragment;
        final ArrayList<View> arrayList = sharedElementsIn;
        final ArrayList<View> arrayList2 = enteringViews;
        final ArrayList<View> arrayList3 = exitingViews;
        final Object obj2 = exitTransition;
        OneShotPreDrawListener.add(sceneRoot, new Runnable() {
            public void run() {
                if (obj != null) {
                    fragmentTransitionImpl.removeTarget(obj, view);
                    arrayList2.addAll(FragmentTransition.configureEnteringExitingViews(fragmentTransitionImpl, obj, fragment, arrayList, view));
                }
                if (arrayList3 != null) {
                    if (obj2 != null) {
                        ArrayList<View> tempExiting = new ArrayList();
                        tempExiting.add(view);
                        fragmentTransitionImpl.replaceTargets(obj2, arrayList3, tempExiting);
                    }
                    arrayList3.clear();
                    arrayList3.add(view);
                }
            }
        });
    }

    private static FragmentTransitionImpl chooseImpl(Fragment outFragment, Fragment inFragment) {
        Object exitTransition;
        Object returnTransition;
        Object sharedReturnTransition;
        ArrayList<Object> transitions = new ArrayList();
        if (outFragment != null) {
            exitTransition = outFragment.getExitTransition();
            if (exitTransition != null) {
                transitions.add(exitTransition);
            }
            returnTransition = outFragment.getReturnTransition();
            if (returnTransition != null) {
                transitions.add(returnTransition);
            }
            sharedReturnTransition = outFragment.getSharedElementReturnTransition();
            if (sharedReturnTransition != null) {
                transitions.add(sharedReturnTransition);
            }
        }
        if (inFragment != null) {
            exitTransition = inFragment.getEnterTransition();
            if (exitTransition != null) {
                transitions.add(exitTransition);
            }
            returnTransition = inFragment.getReenterTransition();
            if (returnTransition != null) {
                transitions.add(returnTransition);
            }
            sharedReturnTransition = inFragment.getSharedElementEnterTransition();
            if (sharedReturnTransition != null) {
                transitions.add(sharedReturnTransition);
            }
        }
        if (transitions.isEmpty()) {
            return null;
        }
        if (PLATFORM_IMPL != null && canHandleAll(PLATFORM_IMPL, transitions)) {
            return PLATFORM_IMPL;
        }
        if (SUPPORT_IMPL != null && canHandleAll(SUPPORT_IMPL, transitions)) {
            return SUPPORT_IMPL;
        }
        if (PLATFORM_IMPL == null && SUPPORT_IMPL == null) {
            return null;
        }
        throw new IllegalArgumentException("Invalid Transition types");
    }

    private static boolean canHandleAll(FragmentTransitionImpl impl, List<Object> transitions) {
        int size = transitions.size();
        for (int i = 0; i < size; i++) {
            if (!impl.canHandle(transitions.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static Object getSharedElementTransition(FragmentTransitionImpl impl, Fragment inFragment, Fragment outFragment, boolean isPop) {
        if (inFragment == null || outFragment == null) {
            return null;
        }
        Object transition;
        if (isPop) {
            transition = outFragment.getSharedElementReturnTransition();
        } else {
            transition = inFragment.getSharedElementEnterTransition();
        }
        return impl.wrapTransitionInSet(impl.cloneTransition(transition));
    }

    private static Object getEnterTransition(FragmentTransitionImpl impl, Fragment inFragment, boolean isPop) {
        if (inFragment == null) {
            return null;
        }
        Object reenterTransition;
        if (isPop) {
            reenterTransition = inFragment.getReenterTransition();
        } else {
            reenterTransition = inFragment.getEnterTransition();
        }
        return impl.cloneTransition(reenterTransition);
    }

    private static Object getExitTransition(FragmentTransitionImpl impl, Fragment outFragment, boolean isPop) {
        if (outFragment == null) {
            return null;
        }
        Object returnTransition;
        if (isPop) {
            returnTransition = outFragment.getReturnTransition();
        } else {
            returnTransition = outFragment.getExitTransition();
        }
        return impl.cloneTransition(returnTransition);
    }

    private static Object configureSharedElementsReordered(FragmentTransitionImpl impl, ViewGroup sceneRoot, View nonExistentView, ArrayMap<String, String> nameOverrides, FragmentContainerTransition fragments, ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn, Object enterTransition, Object exitTransition) {
        FragmentTransitionImpl fragmentTransitionImpl = impl;
        View view = nonExistentView;
        ArrayMap<String, String> arrayMap = nameOverrides;
        FragmentContainerTransition fragmentContainerTransition = fragments;
        ArrayList<View> arrayList = sharedElementsOut;
        ArrayList<View> arrayList2 = sharedElementsIn;
        Object obj = enterTransition;
        Fragment inFragment = fragmentContainerTransition.lastIn;
        Fragment outFragment = fragmentContainerTransition.firstOut;
        if (inFragment != null) {
            inFragment.getView().setVisibility(0);
        }
        ViewGroup viewGroup;
        Fragment fragment;
        if (inFragment == null) {
            viewGroup = sceneRoot;
            fragment = outFragment;
        } else if (outFragment == null) {
            viewGroup = sceneRoot;
            fragment = outFragment;
        } else {
            boolean inIsPop = fragmentContainerTransition.lastInIsPop;
            Object sharedElementTransition = nameOverrides.isEmpty() ? null : getSharedElementTransition(fragmentTransitionImpl, inFragment, outFragment, inIsPop);
            ArrayMap<String, View> outSharedElements = captureOutSharedElements(fragmentTransitionImpl, arrayMap, sharedElementTransition, fragmentContainerTransition);
            ArrayMap<String, View> inSharedElements = captureInSharedElements(fragmentTransitionImpl, arrayMap, sharedElementTransition, fragmentContainerTransition);
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
            Object sharedElementTransition2 = sharedElementTransition;
            if (obj == null && exitTransition == null && sharedElementTransition2 == null) {
                return null;
            }
            Object sharedElementTransition3;
            ArrayMap<String, View> inSharedElements2;
            Rect epicenter;
            View epicenterView;
            callSharedElementStartEnd(inFragment, outFragment, inIsPop, outSharedElements, true);
            if (sharedElementTransition2 != null) {
                arrayList2.add(view);
                fragmentTransitionImpl.setSharedElementTargets(sharedElementTransition2, view, arrayList);
                sharedElementTransition3 = sharedElementTransition2;
                inSharedElements2 = inSharedElements;
                setOutEpicenter(fragmentTransitionImpl, sharedElementTransition2, exitTransition, outSharedElements, fragmentContainerTransition.firstOutIsPop, fragmentContainerTransition.firstOutTransaction);
                Rect epicenter2 = new Rect();
                View epicenterView2 = getInEpicenterView(inSharedElements2, fragmentContainerTransition, obj, inIsPop);
                if (epicenterView2 != null) {
                    fragmentTransitionImpl.setEpicenter(obj, epicenter2);
                }
                epicenter = epicenter2;
                epicenterView = epicenterView2;
            } else {
                sharedElementTransition3 = sharedElementTransition2;
                inSharedElements2 = inSharedElements;
                ArrayMap<String, View> arrayMap2 = outSharedElements;
                epicenterView = null;
                epicenter = null;
            }
            final Fragment fragment2 = inFragment;
            final Fragment fragment3 = outFragment;
            final boolean z = inIsPop;
            AnonymousClass3 anonymousClass3 = r0;
            inSharedElements = inSharedElements2;
            final FragmentTransitionImpl fragmentTransitionImpl2 = fragmentTransitionImpl;
            final Rect outFragment2 = epicenter;
            AnonymousClass3 anonymousClass32 = new Runnable() {
                public void run() {
                    FragmentTransition.callSharedElementStartEnd(fragment2, fragment3, z, inSharedElements, false);
                    if (epicenterView != null) {
                        fragmentTransitionImpl2.getBoundsOnScreen(epicenterView, outFragment2);
                    }
                }
            };
            OneShotPreDrawListener.add(sceneRoot, anonymousClass3);
            return sharedElementTransition3;
        }
        return null;
    }

    private static void addSharedElementsWithMatchingNames(ArrayList<View> views, ArrayMap<String, View> sharedElements, Collection<String> nameOverridesSet) {
        for (int i = sharedElements.size() - 1; i >= 0; i--) {
            View view = (View) sharedElements.valueAt(i);
            if (nameOverridesSet.contains(ViewCompat.getTransitionName(view))) {
                views.add(view);
            }
        }
    }

    private static Object configureSharedElementsOrdered(FragmentTransitionImpl impl, ViewGroup sceneRoot, View nonExistentView, ArrayMap<String, String> nameOverrides, FragmentContainerTransition fragments, ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn, Object enterTransition, Object exitTransition) {
        FragmentTransitionImpl fragmentTransitionImpl = impl;
        FragmentContainerTransition fragmentContainerTransition = fragments;
        ArrayList<View> arrayList = sharedElementsOut;
        Object obj = enterTransition;
        Fragment inFragment = fragmentContainerTransition.lastIn;
        Fragment outFragment = fragmentContainerTransition.firstOut;
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
            boolean inIsPop = fragmentContainerTransition.lastInIsPop;
            Object sharedElementTransition = nameOverrides.isEmpty() ? null : getSharedElementTransition(fragmentTransitionImpl, inFragment, outFragment, inIsPop);
            ArrayMap<String, String> arrayMap = nameOverrides;
            ArrayMap<String, View> outSharedElements = captureOutSharedElements(fragmentTransitionImpl, arrayMap, sharedElementTransition, fragmentContainerTransition);
            if (nameOverrides.isEmpty()) {
                sharedElementTransition = null;
            } else {
                arrayList.addAll(outSharedElements.values());
            }
            Object sharedElementTransition2 = sharedElementTransition;
            if (obj == null && exitTransition == null && sharedElementTransition2 == null) {
                return null;
            }
            Rect inEpicenter;
            callSharedElementStartEnd(inFragment, outFragment, inIsPop, outSharedElements, true);
            ArrayMap<String, View> outSharedElements2;
            if (sharedElementTransition2 != null) {
                Rect inEpicenter2 = new Rect();
                fragmentTransitionImpl.setSharedElementTargets(sharedElementTransition2, nonExistentView, arrayList);
                outSharedElements2 = outSharedElements;
                inEpicenter = inEpicenter2;
                setOutEpicenter(fragmentTransitionImpl, sharedElementTransition2, exitTransition, outSharedElements, fragmentContainerTransition.firstOutIsPop, fragmentContainerTransition.firstOutTransaction);
                if (obj != null) {
                    fragmentTransitionImpl.setEpicenter(obj, inEpicenter);
                }
            } else {
                outSharedElements2 = outSharedElements;
                inEpicenter = null;
            }
            Object sharedElementTransition3 = sharedElementTransition2;
            final Rect inEpicenter3 = inEpicenter;
            final Object finalSharedElementTransition = sharedElementTransition3;
            final FragmentTransitionImpl fragmentTransitionImpl2 = fragmentTransitionImpl;
            final ArrayMap<String, String> arrayMap2 = arrayMap;
            final FragmentContainerTransition fragmentContainerTransition2 = fragmentContainerTransition;
            final ArrayList<View> arrayList2 = sharedElementsIn;
            AnonymousClass4 anonymousClass4 = r0;
            final View view = nonExistentView;
            final Fragment fragment3 = inFragment;
            boolean inIsPop2 = inIsPop;
            final Fragment fragment4 = outFragment;
            final boolean z = inIsPop2;
            final ArrayList<View> arrayList3 = arrayList;
            obj = enterTransition;
            AnonymousClass4 anonymousClass42 = new Runnable() {
                public void run() {
                    ArrayMap<String, View> inSharedElements = FragmentTransition.captureInSharedElements(fragmentTransitionImpl2, arrayMap2, finalSharedElementTransition, fragmentContainerTransition2);
                    if (inSharedElements != null) {
                        arrayList2.addAll(inSharedElements.values());
                        arrayList2.add(view);
                    }
                    FragmentTransition.callSharedElementStartEnd(fragment3, fragment4, z, inSharedElements, false);
                    if (finalSharedElementTransition != null) {
                        fragmentTransitionImpl2.swapSharedElementTargets(finalSharedElementTransition, arrayList3, arrayList2);
                        View inEpicenterView = FragmentTransition.getInEpicenterView(inSharedElements, fragmentContainerTransition2, obj, z);
                        if (inEpicenterView != null) {
                            fragmentTransitionImpl2.getBoundsOnScreen(inEpicenterView, inEpicenter3);
                        }
                    }
                }
            };
            OneShotPreDrawListener.add(sceneRoot, anonymousClass4);
            return sharedElementTransition3;
        }
        return null;
    }

    private static ArrayMap<String, View> captureOutSharedElements(FragmentTransitionImpl impl, ArrayMap<String, String> nameOverrides, Object sharedElementTransition, FragmentContainerTransition fragments) {
        if (nameOverrides.isEmpty() || sharedElementTransition == null) {
            nameOverrides.clear();
            return null;
        }
        SharedElementCallback sharedElementCallback;
        ArrayList<String> names;
        Fragment outFragment = fragments.firstOut;
        ArrayMap<String, View> outSharedElements = new ArrayMap();
        impl.findNamedViews(outSharedElements, outFragment.getView());
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
                } else if (!name.equals(ViewCompat.getTransitionName(view))) {
                    nameOverrides.put(ViewCompat.getTransitionName(view), (String) nameOverrides.remove(name));
                }
            }
        } else {
            nameOverrides.retainAll(outSharedElements.keySet());
        }
        return outSharedElements;
    }

    private static ArrayMap<String, View> captureInSharedElements(FragmentTransitionImpl impl, ArrayMap<String, String> nameOverrides, Object sharedElementTransition, FragmentContainerTransition fragments) {
        Fragment inFragment = fragments.lastIn;
        View fragmentView = inFragment.getView();
        if (nameOverrides.isEmpty() || sharedElementTransition == null || fragmentView == null) {
            nameOverrides.clear();
            return null;
        }
        SharedElementCallback sharedElementCallback;
        ArrayList<String> names;
        ArrayMap<String, View> inSharedElements = new ArrayMap();
        impl.findNamedViews(inSharedElements, fragmentView);
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
            inSharedElements.retainAll(nameOverrides.values());
        }
        if (sharedElementCallback != null) {
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
                } else if (!name.equals(ViewCompat.getTransitionName(view))) {
                    key = findKeyForValue(nameOverrides, name);
                    if (key != null) {
                        nameOverrides.put(key, ViewCompat.getTransitionName(view));
                    }
                }
            }
        } else {
            retainValues(nameOverrides, inSharedElements);
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

    private static View getInEpicenterView(ArrayMap<String, View> inSharedElements, FragmentContainerTransition fragments, Object enterTransition, boolean inIsPop) {
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

    private static void setOutEpicenter(FragmentTransitionImpl impl, Object sharedElementTransition, Object exitTransition, ArrayMap<String, View> outSharedElements, boolean outIsPop, BackStackRecord outTransaction) {
        if (outTransaction.mSharedElementSourceNames != null && !outTransaction.mSharedElementSourceNames.isEmpty()) {
            String sourceName;
            if (outIsPop) {
                sourceName = (String) outTransaction.mSharedElementTargetNames.get(0);
            } else {
                sourceName = (String) outTransaction.mSharedElementSourceNames.get(0);
            }
            View outEpicenterView = (View) outSharedElements.get(sourceName);
            impl.setEpicenter(sharedElementTransition, outEpicenterView);
            if (exitTransition != null) {
                impl.setEpicenter(exitTransition, outEpicenterView);
            }
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
                names.add(sharedElements.keyAt(i));
                views.add(sharedElements.valueAt(i));
                i++;
            }
            if (isStart) {
                sharedElementCallback.onSharedElementStart(names, views, null);
            } else {
                sharedElementCallback.onSharedElementEnd(names, views, null);
            }
        }
    }

    private static ArrayList<View> configureEnteringExitingViews(FragmentTransitionImpl impl, Object transition, Fragment fragment, ArrayList<View> sharedElements, View nonExistentView) {
        ArrayList<View> viewList = null;
        if (transition != null) {
            viewList = new ArrayList();
            View root = fragment.getView();
            if (root != null) {
                impl.captureTransitioningViews(viewList, root);
            }
            if (sharedElements != null) {
                viewList.removeAll(sharedElements);
            }
            if (!viewList.isEmpty()) {
                viewList.add(nonExistentView);
                impl.addTargets(transition, viewList);
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

    private static Object mergeTransitions(FragmentTransitionImpl impl, Object enterTransition, Object exitTransition, Object sharedElementTransition, Fragment inFragment, boolean isPop) {
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
        if (overlap) {
            return impl.mergeTransitionsTogether(exitTransition, enterTransition, sharedElementTransition);
        }
        return impl.mergeTransitionsInSequence(exitTransition, enterTransition, sharedElementTransition);
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

    static boolean supportsTransition() {
        return (PLATFORM_IMPL == null && SUPPORT_IMPL == null) ? false : true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:91:0x0106  */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x00f2  */
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
                                if (!fragment.mAdded && fragment.mView != null && fragment.mView.getVisibility() == 0 && fragment.mPostponedAlpha >= 0.0f) {
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
                    if (fragment.mState < 1 && manager.mCurState >= 1 && !backStackRecord.mReorderingAllowed) {
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

    private FragmentTransition() {
    }
}
