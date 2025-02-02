package org.example.baritonechat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.message.v1.ClientSendChatCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class FabricBaritoneMod implements ClientModInitializer {
    private static final Path CONFIG_PATH = Path.of("config/baritonechat.json");
    private String boundSender = null;
    private static final Gson GSON = new Gson();

    @Override
    public void onInitializeClient() {
        loadConfig();
        ClientSendChatCallback.EVENT.register(this::onChatReceived);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("chatbind")
                    .then(ClientCommandManager.argument("sender", StringArgumentType.string())
                            .executes(context -> {
                                boundSender = StringArgumentType.getString(context, "sender");
                                saveConfig();
                                MinecraftClient.getInstance().player.sendMessage(Text.literal("Bound to sender: " + boundSender), false);
                                return 1;
                            })
                    )
            );
        });
    }

    private void onChatReceived(String message) {
        if (!message.startsWith("<")) return;

        int endIndex = message.indexOf("> ");
        if (endIndex == -1) return;

        String sender = message.substring(1, endIndex);
        String content = message.substring(endIndex + 2);

        if (boundSender != null && sender.equals(boundSender)) {
            int spaceIndex = content.indexOf(" ");
            if (spaceIndex == -1) return;

            String target = content.substring(0, spaceIndex);
            String command = content.substring(spaceIndex + 1);

            if (target.equals(MinecraftClient.getInstance().player.getGameProfile().getName())) {
                executeBaritoneCommand(command);
            }
        }
    }

    private void executeBaritoneCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Executing Baritone command: " + command), false);
            client.player.networkHandler.sendChatCommand("#" + command);
        }
    }

    private void saveConfig() {
        try {
            JsonObject config = new JsonObject();
            config.addProperty("boundSender", boundSender);
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                JsonObject config = GSON.fromJson(content, JsonObject.class);
                if (config.has("boundSender")) {
                    boundSender = config.get("boundSender").getAsString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
