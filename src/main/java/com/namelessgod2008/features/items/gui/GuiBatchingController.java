package com.namelessgod2008.features.items.gui;

import com.namelessgod2008.core.CoreBuffers;
import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.CoreStates;
import com.namelessgod2008.core.backends.states.IBindingState;
import com.namelessgod2008.core.buffers.accelerated.builders.BufferSourceExtension;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.buffers.accelerated.layers.LayerDrawType;
import com.namelessgod2008.core.utils.PoseStackExtension;
import com.namelessgod2008.core.utils.RenderTypeUtils;
import com.namelessgod2008.features.filter.ItemStackFilterStack;
import com.namelessgod2008.features.items.AcceleratedItemRenderingFeature;
import com.namelessgod2008.features.items.gui.contexts.*;
import com.namelessgod2008.features.items.gui.contexts.string.IStringDrawContext;
import com.namelessgod2008.features.items.gui.renderers.AcceleratedBlitRenderer;
import com.namelessgod2008.features.items.gui.renderers.AcceleratedFillRenderer;
import com.namelessgod2008.features.items.gui.renderers.AcceleratedGradientRenderer;
import com.mojang.blaze3d.platform.Lighting;
import it.unimi.dsi.fastutil.floats.Float2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ReferenceSortedMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;


@ExtensionMethod({
		VertexConsumerExtension	.class,
		BufferSourceExtension	.class,
		PoseStackExtension		.class,
})
public class GuiBatchingController {

	public static	final	GuiBatchingController			INSTANCE	= new GuiBatchingController();
	public static	final	float							DELTA		= 1e-6f;

	private			final	IBindingState					scissorDraw;
	private			final	IBindingState					scissorFlush;
	private			final	List<BlitDrawContext>			blitDrawContexts;
	private			final	List<IStringDrawContext>		stringDrawContexts;
	private			final	List<DecoratorDrawContext>		decoratorDrawContexts;
	private			final	List<FillDrawContext>			fillDrawContexts;
	private			final	List<HighlightDrawContext>		highlightDrawContexts;
	private			final	List<GradientDrawContext>		gradientDrawContexts;
	private			final	List<ItemDrawContext>			flatItemDrawContexts;
	private			final	List<ItemDrawContext>			blockItemDrawContexts;
	private			final	Float2ReferenceSortedMap<Layer>	depthLayers;

	private GuiBatchingController() {
		this.scissorDraw			= CoreFeature.createScissorState	();
		this.scissorFlush			= CoreFeature.createScissorState	();
		this.blitDrawContexts		= new ReferenceArrayList		<>	();
		this.stringDrawContexts		= new ReferenceArrayList		<>	();
		this.decoratorDrawContexts	= new ReferenceArrayList		<>	();
		this.fillDrawContexts		= new ReferenceArrayList		<>	();
		this.highlightDrawContexts	= new ReferenceArrayList		<>	();
		this.gradientDrawContexts	= new ReferenceArrayList		<>	();
		this.flatItemDrawContexts	= new ReferenceArrayList		<>	();
		this.blockItemDrawContexts	= new ReferenceArrayList		<>	();
		this.depthLayers			= new Float2ReferenceAVLTreeMap	<>	();
	}

	public boolean startBatching(GuiGraphics graphics) {
			if (true && graphics.bufferSource.getAcceleratable()	.isBufferSourceAcceleratable		()
				&&	AcceleratedItemRenderingFeature				.isEnabled							()
				&&	AcceleratedItemRenderingFeature				.shouldUseAcceleratedPipeline		()
				&&	AcceleratedItemRenderingFeature				.shouldAccelerateInGui				()
				&&	AcceleratedItemRenderingFeature				.shouldUseGuiItemBatching			()
				&&	CoreFeature									.isLoaded							()
				&&	CoreFeature									.shouldForceAccelerateTranslucent	()
		) {
			CoreFeature.setGuiBatching	();
			scissorDraw.record			(graphics);

			return true;
		}

		return false;
	}

	@SuppressWarnings("UnstableApiUsage")
	public float flushBatching(GuiGraphics graphics) {
		if (CoreFeature.isGuiBatching()) {
			var poseStack = graphics.pose();
			var offset    = 0.0f;

			CoreFeature.resetGuiBatching();

			for (var depthLayer : depthLayers.values()) {
				var layerElements	= depthLayer	.getLayerElements	();
				var layerDepth		= depthLayer	.getLayerDepth		();
				var layerNext		= depthLayers	.tailMap			(layerDepth + DELTA);
				var depth			= 0.0f;
				var step			= 0.1f;

				if (!layerNext.isEmpty()) {
					step = 1.0f / layerNext.firstEntry().getValue().getLayerElements().size();
				}

				for (var element : layerElements) {
					element.transform().translateLocal(
							0.0f,
							0.0f,
							depth
					);

					depth += step;
				}

				offset = layerDepth + depth;
			}

			for (int index = 0, size = blitDrawContexts.size(); index < size; index ++) {
					var context = blitDrawContexts.get(index);
var extension = graphics.bufferSource.getBuffer(context.renderTypeGetter() != null ? context.renderTypeGetter().apply(context.atlasLocation()) : GuiRenderTypes.blit(context.atlasLocation())).getAccelerated();

				if (extension.isAccelerated()) {
					extension.doRender(
							AcceleratedBlitRenderer.INSTANCE,
							context,
							context.transform	(),
							context.normal		(),
							context.blitLight	(),
							context.blitOverlay	(),
							context.blitColor	()
					);
				}
			}

			for (int index = 0, size = fillDrawContexts.size(); index < size; index ++) {
					var context = fillDrawContexts.get(index);
var extension = graphics.bufferSource.getBuffer(context.renderType()).getAccelerated();

				if (extension.isAccelerated()) {
					extension.doRender(
							AcceleratedFillRenderer.INSTANCE,
							context,
							context.transform	(),
							context.normal		(),
							context.light		(),
							context.overlay		(),
							context.color		()
					);
				}
			}

			for (int index = 0, size = gradientDrawContexts.size(); index < size; index ++) {
					var context = gradientDrawContexts.get(index);
var extension = graphics.bufferSource.getBuffer(context.renderType()).getAccelerated();

				if (extension.isAccelerated()) {
					extension.doRender(
							AcceleratedGradientRenderer.INSTANCE,
							context,
							context.transform	(),
							context.normal		(),
							context.light		(),
							context.overlay		(),
							-1
					);
				}
			}

			for (int index = 0, size = stringDrawContexts.size(); index < size; index ++) {
					stringDrawContexts.get(index).drawString(graphics.bufferSource);
			}

			scissorFlush.record	(graphics);
			scissorDraw	.restore();

			Lighting	.setupForFlatItems					();
			CoreFeature	.forceSetDefaultLayer				(1);
			CoreFeature	.forceSetDefaultLayerBeforeFunction	(Lighting::setupForFlatItems);
			CoreFeature	.forceSetDefaultLayerAfterFunction	(Lighting::setupFor3DItems);

			var resolver = Minecraft.getInstance().getItemModelResolver();

			renderItemContexts(resolver, graphics, poseStack, flatItemDrawContexts);

			graphics	.flush							();
			Lighting	.setupFor3DItems				();
			CoreFeature	.resetDefaultLayer				();
			CoreFeature	.resetDefaultLayerBeforeFunction();
			CoreFeature	.resetDefaultLayerAfterFunction	();

			renderItemContexts(resolver, graphics, poseStack, blockItemDrawContexts);

			graphics	.flush				();
			flushBatching					();
			// Decorator + highlight batching removed — dead NeoForge API / obsoleted by blitSprite
//			for (var context : decoratorDrawContexts) {
//				poseStack.pushPose	();
//				poseStack.setPose	(context.transform(), context.normal());
//
//				context.handler().render(
//						graphics,
//						context.font	(),
//						context.stack	(),
//						context.xOffset	(),
//						context.yOffset	()
//				);
//
//				graphics.pose().popPose();
//			}

			// Slot highlights batched via blitSprite→submitBlit (see AbstractContainerScreenMixin) — highlight batching disabled
			for (var context : highlightDrawContexts) {
				poseStack.pushPose	();
				poseStack.setPose	(context.transform(), context.normal());

// 1.21.4 disabled: 				Slot highlights routed via blitSprite→submitBlit
// 1.21.4 disabled: 						graphics,
// 1.21.4 disabled: 						context.highlightX	(),
// 1.21.4 disabled: 						context.highlightY	(),
// 1.21.4 disabled: 						context.blitOffset	()
// 1.21.4 disabled: 				);

				graphics.pose().popPose();
			}

			depthLayers				.clear	();
			blitDrawContexts		.clear	();
			stringDrawContexts		.clear	();
			decoratorDrawContexts	.clear	();
			fillDrawContexts		.clear	();
			highlightDrawContexts	.clear	();
			gradientDrawContexts	.clear	();
			flatItemDrawContexts	.clear	();
			blockItemDrawContexts	.clear	();
			scissorFlush			.restore();

			return offset;
		}

		return 0.0f;
	}

	public void flushBatching() {
		CoreStates						.recordBuffers	();
		CoreBuffers.POS					.prepareBuffers	();
		CoreBuffers.POS_TEX_COLOR		.prepareBuffers	();
		CoreBuffers.POS_COLOR_TEX_LIGHT	.prepareBuffers	();
		CoreBuffers.POS_COLOR			.prepareBuffers	();
		CoreBuffers.POS_TEX				.prepareBuffers	();
		CoreStates						.restoreBuffers	();

		CoreBuffers.POS					.drawBuffers	(LayerDrawType.ALL);
		CoreBuffers.POS_TEX_COLOR		.drawBuffers	(LayerDrawType.ALL);
		CoreBuffers.POS_COLOR_TEX_LIGHT	.drawBuffers	(LayerDrawType.ALL);
		CoreBuffers.POS_COLOR			.drawBuffers	(LayerDrawType.ALL);
		CoreBuffers.POS_TEX				.drawBuffers	(LayerDrawType.ALL);

		CoreBuffers.POS					.clearBuffers	();
		CoreBuffers.POS_TEX_COLOR		.clearBuffers	();
		CoreBuffers.POS_COLOR_TEX_LIGHT	.clearBuffers	();
		CoreBuffers.POS_COLOR			.clearBuffers	();
		CoreBuffers.POS_TEX				.clearBuffers	();
	}

	public void submitBlit(
			Matrix4f			transform,
			Matrix3f			normal,
			Identifier	atlasLocation,
			int					minX,
			int					maxX,
			int					minY,
			int					maxY,
			int					blitOffset,
			int					blitColor,
			float				minU,
			float				maxU,
			float				minV,
			float				maxV,
			java.util.function.Function<Identifier, net.minecraft.client.renderer.RenderType> renderTypeGetter
	) {
		var layer = getLayer(getGlobalDepth(
				transform.m22(),
				transform.m32(),
				blitOffset
		));

		var context = new BlitDrawContext(
				new Matrix4f(transform),
				new Matrix3f(normal),
				atlasLocation,
				minX,
				maxX,
				minY,
				maxY,
				blitOffset,
				blitColor,
				0,
				0,
				minU,
				maxU,
				minV,
				maxV,
				renderTypeGetter
		);

		blitDrawContexts.add(context);
		layer			.add(context);
	}

	public void submitItem(
			Matrix4f			transform,
			Matrix3f			normal,
			ItemStack			itemStack,
			ItemDisplayContext	displayContext,
			boolean				leftHand,
			int					combinedLight,
			int					combinedOverlay,
			BakedModel			bakedModel,
			boolean				blockLight
	) {
		var layer = getLayer(getGlobalDepth(
				transform.m22(),
				transform.m32(),
				0.0f
		));

		var context = new ItemDrawContext(
				new Matrix4f(transform),
				new Matrix3f(normal),
				itemStack,
				displayContext,
				leftHand,
				combinedLight,
				combinedOverlay,
				bakedModel
		);

		var contexts = blockLight ? blockItemDrawContexts : flatItemDrawContexts;

		contexts.add(context);
		layer	.add(context);
	}

	public void submitFill(
			Matrix4f	transform,
			Matrix3f	normal,
			RenderType	renderType,
			int			minX,
			int			minY,
			int			maxX,
			int			maxY,
			int			blitOffset,
			int			color
	) {
		if (RenderTypeUtils.hasDepth(renderType)) {
			var layer = getLayer(getGlobalDepth(
					transform.m22(),
					transform.m32(),
					blitOffset
			));

			var context = new FillDrawContext(
					new Matrix4f(transform),
					new Matrix3f(normal),
					renderType,
					minX,
					minY,
					maxX,
					maxY,
					blitOffset,
					color,
					0,
					0
			);

			fillDrawContexts.add(context);
			layer			.add(context);
		} else {
				// Guard: depthLayers may be empty on first call (e.g. simpleshulkerpreview)
				if (depthLayers.isEmpty()) {
					var layer = getLayer(getGlobalDepth(
							transform.m22(),
							transform.m32(),
							blitOffset
					));
					var context = new FillDrawContext(
							new Matrix4f(transform),
							new Matrix3f(normal),
							renderType,
							minX, minY, maxX, maxY,
							blitOffset, color, 0, 0
					);
					fillDrawContexts.add(context);
					layer.add(context);
					return;
				}

				var highestEntry = depthLayers	.lastEntry	();
				var highestDepth = highestEntry	.getKey		();
			var highestLayer = highestEntry	.getValue	();
			var elementLayer = getLayer					(highestDepth + highestLayer.getLayerThickness());

			var originalDepth = getGlobalDepth(
					transform.m22(),
					transform.m32(),
					blitOffset
			);

			var context = new FillDrawContext(
					new Matrix4f				(transform).translateLocal(0.0f, 0.0f, elementLayer.getLayerDepth() - originalDepth),
					new Matrix3f				(normal),
					RenderTypeUtils.withDepth	(renderType),
					minX,
					minY,
					maxX,
					maxY,
					0,
					color,
					0,
					0
			);

			fillDrawContexts.add(context);
			elementLayer	.add(context);
		}
	}

	public void submitGradient(
			Matrix4f	transform,
			Matrix3f	normal,
			RenderType	renderType,
			int			minX,
			int			minY,
			int			maxX,
			int			maxY,
			int			blitOffset,
			int			colorFrom,
			int			colorTo
	) {
		if (RenderTypeUtils.hasDepth(renderType)) {
			var layer = getLayer(getGlobalDepth(
					transform.m22(),
					transform.m32(),
					blitOffset
			));

			var context = new GradientDrawContext(
					new Matrix4f(transform),
					new Matrix3f(normal),
					renderType,
					minX,
					minY,
					maxX,
					maxY,
					blitOffset,
					colorFrom,
					colorTo,
					0,
					0
			);

			gradientDrawContexts.add(context);
			layer				.add(context);
		} else {
				// Guard: depthLayers may be empty on first call (e.g. simpleshulkerpreview)
				if (depthLayers.isEmpty()) {
					var layer = getLayer(getGlobalDepth(
							transform.m22(),
							transform.m32(),
							blitOffset
					));
					var context = new GradientDrawContext(
							new Matrix4f(transform),
							new Matrix3f(normal),
							renderType,
							minX, minY, maxX, maxY,
							blitOffset, colorFrom, colorTo, 0, 0
					);
					gradientDrawContexts.add(context);
					layer.add(context);
					return;
				}

				var highestEntry = depthLayers	.lastEntry	();
				var highestDepth = highestEntry	.getKey		();
			var highestLayer = highestEntry	.getValue	();
			var elementLayer = getLayer					(highestDepth + highestLayer.getLayerThickness());

			var originalDepth = getGlobalDepth(
					transform.m22(),
					transform.m32(),
					blitOffset
			);

			var context = new GradientDrawContext(
					new Matrix4f				(transform).translateLocal(0.0f, 0.0f, elementLayer.getLayerDepth() - originalDepth),
					new Matrix3f				(normal),
					RenderTypeUtils.withDepth	(renderType),
					minX,
					minY,
					maxX,
					maxY,
					0,
					colorFrom,
					colorTo,
					0,
					0
			);

			gradientDrawContexts.add(context);
			elementLayer		.add(context);
		}
	}

//	@SuppressWarnings("UnstableApiUsage")
//	public void submitCustomDecorator(
//			Matrix4f				transform,
//			Matrix3f				normal,
//			ItemDecoratorHandler	handler,
//			Font					font,
//			ItemStack				itemStack,
//			int						xOffset,
//			int						yOffset
//	) {
//		var layer = getLayer(getGlobalDepth(
//				transform.m22(),
//				transform.m32(),
//				10.0f
//		));
//
//		var context = new DecoratorDrawContext(
//				new Matrix4f(transform),
//				new Matrix3f(normal),
//				handler,
//				font,
//				itemStack,
//				xOffset,
//				yOffset
//		);
//
//		decoratorDrawContexts	.add(context);
//		layer					.add(context);
//	}

	public void submitHighlight(
			Matrix4f	transform,
			Matrix3f	normal,
			int			highlightX,
			int			highlightY,
			int			blitOffset,
			int			color
	) {
		var layer = getLayer(getGlobalDepth(
				transform.m22(),
				transform.m32(),
				blitOffset
		));

		var context = new HighlightDrawContext(
				new Matrix4f(transform),
				new Matrix3f(normal),
				highlightX,
				highlightY,
				blitOffset,
				color
		);

		highlightDrawContexts	.add(context);
		layer					.add(context);
	}

	public void submitString(IStringDrawContext context) {
		var layer = getLayer(getGlobalDepth(
				context.transform().m22(),
				context.transform().m32(),
				0.0f
		));

		stringDrawContexts	.add(context);
		layer				.add(context);
	}

	private Layer getLayer(float depth) {
		var layer = depthLayers.get(depth);

		if (layer == null) {
			layer = new Layer(depth);
		}

		return layer;
	}

	private void renderItemContexts(net.minecraft.client.renderer.item.ItemModelResolver resolver, GuiGraphics graphics, com.mojang.blaze3d.vertex.PoseStack poseStack, List<ItemDrawContext> contexts) {
		for (var context : contexts) {
			var state = new net.minecraft.client.renderer.item.ItemStackRenderState();
			ItemStackFilterStack.push(context.itemStack());
			try {
				resolver.updateForTopItem(state, context.itemStack(), context.displayContext(), context.leftHand(), null, null, 0);
				poseStack.pushPose();
				poseStack.setPose(context.transform(), context.normal());
				state.render(poseStack, graphics.bufferSource, context.combinedLight(), context.combinedOverlay());
				poseStack.popPose();
			} finally {
				ItemStackFilterStack.pop();
			}
		}
	}

	public static float getGlobalDepth(
			float m22,
			float m32,
			float localDepth
	) {
		return m22 * localDepth + m32;
	}

	public void delete() {
		scissorDraw	.delete();
		scissorFlush.delete();
	}

	@Getter
	public class Layer {

		private final	List<IGuiElementContext>	layerElements;
		private final	float						layerDepth;
		private			float						layerThickness;

		public Layer(float depth) {
			this.layerElements	= new ReferenceArrayList<>();
			this.layerDepth		= depth;
			this.layerThickness	= 0.0f;

			depthLayers.put(depth, this);
		}

		public void add(IGuiElementContext context) {
			layerElements.add(context);

			if (layerThickness < context.thickness()) {
				layerThickness = context.thickness();
			}
		}
	}
}
