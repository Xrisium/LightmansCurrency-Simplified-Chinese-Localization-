package io.github.lightman314.lightmanscurrency.common.commands;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import io.github.lightman314.lightmanscurrency.LCConfig;
import io.github.lightman314.lightmanscurrency.LCText;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.api.traders.TraderAPI;
import io.github.lightman314.lightmanscurrency.api.traders.blockentity.TraderBlockEntity;
import io.github.lightman314.lightmanscurrency.api.traders.blocks.ITraderBlock;
import io.github.lightman314.lightmanscurrency.common.capability.event_unlocks.CapabilityEventUnlocks;
import io.github.lightman314.lightmanscurrency.common.capability.event_unlocks.IEventUnlocks;
import io.github.lightman314.lightmanscurrency.common.capability.wallet.IWalletHandler;
import io.github.lightman314.lightmanscurrency.common.capability.wallet.WalletCapability;
import io.github.lightman314.lightmanscurrency.common.commands.arguments.TraderArgument;
import io.github.lightman314.lightmanscurrency.api.misc.EasyText;
import io.github.lightman314.lightmanscurrency.common.items.WalletItem;
import io.github.lightman314.lightmanscurrency.common.menus.validation.types.SimpleValidator;
import io.github.lightman314.lightmanscurrency.common.player.LCAdminMode;
import io.github.lightman314.lightmanscurrency.common.taxes.TaxEntry;
import io.github.lightman314.lightmanscurrency.common.taxes.TaxSaveData;
import io.github.lightman314.lightmanscurrency.api.traders.TraderData;
import io.github.lightman314.lightmanscurrency.common.traders.TraderSaveData;
import io.github.lightman314.lightmanscurrency.common.traders.auction.AuctionHouseTrader;
import io.github.lightman314.lightmanscurrency.common.traders.rules.TradeRule;
import io.github.lightman314.lightmanscurrency.common.traders.rules.types.PlayerListing;
import io.github.lightman314.lightmanscurrency.network.message.command.SPacketDebugTrader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;

public class CommandLCAdmin {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context)
	{
		LiteralArgumentBuilder<CommandSourceStack> lcAdminCommand
				= Commands.literal("lcadmin")
				.requires((stack) -> stack.hasPermission(2))
				.then(Commands.literal("toggleadmin")
						.requires(CommandSourceStack::isPlayer)
						.executes(CommandLCAdmin::toggleAdmin))
				.then(Commands.literal("traderdata")
						.then(Commands.literal("list")
								.executes(CommandLCAdmin::listTraderData))
						.then(Commands.literal("search")
								.then(Commands.argument("searchText", StringArgumentType.greedyString())
										.executes(CommandLCAdmin::searchTraderData)))
						.then(Commands.literal("delete")
								.then(Commands.argument("traderID", TraderArgument.trader())
										.executes(CommandLCAdmin::deleteTraderData)))
						.then(Commands.literal("debug")
								.then(Commands.argument("traderID", TraderArgument.trader())
										.executes(CommandLCAdmin::debugTraderData)))
						.then(Commands.literal("addToWhitelist")
								.then(Commands.argument("traderID", TraderArgument.traderWithPersistent())
										.then(Commands.argument("player", EntityArgument.players())
												.executes(CommandLCAdmin::addToTraderWhitelist)))))
				.then(Commands.literal("prepareForStructure")
						.then(Commands.argument("traderPos", BlockPosArgument.blockPos())
								.executes(CommandLCAdmin::setCustomTrader)))
				.then(Commands.literal("replaceWallet").requires((c) -> !LightmansCurrency.isCuriosLoaded())
						.then(Commands.argument("entity", EntityArgument.entities())
								.then(Commands.argument("wallet", ItemArgument.item(context))
										.executes(CommandLCAdmin::replaceWalletSlotWithDefault)
										.then(Commands.argument("keepWalletContents", BoolArgumentType.bool())
												.executes(CommandLCAdmin::replaceWalletSlot)))))
				.then(Commands.literal("taxes")
						.then(Commands.literal("list")
								.executes(CommandLCAdmin::listTaxCollectors))
						.then(Commands.literal("delete")
								.then(Commands.argument("taxCollectorID", LongArgumentType.longArg(0))
										.executes(CommandLCAdmin::deleteTaxCollector)))
						.then(Commands.literal("openServerTax")
								.requires(CommandSourceStack::isPlayer)
								.executes(CommandLCAdmin::openServerTax))
						.then(Commands.literal("forceDisableTaxCollectors")
								.executes(CommandLCAdmin::forceDisableTaxCollectors)))
				.then(Commands.literal("events")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.literal("reward")
										.then(Commands.argument("item",ItemArgument.item(context))
												.then(Commands.argument("count",IntegerArgumentType.integer(1))
														.executes(CommandLCAdmin::giveEventReward))))
								.then(Commands.literal("list")
										.executes(CommandLCAdmin::listUnlockedEvents))
								.then(Commands.literal("unlock")
										.then(Commands.argument("event", StringArgumentType.word())
											.executes(CommandLCAdmin::unlockEvent)))
								.then(Commands.literal("lock")
										.then(Commands.argument("event", StringArgumentType.word())
											.executes(CommandLCAdmin::lockEvent)))));

		dispatcher.register(lcAdminCommand);

	}

	static int toggleAdmin(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException{

		CommandSourceStack source = commandContext.getSource();
		ServerPlayer sourcePlayer = source.getPlayerOrException();

		LCAdminMode.ToggleAdminPlayer(sourcePlayer);
		Component enabledDisabled = LCAdminMode.isAdminPlayer(sourcePlayer) ? LCText.COMMAND_ADMIN_TOGGLE_ADMIN_ENABLED.getWithStyle(ChatFormatting.GREEN) : LCText.COMMAND_ADMIN_TOGGLE_ADMIN_DISABLED.getWithStyle(ChatFormatting.RED);
		EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TOGGLE_ADMIN.get(enabledDisabled), true);

		return 1;
	}

	private static final SimpleCommandExceptionType ERROR_BLOCK_NOT_FOUND = new SimpleCommandExceptionType(LCText.COMMAND_ADMIN_PREPARE_FOR_STRUCTURE_ERROR.get());

	static int setCustomTrader(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {

		CommandSourceStack source = commandContext.getSource();

		BlockPos pos = BlockPosArgument.getLoadedBlockPos(commandContext, "traderPos");

		Level level = source.getLevel();

		BlockState state = level.getBlockState(pos);
		BlockEntity be;
		if(state.getBlock() instanceof ITraderBlock)
			be = ((ITraderBlock)state.getBlock()).getBlockEntity(state, level, pos);
		else
			be = level.getBlockEntity(pos);

		if(be instanceof TraderBlockEntity<?> t)
		{
			t.saveCurrentTraderAsCustomTrader();
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_PREPARE_FOR_STRUCTURE_SUCCESS.get(), true);
			return 1;
		}

		throw ERROR_BLOCK_NOT_FOUND.create();

	}

	static int listTraderData(CommandContext<CommandSourceStack> commandContext) {

		CommandSourceStack source = commandContext.getSource();
		List<TraderData> allTraders = TraderSaveData.GetAllTraders(false);

		if(!allTraders.isEmpty())
		{

			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_TITLE.get(), true);

			for(int i = 0; i < allTraders.size(); i++)
			{
				TraderData thisTrader = allTraders.get(i);
				//Spacer
				if(i > 0) //No spacer on the first output
					EasyText.sendCommandSucess(source, EasyText.empty(), true);

				sendTraderDataFeedback(thisTrader, source);
			}
		}
		else
		{
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_NONE.get(), true);
		}

		return 1;
	}

	static int searchTraderData(CommandContext<CommandSourceStack> commandContext) {

		CommandSourceStack source = commandContext.getSource();

		String searchText = StringArgumentType.getString(commandContext,"searchText");

		List<TraderData> results = TraderSaveData.GetAllTraders(false).stream().filter(trader -> TraderAPI.filterTrader(trader, searchText)).toList();
		if(!results.isEmpty())
		{

			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_TITLE.get(), true);
			for(int i = 0; i < results.size(); i++)
			{
				TraderData thisTrader = results.get(i);
				//Spacer
				if(i > 0) //No spacer on the first output
					EasyText.sendCommandSucess(source, EasyText.empty(), true);

				sendTraderDataFeedback(thisTrader, source);
			}
		}
		else
		{
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_SEARCH_NONE.get(), true);
		}

		return 1;
	}

	private static void sendTraderDataFeedback(TraderData thisTrader, CommandSourceStack source)
	{
		//Trader ID
		String traderID = String.valueOf(thisTrader.getID());
		EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_TRADER_ID.get(EasyText.literal(traderID).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, traderID)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, LCText.COMMAND_ADMIN_TRADERDATA_LIST_TRADER_ID_TOOLTIP.get())))), false);
		//Persistent ID
		if(thisTrader.isPersistent())
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_PERSISTENT_ID.get(thisTrader.getPersistentID()), false);

		//Type
		EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_TYPE.get(thisTrader.type), false);

		//Ignore everything else for auction houses
		if(thisTrader instanceof AuctionHouseTrader)
			return;

		//Owner
		EasyText.sendCommandSucess(source, thisTrader.getOwner().getValidOwner().getCommandLabel(), false);

		if(!thisTrader.isPersistent())
		{
			//Dimension
			String dimension = thisTrader.getLevel().location().toString();
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_DIMENSION.get(dimension), false);
			//Position
			BlockPos pos = thisTrader.getPos();
			String position = pos.getX() + " " + pos.getY() + " " + pos.getZ();
			String teleportPosition = pos.getX() + " " + (pos.getY() + 1) + " " + pos.getZ();
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_POSITION.get(EasyText.literal(position).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/execute in " + dimension + " run tp @s " + teleportPosition)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, LCText.COMMAND_ADMIN_TRADERDATA_LIST_POSITION_TOOLTIP.get())))), true);
		}
		//Custom Name (if applicable)
		if(thisTrader.hasCustomName())
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_LIST_NAME.get(thisTrader.getName()), true);
	}

	static int deleteTraderData(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{

		CommandSourceStack source = commandContext.getSource();

		TraderData trader = TraderArgument.getTrader(commandContext, "traderID");

		//Remove the trader
		TraderSaveData.DeleteTrader(trader.getID());
		//Send success message
		EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_DELETE_SUCCESS.get(trader.getName()), true);
		return 1;

	}

	static int debugTraderData(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{
		CommandSourceStack source = commandContext.getSource();

		TraderData trader = TraderArgument.getTrader(commandContext, "traderID");
		EasyText.sendCommandSucess(source, EasyText.literal(trader.save().getAsString()), false);
		if(commandContext.getSource().isPlayer())
			new SPacketDebugTrader(trader.getID()).sendTo(commandContext.getSource().getPlayerOrException());
		return 1;
	}

	static int addToTraderWhitelist(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{

		CommandSourceStack source = commandContext.getSource();

		TraderData trader = TraderArgument.getTrader(commandContext, "traderID");

		TradeRule rule = TradeRule.getRule(PlayerListing.TYPE.type, trader.getRules());
		if(rule instanceof PlayerListing whitelist)
		{
			Collection<ServerPlayer> players = EntityArgument.getPlayers(commandContext, "player");
			int count = 0;
			for(ServerPlayer player : players)
			{
				if(whitelist.addToWhitelist(player))
					count++;
			}
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TRADERDATA_ADD_TO_WHITELIST_SUCCESS.get(count, trader.getName()), true);

			if(count > 0)
				trader.markTradeRulesDirty();

			return count;
		}
		else
		{
			EasyText.sendCommandFail(source, LCText.COMMAND_ADMIN_TRADERDATA_ADD_TO_WHITELIST_MISSING.get());
			return 0;
		}

	}

	static int replaceWalletSlotWithDefault(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{
		return replaceWalletSlotInternal(commandContext, true);
	}

	static int replaceWalletSlot(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
		return replaceWalletSlotInternal(commandContext, BoolArgumentType.getBool(commandContext, "keepWalletContents"));
	}

	static int replaceWalletSlotInternal(CommandContext<CommandSourceStack> commandContext, boolean keepWalletContents) throws CommandSyntaxException {
		int count = 0;
		ItemInput input = ItemArgument.getItem(commandContext,"wallet");
		if(!(input.getItem() instanceof WalletItem) && input.getItem() != Items.AIR)
			throw new CommandSyntaxException(new CommandExceptionType() {}, LCText.COMMAND_ADMIN_REPLACE_WALLET_NOT_A_WALLET.get());
		for(Entity entity : EntityArgument.getEntities(commandContext,"entity"))
		{
			IWalletHandler walletHandler = WalletCapability.lazyGetWalletHandler(entity);
			if(walletHandler != null)
			{
				ItemStack newWallet = input.createItemStack(1, true);
				if(newWallet.isEmpty() && walletHandler.getWallet().isEmpty())
					continue;
				if(keepWalletContents)
				{
					ItemStack oldWallet = walletHandler.getWallet();
					if(WalletItem.isWallet(oldWallet))
						newWallet.setTag(oldWallet.getOrCreateTag().copy());
				}
				walletHandler.setWallet(newWallet);
			}
		}
		return count;
	}

	static int openServerTax(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{
		TaxEntry entry = TaxSaveData.GetServerTaxEntry(false);
		if(entry != null)
		{
			entry.openMenu(commandContext.getSource().getPlayerOrException(), SimpleValidator.NULL);
			return 1;
		}
		else
			EasyText.sendCommandFail(commandContext.getSource(), LCText.COMMAND_ADMIN_TAXES_OPEN_SERVER_TAX_ERROR.get());
		return 0;
	}

	static int listTaxCollectors(CommandContext<CommandSourceStack> commandContext)
	{
		CommandSourceStack source = commandContext.getSource();
		EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_TITLE.get(), false);
		for(TaxEntry entry : TaxSaveData.GetAllTaxEntries(false))
			sendTaxDataFeedback(entry, source);
		return 1;
	}

	private static void sendTaxDataFeedback(TaxEntry taxEntry, CommandSourceStack source)
	{
		if(taxEntry.isServerEntry())
			return;
		//Trader ID
		String id = String.valueOf(taxEntry.getID());
		EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_ID.get(EasyText.literal(id).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, LCText.COMMAND_ADMIN_TAXES_LIST_ID_TOOLTIP.get())))), false);

		//Team / Team ID
		if(!taxEntry.isServerEntry())
			EasyText.sendCommandSucess(source,taxEntry.getOwner().getValidOwner().getCommandLabel(), false);

		//Dimension
		String dimension = taxEntry.getArea().getCenter().getDimension().location().toString();
		EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_DIMENSION.get(dimension), false);
		//Position
		BlockPos pos = taxEntry.getArea().getCenter().getPos();
		String position = pos.getX() + " " + pos.getY() + " " + pos.getZ();
		String teleportPosition = pos.getX() + " " + (pos.getY() + 1) + " " + pos.getZ();
		EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_POSITION.get(EasyText.literal(position).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/execute in " + dimension + " run tp @s " + teleportPosition)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, LCText.COMMAND_ADMIN_TAXES_LIST_POSITION_TOOLTIP.get())))), true);
		//Range
		if(taxEntry.isInfiniteRange())
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_INFINITE_RANGE.get(), false);
		else
		{
			//Radius
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_RADIUS.get(taxEntry.getRadius()), false);
			//Height
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_HEIGHT.get(taxEntry.getHeight()), false);
			//Vertical Offset
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_OFFSET.get(taxEntry.getVertOffset()), false);
		}
		if(taxEntry.forcesAcceptance())
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_FORCE_ACCEPTANCE.get(), false);

		//Custom Name (if applicable)
		if(taxEntry.hasCustomName())
			EasyText.sendCommandSucess(source, LCText.COMMAND_ADMIN_TAXES_LIST_NAME.get(taxEntry.getName()), true);
	}

	static int deleteTaxCollector(CommandContext<CommandSourceStack> commandContext)
	{
		long taxCollectorID = LongArgumentType.getLong(commandContext, "taxCollectorID");

		TaxEntry entry = TaxSaveData.GetTaxEntry(taxCollectorID, false);
		if(entry == null || entry.isServerEntry())
		{
			EasyText.sendCommandFail(commandContext.getSource(), LCText.COMMAND_ADMIN_TAXES_DELETE_FAIL.get());
			return 0;
		}
		TaxSaveData.RemoveEntry(taxCollectorID);
		EasyText.sendCommandSucess(commandContext.getSource(), LCText.COMMAND_ADMIN_TAXES_DELETE_SUCCESS.get(entry.getName()), true);
		return 1;
	}

	static int forceDisableTaxCollectors(CommandContext<CommandSourceStack> commandContext)
	{
		int count = 0;
		for(TaxEntry entry : TaxSaveData.GetAllTaxEntries(false))
		{
			if(entry.isActive())
			{
				entry.setActive(false, null);
				count++;
			}
		}
		if(count > 0)
			EasyText.sendCommandSucess(commandContext.getSource(), LCText.COMMAND_ADMIN_TAXES_FORCE_DISABLE_SUCCESS.get(count), true);
		else
			EasyText.sendCommandFail(commandContext.getSource(), LCText.COMMAND_ADMIN_TAXES_FORCE_DISABLE_FAIL.get());
		return count;
	}


	static int giveEventReward(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{
		if(!LCConfig.COMMON.eventAdvancementRewards.get())
			return 0;
		int success = 0;
		int count = IntegerArgumentType.getInteger(commandContext,"count");
		ItemStack reward = ItemArgument.getItem(commandContext, "item").createItemStack(count,false);
		for(ServerPlayer player : EntityArgument.getPlayers(commandContext,"player"))
		{
			ItemHandlerHelper.giveItemToPlayer(player,reward.copy());
			success++;
		}
		return success;
	}

	static int listUnlockedEvents(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{
		Player player = EntityArgument.getPlayer(commandContext, "player");

		IEventUnlocks eventUnlocks = CapabilityEventUnlocks.getCapability(player);
		if(eventUnlocks != null)
		{
			List<String> unlocks = eventUnlocks.getUnlockedList();
			if(!unlocks.isEmpty())
			{
				StringBuilder list = new StringBuilder();
				for(String v : eventUnlocks.getUnlockedList())
				{
					if(!list.isEmpty())
						list.append(", ");
					list.append(v);
				}
				EasyText.sendCommandSucess(commandContext.getSource(), EasyText.literal(list.toString()), false);
			}
			else
				EasyText.sendCommandSucess(commandContext.getSource(), LCText.COMMAND_ADMIN_EVENT_LIST_NONE.get(), false);
			return 1;
		}
		else
			return 0;
	}

	static int unlockEvent(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{
		Player player = EntityArgument.getPlayer(commandContext, "player");

		String event = StringArgumentType.getString(commandContext, "event");

		if(CapabilityEventUnlocks.isUnlocked(player, event))
		{
			EasyText.sendCommandFail(commandContext.getSource(), LCText.COMMAND_ADMIN_EVENT_UNLOCK_FAIL.get(event));
			return 0;
		}

		CapabilityEventUnlocks.unlock(player, event);
		EasyText.sendCommandSucess(commandContext.getSource(), LCText.COMMAND_ADMIN_EVENT_UNLOCK_SUCCESS.get(event), false);

		return 1;
	}

	static int lockEvent(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException
	{
		Player player = EntityArgument.getPlayer(commandContext, "player");

		String event = StringArgumentType.getString(commandContext, "event");

		if(!CapabilityEventUnlocks.isUnlocked(player, event))
		{
			EasyText.sendCommandFail(commandContext.getSource(), LCText.COMMAND_ADMIN_EVENT_LOCK_FAIL.get(event));
			return 0;
		}

		CapabilityEventUnlocks.lock(player, event);
		EasyText.sendCommandSucess(commandContext.getSource(), LCText.COMMAND_ADMIN_EVENT_LOCK_SUCCESS.get(event), false);

		return 1;
	}

}