package com.namelessgod2008.compat.iris.mixins.plugin;

import com.namelessgod2008.compat.AbstractCompatMixinPlugin;

import java.util.List;

public class IrisCompatMixinPlugin extends AbstractCompatMixinPlugin {

	@Override
	protected List<String> getModIDs() {
		return List.of("iris");
	}
}
