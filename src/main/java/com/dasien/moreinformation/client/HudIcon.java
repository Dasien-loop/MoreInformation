package com.dasien.moreinformation.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

enum HudIcon {
    PERFORMANCE(new ItemStack(Items.REPEATER)),
    NETWORK(null),
    TIME(new ItemStack(Items.CLOCK)),
    WORLD(new ItemStack(Blocks.GRASS_BLOCK.asItem())),
    BIOME(new ItemStack(Items.OAK_LEAVES)),
    ENTITIES(new ItemStack(Items.REDSTONE)),
    POSITION(new ItemStack(Items.COMPASS)),
    LIGHT(new ItemStack(Items.TORCH));

    private final ItemStack stack;

    HudIcon(ItemStack stack) {
        this.stack = stack;
    }

    ItemStack createStack() {
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
