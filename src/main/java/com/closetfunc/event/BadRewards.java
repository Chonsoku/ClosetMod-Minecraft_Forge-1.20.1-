package com.closetfunc.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import java.util.List;

@SuppressWarnings("null")
public class BadRewards {
    public static void execute(int rewardId, ServerPlayer player, ServerLevel level, int duration) {
        switch (rewardId) {
            case 2: // Исход 1: Сильное утомление страха
                executeFatigue(player, duration);
                break;
                
            case 4: // Исход 2: Притяжение бездны (Магнит криперов + быстрый взрыв)
                executeCreeperMagnet(player, level);
                break;
                
            // Сюда будешь добавлять новые кейсы: case 6, case 8, case 10...
        }
    }

    // Логика утомления (Событие 1)
    private static void executeFatigue(ServerPlayer player, int duration) {
        if (!player.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, duration, 1, false, false
            ));
        }
    }

    // Логика притяжения бездны (Событие 2)
    private static void executeCreeperMagnet(ServerPlayer player, ServerLevel level) {
        // Запрещаем игроку прыгать (принудительно гасим вертикальное движение)
        if (player.getDeltaMovement().y > 0) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
            player.hurtMarked = true;
        }

        // Ищем всех криперов в радиусе 24 блоков вокруг игрока
        List<Creeper> creepers = level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(24.0D));
        for (Creeper creeper : creepers) {
            if (creeper.isAlive()) {
                // Вычисляем вектор направления от крипера к игроку
                Vec3 direction = player.position().subtract(creeper.position()).normalize();
                
                // Притягиваем крипера по горизонтали со скоростью 0.25 (он будет плавно «скользить» к игроку)
                creeper.setDeltaMovement(new Vec3(direction.x * 0.25D, creeper.getDeltaMovement().y, direction.z * 0.25D));
                creeper.hurtMarked = true;

                // Ускоряем взрыв крипера, если он подошел достаточно близко (меньше 4 блоков)
                if (creeper.distanceToSqr(player) < 16.0D) {
                    creeper.ignite();
                }
            }
        }
    }
}
