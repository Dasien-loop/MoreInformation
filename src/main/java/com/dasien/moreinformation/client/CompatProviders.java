package com.dasien.moreinformation.client;

import com.dasien.moreinformation.api.HudContext;
import com.dasien.moreinformation.api.HudInfoProvider;
import com.dasien.moreinformation.api.HudInfoRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Soft integrations. No optional mod class is linked by the HUD mod's class loader. */
@OnlyIn(Dist.CLIENT)
final class CompatProviders {
    private static boolean registered;

    private CompatProviders() {
    }

    static void register() {
        if (registered) return;
        registered = true;
        HudInfoRegistry.register(new EclipticSeasonsProvider());
        HudInfoRegistry.register(new BloodMagicProvider());
        HudInfoRegistry.register(new TerraFirmaCraftProvider());
        HudInfoRegistry.register(new L2HostilityProvider());
    }

    private static final class EclipticSeasonsProvider implements HudInfoProvider {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("eclipticseasons", "season");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public String requiredModId() {
            return "eclipticseasons";
        }

        @Override
        public int order() {
            return 100;
        }

        @Override
        public int refreshIntervalTicks() {
            return 40;
        }

        @Override
        public ItemStack icon() {
            return modIcon("eclipticseasons", "calendar", Items.SUNFLOWER);
        }

        @Override
        public List<Component> collect(HudContext context) {
            Object[] args = arguments(context);
            Object season = Reflection.staticCall(new String[]{
                    "com.teamtea.eclipticseasons.api.SeasonApi",
                    "com.teamtea.eclipticseasons.api.EclipticSeasonsApi",
                    "com.teamtea.eclipticseasons.common.core.SeasonManager"
            }, new String[]{"getSeason", "getCurrentSeason", "season"}, args);
            Object term = Reflection.staticCall(new String[]{
                    "com.teamtea.eclipticseasons.api.SeasonApi",
                    "com.teamtea.eclipticseasons.api.EclipticSeasonsApi",
                    "com.teamtea.eclipticseasons.common.core.SeasonManager"
            }, new String[]{"getSolarTerm", "getCurrentSolarTerm", "getTerm", "solarTerm"}, args);
            if (season == Reflection.MISSING && term == Reflection.MISSING) return List.of();

            MutableComponent line = label("moreinformation.integration.ecliptic_seasons");
            if (season != Reflection.MISSING) {
                line.append(label("moreinformation.integration.season"))
                        .append(value(display(season), ChatFormatting.GREEN));
            }
            if (term != Reflection.MISSING) {
                line.append(label("moreinformation.integration.solar_term"))
                        .append(value(display(term), ChatFormatting.AQUA));
            }
            return List.of(line);
        }
    }

    private static final class BloodMagicProvider implements HudInfoProvider {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("bloodmagic", "lp");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public String requiredModId() {
            return "bloodmagic";
        }

        @Override
        public int order() {
            return 110;
        }

        @Override
        public ItemStack icon() {
            return modIcon("bloodmagic", "weakbloodorb", Items.REDSTONE);
        }

        @Override
        public List<Component> collect(HudContext context) {
            Object network = Reflection.staticCall(new String[]{
                    "wayoftime.bloodmagic.util.helper.NetworkHelper"
            }, new String[]{"getSoulNetwork"}, new Object[]{context.player()});
            if (network == Reflection.MISSING) return List.of();

            Object current = Reflection.instanceCall(network, new String[]{"getCurrentEssence"});
            Object orbTier = Reflection.instanceCall(network, new String[]{"getOrbTier"});
            Object maximum = orbTier == Reflection.MISSING ? Reflection.MISSING : Reflection.staticCall(new String[]{
                    "wayoftime.bloodmagic.util.helper.NetworkHelper"
            }, new String[]{"getMaximumForTier"}, new Object[]{orbTier});
            if (current == Reflection.MISSING && maximum == Reflection.MISSING) return List.of();

            MutableComponent line = label("moreinformation.integration.blood_magic");
            if (current != Reflection.MISSING) {
                line.append(label("moreinformation.integration.current_lp"))
                        .append(value(display(current), ChatFormatting.RED));
            }
            if (maximum != Reflection.MISSING) {
                line.append(label("moreinformation.integration.max_lp"))
                        .append(value(display(maximum), ChatFormatting.DARK_RED));
            }
            return List.of(line);
        }
    }

    private static final class TerraFirmaCraftProvider implements HudInfoProvider {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tfc", "climate");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public String requiredModId() {
            return "tfc";
        }

        @Override
        public int order() {
            return 120;
        }

        @Override
        public ItemStack icon() {
            return modIcon("tfc", "wooden_bucket", Items.WATER_BUCKET);
        }

        @Override
        public List<Component> collect(HudContext context) {
            Object[] climateArgs = new Object[]{context.level(), context.player().blockPosition()};
            Object rainfall = Reflection.staticCall(new String[]{
                    "net.dries007.tfc.util.climate.Climate"
            }, new String[]{"getRainfall"}, climateArgs);
            Object temperature = Reflection.staticCall(new String[]{
                    "net.dries007.tfc.util.climate.Climate"
            }, new String[]{"getTemperature"}, climateArgs);
            if (rainfall == Reflection.MISSING && temperature == Reflection.MISSING) return List.of();

            MutableComponent line = label("moreinformation.integration.tfc");
            if (rainfall != Reflection.MISSING) {
                line.append(label("moreinformation.integration.rainfall"))
                        .append(value(formatNumber(rainfall), ChatFormatting.AQUA))
                        .append(label("moreinformation.integration.rainfall.unit"));
            }
            if (temperature != Reflection.MISSING) {
                line.append(label("moreinformation.integration.tfc_temperature"))
                        .append(value(formatNumber(temperature), ChatFormatting.YELLOW))
                        .append(label("moreinformation.integration.temperature.unit"));
            }
            return List.of(line);
        }
    }

    private static final class L2HostilityProvider implements HudInfoProvider {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("l2hostility", "hostility");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public String requiredModId() {
            return "l2hostility";
        }

        @Override
        public int order() {
            return 130;
        }

        @Override
        public int refreshIntervalTicks() {
            return 40;
        }

        @Override
        public ItemStack icon() {
            return modIcon("l2hostility", "hostility_orb", Items.NETHER_STAR);
        }

        @Override
        public List<Component> collect(HudContext context) {
            Object holder = Reflection.classField(
                    "dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty", "HOLDER");
            Object playerData = Reflection.instanceCall(holder, new String[]{"get"}, context.player());
            Object difficulty = Reflection.instanceCall(playerData, new String[]{"getLevel"});
            Object current = Reflection.instanceCall(difficulty, new String[]{"getLevel"});

            Object section = Reflection.unwrap(Reflection.staticCall(new String[]{
                    "dev.xkmc.l2hostility.content.capability.chunk.SectionDifficulty"
            }, new String[]{"sectionAt"}, new Object[]{context.level(), context.player().blockPosition()}));
            Object editor = Reflection.instanceCall(section, new String[]{"getLevelEditor"},
                    context.level(), context.player().blockPosition());
            Object chunk = Reflection.instanceCall(editor, new String[]{"getTotal"});

            Object common = Reflection.classField("dev.xkmc.l2hostility.init.data.LHConfig", "COMMON");
            Object maxValue = Reflection.instanceField(common, "maxPlayerLevel");
            Object maximum = Reflection.instanceCall(maxValue, new String[]{"get"});
            if (current == Reflection.MISSING && chunk == Reflection.MISSING && maximum == Reflection.MISSING) return List.of();

            MutableComponent line = label("moreinformation.integration.l2_hostility");
            if (current != Reflection.MISSING) {
                line.append(label("moreinformation.integration.current_hostility"))
                        .append(value(display(current), ChatFormatting.RED));
            }
            if (chunk != Reflection.MISSING) {
                line.append(label("moreinformation.integration.chunk_hostility"))
                        .append(value(display(chunk), ChatFormatting.GOLD));
            }
            if (maximum != Reflection.MISSING) {
                line.append(label("moreinformation.integration.max_hostility"))
                        .append(value(display(maximum), ChatFormatting.DARK_RED));
            }
            return List.of(line);
        }
    }

    private static Object[] arguments(HudContext context) {
        BlockPos pos = context.player().blockPosition();
        return new Object[]{context.minecraft(), context.level(), context.player(), pos,
                new ChunkPos(pos), context.level().getGameTime(), context.level().getDayTime()};
    }

    private static MutableComponent label(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.WHITE);
    }

    private static MutableComponent value(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    private static String display(Object object) {
        if (object == null) return "?";
        Object translation = Reflection.instanceCall(object, new String[]{"getTranslation"});
        if (translation instanceof Component component) return component.getString();
        Object name = Reflection.instanceCall(object, new String[]{"getSerializedName", "getName", "name"});
        Object result = name == Reflection.MISSING ? object : name;
        if (result instanceof Component component) return component.getString();
        return String.valueOf(result);
    }

    private static ItemStack modIcon(String namespace, String path, Item fallback) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(namespace, path));
        return new ItemStack(item == null || item == Items.AIR ? fallback : item);
    }

    private static String formatNumber(Object object) {
        if (!(object instanceof Number number)) return display(object);
        double value = number.doubleValue();
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class Reflection {
        private static final Object MISSING = new Object();

        private Reflection() {
        }

        private static Object staticCall(String[] classNames, String[] methodNames, Object[] args) {
            for (String className : classNames) {
                try {
                    Class<?> type = Class.forName(className, false, CompatProviders.class.getClassLoader());
                    Object value = invoke(type, null, methodNames, args, true);
                    if (value != MISSING) return value;
                    Object instance = staticField(type, "INSTANCE", "instance", "INSTANCE_FIELD");
                    if (instance == MISSING) {
                        instance = invoke(type, null, new String[]{"getInstance", "instance"}, new Object[0], true);
                    }
                    if (instance != MISSING) {
                        value = invoke(type, instance, methodNames, args, false);
                        if (value != MISSING) return value;
                    }
                } catch (Throwable ignored) {
                    // Optional classes are intentionally invisible when the mod is absent or has a different API.
                }
            }
            return MISSING;
        }

        private static Object instanceCall(Object target, String[] methodNames, Object... args) {
            if (target == null || target == MISSING) return MISSING;
            return invoke(target.getClass(), target, methodNames, args, false);
        }

        private static Object unwrap(Object value) {
            if (value instanceof java.util.Optional<?> optional) return optional.isPresent() ? optional.get() : MISSING;
            return value;
        }

        private static Object classField(String className, String... names) {
            try {
                return staticField(Class.forName(className, false, CompatProviders.class.getClassLoader()), names);
            } catch (Throwable ignored) {
                return MISSING;
            }
        }

        private static Object instanceField(Object target, String... names) {
            if (target == null || target == MISSING) return MISSING;
            for (String name : names) {
                try {
                    Field field = target.getClass().getField(name);
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value != null) return value;
                } catch (Throwable ignored) {
                }
            }
            return MISSING;
        }

        private static Object invoke(Class<?> type, Object target, String[] methodNames, Object[] args, boolean staticOnly) {
            for (Method method : allMethods(type)) {
                if (!matches(method, methodNames) || (staticOnly && !Modifier.isStatic(method.getModifiers()))) continue;
                Object[] invocationArgs = mapArguments(method.getParameterTypes(), args);
                if (invocationArgs == null) continue;
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(target, invocationArgs);
                    return result == null ? MISSING : result;
                } catch (Throwable ignored) {
                    // Try another overload; a provider must never affect the base HUD.
                }
            }
            return MISSING;
        }

        private static List<Method> allMethods(Class<?> type) {
            List<Method> methods = new ArrayList<>();
            for (Method method : type.getMethods()) methods.add(method);
            for (Method method : type.getDeclaredMethods()) if (!methods.contains(method)) methods.add(method);
            return methods;
        }

        private static boolean matches(Method method, String[] names) {
            for (String name : names) if (method.getName().equals(name)) return true;
            return false;
        }

        private static Object[] mapArguments(Class<?>[] parameterTypes, Object[] args) {
            Object[] result = new Object[parameterTypes.length];
            boolean[] used = new boolean[args.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                int found = -1;
                for (int j = 0; j < args.length; j++) {
                    if (!used[j] && compatible(parameterTypes[i], args[j])) {
                        found = j;
                        break;
                    }
                }
                if (found < 0) return null;
                used[found] = true;
                result[i] = args[found];
            }
            return result;
        }

        private static boolean compatible(Class<?> type, Object value) {
            if (value == null) return !type.isPrimitive();
            if (type.isInstance(value)) return true;
            if (!type.isPrimitive()) return false;
            return (type == long.class && value instanceof Number)
                    || (type == int.class && value instanceof Number)
                    || (type == short.class && value instanceof Number)
                    || (type == byte.class && value instanceof Number)
                    || (type == double.class && value instanceof Number)
                    || (type == float.class && value instanceof Number)
                    || (type == boolean.class && value instanceof Boolean);
        }

        private static Object staticField(Class<?> type, String... names) {
            for (String name : names) {
                try {
                    Field field = type.getDeclaredField(name);
                    if (Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if (value != null) return value;
                    }
                } catch (Throwable ignored) {
                }
            }
            return MISSING;
        }
    }
}
