package com.webscreen;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WebScreenMod implements ModInitializer {
    public static final String ID = "webscreen";
    /** client -> server: change a screen's url / size / offset */
    public static final Identifier UPDATE = new Identifier(ID, "update");

    public static Block SCREEN_BLOCK;
    public static BlockEntityType<ScreenBlockEntity> SCREEN_BE;

    @Override
    public void onInitialize() {
        SCREEN_BLOCK = Registry.register(Registries.BLOCK, new Identifier(ID, "screen"),
                new ScreenBlock(FabricBlockSettings.create().strength(1.5f).sounds(BlockSoundGroup.METAL).nonOpaque()));
        BlockItem item = Registry.register(Registries.ITEM, new Identifier(ID, "screen"),
                new BlockItem(SCREEN_BLOCK, new FabricItemSettings()));
        SCREEN_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(ID, "screen"),
                FabricBlockEntityTypeBuilder.create(ScreenBlockEntity::new, SCREEN_BLOCK).build());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(item));

        ServerPlayNetworking.registerGlobalReceiver(UPDATE, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            String url = buf.readString(2048);
            int w = buf.readInt();
            int h = buf.readInt();
            float ox = buf.readFloat();
            float oy = buf.readFloat();
            server.execute(() -> {
                if (player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 12 * 12) return; // must be near it
                BlockEntity be = player.getServerWorld().getBlockEntity(pos);
                if (be instanceof ScreenBlockEntity screen) {
                    screen.applySettings(url, w, h, ox, oy);
                }
            });
        });
    }
}
