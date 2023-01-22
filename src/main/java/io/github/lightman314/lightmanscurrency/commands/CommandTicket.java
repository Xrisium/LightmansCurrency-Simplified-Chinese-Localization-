package io.github.lightman314.lightmanscurrency.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.lightman314.lightmanscurrency.commands.arguments.ColorArgument;
import io.github.lightman314.lightmanscurrency.common.tickets.TicketSaveData;
import io.github.lightman314.lightmanscurrency.items.TicketItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class CommandTicket {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command =
                Commands.literal("tickets")
                        .requires(commandSource -> commandSource.isPlayer() && commandSource.hasPermission(2))
                        .then(Commands.literal("changeColor")
                                .then(Commands.argument("color", ColorArgument.argument())
                                        .executes(CommandTicket::changeColor)))
                        .then(Commands.literal("create")
                                .executes(CommandTicket::createTicketNonColored)
                                .then(Commands.argument("color", ColorArgument.argument())
                                        .executes(CommandTicket::createTicketColored)));

        dispatcher.register(command);
    }

    static int changeColor(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();
        if(heldItem.getItem() instanceof TicketItem)
        {
            int color = ColorArgument.getColor(commandContext, "color");
            TicketItem.SetTicketColor(heldItem, color);
        }
        else
            commandContext.getSource().sendFailure(Component.translatable("command.lightmanscurrency.ticket.color.not_held"));
        return 0;
    }

    static int createTicketNonColored(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        ItemStack ticket = TicketItem.CreateMasterTicket(TicketSaveData.createNextID());
        giveItemToPlayer(player, ticket);
        return 1;
    }

    static int createTicketColored(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        int color = ColorArgument.getColor(commandContext,"color");
        ItemStack ticket = TicketItem.CreateMasterTicket(TicketSaveData.createNextID(), color);
        giveItemToPlayer(player, ticket);
        return 1;
    }

    private static void giveItemToPlayer(ServerPlayer player, ItemStack item) {
        Inventory inv = player.getInventory();
        if(inv.getItem(inv.selected).isEmpty())
            player.getInventory().setItem(inv.selected, item);
        else
            ItemHandlerHelper.giveItemToPlayer(player, item);
    }

}