package dev.liquidcatmofu.mtrpatches.mixin;

import dev.liquidcatmofu.mtrpatches.PatchConfig;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.render.DynamicVehicleModel;
import org.mtr.mod.resource.OptimizedModelWrapper;
import org.mtr.mod.resource.StoredModelResourceBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "org.mtr.mod.resource.StoredModelResourceBase", remap = false)
public interface StoredModelResourceBaseMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/mtr/mod/resource/StoredModelResourceBase;getOptimizedModel()Lorg/mtr/mod/resource/OptimizedModelWrapper;"
            ),
            remap = false,
            require = 1
    )
    private static OptimizedModelWrapper mtrPatches$skipUnusedOptimizedModel(StoredModelResourceBase resource) {
        if (PatchConfig.FIX_STORED_MODEL_RESOURCE_DOUBLE_FETCH.get() && !OptimizedRenderer.hasOptimizedRendering()) {
            return null;
        }
        return resource.getOptimizedModel();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/mtr/mod/resource/StoredModelResourceBase;getDynamicVehicleModel()Lorg/mtr/mod/render/DynamicVehicleModel;"
            ),
            remap = false,
            require = 1
    )
    private static DynamicVehicleModel mtrPatches$skipUnusedDynamicModel(StoredModelResourceBase resource) {
        if (PatchConfig.FIX_STORED_MODEL_RESOURCE_DOUBLE_FETCH.get() && OptimizedRenderer.hasOptimizedRendering()) {
            return null;
        }
        return resource.getDynamicVehicleModel();
    }
}
