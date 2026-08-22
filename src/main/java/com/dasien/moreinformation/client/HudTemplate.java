package com.dasien.moreinformation.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Map;

/** Replaces `{name}` tokens and `&` color codes while keeping surrounding player-defined text. */
public final class HudTemplate {
    public record Token(String text, ChatFormatting color) {
        public Token {
            text = text == null ? "" : text;
            color = color == null ? ChatFormatting.WHITE : color;
        }
    }

    private HudTemplate() {
    }

    public static Component apply(String template, Map<String, Token> tokens) {
        MutableComponent result = Component.empty();
        if (template == null || template.isEmpty()) {
            return result;
        }
        Map<String, Token> values = tokens == null ? Map.of() : tokens;
        Style style = Style.EMPTY.withColor(ChatFormatting.WHITE);
        int index = 0;
        while (index < template.length()) {
            int open = template.indexOf('{', index);
            if (open < 0) {
                appendLiteral(result, template.substring(index), style);
                break;
            }
            if (open > index) {
                style = appendLiteral(result, template.substring(index, open), style);
            }
            int close = template.indexOf('}', open + 1);
            if (close < 0) {
                appendLiteral(result, template.substring(open), style);
                break;
            }
            Token token = values.get(template.substring(open + 1, close));
            if (token == null) {
                style = appendLiteral(result, template.substring(open, close + 1), style);
            } else {
                result.append(Component.literal(token.text()).withStyle(token.color()));
            }
            index = close + 1;
        }
        return result;
    }

    static Style appendLiteral(MutableComponent result, String text, Style style) {
        StringBuilder buffer = new StringBuilder();
        Style current = style;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == '&') {
                    buffer.append('&');
                    i++;
                    continue;
                }
                ChatFormatting formatting = ChatFormatting.getByCode(Character.toLowerCase(next));
                if (formatting != null) {
                    flush(result, buffer, current);
                    current = applyFormat(current, formatting);
                    i++;
                    continue;
                }
            }
            buffer.append(c);
        }
        flush(result, buffer, current);
        return current;
    }

    private static Style applyFormat(Style current, ChatFormatting formatting) {
        if (formatting == ChatFormatting.RESET) {
            return Style.EMPTY.withColor(ChatFormatting.WHITE);
        }
        return current.applyFormat(formatting);
    }

    private static void flush(MutableComponent result, StringBuilder buffer, Style style) {
        if (buffer.isEmpty()) {
            return;
        }
        result.append(Component.literal(buffer.toString()).withStyle(style));
        buffer.setLength(0);
    }
}
