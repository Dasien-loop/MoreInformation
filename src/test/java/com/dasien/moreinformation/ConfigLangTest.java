package com.dasien.moreinformation;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigLangTest {
    @Test
    void translatesAllMoonPhasesAndIncludesHostileCount() throws Exception {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            try (var stream = getClass().getResourceAsStream("/assets/moreinformation/lang/" + language + ".json")) {
                org.junit.jupiter.api.Assertions.assertNotNull(stream);
                JsonObject values = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
                for (String phase : new String[]{"full_moon", "waning_gibbous", "last_quarter", "waning_crescent",
                        "new_moon", "waxing_crescent", "first_quarter", "waxing_gibbous"}) {
                    String key = "moreinformation.hud.moon_phase." + phase;
                    org.junit.jupiter.api.Assertions.assertTrue(values.has(key), key);
                    org.junit.jupiter.api.Assertions.assertFalse(values.get(key).getAsString().isBlank());
                }
                for (String key : new String[]{"moreinformation.integration.neovitae",
                        "moreinformation.config.integrations.showNeoVitae"}) {
                    org.junit.jupiter.api.Assertions.assertTrue(values.has(key), key);
                    org.junit.jupiter.api.Assertions.assertFalse(values.get(key).getAsString().isBlank());
                }
                org.junit.jupiter.api.Assertions.assertTrue(values.get(
                        "moreinformation.config.integrations.showNeoVitae").getAsString().contains("500"));
                String template = values.get("moreinformation.hud.template.entities").getAsString();
                org.junit.jupiter.api.Assertions.assertTrue(template.indexOf("{entities}") < template.indexOf("{hostile}"));
                org.junit.jupiter.api.Assertions.assertTrue(template.indexOf("{hostile}") < template.indexOf("{rendered}"));
            }
        }
    }

    @Test
    void readsLanguageFromOptionsTxt() {
        assertEquals("zh_cn", ConfigLang.languageFromOptions("fov:1.0\nlang:zh_cn\ngamma:1.0\n"));
        assertEquals("en_us", ConfigLang.languageFromOptions("lang:\"en_us\""));
        assertNull(ConfigLang.languageFromOptions("fov:1.0\n"));
    }

    @Test
    void convertsSystemLocaleToMinecraftLanguage() {
        assertEquals("zh_cn", ConfigLang.fromLocale(Locale.SIMPLIFIED_CHINESE));
        assertEquals("en_us", ConfigLang.fromLocale(Locale.US));
    }

    @Test
    void overlaysSelectedLanguageOnEnglish() {
        Map<String, String> values = ConfigLang.load("zh_cn", language -> switch (language) {
            case "en_us" -> Map.of("moreinformation.config.hud.enabled", "Show the information HUD");
            case "zh_cn" -> Map.of("moreinformation.config.hud.enabled", "显示信息 HUD");
            default -> Map.of();
        });
        assertEquals("显示信息 HUD", values.get("moreinformation.config.hud.enabled"));
    }
}
