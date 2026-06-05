package com.closetfunc.client;

import com.closetfunc.block_entity.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;


@SuppressWarnings("null")
public class ClosetClient {
    private static final ResourceLocation PAGE_TEXTURE = new ResourceLocation("closet_mod", "textures/block/page.png");

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
                System.arraycopy(be.pagesText, 0, this.localPagesText, 0, be.pagesText.length);
                actualPaper = be.insertedPaperCount;
            }

            this.maxPages = Math.min(actualPaper * 2, 128);
            for (int i = 0; i < localPagesText.length; i++) {
                if (localPagesText[i] == null) localPagesText[i] = "";
            }
        }

        // Кнопки для перелистывания страниц
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
            // Сам листок и его рендер
            this.renderBackground(guiGraphics);

            int textureWidth = 256; 
            int textureHeight = 256;
            int x = (this.width - textureWidth) / 2;
            int y = (this.height - textureHeight) / 2;

            guiGraphics.blit(PAGE_TEXTURE, x, y, 0, 0, textureWidth, textureHeight, 256, 256);

            // Курсор в листке машинки
            String textToShow = localPagesText[currentPage];
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                textToShow += "_";
            }
        
            int textColor = 0x3A3A3A; // По умолчанию антрацитовый цвет чернил машинки

            // Основная логика пасхалки на "Death Note"
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

            // Вывод текста с динамическим рассчитанным цветом
            guiGraphics.drawWordWrap(Minecraft.getInstance().font, 
                    Component.literal(textToShow), 
                    x + 40, y + 33, 180, textColor);

            // Счётчик страниц
            String pageInfo = (currentPage + 1) + " / " + maxPages;
            int pageInfoWidth = Minecraft.getInstance().font.width(pageInfo);
            guiGraphics.drawString(Minecraft.getInstance().font, pageInfo, 
                    x + 125 - (pageInfoWidth / 2), y + 240, 0x555555, false);

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
                localPagesText[currentPage] = localPagesText[currentPage].substring(0, localPagesText[currentPage].length() - 1);
                saveTextToBlockEntity();
                return true;
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
                be.updateTextFromServer(this.localPagesText);
                Minecraft.getInstance().level.sendBlockUpdated(blockPos, Minecraft.getInstance().level.getBlockState(blockPos), Minecraft.getInstance().level.getBlockState(blockPos), 3);
                
                // Проверка пасхалки "Death Note" на клиенте
                Player player = Minecraft.getInstance().player;
                if (player != null && !player.getPersistentData().contains("DeathNoteTriggeredClient")) {
                    String playerName = player.getGameProfile().getName().toLowerCase().trim();
                    
                    for (String pageText : this.localPagesText) {
                        if (pageText != null && !pageText.isEmpty()) {
                            String cleanText = pageText.replace("\n", "").replace("\r", "").toLowerCase().trim();
                            
                            if (cleanText.contains(playerName)) {
                                player.getPersistentData().putBoolean("DeathNoteTriggeredClient", true);

                                player.sendSystemMessage(Component.translatable("chat.closet_mod.typewriter.wrong")
                                        .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.OBFUSCATED));

                                Minecraft.getInstance().level.playSound(player, blockPos, 
                                        net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

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