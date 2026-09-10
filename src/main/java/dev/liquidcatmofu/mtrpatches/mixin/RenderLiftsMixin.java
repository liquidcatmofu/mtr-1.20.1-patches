package dev.liquidcatmofu.mtrpatches.mixin;

import dev.liquidcatmofu.mtrpatches.LiftModelCache;
import dev.liquidcatmofu.mtrpatches.PatchConfig;
import org.mtr.mod.model.ModelLift1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "org.mtr.mod.render.RenderLifts", remap = false)
public abstract class RenderLiftsMixin {
    @Redirect(
            method = "render",
            at = @At(value = "NEW", target = "Lorg/mtr/mod/model/ModelLift1;<init>(IIIZ)V"),
            remap = false,
            require = 0
    )
    private static ModelLift1 mtrPatches$reuseLiftModel(int height, int width, int depth, boolean isDoubleSided) {
        if (!PatchConfig.FIX_LIFT_MODEL_REBUILD.get()) {
            return new ModelLift1(height, width, depth, isDoubleSided);
        }
        return LiftModelCache.get(height, width, depth, isDoubleSided);
    }
}
