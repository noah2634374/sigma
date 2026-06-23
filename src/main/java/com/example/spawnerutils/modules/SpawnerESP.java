package com.example.spawnerutils.modules;

import com.example.spawnerutils.SpawnerUtilsAddon;
import meteordevelopment.meteorclient.eventbus.EventHandler;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;

public class SpawnerESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> chunkRange = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-range")
        .description("Wie viele Chunks um dich herum nach Spawnern abgesucht werden.")
        .defaultValue(12)
        .min(1).sliderRange(1, 32)
        .build());

    private final Setting<Boolean> trialSpawners = sgGeneral.add(new BoolSetting.Builder()
        .name("trial-spawner")
        .description("Auch Trial-Spawner markieren.")
        .defaultValue(true)
        .build());

    private final Setting<Double> lineHeight = sgRender.add(new DoubleSetting.Builder()
        .name("line-height")
        .description("Höhe der Linie, die vom Spawner aufsteigt.")
        .defaultValue(256)
        .min(1).sliderRange(1, 320)
        .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Farbe der aufsteigenden Linie.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build());

    private final Setting<Boolean> drawBox = sgRender.add(new BoolSetting.Builder()
        .name("box")
        .description("Zusätzlich eine Box um den Spawner zeichnen.")
        .defaultValue(true)
        .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Füllfarbe der Box.")
        .defaultValue(new SettingColor(255, 0, 0, 40))
        .visible(drawBox::get)
        .build());

    public SpawnerESP() {
        super(SpawnerUtilsAddon.CATEGORY, "spawner-esp",
            "Zeichnet eine rote Linie, die von jedem Spawner aufsteigt, damit man sie durch Wände und aus der Ferne sieht.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;
        int r = chunkRange.get();

        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                WorldChunk chunk = mc.world.getChunk(cx, cz);
                if (chunk == null) continue;

                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = entry.getValue();
                    boolean isSpawner = be instanceof MobSpawnerBlockEntity
                        || (trialSpawners.get() && be instanceof TrialSpawnerBlockEntity);
                    if (!isSpawner) continue;

                    renderSpawner(event, entry.getKey());
                }
            }
        }
    }

    private void renderSpawner(Render3DEvent event, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        // Senkrechte Linie nach oben.
        event.renderer.line(x, y, z, x, y + lineHeight.get(), z, lineColor.get());

        if (drawBox.get()) {
            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
