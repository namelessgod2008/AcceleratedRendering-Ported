package com.namelessgod2008.core.backends.states.viewports;

import com.namelessgod2008.core.backends.states.EmptyBindingState;
import com.namelessgod2008.core.backends.states.IBindingState;

public enum ViewportBindingStateType {

	IGNORED,
	MOJANG,
	OPENGL;

	public IBindingState create() {
		return create(this);
	}

	public static IBindingState create(ViewportBindingStateType type) {
		return switch (type) {
			case IGNORED	-> EmptyBindingState.INSTANCE;
			case MOJANG		-> new MojangViewportBindingState();
			case OPENGL		-> new OpenGLViewportBindingState();
		};
	}
}
