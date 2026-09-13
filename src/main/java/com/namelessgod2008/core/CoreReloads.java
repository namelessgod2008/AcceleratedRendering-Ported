package com.namelessgod2008.core;

import com.namelessgod2008.core.meshes.ClientMesh;
import com.namelessgod2008.core.meshes.ServerMesh;
import com.namelessgod2008.core.meshes.data.cache.MeshDataCaches;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import com.namelessgod2008.core.utils.TextureUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class CoreReloads implements ResourceManagerReloadListener {

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		TextureUtils					.reload();
		RenderTypeUtils					.clearCaches();
		ServerMesh.Builder	.INSTANCE	.reload();
		ClientMesh.Builder	.INSTANCE	.reload();
		MeshDataCaches		.SERVER		.reload();
		MeshDataCaches		.CLIENT		.reload();
	}
}
