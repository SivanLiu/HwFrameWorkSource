package com.android.systemui.shared.system;

import android.view.RemoteAnimationDefinition;

public class RemoteAnimationDefinitionCompat {
    private final RemoteAnimationDefinition mWrapped = new RemoteAnimationDefinition();

    public void addRemoteAnimation(int transition, RemoteAnimationAdapterCompat adapter) {
        this.mWrapped.addRemoteAnimation(transition, adapter.getWrapped());
    }

    public void addRemoteAnimation(int transition, int activityTypeFilter, RemoteAnimationAdapterCompat adapter) {
        this.mWrapped.addRemoteAnimation(transition, activityTypeFilter, adapter.getWrapped());
    }

    RemoteAnimationDefinition getWrapped() {
        return this.mWrapped;
    }
}
