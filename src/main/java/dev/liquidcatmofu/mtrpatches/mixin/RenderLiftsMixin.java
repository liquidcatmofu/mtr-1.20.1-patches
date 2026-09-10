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
    /**
     * The ModelLift1 allocation lives in the synthetic lambda generated for
     * RenderLifts#render, not in render() itself. Runtime profiler call paths on
     * the targeted MTR 1.20.1 build identify this method as lambda$render$6.
     */
    @Redirect(
            method = "lambda$render$6",
            at = @At(value = "NEW", target = "Lorg/mtr/mod/model/ModelLift1;"),
            remap = false,
            require = 1
    )
    private static ModelLift1 mtrPatches$reuseLiftModel(int height, int width, int depth, boolean isDoubleSided) {
        if (!PatchConfig.FIX_LIFT_MODEL_REBUILD.get()) {
            return new ModelLift1(height, width, depth, isDoubleSided);
        }
        return LiftModelCache.get(height, width, depth, isDoubleSided);
    }
}
