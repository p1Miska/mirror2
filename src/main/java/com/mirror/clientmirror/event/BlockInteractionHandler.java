package com.mirror.clientmirror.event;

import com.mirror.clientmirror.network.OutboundSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Требование п.4: локальные break/place НИКОГДА не применяются напрямую —
 * мы их отменяем и вместо этого шлём эквивалентную команду в WS. Настоящее
 * изменение блока произойдёт только когда придёт "blockChange" от сервера
 * (см. WorldSyncHandler.applyBlockChange), т.е. подтверждено реальным сервером.
 */
public class BlockInteractionHandler {

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (!isLocalPlayer(event.getPlayer())) return;
        event.setCanceled(true); // блок физически не ломается локально
        OutboundSender.sendDig(event.getPos());
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!isLocalPlayer(event.getEntityPlayer())) return;
        event.setCanceled(true);
        event.setUseBlock(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        event.setUseItem(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        OutboundSender.sendInteract(event.getPos(), event.getFace());
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        // Подстраховка: если что-то всё же прорвалось мимо RightClickBlock
        // (например, установка через использование предмета без явного клика по блоку) —
        // всё равно отменяем локальное размещение.
        if (event.getEntity() != Minecraft.getMinecraft().player) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onAttack(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (!isLocalPlayer(event.getEntityPlayer())) return;
        // Локально всегда отменяем: в пустом/плоском мире это либо наша же
        // фейковая EntityOtherPlayerMP (у неё нет реального здоровья/лута
        // на нашей стороне, бить её "по-настоящему" бессмысленно), либо
        // случайная сущность локального мира, которой на сервере вообще нет.
        event.setCanceled(true);

        net.minecraft.entity.Entity target = event.getTarget();
        if (target instanceof net.minecraft.client.entity.EntityOtherPlayerMP) {
            OutboundSender.sendAttack(target.getName());
        }
        // Атака мобов по имени сервер сейчас не поддерживает (case 'attack' в
        // server.js ищет только bot.players[username] — то есть только игроков).
        // Если понадобится бить мобов, сервер нужно доучить резолвить их по id.
    }

    private boolean isLocalPlayer(net.minecraft.entity.player.EntityPlayer player) {
        EntityPlayerSP local = Minecraft.getMinecraft().player;
        return local != null && player == local;
    }
}
