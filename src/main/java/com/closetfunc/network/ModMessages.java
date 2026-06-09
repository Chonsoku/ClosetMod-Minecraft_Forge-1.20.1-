package com.closetfunc.network;

import com.closetfunc.MainCloset;
import com.closetfunc.block_entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
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
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
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
                ServerLevel level = context.getSender().serverLevel();
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof ModBlockEntities.TypewriterBlockEntity be) {
                    be.updateTextFromServer(this.pagesText);
                    be.dialogueStep = this.dialogueStep;
                    be.rewardType = this.rewardType;
                    be.currentEventId = this.currentEventId;
                    be.firstAnswerWasBad = this.firstAnswerWasBad;
                    
                    be.setChanged();
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                }
            });
            return true;
        }
    }
}