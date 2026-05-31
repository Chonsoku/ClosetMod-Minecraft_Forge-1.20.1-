package com.closetfunc;

import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClosetClient {
    
    public static void init() {
        MinecraftForge.EVENT_BUS.register(ClosetClient.class);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player != null && player.getPersistentData().getBoolean("IsInCloset")) {
            Input input = event.getInput();
            if (input.shiftKeyDown) {
                player.getPersistentData().putBoolean("IsInCloset", false);
                return;
            }
            input.leftImpulse = 0;
            input.forwardImpulse = 0;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
        }
    }
}
