package com.dasien.moreinformation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Resolves config comments from language files before the HUD translator is ready. */
public final class ConfigLang {
    private static final Gson GSON = new Gson();
    private static final Map<String, String> VALUES = load();

    private ConfigLang() {
    }

    public static String get(String key) {
        String value = VALUES.get(key);
        return value == null || value.isBlank() ? key : value;
    }

    static Map<String, String> load() {
        return load(detectLanguage(), ConfigLang::readLangResource);
    }

    static Map<String, String> load(String language, Function<String, Map<String, String>> reader) {
        Map<String, String> values = new LinkedHashMap<>(reader.apply("en_us"));
        String normalized = normalizeLanguage(language);
        if (!normalized.isBlank() && !"en_us".equals(normalized)) {
            values.putAll(reader.apply(normalized));
            String languageOnly = languageOnly(normalized);
            if (!languageOnly.equals(normalized)) {
                for (Map.Entry<String, String> entry : reader.apply(languageOnly).entrySet()) {
                    values.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        return Map.copyOf(values);
    }

    static String detectLanguage() {
        String fromOptions = languageFromOptions(readOptionsText());
        if (fromOptions != null) {
            return normalizeLanguage(fromOptions);
        }
        return fromLocale(Locale.getDefault());
    }

    static String languageFromOptions(String optionsText) {
        if (optionsText == null || optionsText.isBlank()) {
            return null;
        }
        for (String line : optionsText.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("lang:")) {
                continue;
            }
            String value = trimmed.substring(5).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value.isBlank() ? null : value;
        }
        return null;
    }

    static String fromLocale(Locale locale) {
        if (locale == null || locale.getLanguage() == null || locale.getLanguage().isBlank()) {
            return "en_us";
        }
        String language = locale.getLanguage().toLowerCase(Locale.ROOT);
        String country = locale.getCountry();
        if (country == null || country.isBlank()) {
            return language;
        }
        return language + "_" + country.toLowerCase(Locale.ROOT);
    }

    static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "";
        }
        return language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String languageOnly(String language) {
        int split = language.indexOf('_');
        return split < 0 ? language : language.substring(0, split);
    }

    private static Map<String, String> readLangResource(String language) {
        String path = "/assets/moreinformation/lang/" + language + ".json";
        try (InputStream in = ConfigLang.class.getResourceAsStream(path)) {
            if (in == null) {
                return Map.of();
            }
            Map<String, String> parsed = GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() {}.getType());
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static String readOptionsText() {
        try {
            Path options = gameDir().resolve("options.txt");
            if (!Files.isRegularFile(options)) {
                return null;
            }
            return Files.readString(options, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path gameDir() {
        try {
            return FMLPaths.GAMEDIR.get();
        } catch (Throwable ignored) {
            return Path.of(System.getProperty("user.dir", "."));
        }
    }
}
