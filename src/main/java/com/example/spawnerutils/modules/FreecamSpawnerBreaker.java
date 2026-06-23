package com.example.spawnerutils.modules;

import com.example.spawnerutils.SpawnerUtilsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;

public class FreecamSpawnerBreaker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Reichweite in Bloecken, gemessen vom echten Standort des Spielers (nicht von der Freecam).")
        .defaultValue(6)
        .min(1).sliderRange(1, 64)
        .build());

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks Pause zwischen zwei Abbau-Versuchen. 0 = jeden Tick.")
        .defaultValue(0)
        .min(0).sliderRange(0, 20)
        .build());

    private final Setting<Boolean> onlyFreecam = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-freecam-active")
        .description("Bricht nur ab, waehrend Meteors Freecam aktiv ist.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> nearestOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("nearest-only")
        .description("Nur den naechstgelegenen Spawner abbauen (gut fuer gestackte Spawner an einer Position).")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Arm beim Abbauen schwingen.")
        .defaultValue(true)
        .build());

    private int timer;

    public FreecamSpawnerBreaker() {
        super(SpawnerUtilsAddon.CATEGORY, "freecam-spawner-breaker",
            "Baut Spawner (auch gestackte) ohne Sichtlinie durch Bloecke ab. Ideal mit der Freecam.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null || mc.gameMode == null) return;

        if (onlyFreecam.get()) {
            Freecam freecam = Modules.get().get(Freecam.class);
            if (freecam == null || !freecam.isActive()) return;
        }

        if (timer > 0) {
            timer--;
            return;
        }

        double rangeSq = range.get() * range.get();
        BlockPos origin = mc.player.blockPosition();

        int chunkR = (int) Math.ceil(range.get() / 16.0) + 1;
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int cx = pcx - chunkR; cx <= pcx + chunkR; cx++) {
            for (int cz = pcz - chunkR; cz <= pcz + chunkR; cz++) {
                LevelChunk chunk = mc.level.getChunk(cx, cz);
                if (chunk == null) continue;

                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof SpawnerBlockEntity)) continue;

                    BlockPos pos = entry.getKey();
                    double dist = origin.distSqr(pos);
                    if (dist > rangeSq) continue;

                    if (!nearestOnly.get()) {
                        BlockUtils.breakBlock(pos, swing.get());
                    } else if (dist < bestDist) {
                        bestDist = dist;
                        best = pos;
                    }
                }
            }
        }

        if (nearestOnly.get() && best != null) {
            // Gestackte Spawner bleiben nach jedem "Pop" weiterhin Spawner an
            // derselben Stelle, deshalb wird hier wiederholt dieselbe Position getroffen.
            BlockUtils.breakBlock(best, swing.get());
        }

        timer = delay.get();
    }
}
