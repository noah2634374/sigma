package com.example.spawnerutils.modules;

import com.example.spawnerutils.SpawnerUtilsAddon;
import meteordevelopment.meteorclient.eventbus.EventHandler;
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
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Sucht geladene Chunks nach Amethyst-Clustern ab.
 * Amethyst, der oberhalb der typischen Geoden-Generierung gefunden wird,
 * wird als "verdächtig" (sus) markiert – ein Hinweis auf von Spielern
 * gebaute Farmen oder versteckte Stashes.
 */
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
        .description("Wie viele neue Chunks pro Tick gescannt werden. Höher = schneller, aber mehr Last.")
        .defaultValue(2)
        .min(1).sliderRange(1, 8)
        .build());

    private final Setting<Integer> minY = sgGeneral.add(new IntSetting.Builder()
        .name("min-y")
        .description("Unterste Y-Höhe, die gescannt wird.")
        .defaultValue(-64)
        .sliderRange(-64, 320)
        .build());

    private final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
        .name("max-y")
        .description("Oberste Y-Höhe, die gescannt wird.")
        .defaultValue(120)
        .sliderRange(-64, 320)
        .build());

    private final Setting<Integer> susAboveY = sgGeneral.add(new IntSetting.Builder()
        .name("sus-above-y")
        .description("Amethyst oberhalb dieser Höhe gilt als verdächtig (Geoden generieren normalerweise tiefer).")
        .defaultValue(30)
        .sliderRange(-64, 320)
        .build());

    private final Setting<Boolean> susOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("sus-only")
        .description("Nur verdächtigen Amethyst rendern und melden.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-notify")
        .description("Im Chat melden, wenn ein verdächtiger Chunk gefunden wird.")
        .defaultValue(true)
        .build());

    private final Setting<SettingColor> normalColor = sgRender.add(new ColorSetting.Builder()
        .name("normal-color")
        .description("Farbe für normalen Amethyst.")
        .defaultValue(new SettingColor(180, 100, 255, 120))
        .build());

    private final Setting<SettingColor> susColor = sgRender.add(new ColorSetting.Builder()
        .name("sus-color")
        .description("Farbe für verdächtigen Amethyst.")
        .defaultValue(new SettingColor(255, 60, 60, 160))
        .build());

    // Bereits gescannte Chunks (als long-Key) und gefundene Amethyst-Positionen.
    private final Set<Long> scanned = new HashSet<>();
    private final Map<BlockPos, Boolean> found = new HashMap<>(); // pos -> sus?
    private final Set<Long> reportedChunks = new HashSet<>();

    public SusChunkFinder() {
        super(SpawnerUtilsAddon.CATEGORY, "sus-chunk-finder",
            "Scannt Chunks nach Amethyst-Clustern und markiert verdächtige Vorkommen.");
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
        if (mc.world == null || mc.player == null) return;

        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;
        int r = chunkRange.get();
        int budget = chunksPerTick.get();

        for (int cx = pcx - r; cx <= pcx + r && budget > 0; cx++) {
            for (int cz = pcz - r; cz <= pcz + r && budget > 0; cz++) {
                long key = ChunkPos.toLong(cx, cz);
                if (scanned.contains(key)) continue;

                WorldChunk chunk = mc.world.getChunk(cx, cz);
                if (chunk == null) continue;

                scanChunk(chunk, cx, cz);
                scanned.add(key);
                budget--;
            }
        }
    }

    private void scanChunk(WorldChunk chunk, int cx, int cz) {
        int lo = Math.min(minY.get(), maxY.get());
        int hi = Math.max(minY.get(), maxY.get());

        BlockPos.Mutable pos = new BlockPos.Mutable();
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

                    found.put(pos.toImmutable(), sus);
                    count++;
                    if (sus) susInThisChunk = true;
                }
            }
        }

        if (notify.get() && susInThisChunk) {
            long key = ChunkPos.toLong(cx, cz);
            if (reportedChunks.add(key)) {
                info("Verdächtiger Amethyst in Chunk [%d, %d] (Mitte: x=%d z=%d, %d Blöcke).",
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
        if (found.isEmpty()) return;

        // Aufräumen: Positionen entfernen, deren Block nicht mehr existiert.
        Iterator<Map.Entry<BlockPos, Boolean>> it = found.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Boolean> entry = it.next();
            BlockPos pos = entry.getKey();
            boolean sus = entry.getValue();

            if (mc.world != null && mc.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)
                && !isAmethyst(mc.world.getBlockState(pos).getBlock())) {
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
