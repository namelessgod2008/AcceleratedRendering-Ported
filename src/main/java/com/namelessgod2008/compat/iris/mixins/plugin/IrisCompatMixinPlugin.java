package com.namelessgod2008.compat.iris.mixins.plugin;

import com.namelessgod2008.compat.AbstractCompatMixinPlugin;
import com.namelessgod2008.compat.iris.IrisGbufferBridge;
import com.namelessgod2008.core.backends.GbufferBridge;

import java.util.List;

public class IrisCompatMixinPlugin extends AbstractCompatMixinPlugin {

	@Override
	protected List<String> getModIDs() {
		return List.of("iris");
	}

	/**
	 * 登记 gbuffer 重定向执行器。
	 *
	 * <p>选在插件里登记而不是静态块：插件由 Mixin 框架在加载本 config 时必定调用一次，
	 * 而 {@code IrisGbufferBridge} 作为普通类可能直到首次使用时才被加载 ——
	 * 那时 {@code drawBuffers} 早已在跑了，桥接表还是空的。
	 */
	@Override
	public void onLoad(String mixinPackage) {
		super.onLoad(mixinPackage);

		GbufferBridge.register(IrisGbufferBridge.INSTANCE);
	}
}
