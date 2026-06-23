package com.example.spawnerutils.modules;

import com.example.spawnerutils.SpawnerUtilsAddon;
import meteordevelopment.meteorclient.eventbus.EventHandler;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;

/**
 * Block-Entity-Debugger.
 *
 * Block-Entities (Kisten, Spawner, Shulker, Öfen, Schilder ...) werden vom
 * Server unabhängig davon zum Client geschickt, ob sie hinter Blöcken liegen.
 * Dadurch sieht dieses Modul automatisch auch Block-Entities, die tief unter
 * dem Deepslate-Layer verborgen sind – es braucht dafür kein zusätzliches X-Ray.
 */
public class BlockEntityDebug extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> chunkRange = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-range")
        .description("Radius in Chunks, der nach Block-Entities abgesucht wird.")
        .defaultValue(12)
        .min(1).sliderRange(1, 32)
        .build());

    private final Setting<Boolean> storageOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("storage-only")
        .description("Nur Behälter (Kisten, Shulker, Barrel, Öfen ...) anzeigen.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> belowDeepslateOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("below-deepslate-only")
        .description("Nur Block-Entities unterhalb der eingestellten Höhe anzeigen – nützlich, um gezielt unter den Deepslate-Layer zu schauen.")
        .defaultValue(false)
        .build());

    private final Setting<Integer> belowY = sgGeneral.add(new IntSetting.Builder()
        .name("below-y")
        .description("Höhe, unter der gefiltert wird (Deepslate beginnt etwa bei Y 0 bis 8).")
        .defaultValue(0)
        .sliderRange(-64, 64)
        .visible(belowDeepslateOnly::get)
        .build());

    private final Setting<Boolean> logCounts = sgGeneral.add(new BoolSetting.Builder()
        .name("log-on-activate")
        .description("Beim Aktivieren die Anzahl gefundener Block-Entities in den Chat schreiben.")
        .defaultValue(false)
        .build());

    private final Setting<SettingColor> storageColor = sgRender.add(new ColorSetting.Builder()
        .name("storage-color")
        .description("Farbe für Behälter.")
        .defaultValue(new SettingColor(255, 200, 0, 60))
        .build());

    private final Setting<SettingColor> spawnerColor = sgRender.add(new ColorSetting.Builder()
        .name("spawner-color")
        .description("Farbe für Spawner.")
        .defaultValue(new SettingColor(255, 0, 0, 60))
        .build());

    private final Setting<SettingColor> otherColor = sgRender.add(new ColorSetting.Builder()
        .name("other-color")
        .description("Farbe für alle übrigen Block-Entities.")
        .defaultValue(new SettingColor(0, 200, 255, 50))
        .build());

    public BlockEntityDebug() {
        super(SpawnerUtilsAddon.CATEGORY, "block-entity-debug",
            "Markiert alle Block-Entities, auch tief unter dem Deepslate-Layer.");
    }

    @Override
    public void onActivate() {
        if (logCounts.get()) {
            info("Block-Entities in Reichweite: %d", countAll());
        }
    }

    private int countAll() {
        if (mc.world == null || mc.player == null) return 0;
        int total = 0;
        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;
        int r = chunkRange.get();
        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                WorldChunk chunk = mc.world.getChunk(cx, cz);
                if (chunk == null) continue;
                total += chunk.getBlockEntities().size();
            }
        }
        return total;
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
                    BlockPos pos = entry.getKey();
                    BlockEntity be = entry.getValue();

                    if (belowDeepslateOnly.get() && pos.getY() >= belowY.get()) continue;

                    boolean storage = isStorage(be);
                    boolean spawner = be instanceof MobSpawnerBlockEntity;

                    if (storageOnly.get() && !storage) continue;

                    SettingColor side = spawner ? spawnerColor.get()
                        : storage ? storageColor.get()
                        : otherColor.get();
                    SettingColor line = new SettingColor(side.r, side.g, side.b, 255);

                    event.renderer.box(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                        side, line, ShapeMode.Both, 0);
                }
            }
        }
    }

    private boolean isStorage(BlockEntity be) {
        return be instanceof LootableContainerBlockEntity
            || be instanceof ChestBlockEntity
            || be instanceof ShulkerBoxBlockEntity
            || be instanceof EnderChestBlockEntity;
    }
}
