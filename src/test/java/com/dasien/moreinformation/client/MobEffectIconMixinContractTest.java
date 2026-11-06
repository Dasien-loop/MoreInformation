package com.dasien.moreinformation.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Bytecode contracts complement the policy tests; actual in-game mixin application needs a client smoke test. */
class MobEffectIconMixinContractTest {
    private static final String MIXIN = "com/dasien/moreinformation/mixin/client/MobEffectIconMixin";
    private static final String EFFECT = "net/minecraft/world/effect/MobEffectInstance";

    @Test
    void serializationRedirectStillMatchesTheActualMinecraftVersion() throws Exception {
        MethodNode details = method(readClass(EFFECT), "asDetails");
        long iconCalls = Arrays.stream(details.instructions.toArray())
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(EFFECT) && call.name.equals("showIcon") && call.desc.equals("()Z"))
                .count();
        assertEquals(1, iconCalls, "Both vanilla codecs must continue to route through this protected getter");
    }

    @Test
    void updatePacketAlsoHasItsDirectIconQueryProtected() throws Exception {
        MethodNode constructor = readClass("net/minecraft/network/protocol/game/ClientboundUpdateMobEffectPacket")
                .methods.stream().filter(method -> method.name.equals("<init>")
                        && method.desc.equals("(ILnet/minecraft/world/effect/MobEffectInstance;Z)V"))
                .findFirst().orElseThrow();
        assertEquals(1, Arrays.stream(constructor.instructions.toArray())
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(EFFECT) && call.name.equals("showIcon") && call.desc.equals("()Z"))
                .count());
        MethodNode redirect = method(readClass("com/dasien/moreinformation/mixin/client/MobEffectPacketMixin"),
                "moreinformation$serializeOriginalIcon");
        assertTrue(Arrays.stream(redirect.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.name.equals("moreinformation$getOriginalShowIcon")));
    }

    @Test
    void serializerReadsTheRawFlagAndTheMixinNeverMutatesIt() throws Exception {
        ClassNode mixin = readClass(MIXIN);
        MethodNode serializer = method(mixin, "moreinformation$serializeOriginalIcon");
        assertTrue(Arrays.stream(serializer.instructions.toArray())
                .anyMatch(instruction -> instruction instanceof FieldInsnNode field
                        && field.getOpcode() == Opcodes.GETFIELD && field.name.equals("showIcon")));
        assertFalse(Arrays.stream(serializer.instructions.toArray())
                .anyMatch(MethodInsnNode.class::isInstance), "Serialization must not consult the client visibility view");
        for (MethodNode method : mixin.methods) {
            assertFalse(Arrays.stream(method.instructions.toArray())
                    .anyMatch(instruction -> instruction instanceof FieldInsnNode field
                            && field.getOpcode() == Opcodes.PUTFIELD && field.name.equals("showIcon")),
                    "Never mutate the synchronized/saved icon flag");
        }
    }

    @Test
    void genericMixinIsClientOnlyAndReplacesTheXaeroSpecificHook() throws Exception {
        try (var stream = getClass().getResourceAsStream("/moreinformation.mixins.json")) {
            assertNotNull(stream);
            var config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            String clientMixins = config.getAsJsonArray("client").toString();
            assertTrue(clientMixins.contains("client.MobEffectIconMixin"));
            assertTrue(clientMixins.contains("client.MobEffectIconAccessor"));
            assertTrue(clientMixins.contains("client.MobEffectPacketMixin"));
            assertFalse(clientMixins.contains("XaeroPotionEffectsMixin"));
            assertFalse(config.has("mixins"), "The visibility view must not run on a dedicated server");
        }
    }

    private static ClassNode readClass(String name) throws Exception {
        try (var stream = MobEffectIconMixinContractTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream, name);
            ClassNode result = new ClassNode();
            new ClassReader(stream).accept(result, 0);
            return result;
        }
    }

    private static MethodNode method(ClassNode type, String name) {
        return type.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }
}
