package com.closetfunc.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface ITypewriterReward {
    void tick(ServerPlayer player, ServerLevel level, int duration);
    
    void cleanup(ServerPlayer player, ServerLevel level);
}
