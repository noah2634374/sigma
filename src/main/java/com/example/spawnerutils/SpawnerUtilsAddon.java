package com.example.spawnerutils;

import com.example.spawnerutils.modules.BlockEntityDebug;
import com.example.spawnerutils.modules.FreecamSpawnerBreaker;
import com.example.spawnerutils.modules.SpawnerESP;
import com.example.spawnerutils.modules.SusChunkFinder;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class SpawnerUtilsAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    // Eigene Kategorie, unter der alle Module im Meteor-GUI erscheinen.
    public static final Category CATEGORY = new Category("SpawnerUtils");

    @Override
    public void onInitialize() {
        LOG.info("Initializing SpawnerUtils");

        Modules modules = Modules.get();
        modules.add(new FreecamSpawnerBreaker());
        modules.add(new SpawnerESP());
        modules.add(new SusChunkFinder());
        modules.add(new BlockEntityDebug());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.spawnerutils";
    }
}
