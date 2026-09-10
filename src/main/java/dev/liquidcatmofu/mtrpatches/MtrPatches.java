package dev.liquidcatmofu.mtrpatches;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(MtrPatches.MOD_ID)
public final class MtrPatches {
    public static final String MOD_ID = "mtr_patches";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MtrPatches() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PatchConfig.SPEC);
        LOGGER.info("MTR 1.20.1 Patches loaded");
    }
}
