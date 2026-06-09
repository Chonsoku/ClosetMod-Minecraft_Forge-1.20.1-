package com.closetfunc.event;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("null")
public class GoodRewards {
    public static void execute(int rewardId, ServerPlayer player, ServerLevel level, int duration) {
        switch (rewardId) {
            case 1: // Исход 1: Тотальное выжигание монстров вокруг игрока
                executeAnnihilation(player, level);
                break;
                
            case 3: // Исход 2: Аномальная гравитация (Высокий прыжок + подброс мобов)
                executeLowGravity(player, level, duration);
                break;
                
            // Сюда будешь добавлять новые кейсы: case 5, case 7, case 9...
        }
    }

    // Логика выжигания мобов (Событие 1)
    private static void executeAnnihilation(ServerPlayer player, ServerLevel level) {
        List<?> entities = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(128.0D));
        for (Object obj : entities) {
            if (obj instanceof Monster monster && monster.isAlive()) {
                monster.hurt(level.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
        }
    }

    // Логика аномальной гравитации (Событие 2)
    private static void executeLowGravity(ServerPlayer player, ServerLevel level, int duration) {
        // Отключаем урон от падения
        player.fallDistance = 0.0F;
        
        // Выдаем прыгучесть IV
        if (!player.hasEffect(net.minecraft.world.effect.MobEffects.JUMP)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.JUMP, duration, 3, false, false
            ));
        }

        // Если игрок в этот тик находится в воздухе и прыгает (летит вверх)
        if (!player.onGround() && player.getDeltaMovement().y > 0.1) {
            // Ищем монстров в радиусе 15 блоков вокруг игрока
            List<Monster> nearbyMonsters = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(15.0D));
            for (Monster monster : nearbyMonsters) {
                if (monster.isAlive()) {
                    monster.setDeltaMovement(new Vec3(0, 0.6D, 0));
                    monster.hurtMarked = true;
                }
            }
        }
    }
}
