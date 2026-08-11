package com.horrortricks.phantomsteps;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import org.slf4j.Logger;

@Mod("phantomsteps")
public class PhantomStepsMod {
   public static final String MOD_ID = "phantomsteps";
   private static final Logger LOGGER = LogUtils.getLogger();

   public PhantomStepsMod(IEventBus modEventBus, ModContainer modContainer) {
      modContainer.registerConfig(Type.COMMON, PhantomStepsConfig.SPEC);
      LOGGER.info("Phantom Steps initialized.");
   }
}
