package com.dasien.moreinformation.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudTemplateTest {
    @Test
    void keepsCustomTextAroundTokens() {
        assertEquals("TPS：20.0 很稳 RAM：512/2048MB", HudTemplate.apply(
                "TPS：{tps} 很稳 RAM：{ram}",
                Map.of(
                        "tps", new HudTemplate.Token("20.0", ChatFormatting.GREEN),
                        "ram", new HudTemplate.Token("512/2048MB", ChatFormatting.YELLOW)
                )).getString());
    }

    @Test
    void leavesUnknownTokensUnchanged() {
        assertEquals("{nope}", HudTemplate.apply("{nope}", Map.of()).getString());
    }

    @Test
    void treatsUnclosedBraceAsLiteralText() {
        assertEquals("RAM：{ram", HudTemplate.apply("RAM：{ram", Map.of(
                "ram", new HudTemplate.Token("1/2MB", ChatFormatting.WHITE)
        )).getString());
    }

    @Test
    void appliesColorCodesWithoutChangingVisibleText() {
        Component result = HudTemplate.apply("&aTPS：{tps} &e很稳", Map.of(
                "tps", new HudTemplate.Token("20.0", ChatFormatting.GREEN)
        ));
        assertEquals("TPS：20.0 很稳", result.getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), result.getSiblings().get(0).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), result.getSiblings().get(result.getSiblings().size() - 1).getStyle().getColor());
    }

    @Test
    void treatsDoubleAmpersandAsLiteral() {
        assertEquals("&注意", HudTemplate.apply("&&注意", Map.of()).getString());
    }

    @Test
    void rendersEmptyTokenWithoutTemplateText() {
        assertEquals("位置：方块 1 2 3 朝向：北", HudTemplate.apply(
                "位置：方块 {block} {portal}朝向：{facing}",
                Map.of(
                        "block", new HudTemplate.Token("1 2 3", ChatFormatting.YELLOW),
                        "portal", new HudTemplate.Token("", ChatFormatting.LIGHT_PURPLE),
                        "facing", new HudTemplate.Token("北", ChatFormatting.GREEN)
                )).getString());
    }
}
