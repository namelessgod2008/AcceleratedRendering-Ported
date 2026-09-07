package com.namelessgod2008.features.modelparts.mixins;

import com.namelessgod2008.core.CoreFeature;
import com.namelessgod2008.core.buffers.accelerated.builders.IBufferGraph;
import com.namelessgod2008.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.namelessgod2008.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.namelessgod2008.core.utils.FastColorCompat;
import com.namelessgod2008.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.namelessgod2008.core.meshes.IMesh;
import com.namelessgod2008.core.meshes.data.MeshData;
import com.namelessgod2008.features.entities.AcceleratedEntityRenderingFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * 1.21.4 ModelPartMixin — compile() uses doRender for wrapper delegation,
 * render() follows the proven old 1.21.1 logic exactly.
 */
@ExtensionMethod(VertexConsumerExtension.class)
@Mixin(ModelPart.class)
public class ModelPartMixin implements IAcceleratedRenderer<Void> {

    @Shadow @Final public List<ModelPart.Cube> cubes;

    @Unique private final Map<IBufferGraph, IMesh> meshes = new Object2ObjectOpenHashMap<>();
    @Unique private final Map<MeshData, IMesh> merges = new Object2ObjectOpenHashMap<>();

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    public void compile(
            PoseStack.Pose pPose,
            VertexConsumer pBuffer,
            int pPackedLight,
            int pPackedOverlay,
            int pColor,
            CallbackInfo ci
    ) {
        var extension = pBuffer.getAccelerated();

        // Only accelerate during world rendering — skip hand/GUI rendering
        if (!CoreFeature.isRenderingLevel()) {
            return;
        }
        if (CoreFeature.isRenderingHand()) {
            return;
        }

        if (!AcceleratedEntityRenderingFeature.isEnabled()) {
            return;
        }

        if (!AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()) {
            return;
        }

        if (!extension.isAccelerated()) {
            return;
        }

        ci.cancel();
        extension.doRender(
                this,
                null,
                pPose.pose(),
                pPose.normal(),
                pPackedLight,
                pPackedOverlay,
                pColor
        );
    }

    @Unique
    @Override
    public void render(
            VertexConsumer vertexConsumer,
            Void context,
            Matrix4f transform,
            Matrix3f normal,
            int light,
            int overlay,
            int color
    ) {
        var extension = vertexConsumer.getAccelerated();

        extension.beginTransform(transform, normal);

        var mesh = meshes.get(extension);

        if (mesh != null) {
            mesh.write(extension, color, light, overlay);
            extension.endTransform();
            return;
        }

        var meshCollector = CoreFeature.createMeshCollector(extension);
        var meshBuilder  = extension.decorate(meshCollector);

        for (ModelPart.Cube cube : cubes) {
            for (ModelPart.Polygon polygon : cube.polygons) {
                Vector3f polygonNormal = polygon.normal;

                for (ModelPart.Vertex vertex : polygon.vertices) {
                    meshBuilder.addVertex(
                            vertex.pos.x / 16.0f,
                            vertex.pos.y / 16.0f,
                            vertex.pos.z / 16.0f,
                            -1,
                            vertex.u,
                            vertex.v,
                            overlay,
                            0,
                            polygonNormal.x,
                            polygonNormal.y,
                            polygonNormal.z
                    );
                }
            }
        }

        meshCollector.flush();

        var data   = meshCollector.getData();
        var buffer = meshCollector.getBuffer();
        mesh = merges.get(data);

        if (mesh != null) {
            buffer.discard();
            buffer.close();
        } else {
            mesh = AcceleratedEntityRenderingFeature
                    .getMeshType()
                    .getBuilder()
                    .build(meshCollector);
        }

        meshes.put(extension, mesh);
        merges.put(data, mesh);

        mesh.write(extension, color, light, overlay);
        extension.endTransform();
    }
}
