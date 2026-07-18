package com.namelessgod2008.compat.iris.mixins.acceleratedrendering;

import com.namelessgod2008.compat.iris.interfaces.IIrisMeshInfo;
import com.namelessgod2008.compat.iris.interfaces.IIrisMeshInfoCache;
import com.namelessgod2008.core.buffers.accelerated.pools.meshes.SimpleMeshInfo;
import com.namelessgod2008.core.buffers.accelerated.pools.meshes.SimpleMeshInfoCache;
import com.namelessgod2008.core.utils.SimpleCachedArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SimpleMeshInfoCache.class)
public class SimpleMeshInfoCacheMixin implements IIrisMeshInfoCache {

	@Shadow @Final private SimpleCachedArray<SimpleMeshInfo> meshInfos;

	@Override
	public short getRenderedEntity(int i) {
		return ((IIrisMeshInfo) meshInfos.at(i)).getRenderedEntity();
	}

	@Override
	public short getRenderedBlockEntity(int i) {
		return ((IIrisMeshInfo) meshInfos.at(i)).getRenderedBlockEntity();
	}

	@Override
	public short getRenderedItem(int i) {
		return ((IIrisMeshInfo) meshInfos.at(i)).getRenderedItem();
	}
}
