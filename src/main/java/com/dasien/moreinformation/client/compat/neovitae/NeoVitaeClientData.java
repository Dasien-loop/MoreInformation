package com.dasien.moreinformation.client.compat.neovitae;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.OptionalInt;
import java.util.UUID;

/** Read-only adapter for Neo Vitae 1.21.1-1.1.51's client-synchronized Anima cache. */
final class NeoVitaeClientData {
    private final Method has;
    private final Method currentEv;

    private NeoVitaeClientData(Method has, Method currentEv) {
        this.has = has;
        this.currentEv = currentEv;
    }

    static NeoVitaeClientData load() {
        try {
            return forCache(Class.forName("com.breakinblocks.neovitae.client.ClientAnimaCache", false,
                    NeoVitaeClientData.class.getClassLoader()));
        } catch (ClassNotFoundException | LinkageError exception) {
            return unavailable();
        }
    }

    static NeoVitaeClientData forCache(Class<?> cache) {
        try {
            Method has = cache.getMethod("has", UUID.class);
            Method currentEv = cache.getMethod("getCurrentEV");
            if (!Modifier.isStatic(has.getModifiers()) || has.getReturnType() != boolean.class
                    || !Modifier.isStatic(currentEv.getModifiers()) || currentEv.getReturnType() != int.class) {
                return unavailable();
            }
            return new NeoVitaeClientData(has, currentEv);
        } catch (NoSuchMethodException | SecurityException exception) {
            return unavailable();
        }
    }

    static NeoVitaeClientData unavailable() {
        return new NeoVitaeClientData(null, null);
    }

    OptionalInt currentEv(UUID player) {
        if (has == null || player == null) return OptionalInt.empty();
        try {
            if (!Boolean.TRUE.equals(has.invoke(null, player))) return OptionalInt.empty();
            int amount = (Integer) currentEv.invoke(null);
            return amount >= 0 ? OptionalInt.of(amount) : OptionalInt.empty();
        } catch (ReflectiveOperationException | LinkageError exception) {
            return OptionalInt.empty();
        }
    }
}
