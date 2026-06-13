package com.closetfunc.client;

import com.closetfunc.block_entity.ModBlockEntities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;


@SuppressWarnings("null")
public class ClosetClient {
    private static final ResourceLocation PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath("closet_mod", "textures/block/page.png");
    public static boolean isTypewriterHardcoreActive = false;


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

@net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Перехватываем отрисовку здоровья
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            if (isTypewriterHardcoreActive) {
                // Подменяем флаг хардкора в мире на true перед рендером
                LevelInfoAccessor(mc.level, true);
            }
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            if (isTypewriterHardcoreActive) {
                LevelInfoAccessor(mc.level, false);
            }
        }
    }

    private static void LevelInfoAccessor(ClientLevel level, boolean isHardcore) {
        try {
            net.minecraft.client.multiplayer.ClientLevel.ClientLevelData data = level.getLevelData();
            java.lang.reflect.Field hardcoreField = null;
            Class<?> clazz = data.getClass();
            
            while (clazz != null) {
                try {
                    hardcoreField = clazz.getDeclaredField("hardcore");
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            
            if (hardcoreField != null) {
                hardcoreField.setAccessible(true);
                hardcoreField.setBoolean(data, isHardcore);
            }
        } catch (Exception e) {
        }
    }

    public static void openCustomTypewriterScreen(BlockPos pos, int paperCount, int surveyDay, String serverFirstPageText) {
        if ("HARDCORE_SYNC".equals(serverFirstPageText)) {
            isTypewriterHardcoreActive = true;
            return;
        }

        if (surveyDay == 2) {
            isTypewriterHardcoreActive = true;
        } else {
            isTypewriterHardcoreActive = false;
        }

        Minecraft.getInstance().setScreen(new TypewriterSingleScreen(pos, paperCount, surveyDay, serverFirstPageText));
    }


    // --- ОДИНОЧНЫЙ ЛИСТ ФОРМАТА А4 ---
    public static class TypewriterSingleScreen extends Screen {
        private final BlockPos blockPos;
        private final int maxPages;
        private final int currentSurveyDay;
        private int currentPage = 0;
        private String[] localPagesText = new String[128];

        public TypewriterSingleScreen(BlockPos pos, int paperCount, int surveyDay, String serverFirstPageText) {
            super(Component.literal("Typewriter"));
            this.blockPos = pos;
            this.currentSurveyDay = surveyDay;
            
            int actualPaper = paperCount;
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                actualPaper = be.insertedPaperCount;
            }
            this.maxPages = Math.max(2, Math.min(actualPaper * 2, 128));

            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                for (int i = 0; i < be.pagesText.length; i++) {
                    this.localPagesText[i] = be.pagesText[i] != null ? be.pagesText[i] : "";
                }
            }
            this.localPagesText[0] = serverFirstPageText != null ? serverFirstPageText : " ";
        }

        @Override
        protected void init() {
            this.clearWidgets();
            int x = (this.width - 340) / 2;
            int y = (this.height - 192) / 2;
            
            Button nextBtn = Button.builder(Component.literal(">"), (btn) -> {
                if (currentPage + 1 < maxPages) {
                    currentPage++;
                    this.setFocused(null);
                }
            }).bounds(x + 240, y + 200, 20, 20).build();
            nextBtn.setFocused(false);

            Button prevBtn = Button.builder(Component.literal("<"), (btn) -> {
                if (currentPage > 0) {
                    currentPage--;
                    this.setFocused(null);
                }
            }).bounds(x + 80, y + 200, 20, 20).build();
            prevBtn.setFocused(false);

            this.addRenderableWidget(nextBtn);
            this.addRenderableWidget(prevBtn);

            this.setFocused(null);
        }

        @Override
        public void onClose() {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(blockPos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                if (be.dialogueStep < 3) {
                    com.closetfunc.network.ModMessages.sendToServer(new com.closetfunc.network.ModMessages.ServerboundTypewriterTextPacket(
                        blockPos, this.localPagesText, be.dialogueStep, be.rewardType, be.currentEventId, be.firstAnswerWasBad
                    ));
                    super.onClose();
                    return;
                }
            }
            saveTextToBlockEntity();
            super.onClose();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);

            int textureWidth = 256; 
            int textureHeight = 256;
            int x = (this.width - textureWidth) / 2;
            int y = (this.height - textureHeight) / 2;

            guiGraphics.blit(PAGE_TEXTURE, x, y, 0, 0, textureWidth, textureHeight, 256, 256);

            int currentStep = 1;
            int rewardType = 0;
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(blockPos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                currentStep = be.dialogueStep == 0 ? 1 : be.dialogueStep;
                rewardType = be.rewardType;
            }

            int currentYOffset = 0;

            // Базовые цвета текста
            int currentEntityColor = 0x990000; // Красный для сущности
            int currentPlayerColor = 0x007700; // Зеленый для игрока

            String fullPlayerText = this.localPagesText[currentPage];

            // --- ЛОГИКА ДЛЯ СТРАНИЦЫ 0 (СЮЖЕТНЫЙ ОПРОС СУЩНОСТИ) ---
            if (currentPage == 0) {
                int clientWorldDay = 1;
                String firstAnswer = "";
                String secondAnswer = "";

                String[] splitAnswers = fullPlayerText.split("\n\n");
                if (splitAnswers.length > 0) firstAnswer = splitAnswers[0].replace(" [FINISHED]", "").trim();
                if (splitAnswers.length > 1) secondAnswer = splitAnswers[1].replace(" [FINISHED]", "").trim();

                if (Minecraft.getInstance().level != null) {
                    clientWorldDay = (int) (Minecraft.getInstance().level.getDayTime() / 24000L) + 1;
                }

                if (currentStep == 1 && (System.currentTimeMillis() / 500) % 2 == 0) {
                    firstAnswer += "_";
                } else if (currentStep == 2 && (System.currentTimeMillis() / 500) % 2 == 0) {
                    secondAnswer += "_";
                }

                // ПАСХАЛКА DEATH NOTE (Проверяем никнеймы на сюжетной странице)
                if (Minecraft.getInstance().getConnection() != null) {
                    String cleanFirst = firstAnswer.replace("_", "").toLowerCase().trim();
                    String cleanSecond = secondAnswer.replace("_", "").toLowerCase().trim();
                    
                    for (net.minecraft.client.multiplayer.PlayerInfo playerInfo : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
                        String onlinePlayerName = playerInfo.getProfile().getName().toLowerCase().trim();
                        if (!onlinePlayerName.isEmpty() && (cleanFirst.contains(onlinePlayerName) || cleanSecond.contains(onlinePlayerName))) {
                            currentEntityColor = 0x990000;   
                            currentPlayerColor = 0x990000;
                            break;
                        }
                    }
                }

                // ОТРИСОВКА БЛОКА №1: Вопрос 1.1 + Ответ 1.1
                String q1Key = "text.closet_mod.typewriter.step" + this.currentSurveyDay + ".1";
                String q1 = net.minecraft.client.resources.language.I18n.get(q1Key);
                java.util.List<net.minecraft.util.FormattedCharSequence> q1Lines = Minecraft.getInstance().font.split(net.minecraft.network.chat.Component.literal(q1), 140);
                for (net.minecraft.util.FormattedCharSequence line : q1Lines) {
                    guiGraphics.drawString(Minecraft.getInstance().font, line, x + 40, y + 33 + currentYOffset, currentEntityColor, false);
                    currentYOffset += 9;
                }
                
                currentYOffset += 2;
                
                java.util.List<net.minecraft.util.FormattedCharSequence> a1Lines = Minecraft.getInstance().font.split(net.minecraft.network.chat.Component.literal(firstAnswer), 140);
                for (net.minecraft.util.FormattedCharSequence line : a1Lines) {
                    guiGraphics.drawString(Minecraft.getInstance().font, line, x + 40, y + 33 + currentYOffset, currentPlayerColor, false);
                    currentYOffset += 9;
                }

                // ОТРИСОВКА БЛОКА №2: Вопрос 1.2/2.2 + Ответ
                if (currentStep >= 2) {
                    currentYOffset += 8;
                    
                    String q2Key = "text.closet_mod.typewriter.step" + this.currentSurveyDay + ".2";
                    String q2 = net.minecraft.client.resources.language.I18n.get(q2Key);
                    java.util.List<net.minecraft.util.FormattedCharSequence> q2Lines = Minecraft.getInstance().font.split(net.minecraft.network.chat.Component.literal(q2), 140);
                    for (net.minecraft.util.FormattedCharSequence line : q2Lines) {
                        guiGraphics.drawString(Minecraft.getInstance().font, line, x + 40, y + 33 + currentYOffset, currentEntityColor, false);
                        currentYOffset += 9;
                    }
                    
                    currentYOffset += 2;
                    
                    java.util.List<net.minecraft.util.FormattedCharSequence> a2Lines = Minecraft.getInstance().font.split(net.minecraft.network.chat.Component.literal(secondAnswer), 140);
                    for (net.minecraft.util.FormattedCharSequence line : a2Lines) {
                        guiGraphics.drawString(Minecraft.getInstance().font, line, x + 40, y + 33 + currentYOffset, currentPlayerColor, false);
                        currentYOffset += 9;
                    }
                }

                // ОТРИСОВКА БЛОКА №3: Финальный вердикт сущности (event1 или event2)
                if (currentStep == 3) {
                    currentYOffset += 8;
                    
                    boolean isBadVerdict = (rewardType % 2 == 0);
                    String verdictSuffix = isBadVerdict ? ".bad" : ".good";
                    
                    String finalKey = "text.closet_mod.typewriter.event" + this.currentSurveyDay + verdictSuffix;
                    
                    String q3 = net.minecraft.client.resources.language.I18n.get(finalKey);
                    java.util.List<net.minecraft.util.FormattedCharSequence> q3Lines = Minecraft.getInstance().font.split(net.minecraft.network.chat.Component.literal(q3), 140);
                    for (net.minecraft.util.FormattedCharSequence line : q3Lines) {
                        guiGraphics.drawString(Minecraft.getInstance().font, line, x + 40, y + 33 + currentYOffset, currentEntityColor, false);
                        currentYOffset += 9;
                    }
                }
            }
            else {
                String freeTextWithCursor = fullPlayerText;
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    freeTextWithCursor += "_";
                }

                int freePageColor = 0x222222;

                // ПАСХАЛКА на "DEATH NOTE" (Проверяем никнеймы на свободных страницах)
                if (Minecraft.getInstance().getConnection() != null) {
                    String cleanFree = fullPlayerText.toLowerCase().trim();
                    for (net.minecraft.client.multiplayer.PlayerInfo playerInfo : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
                        String onlinePlayerName = playerInfo.getProfile().getName().toLowerCase().trim();
                        if (!onlinePlayerName.isEmpty() && cleanFree.contains(onlinePlayerName)) {
                            currentPlayerColor = 0x990000;
                            break;
                        }
                    }
                }
                java.util.List<net.minecraft.util.FormattedCharSequence> freeLines = Minecraft.getInstance().font.split(net.minecraft.network.chat.Component.literal(freeTextWithCursor), 140);
                for (net.minecraft.util.FormattedCharSequence line : freeLines) {
                    guiGraphics.drawString(Minecraft.getInstance().font, line, x + 40, y + 33 + currentYOffset, freePageColor, false);
                    currentYOffset += 9;
                }
            }

            // Рендер номера страницы внизу листа
            String pageString = (this.currentPage + 1) + " / " + maxPages;
            int stringWidth = Minecraft.getInstance().font.width(pageString);
            guiGraphics.drawString(Minecraft.getInstance().font, pageString, x + (textureWidth / 2) - (stringWidth / 2), y + 240, 0x5A5A5A, false);

            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }


        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (currentPage < maxPages) {
                int currentStep = 1;
                if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(blockPos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                    currentStep = be.dialogueStep == 0 ? 1 : be.dialogueStep;
                }

                if (currentPage == 0 && currentStep >= 3) {
                    return false;
                }

                String currentText = localPagesText[currentPage];
                String testText = currentText + codePoint;
                
                int textHeight = Minecraft.getInstance().font.wordWrapHeight(testText, 140);
                if (textHeight <= 184 && currentText.length() < 500) {
                    localPagesText[currentPage] = testText;
                    
                    if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(blockPos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                        com.closetfunc.network.ModMessages.sendToServer(new com.closetfunc.network.ModMessages.ServerboundTypewriterTextPacket(blockPos, this.localPagesText, be.dialogueStep, be.rewardType, be.currentEventId, be.firstAnswerWasBad));
                    }
                    return true;
                }
            }
            return super.charTyped(codePoint, modifiers);
        }


        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            // Перехват нажатия пробела, чтобы он использовался игрой как символом, а не для переключения между страницами!
            int currentStep = 1;
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(blockPos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                currentStep = be.dialogueStep == 0 ? 1 : be.dialogueStep;
            }

            // Код 32 = Space (Пробел)
            if (keyCode == 32) {
                if (currentPage == 0 && currentStep >= 3) {
                    return false;
                }
                String currentText = localPagesText[currentPage];
                String testText = currentText + " ";
                int textHeight = Minecraft.getInstance().font.wordWrapHeight(testText, 140);
                if (textHeight <= 184 && currentText.length() < 500) {
                    localPagesText[currentPage] = testText;
                    saveTextToBlockEntity();
                }
                return true; 
            }

            // Коды стрелок: Влево (263), Вправо (262), Вверх (265), Вниз (264)
            if (keyCode == 263 || keyCode == 262 || keyCode == 265 || keyCode == 264) {
                return true; 
            }

            // Основная часть метода keyPressed
            if (keyCode == 259 && !localPagesText[currentPage].isEmpty()) {
                String currentText = localPagesText[currentPage];

                if (currentPage == 0) {
                    if (currentStep >= 3) {
                        return false; 
                    }
                    
                    if (currentStep == 2) {
                        int lastSplit = currentText.lastIndexOf("\n\n");
                        if (lastSplit != -1) {
                            int protectedLength = lastSplit + 2; 
                            if (currentText.length() > protectedLength) {
                                localPagesText[currentPage] = currentText.substring(0, currentText.length() - 1);
                                saveTextToBlockEntity();
                                return true;
                            }
                            return false; 
                        }
                    }
                }
                
                localPagesText[currentPage] = currentText.substring(0, currentText.length() - 1);
                saveTextToBlockEntity();
                return true;
            }

            // Кнопка Enter (коды 257 и 335)
            if (keyCode == 257 || keyCode == 335) {
                if (currentPage == 0 && currentStep < 3) {
                    saveTextToBlockEntity();
                } else {
                    String currentText = localPagesText[currentPage];
                    String testText = currentText + "\n";
                    int textHeight = Minecraft.getInstance().font.wordWrapHeight(testText, 140);
                    if (textHeight <= 184) {
                        localPagesText[currentPage] = testText;
                        saveTextToBlockEntity();
                    }
                }
                return true;
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }


        private void saveTextToBlockEntity() {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(blockPos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                String fullPageText = this.localPagesText[currentPage];
                String currentTextLower = fullPageText.toLowerCase().trim();

                if (be.dialogueStep == 1 || be.dialogueStep == 0) {
                    be.dialogueStep = 1;

                    boolean isGood = currentTextLower.contains("хорошо") || currentTextLower.contains("good") || currentTextLower.contains("да") || currentTextLower.contains("yes");
                    boolean isBad = currentTextLower.contains("плохо") || currentTextLower.contains("bad") || currentTextLower.contains("нет") || currentTextLower.contains("no");

                    if (isGood || isBad) {
                        be.firstAnswerWasBad = isBad;
                        be.dialogueStep = 2;
                        this.localPagesText[currentPage] = fullPageText + "\n\n"; 
                        
                        Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, blockPos, 
                                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.2F);
                    }
                }

                else if (be.dialogueStep == 2) {
                    String playerInput = "";
                    int lastNewLine = currentTextLower.lastIndexOf("\n");
                    if (lastNewLine != -1 && lastNewLine < currentTextLower.length() - 1) {
                        playerInput = currentTextLower.substring(lastNewLine + 1).trim();
                    } else {
                        playerInput = currentTextLower;
                    }

                    boolean responseIsGood = playerInput.contains("да") || playerInput.contains("хорошо") || playerInput.contains("yes") || playerInput.contains("good");
                    boolean responseIsBad = playerInput.contains("нет") || playerInput.contains("плохо") || playerInput.contains("no") || playerInput.contains("bad");

                    if (responseIsGood || responseIsBad) {
                        be.dialogueStep = 3;
                        boolean totalNegative = be.firstAnswerWasBad && responseIsBad;

                        be.currentEventId = this.currentSurveyDay; 

                        be.rewardType = com.closetfunc.event.SurveyManager.calculateRewardType(this.currentSurveyDay, totalNegative);
                        
                        this.localPagesText[currentPage] = fullPageText + " \n§7загляни в эту печатную машинку завтра\nя буду тебя там ждать*§r";
                        
                        net.minecraft.client.player.LocalPlayer localPlayer = Minecraft.getInstance().player;
                        if (localPlayer != null) {
                            net.minecraft.sounds.SoundEvent sound = totalNegative ? net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get() : net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.get();
                            float pitch = totalNegative ? 0.5F : 1.2F;
                            Minecraft.getInstance().level.playSound(localPlayer, blockPos, sound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, pitch);
                        }
                    }
                }

                // Локальная проверка пасхалки на "Death Note"
                Player player = Minecraft.getInstance().player;
                if (player != null && !player.getPersistentData().contains("DeathNoteTriggeredClient")) {
                    String playerName = player.getGameProfile().getName().toLowerCase().trim();
                    
                    String cleanText = this.localPagesText[currentPage]
                            .replace("\n", "")
                            .replace("\r", "")
                            .replace("", "")
                            .toLowerCase()
                            .trim();
                    
                    if (!playerName.isEmpty() && cleanText.contains(playerName)) {
                        player.getPersistentData().putBoolean("DeathNoteTriggeredClient", true);
                        
                        net.minecraft.network.chat.MutableComponent scaryMessage = net.minecraft.network.chat.Component.literal("DEATHNOTE_ACTIVATED_DOOM_AWAITS")
                                .withStyle(net.minecraft.ChatFormatting.RED)
                                .withStyle(net.minecraft.ChatFormatting.OBFUSCATED);
                        
                        player.sendSystemMessage(scaryMessage);

                        if (Minecraft.getInstance().level != null) {
                            Minecraft.getInstance().level.playSound(player, blockPos, 
                                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 1.0F, 0.8F);
                        }

                        if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer) {
                            localPlayer.connection.sendCommand("closetmod_trigger_heartattack");
                        }
                    }
                }
                com.closetfunc.network.ModMessages.sendToServer(new com.closetfunc.network.ModMessages.ServerboundTypewriterTextPacket(
                    blockPos, this.localPagesText, be.dialogueStep, be.rewardType, be.currentEventId, be.firstAnswerWasBad
                ));
            }
        }
    }
}