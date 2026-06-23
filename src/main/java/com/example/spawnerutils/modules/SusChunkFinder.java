package com.example.spawnerutils.modules;

import com.example.spawnerutils.SpawnerUtilsAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SusChunkFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> chunkRange = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-range")
        .description("Radius in Chunks, der um den Spieler herum gescannt wird.")
        .defaultValue(8)
        .min(1).sliderRange(1, 16)
        .build());

    private final Setting<Integer> chunksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("chunks-per-tick")
        .description("Wie viele neue Chunks pro Tick gescannt werden.")
        .defaultValue(2)
        .min(1).sliderRange(1, 8)
        .build());

    private final Setting<Integer> minY = sgGeneral.add(new IntSetting.Builder()
        .name("min-y")
        .description("Unterste Y-Hoehe, die gescannt wird.")
        .defaultValue(-64)
        .sliderRange(-64, 320)
        .build());

    private final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
        .name("max-y")
        .description("Oberste Y-Hoehe, die gescannt wird.")
        .defaultValue(120)
        .sliderRange(-64, 320)
        .build());

    private final Setting<Integer> susAboveY = sgGeneral.add(new IntSetting.Builder()
        .name("sus-above-y")
        .description("Amethyst oberhalb dieser Hoehe gilt als verdaechtig.")
        .defaultValue(30)
        .sliderRange(-64, 320)
        .build());

    private final Setting<Boolean> susOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("sus-only")
        .description("Nur verdaechtigen Amethyst rendern und melden.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-notify")
        .description("Im Chat melden, wenn ein verdaechtiger Chunk gefunden wird.")
        .defaultValue(true)
        .build());

    private final Setting<SettingColor> normalColor = sgRender.add(new ColorSetting.Builder()
        .name("normal-color")
        .description("Farbe fuer normalen Amethyst.")
        .defaultValue(new SettingColor(180, 100, 255, 120))
        .build());

    private final Setting<SettingColor> susColor = sgRender.add(new ColorSetting.Builder()
        .name("sus-color")
        .description("Farbe fuer verdaechtigen Amethyst.")
        .defaultValue(new SettingColor(255, 60, 60, 160))
        .build());

    private final Set<Long> scanned = new HashSet<>();
    private final Map<BlockPos, Boolean> found = new HashMap<>();
    private final Set<Long> reportedChunks = new HashSet<>();

    public SusChunkFinder() {
        super(SpawnerUtilsAddon.CATEGORY, "sus-chunk-finder",
            "Scannt Chunks nach Amethyst-Clustern und markiert verdaechtige Vorkommen.");
    }

    @Override
    public void onActivate() {
        clear();
    }

    @Override
    public void onDeactivate() {
        clear();
    }

    private void clear() {
        scanned.clear();
        found.clear();
        reportedChunks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null) return;

        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int r = chunkRange.get();
        int budget = chunksPerTick.get();

        for (int cx = pcx - r; cx <= pcx + r && budget > 0; cx++) {
            for (int cz = pcz - r; cz <= pcz + r && budget > 0; cz++) {
                long key = (((long) cx) << 32) | (cz & 0xffffffffL);
                if (scanned.contains(key)) continue;

                LevelChunk chunk = mc.level.getChunk(cx, cz);
                if (chunk == null) continue;

                scanChunk(chunk, cx, cz);
                scanned.add(key);
                budget--;
            }
        }
    }

    private void scanChunk(LevelChunk chunk, int cx, int cz) {
        int lo = Math.min(minY.get(), maxY.get());
        int hi = Math.max(minY.get(), maxY.get());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean susInThisChunk = false;
        int count = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (cx << 4) + x;
                int worldZ = (cz << 4) + z;
                for (int y = lo; y <= hi; y++) {
                    pos.set(worldX, y, worldZ);
                    if (!isAmethyst(chunk.getBlockState(pos).getBlock())) continue;

                    boolean sus = y > susAboveY.get();
                    if (susOnly.get() && !sus) continue;

                    found.put(pos.immutable(), sus);
                    count++;
                    if (sus) susInThisChunk = true;
                }
            }
        }

        if (notify.get() && susInThisChunk) {
            long key = (((long) cx) << 32) | (cz & 0xffffffffL);
            if (reportedChunks.add(key)) {
                info("Verdaechtiger Amethyst in Chunk [%d, %d] (Mitte: x=%d z=%d, %d Bloecke).",
                    cx, cz, (cx << 4) + 8, (cz << 4) + 8, count);
            }
        }
    }

    private boolean isAmethyst(Block b) {
        return b == Blocks.AMETHYST_CLUSTER
            || b == Blocks.SMALL_AMETHYST_BUD
            || b == Blocks.MEDIUM_AMETHYST_BUD
            || b == Blocks.LARGE_AMETHYST_BUD
            || b == Blocks.BUDDING_AMETHYST;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (found.isEmpty() || mc.level == null) return;

        Iterator<Map.Entry<BlockPos, Boolean>> it = found.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Boolean> entry = it.next();
            BlockPos pos = entry.getKey();
            boolean sus = entry.getValue();

            if (mc.level.isLoaded(pos) && !isAmethyst(mc.level.getBlockState(pos).getBlock())) {
                it.remove();
                continue;
            }

            SettingColor c = sus ? susColor.get() : normalColor.get();
            SettingColor line = new SettingColor(c.r, c.g, c.b, 255);

            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                c, line, ShapeMode.Both, 0);
        }
    }
}
