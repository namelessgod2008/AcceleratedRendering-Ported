package com.namelessgod2008.core.backends.states.scissors;

import com.namelessgod2008.core.backends.states.EmptyBindingState;
import com.namelessgod2008.core.backends.states.IBindingState;

public enum ScissorBindingStateType {

	IGNORED,
	MOJANG,
	OPENGL;

	public IBindingState create() {
		return create(this);
	}

	public static IBindingState create(ScissorBindingStateType type) {
		return switch (type) {
			case IGNORED	-> EmptyBindingState.INSTANCE;
			case MOJANG		-> new MojangScissorBindingState();
			case OPENGL		-> new OpenGLScissorBindingState();
		};
	}
}
