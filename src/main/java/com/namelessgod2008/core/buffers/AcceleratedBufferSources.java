package com.namelessgod2008.core.buffers;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.AcceleratedBufferSource;
import com.namelessgod2008.core.buffers.accelerated.IAcceleratedBufferSource;
import com.namelessgod2008.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Map;
import java.util.Set;

public class AcceleratedBufferSources implements IAcceleratedBufferSource {

	private final Map<VertexFormat, IAcceleratedBufferSource>	sources;
	private final IAcceleratedBufferSource						defaultSource;
	private final Set<VertexFormat.Mode>						validModes;
	private final boolean										supportTranslucent;
	private final boolean										supportDynamic;

	private AcceleratedBufferSources(
			Map<VertexFormat, IAcceleratedBufferSource>	sources,
			IAcceleratedBufferSource					defaultSource,
			Set<VertexFormat.Mode>						validModes,
			boolean										supportTranslucent,
			boolean										supportDynamic
	) {
		this.sources			= sources;
		this.defaultSource		= defaultSource;
		this.validModes			= validModes;
		this.supportTranslucent	= supportTranslucent;
		this.supportDynamic		= supportDynamic;
	}

	@Override
	public AcceleratedBufferBuilder getBuffer(
			RenderType	renderType,
			Runnable	before,
			Runnable	after,
			int			layer
	) {
		if (			renderType		!= null
				&& 	(	CoreFeature		.shouldForceAccelerateTranslucent	()	|| supportTranslucent	|| !RenderTypeUtils.isTranslucent	(renderType))
				&& 	(	CoreFeature		.shouldCacheDynamicRenderType		()	|| supportDynamic		|| !RenderTypeUtils.isDynamic		(renderType))
				&&		validModes		.contains							(renderType.mode())
		) {
			return sources
					.getOrDefault	(renderType.format(), defaultSource)
					.getBuffer		(
							renderType,
							before,
							after,
							layer
					);
		}

		return null;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final	Map<VertexFormat, IAcceleratedBufferSource>	sources;
		private final	Set<VertexFormat.Mode>						validModes;

		private			IAcceleratedBufferSource					defaultSource;
		private			boolean										supportTranslucent;
		private			boolean										supportDynamic;

		private Builder() {
			this.sources			= new Reference2ObjectOpenHashMap	<>();
			this.validModes			= new ReferenceOpenHashSet			<>();

			this.defaultSource		= EmptyAcceleratedBufferSources.INSTANCE;
			this.supportTranslucent	= false;
			this.supportDynamic		= false;
		}

		public Builder source(AcceleratedBufferSource bufferSource) {
			for (var format : bufferSource
					.getEnvironment		()
					.getVertexFormats	()
			) {
				sources.put(format, bufferSource);
			}

			return this;
		}

		public Builder defaultSource(IAcceleratedBufferSource source) {
			defaultSource = source;
			return this;
		}

		public Builder mode(VertexFormat.Mode mode) {
			validModes.add(mode);
			return this;
		}

		public Builder supportTranslucent() {
			supportTranslucent = true;
			return this;
		}

		public Builder supportDynamic() {
			supportDynamic = true;
			return this;
		}

		public AcceleratedBufferSources build() {
			return new AcceleratedBufferSources(
					sources,
					defaultSource,
					validModes,
					supportTranslucent,
					supportDynamic
			);
		}
	}
}
