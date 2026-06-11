package com.closetfunc.network;

import com.closetfunc.MainCloset;
import com.closetfunc.block_entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() { return packetId++; }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(MainCloset.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ServerboundTypewriterTextPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ServerboundTypewriterTextPacket::new)
                .encoder(ServerboundTypewriterTextPacket::toBytes)
                .consumerMainThread(ServerboundTypewriterTextPacket::handle)
                .add();

        net.messageBuilder(ClientboundOpenTypewriterPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        .decoder(ClientboundOpenTypewriterPacket::new)
        .encoder(ClientboundOpenTypewriterPacket::toBytes)
        .consumerMainThread(ClientboundOpenTypewriterPacket::handle)
        .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
    INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static class ServerboundTypewriterTextPacket {
        private final BlockPos pos;
        private final String[] pagesText;
        private final int dialogueStep;
        private final int rewardType;
        private final int currentEventId;
        private final boolean firstAnswerWasBad;

        public ServerboundTypewriterTextPacket(BlockPos pos, String[] pagesText, int dialogueStep, int rewardType, int currentEventId, boolean firstAnswerWasBad) {
            this.pos = pos;
            this.pagesText = pagesText;
            this.dialogueStep = dialogueStep;
            this.rewardType = rewardType;
            this.currentEventId = currentEventId;
            this.firstAnswerWasBad = firstAnswerWasBad;
        }

        public ServerboundTypewriterTextPacket(FriendlyByteBuf buf) {
            this.pos = buf.readBlockPos();
            this.dialogueStep = buf.readInt();
            this.rewardType = buf.readInt();
            this.currentEventId = buf.readInt();
            this.firstAnswerWasBad = buf.readBoolean();
            this.pagesText = new String[128];
            for (int i = 0; i < 128; i++) {
                this.pagesText[i] = buf.readUtf();
            }
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeInt(dialogueStep);
            buf.writeInt(rewardType);
            buf.writeInt(currentEventId);
            buf.writeBoolean(firstAnswerWasBad);
            for (String text : pagesText) {
                buf.writeUtf(text != null ? text : "");
            }
        }

        public boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer sender = context.getSender();
                if (sender == null) return;
                
                ServerLevel level = sender.serverLevel();
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                    
                    be.updateTextFromServer(this.pagesText);
                    be.dialogueStep = this.dialogueStep;
                    be.rewardType = this.rewardType;
                    be.currentEventId = this.currentEventId;
                    be.firstAnswerWasBad = this.firstAnswerWasBad;
                    
                    if (be.dialogueStep == 3) {
                        be.lastCompletedDay = level.getDayTime() / 24000L;

                        long timeOfDay = level.getDayTime() % 24000L;
                        int ticksUntilNewDay = (int) (24000L - timeOfDay);
                        if (ticksUntilNewDay <= 0) ticksUntilNewDay = 1;

                        if (be.rewardType % 2 != 0) {
                            com.closetfunc.event.GoodRewards.execute(be.rewardType, sender, level, ticksUntilNewDay);
                        } else {
                            com.closetfunc.event.BadRewards.execute(be.rewardType, sender, level, ticksUntilNewDay);
                        }
                    }
                    
                    be.setChanged();
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                }
            });
            return true;
        }
    }

        public static class ClientboundOpenTypewriterPacket {
        private final BlockPos pos;
        private final int paperCount;
        private final int surveyDay;
        private final String firstPageText;

        public ClientboundOpenTypewriterPacket(BlockPos pos, int paperCount, int surveyDay, String firstPageText) {
            this.pos = pos;
            this.paperCount = paperCount;
            this.surveyDay = surveyDay;
            this.firstPageText = firstPageText != null ? firstPageText : "";
        }

        public ClientboundOpenTypewriterPacket(FriendlyByteBuf buf) {
            this.pos = buf.readBlockPos();
            this.paperCount = buf.readInt();
            this.surveyDay = buf.readInt();
            this.firstPageText = buf.readUtf();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeInt(paperCount);
            buf.writeInt(surveyDay);
            buf.writeUtf(firstPageText);
        }

        public boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                com.closetfunc.client.ClosetClient.openCustomTypewriterScreen(this.pos, this.paperCount, this.surveyDay, this.firstPageText);
            });
            return true;
        }
    }
}