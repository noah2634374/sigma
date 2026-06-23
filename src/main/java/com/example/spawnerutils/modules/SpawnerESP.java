package com.example.spawnerutils.modules;

import com.example.spawnerutils.SpawnerUtilsAddon;
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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

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
        .description("Hoehe der Linie, die vom Spawner aufsteigt.")
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
        .description("Zusaetzlich eine Box um den Spawner zeichnen.")
        .defaultValue(true)
        .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Fuellfarbe der Box.")
        .defaultValue(new SettingColor(255, 0, 0, 40))
        .visible(drawBox::get)
        .build());

    public SpawnerESP() {
        super(SpawnerUtilsAddon.CATEGORY, "spawner-esp",
            "Zeichnet eine rote Linie, die von jedem Spawner aufsteigt.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.level == null || mc.player == null) return;

        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int r = chunkRange.get();

        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                LevelChunk chunk = mc.level.getChunk(cx, cz);
                if (chunk == null) continue;

                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = entry.getValue();
                    boolean isSpawner = be instanceof SpawnerBlockEntity
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

        event.renderer.line(x, y, z, x, y + lineHeight.get(), z, lineColor.get());

        if (drawBox.get()) {
            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
