package com.dasien.moreinformation.client.compat.neovitae;

import com.dasien.moreinformation.api.HudContext;
import com.dasien.moreinformation.api.HudInfoProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

/** Uses only client data, never the server-side Anima SavedData API. */
public final class NeoVitaeProvider implements HudInfoProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("neovitae", "anima");
    static final ResourceLocation ICON_ID = ResourceLocation.fromNamespaceAndPath("neovitae", "ara_vitae");
    private NeoVitaeClientData data;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int order() {
        return 160;
    }

    @Override
    public ItemStack icon() {
        Item item = BuiltInRegistries.ITEM.get(ICON_ID);
        return new ItemStack(item == Items.AIR ? Items.REDSTONE : item);
    }

    @Override
    public List<Component> collect(HudContext context) {
        // Resolve the optional API once, only after the provider's mod/config guards have passed.
        if (data == null) data = NeoVitaeClientData.load();
        OptionalInt amount = data.currentEv(context.player().getUUID());
        if (amount.isEmpty()) return List.of();
        // Display the synchronized value without a prefix; the config explains Neo Vitae's 500-EV rounding.
        return List.of(Component.translatable("moreinformation.integration.neovitae").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(formatEv(amount.getAsInt()))
                        .withStyle(ChatFormatting.RED)));
    }

    static String formatEv(int amount) {
        return String.format(Locale.ROOT, "%,d", amount) + " EV";
    }
}
