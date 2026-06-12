package com.closetfunc.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class SurveyManager {
    public static final int MAX_DAYS = 10;
    public static int calculateRewardType(int currentDay, boolean totalNegative) {
        if (totalNegative) {
            return currentDay * 2;
        } else {
            return (currentDay * 2) - 1;
        }
    }

    public static void triggerReward(int rewardType, ServerPlayer player, ServerLevel level, int duration) {
        player.getPersistentData().putInt("ActiveTypewriterRewardId", rewardType);
        
        long currentWorldDayIndex = level.getDayTime() / 24000L;
        player.getPersistentData().putLong("TypewriterEffectsExpiryDay", currentWorldDayIndex + 1);

        if (rewardType % 2 != 0) {
            GoodRewards.execute(rewardType, player, level, duration);
        } else {
            BadRewards.execute(rewardType, player, level, duration);
        }
    }
}
