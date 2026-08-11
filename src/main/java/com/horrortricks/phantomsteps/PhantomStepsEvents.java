package com.horrortricks.phantomsteps;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;

@EventBusSubscriber(modid = "phantomsteps")
public final class PhantomStepsEvents {
   private static final float STEP_VOLUME_SCALE = 0.15F;
   private static final int SEARCH_ABOVE = 2;
   private static final int SEARCH_BELOW = 8;
   private static final int MIN_STEPS_PER_SEQUENCE = 2;
   private static final int MAX_STEPS_PER_SEQUENCE = 3;
   private static final double STEP_DISTANCE = 1.667;
   private static final int BASE_STEP_INTERVAL_TICKS = 8;
   private static final int STEP_INTERVAL_JITTER_TICKS = 1;
   private static final Map<UUID, Long> NEXT_SEQUENCE_GAME_TIME = new HashMap<>();
   private static final Map<UUID, Deque<PhantomStepsEvents.PendingStep>> PENDING_STEPS = new HashMap<>();

   private PhantomStepsEvents() {
   }

   @SubscribeEvent
   public static void onLevelTick(Post event) {
      if (event.getLevel() instanceof ServerLevel level) {
         long var6 = level.getGameTime();

         for (ServerPlayer player : level.players()) {
            processPendingSteps(level, player, var6);
            if ((Boolean)PhantomStepsConfig.ENABLED.get()) {
               processSequenceTimer(level, player, var6);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      UUID id = event.getEntity().getUUID();
      NEXT_SEQUENCE_GAME_TIME.remove(id);
      PENDING_STEPS.remove(id);
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      PENDING_STEPS.remove(event.getEntity().getUUID());
   }

   private static void processSequenceTimer(ServerLevel level, ServerPlayer player, long gameTime) {
      UUID id = player.getUUID();
      Long next = NEXT_SEQUENCE_GAME_TIME.get(id);
      if (next == null) {
         scheduleNextSequence(id, gameTime, player.getRandom());
      } else if (gameTime >= next) {
         startPhantomWalk(level, player, gameTime);
         scheduleNextSequence(id, gameTime, player.getRandom());
      }
   }

   private static void scheduleNextSequence(UUID id, long gameTime, RandomSource random) {
      int min = (Integer)PhantomStepsConfig.MIN_INTERVAL_TICKS.get();
      int max = Math.max(min, (Integer)PhantomStepsConfig.MAX_INTERVAL_TICKS.get());
      int delay = min + random.nextInt(max - min + 1);
      NEXT_SEQUENCE_GAME_TIME.put(id, gameTime + delay);
   }

   private static void startPhantomWalk(ServerLevel level, ServerPlayer player, long gameTime) {
      RandomSource random = player.getRandom();
      double minRadius = (Double)PhantomStepsConfig.MIN_RADIUS.get();
      double maxRadius = Math.max(minRadius, (Double)PhantomStepsConfig.MAX_RADIUS.get());
      double offsetAngle = random.nextDouble() * Math.PI * 2.0;
      double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
      double anchorX = player.getBlockX() + Math.cos(offsetAngle) * distance;
      double anchorZ = player.getBlockZ() + Math.sin(offsetAngle) * distance;
      int baseY = player.getBlockY();
      int stepCount = 2 + random.nextInt(2);
      double walkAngle = random.nextDouble() * Math.PI * 2.0;
      double dirX = Math.cos(walkAngle);
      double dirZ = Math.sin(walkAngle);
      Deque<PhantomStepsEvents.PendingStep> queue = PENDING_STEPS.computeIfAbsent(player.getUUID(), unused -> new ArrayDeque<>());
      long stepTime = gameTime;

      for (int i = 0; i < stepCount; i++) {
         if (i > 0) {
            stepTime += 8 + random.nextInt(3) - 1;
         }

         int stepX = (int)Math.floor(anchorX + dirX * 1.667 * i);
         int stepZ = (int)Math.floor(anchorZ + dirZ * 1.667 * i);
         queue.addLast(new PhantomStepsEvents.PendingStep(level, stepTime, stepX, stepZ, baseY));
      }
   }

   private static void processPendingSteps(ServerLevel level, ServerPlayer player, long gameTime) {
      Deque<PhantomStepsEvents.PendingStep> queue = PENDING_STEPS.get(player.getUUID());
      if (queue != null) {
         while (!queue.isEmpty() && queue.peekFirst().triggerGameTime() <= gameTime) {
            PhantomStepsEvents.PendingStep step = queue.pollFirst();
            if (step.level() == level) {
               playStepIfGrounded(level, step);
            }
         }
      }
   }

   private static void playStepIfGrounded(ServerLevel level, PhantomStepsEvents.PendingStep step) {
      BlockPos groundPos = findGroundPos(level, step.x(), step.baseY(), step.z());
      if (groundPos != null) {
         RandomSource random = level.getRandom();
         SoundType soundType = level.getBlockState(groundPos).getSoundType(level, groundPos, null);
         SoundEvent stepSound = soundType.getStepSound();
         float volume = soundType.getVolume() * 0.15F * ((Double)PhantomStepsConfig.VOLUME.get()).floatValue();
         float pitch = soundType.getPitch() * (0.9F + random.nextFloat() * 0.2F);
         level.playSound(null, groundPos.getX() + 0.5, groundPos.getY() + 1.0, groundPos.getZ() + 0.5, stepSound, SoundSource.AMBIENT, volume, pitch);
      }
   }

   private static BlockPos findGroundPos(ServerLevel level, int x, int baseY, int z) {
      for (int dy = 2; dy >= -8; dy--) {
         BlockPos pos = new BlockPos(x, baseY + dy, z);
         if (level.isInWorldBounds(pos) && level.isLoaded(pos)) {
            BlockState state = level.getBlockState(pos);
            BlockPos abovePos = pos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            if (state.isFaceSturdy(level, pos, Direction.UP) && aboveState.getCollisionShape(level, abovePos).isEmpty()) {
               return pos;
            }
         }
      }

      return null;
   }

   private record PendingStep(ServerLevel level, long triggerGameTime, int x, int z, int baseY) {
   }
}
