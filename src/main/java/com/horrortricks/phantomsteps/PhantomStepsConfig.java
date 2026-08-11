package com.horrortricks.phantomsteps;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class PhantomStepsConfig {
   private static final Builder BUILDER = new Builder();
   public static final BooleanValue ENABLED = BUILDER.comment("Master switch for the phantom footstep effect.").define("enabled", true);
   public static final IntValue MIN_INTERVAL_TICKS = BUILDER.comment(
         new String[]{"Minimum ticks (20 ticks/second) between phantom footsteps for a given player.", "Default of 600 is 30 seconds."}
      )
      .defineInRange("minIntervalTicks", 600, 20, Integer.MAX_VALUE);
   public static final IntValue MAX_INTERVAL_TICKS = BUILDER.comment(
         new String[]{"Maximum ticks (20 ticks/second) between phantom footsteps for a given player.", "Default of 1600 is 80 seconds."}
      )
      .defineInRange("maxIntervalTicks", 1600, 20, Integer.MAX_VALUE);
   public static final DoubleValue MIN_RADIUS = BUILDER.comment("Minimum horizontal distance (in blocks) from the player the fake footstep can play at.")
      .defineInRange("minRadius", 3.0, 1.0, 64.0);
   public static final DoubleValue MAX_RADIUS = BUILDER.comment("Maximum horizontal distance (in blocks) from the player the fake footstep can play at.")
      .defineInRange("maxRadius", 8.0, 1.0, 64.0);
   public static final DoubleValue VOLUME = BUILDER.comment("Volume multiplier applied on top of the ground block's own step-sound volume.")
      .defineInRange("volume", 1.0, 0.0, 5.0);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private PhantomStepsConfig() {
   }

   static {
      BUILDER.push("general");
      BUILDER.pop();
   }
}
