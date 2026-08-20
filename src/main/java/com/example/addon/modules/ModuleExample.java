package com.example.addon.modules;

import com.example.addon.AddonTemplate;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public class ModuleExample extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> cactus = sgGeneral.add(new BoolSetting.Builder()
        .name("cactus")
        .description("Detects suspicious cactus growth.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chorus = sgGeneral.add(new BoolSetting.Builder()
        .name("chorus")
        .description("Detects suspicious chorus growth.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Color of suspicious growth markers.")
        .defaultValue(new Color(0, 255, 0, 255))
        .build()
    );

    private final Set<BlockPos> suspicious = new HashSet<>();

    public ModuleExample() {
        super(
            AddonTemplate.CATEGORY,
            "suspicious-grow",
            "Detects suspicious cactus and chorus growth."
        );
    }

    @Override
    public void onDeactivate() {
        suspicious.clear();
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (mc.level == null) return;

        BlockPos pos = event.pos;
        BlockState oldState = event.oldState;
        BlockState newState = event.newState;

        // CACTUS
        if (cactus.get()
            && newState.is(Blocks.CACTUS)
            && !oldState.is(Blocks.CACTUS)) {

            BlockPos below = pos.below();

            if (mc.level.getBlockState(below).is(Blocks.CACTUS)) {
                suspicious.add(pos.immutable());
            }
        }

        // CHORUS
        if (chorus.get()
            && isChorus(newState)
            && !isChorus(oldState)) {

            if (hasChorusNeighbour(pos)) {
                suspicious.add(pos.immutable());
            }
        }

        // Remove marker if the block disappears
        if (!isChorus(newState) && !newState.is(Blocks.CACTUS)) {
            suspicious.remove(pos);
        }
    }

    private boolean isChorus(BlockState state) {
        return state.is(Blocks.CHORUS_FLOWER)
            || state.is(Blocks.CHORUS_PLANT);
    }

    private boolean hasChorusNeighbour(BlockPos pos) {
        return isChorus(mc.level.getBlockState(pos.above()))
            || isChorus(mc.level.getBlockState(pos.below()))
            || isChorus(mc.level.getBlockState(pos.north()))
            || isChorus(mc.level.getBlockState(pos.south()))
            || isChorus(mc.level.getBlockState(pos.east()))
            || isChorus(mc.level.getBlockState(pos.west()));
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.level == null) return;

        suspicious.removeIf(pos -> {
            BlockState state = mc.level.getBlockState(pos);

            return !state.is(Blocks.CACTUS)
                && !isChorus(state);
        });

        for (BlockPos pos : suspicious) {
            AABB box = new AABB(pos);

            event.renderer.box(
                box,
                color.get(),
                color.get(),
                ShapeMode.Both,
                0
            );
        }
    }
}
