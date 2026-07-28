package com.namelessgod2008.compat.rei;

import com.namelessgod2008.compat.AbstractCompatMixinPlugin;

import java.util.List;

/**
 * REI (Roughly Enough Items) 兼容 Mixin 插件。
 * REI 未安装时静默跳过所有注入。
 */
public class ReiCompatMixinPlugin extends AbstractCompatMixinPlugin {

	@Override
	protected List<String> getModIDs() {
		return List.of("roughlyenoughitems");
	}
}
