package com.closetfunc.client;

import com.closetfunc.block_entity.ModBlockEntities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;


@SuppressWarnings("null")
public class ClosetClient {
    private static final ResourceLocation PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath("closet_mod", "textures/block/page.png");


    public static void init() {
        MinecraftForge.EVENT_BUS.register(ClosetClient.class);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onMovementInput(net.minecraftforge.client.event.MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player != null && player.getPersistentData().getBoolean("IsInCloset")) {
            net.minecraft.client.player.Input input = event.getInput();
            if (input.shiftKeyDown) {
                player.getPersistentData().putBoolean("IsInCloset", false);
                return;
            }
            input.leftImpulse = 0; input.forwardImpulse = 0;
            input.up = false; input.down = false; input.left = false; input.right = false; input.jumping = false;
        }
    }

    public static void openCustomTypewriterScreen(BlockPos pos, int paperCount) {
        Minecraft.getInstance().setScreen(new TypewriterSingleScreen(pos, paperCount));
    }

    // --- ОДИНОЧНЫЙ ЛИСТ ФОРМАТА А4 ---
    public static class TypewriterSingleScreen extends Screen {
        private final BlockPos blockPos;
        private final int maxPages;
        private int currentPage = 0;
        private String[] localPagesText = new String[128];

        public TypewriterSingleScreen(BlockPos pos, int paperCount) {
            super(Component.literal("Typewriter"));
            this.blockPos = pos;
            
            int actualPaper = paperCount;
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                actualPaper = be.insertedPaperCount;
            }
            this.maxPages = Math.max(2, Math.min(actualPaper * 2, 128));

            for (int i = 0; i < 128; i++) {
                this.localPagesText[i] = "";
            }

            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                if (be.pagesText[0] == null || be.pagesText[0].isEmpty()) {
                    this.localPagesText[0] = net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step1");
                }

                for (int i = 0; i < be.pagesText.length; i++) {
                    String rawText = be.pagesText[i];
                    if (rawText != null && !rawText.isEmpty()) {
                        String processed = rawText;
                        if (processed.contains("text.closet_mod.typewriter.step1")) {
                            processed = processed.replace("text.closet_mod.typewriter.step1", net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step1"));
                        }
                        if (processed.contains("text.closet_mod.typewriter.step2")) {
                            processed = processed.replace("text.closet_mod.typewriter.step2", net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step2"));
                        }
                        this.localPagesText[i] = processed;
                    }
                }
            }
        }

        @Override
        protected void init() {
            this.clearWidgets();
            int x = (this.width - 340) / 2;
            int y = (this.height - 192) / 2;
            
            this.addRenderableWidget(Button.builder(Component.literal(">"), (btn) -> {
                if (currentPage + 1 < maxPages) currentPage++;
            }).bounds(x + 240, y + 200, 20, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("<"), (btn) -> {
                if (currentPage > 0) currentPage--;
            }).bounds(x + 80, y + 200, 20, 20).build());
        }
        
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);

            int textureWidth = 256; 
            int textureHeight = 256;
            int x = (this.width - textureWidth) / 2;
            int y = (this.height - textureHeight) / 2;

            guiGraphics.blit(PAGE_TEXTURE, x, y, 0, 0, textureWidth, textureHeight, 256, 256);

            String textToShow = localPagesText[currentPage];
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                textToShow += "_";
            }

            int textColor = 0x3A3A3A;

            // Пасхалка на "Death Note"
            if (Minecraft.getInstance().getConnection() != null) {
                for (net.minecraft.client.multiplayer.PlayerInfo playerInfo : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
                    String onlinePlayerName = playerInfo.getProfile().getName().toLowerCase().trim();
                    String cleanLocalText = textToShow.replace("\n", "").replace("\r", "").toLowerCase().trim();
                    
                    if (!onlinePlayerName.isEmpty() && cleanLocalText.contains(onlinePlayerName)) {
                        textColor = 0x990000;   
                        break;
                    }
                }
            }

            String q1 = net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step1");
            String q2 = net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step2");

            int currentYOffset = 0;
            java.util.List<net.minecraft.util.FormattedCharSequence> splitLines = 
                Minecraft.getInstance().font.split(net.minecraft.network.chat.Component.literal(textToShow), 140);

            for (net.minecraft.util.FormattedCharSequence line : splitLines) {
                int currentLineColor = textColor == 0x990000 ? 0x990000 : 0x3A3A3A;
                
                StringBuilder builder = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    builder.appendCodePoint(codePoint);
                    return true;
                });
                String plainLine = builder.toString();

                if (plainLine.contains(q1) || q1.contains(plainLine) && !plainLine.isEmpty() || 
                    plainLine.contains(q2) || q2.contains(plainLine) && !plainLine.isEmpty()) {
                    currentLineColor = 0x990000;
                } 
                else {
                    String lowerLine = plainLine.toLowerCase();
                    if (lowerLine.contains("хорошо") || lowerLine.contains("не очень") || 
                        lowerLine.contains("good") || lowerLine.contains("not") || 
                        lowerLine.contains("да") || lowerLine.contains("нет") || 
                        lowerLine.contains("yes") || lowerLine.contains("no")) {
                        currentLineColor = 0x007700;
                    }
                }

                guiGraphics.drawString(Minecraft.getInstance().font, line, x + 40, y + 33 + currentYOffset, currentLineColor, false);
                currentYOffset += 9; 
            }

            int totalPages = localPagesText.length > 0 ? localPagesText.length : 1;
            String pageString = (this.currentPage + 1) + " / " + totalPages;
            
            int stringWidth = Minecraft.getInstance().font.width(pageString);
            
            int pageX = x + (textureWidth / 2) - (stringWidth / 2);
            int pageY = y + 240; 

            guiGraphics.drawString(Minecraft.getInstance().font, pageString, pageX, pageY, 0x5A5A5A, false);

            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }


        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (currentPage < maxPages) {
                String currentText = localPagesText[currentPage];
                String testText = currentText + codePoint;
                
                int textHeight = Minecraft.getInstance().font.wordWrapHeight(testText, 140);
                if (textHeight <= 184 && currentText.length() < 500) {
                    localPagesText[currentPage] = testText;
                    saveTextToBlockEntity();
                    return true;
                }
            }
            return super.charTyped(codePoint, modifiers);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 259 && currentPage < maxPages && !localPagesText[currentPage].isEmpty()) {
                String currentText = localPagesText[currentPage];
                
                String q1 = net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step1");
                String q2 = net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step2");

                int protectedLength = 0;
                if (currentText.startsWith(q1)) {
                    protectedLength += q1.length();
                }
                if (currentText.contains(q2)) {
                    int index = currentText.indexOf(q2);
                    protectedLength = index + q2.length();
                }
                
                if (currentText.length() > protectedLength) {
                    localPagesText[currentPage] = currentText.substring(0, currentText.length() - 1);
                    saveTextToBlockEntity();
                    return true;
                }
                return false;
            }

            if ((keyCode == 257 || keyCode == 335) && currentPage < maxPages) {
                String currentText = localPagesText[currentPage];
                String testText = currentText + "\n";
                int textHeight = Minecraft.getInstance().font.wordWrapHeight(testText, 140);
                if (textHeight <= 184) {
                    localPagesText[currentPage] = testText;
                    saveTextToBlockEntity();
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }


        private void saveTextToBlockEntity() {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(blockPos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                
                if (be.dialogueStep == 1) {
                    String currentText = this.localPagesText[currentPage].toLowerCase();
                    String q1 = net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step1").toLowerCase();
                    
                    if (currentText.length() > q1.length()) {
                        String playerResponse = currentText.substring(q1.length()).trim();
                        
                        if (playerResponse.contains("хорошо") || playerResponse.contains("не очень") || 
                            playerResponse.contains("good") || playerResponse.contains("not") || 
                            playerResponse.contains("нет") || playerResponse.contains("да") || 
                            playerResponse.contains("yes") || playerResponse.contains("no")) {
                            
                            be.dialogueStep = 2;
                            
                            String q2 = net.minecraft.client.resources.language.I18n.get("text.closet_mod.typewriter.step2");
                            this.localPagesText[currentPage] += "\n\n" + q2;
                            
                            Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, blockPos, 
                                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.2F);
                        }
                    }
                }

                // Отправляем финальный текст на сервер
                com.closetfunc.network.ModMessages.sendToServer(new com.closetfunc.network.ModMessages.ServerboundTypewriterTextPacket(blockPos, this.localPagesText, be.dialogueStep));

                // Локальная проверка пасхалки Death Note
                Player player = Minecraft.getInstance().player;
                if (player != null && !player.getPersistentData().contains("DeathNoteTriggeredClient")) {
                    String playerName = player.getGameProfile().getName().toLowerCase().trim();
                    
                    for (String pageText : this.localPagesText) {
                        if (pageText != null && !pageText.isEmpty()) {
                            String cleanText = pageText.replace("\n", "").replace("\r", "").toLowerCase().trim();
                            
                            if (cleanText.contains(playerName)) {
                                player.getPersistentData().putBoolean("DeathNoteTriggeredClient", true);
                                if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer) {
                                    localPlayer.connection.sendCommand("closetmod_trigger_heartattack");
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean isPauseScreen() { 
            return false; 
        }
    }
}