package io.github.lightman314.lightmanscurrency.discord.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

import io.github.lightman314.lightmansconsole.LightmansDiscordIntegration;
import io.github.lightman314.lightmansconsole.discord.links.AccountManager;
import io.github.lightman314.lightmansconsole.discord.links.LinkedAccount;
import io.github.lightman314.lightmansconsole.discord.listeners.types.SingleChannelListener;
import io.github.lightman314.lightmansconsole.util.MessageUtil;
import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalItemTraderData;
import io.github.lightman314.lightmanscurrency.discord.CurrencyMessages;
import io.github.lightman314.lightmanscurrency.discord.events.DiscordPostTradeEvent;
import io.github.lightman314.lightmanscurrency.discord.events.DiscordTraderSearchEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.events.UniversalTraderEvent.UniversalTradeCreateEvent;
import io.github.lightman314.lightmanscurrency.trader.IItemTrader;
import io.github.lightman314.lightmanscurrency.trader.settings.PlayerReference;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CurrencyListener extends SingleChannelListener{
	
	private final Timer timer;
	
	private static final long PENDING_MESSAGE_TIMER = 300000; //5m timer cycle for sending pending messages.
	private static final long ANNOUCEMENT_DELAY = 60000; //60s delay before announcing to give the owner time to set a name, etc.
	
	Map<String,List<String>> pendingMessages = new HashMap<>();
	
	public CurrencyListener(Supplier<String> consoleChannel)
	{
		super(consoleChannel, () -> LightmansDiscordIntegration.PROXY.getJDA());
		this.timer = new Timer();
		this.timer.scheduleAtFixedRate(new NotifyTraderOwnerTask(this), 0, PENDING_MESSAGE_TIMER);
	}

	@Override
	protected void onChannelMessageReceived(MessageReceivedEvent event) {
		
		handleMessage(event.getChannel(), event.getMessage(), event.getAuthor());
	}
	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event)
	{
		handleMessage(event.getChannel(), event.getMessage(), event.getAuthor());
	}
	
	private void handleMessage(MessageChannel channel, Message message, User author)
	{
		if(author.isBot())
			return;
		
		//Run command
		String input = message.getContentDisplay();
		String prefix = Config.SERVER.currencyCommandPrefix.get();
		if(input.startsWith(prefix))
		{
			String command = input.substring(prefix.length());
			if(command.startsWith("help"))
			{
				List<String> output = new ArrayList<>();
				output.add(prefix + "notifications <help|enable|disable> - " + CurrencyMessages.M_HELP_LC_NOTIFICATIONS.get());
				output.add(prefix + "search <sales|purchases|barters|all> [searchText] - " + CurrencyMessages.M_HELP_LC_SEARCH1.get());
				output.add(prefix + "search <players|shops> [searchText] - " + CurrencyMessages.M_HELP_LC_SEARCH2.get());
				MessageUtil.sendTextMessage(channel, output);
			}
			else if(command.startsWith("notifications "))
			{
				String subcommand = command.substring(14);
				if(subcommand.startsWith("help"))
				{
					List<String> output = new ArrayList<>();
					output.add(AccountManager.currencyNotificationsEnabled(author) ? CurrencyMessages.M_NOTIFICATIONS_ENABLED.get() : CurrencyMessages.M_NOTIFICATIONS_DISABLED.get());
					output.addAll(Lists.newArrayList(CurrencyMessages.M_NOTIFICATIONS_HELP.get().split("\n")));
					
					MessageUtil.sendTextMessage(channel, output);
				}
				else if(subcommand.startsWith("enable"))
				{
					if(AccountManager.enableCurrencyNotifications(author))
						MessageUtil.sendTextMessage(channel, CurrencyMessages.M_NOTIFICATIONS_ENABLE_SUCCESS.get());
					else
						MessageUtil.sendTextMessage(channel, CurrencyMessages.M_NOTIFICATIONS_ENABLE_FAIL.get());
				}
				else if(subcommand.startsWith("disable"))
				{
					if(AccountManager.disableCurrencyNotifications(author))
						MessageUtil.sendTextMessage(channel, CurrencyMessages.M_NOTIFICATIONS_DISABLE_SUCCESS.get());
					else
						MessageUtil.sendTextMessage(channel, CurrencyMessages.M_NOTIFICATIONS_DISABLE_FAIL.get());
				}
			}
			else if(command.startsWith("search "))
			{
				String subcommand = command.substring(7);
				AtomicReference<String> searchText = new AtomicReference<>("");
				AtomicBoolean findSales = new AtomicBoolean(false);
				AtomicBoolean findPurchases = new AtomicBoolean(false);
				AtomicBoolean findBarters = new AtomicBoolean(false);
				AtomicBoolean findOwners = new AtomicBoolean(false);
				AtomicBoolean findTraders = new AtomicBoolean(false);
				if(subcommand.startsWith("sales"))
				{
					findSales.set(true);
					if(subcommand.length() > 6)
						searchText.set(subcommand.substring(6).toLowerCase());
				}
				else if(subcommand.startsWith("purchases"))
				{
					findPurchases.set(true);
					if(subcommand.length() > 10)
						searchText.set(subcommand.substring(10).toLowerCase());
				}
				else if(subcommand.startsWith("barters"))
				{
					findBarters.set(true);
					if(subcommand.length() > 10)
						searchText.set(subcommand.substring(10).toLowerCase());
				}
				else if(subcommand.startsWith("players"))
				{
					findOwners.set(true);
					if(subcommand.length() > 8)
						searchText.set(subcommand.substring(8).toLowerCase());
				}
				else if(subcommand.startsWith("shops"))
				{
					findTraders.set(true);
					if(subcommand.length() > 6)
						searchText.set(subcommand.substring(6).toLowerCase());
				}
				else if(subcommand.startsWith("all"))
				{
					//All
					findSales.set(true);
					findPurchases.set(true);
					findBarters.set(true);
					//findOwners.set(true);
					//findTraders.set(true);
					if(subcommand.length() > 4)
						searchText.set(subcommand.substring(4).toLowerCase());
				}
				List<String> output = new ArrayList<>();
				TradingOffice.getTraders().forEach(trader -> {
					try {
						boolean listTrader = (findOwners.get() && (searchText.get().isEmpty() || trader.getCoreSettings().getOwnerName().toLowerCase().contains(searchText.get())))
								|| (findTraders.get() && (searchText.get().isEmpty() || trader.getName().getString().toLowerCase().contains(searchText.get())));
						
						if(trader instanceof UniversalItemTraderData) //Can't search for non item traders at this time
						{
							UniversalItemTraderData itemTrader = (UniversalItemTraderData)trader;
							if(listTrader)
							{
								boolean firstTrade = true;
								for(int i = 0; i < itemTrader.getTradeCount(); ++i)
								{
									ItemTradeData trade = itemTrader.getTrade(i);
									if(trade.isValid())
									{
										if(firstTrade)
										{
											output.add("--" + itemTrader.getCoreSettings().getOwnerName() + "'s **" + itemTrader.getName().getString() + "**--");
											firstTrade = false;
										}
										if(trade.isSale())
										{
											String priceText = trade.getCost().getString();
											output.add("Selling " + getItemNamesAndCount(trade.getSellItem(0), trade.getCustomName(0), trade.getSellItem(1), trade.getCustomName(1)) + " for " + priceText);
										}
										else if(trade.isPurchase())
										{
											String priceText = trade.getCost().getString();
											output.add("Purchasing " + getItemNamesAndCount(trade.getSellItem(0), "", trade.getSellItem(1), "") + " for " + priceText);
										}
										else if(trade.isBarter())
										{
											output.add("Bartering " + getItemNamesAndCount(trade.getBarterItem(0), "", trade.getBarterItem(1), "") + " for " + getItemNamesAndCount(trade.getSellItem(0), trade.getCustomName(0), trade.getSellItem(1), trade.getCustomName(1)));
										}
									}
								}
							}
							else
							{
								for(int i = 0; i < itemTrader.getTradeCount(); ++i)
								{
									ItemTradeData trade = itemTrader.getTrade(i);
									if(trade.isValid())
									{
										if(trade.isSale() && findSales.get())
										{
											String itemName1 = getItemName(trade.getSellItem(0), trade.getCustomName(0));
											String itemName2 = getItemName(trade.getSellItem(1), trade.getCustomName(0));
											
											//LightmansConsole.LOGGER.info("Item Name: " + itemName.toString());
											if(searchText.get().isEmpty() || itemName1.toLowerCase().contains(searchText.get()) || itemName2.toLowerCase().contains(searchText.get()))
											{
												//Passed the search
												String priceText = trade.getCost().getString();
												output.add(itemTrader.getCoreSettings().getOwnerName() + " is selling " + getItemNamesAndCount(trade.getSellItem(0), trade.getCustomName(0), trade.getSellItem(1), trade.getCustomName(1)) + " at " + itemTrader.getName().getString() + " for " + priceText);
											}
										}
										else if(trade.isPurchase() && findPurchases.get())
										{
											String itemName1 = getItemName(trade.getSellItem(0), "");
											String itemName2 = getItemName(trade.getSellItem(1), "");
											
											//LightmansConsole.LOGGER.info("Item Name: " + itemName.toString());
											if(searchText.get().isEmpty() || itemName1.toLowerCase().contains(searchText.get()) || itemName2.toLowerCase().contains(searchText.get()))
											{
												//Passed the search
												String priceText = trade.getCost().getString();
												output.add(itemTrader.getCoreSettings().getOwnerName() + " is buying " + getItemNamesAndCount(trade.getSellItem(0), "", trade.getSellItem(1), "") + " at " + itemTrader.getName().getString() + " for " + priceText);
											}
										}
										else if(trade.isBarter() && findBarters.get())
										{
											String itemName1 = getItemName(trade.getSellItem(0), trade.getCustomName(0));
											String itemName2 = getItemName(trade.getSellItem(1), trade.getCustomName(1));
											String itemName3 = getItemName(trade.getBarterItem(0), "");
											String itemName4 = getItemName(trade.getBarterItem(1), "");
											
											if(searchText.get().isEmpty() || itemName1.toLowerCase().contains(searchText.get()) || itemName2.toLowerCase().contains(searchText.get()) || itemName3.toLowerCase().contains(searchText.get()) || itemName4.toLowerCase().contains(searchText.get()))
											{
												output.add(itemTrader.getCoreSettings().getOwnerName() + " is bartering " + getItemNamesAndCount(trade.getBarterItem(0), "", trade.getBarterItem(1), "") + " for " + getItemNamesAndCount(trade.getSellItem(0), trade.getCustomName(0), trade.getSellItem(1), trade.getCustomName(1)) + " at " + itemTrader.getName().getString());
											}
											
										}
									}
								}
							}
						}
						else
							MinecraftForge.EVENT_BUS.post(new DiscordTraderSearchEvent(trader, searchText.get(), findSales.get(), findPurchases.get(), findBarters.get(), findOwners.get(), findTraders.get(), output));
					} catch(Exception e) { e.printStackTrace(); }
				});
				if(output.size() > 0)
					MessageUtil.sendTextMessage(channel, output);
				else
					MessageUtil.sendTextMessage(channel, CurrencyMessages.M_SEARCH_NORESULTS.get());
				
			}
		}
	}
	
	private static String getItemName(ItemStack item, String customName)
	{
		if(item.isEmpty())
			return "";
		//Ignore custom names on purchases
		StringBuffer itemName = new StringBuffer();
		if(customName.isEmpty())
			itemName.append(item.getDisplayName().getString());
		else
			itemName.append("*").append(customName).append("*");
		//Get enchantment data (if present)
		AtomicBoolean firstEnchantment = new AtomicBoolean(true);
		EnchantmentHelper.getEnchantments(item).forEach((enchantment, level) ->{
			if(firstEnchantment.get())
			{
				itemName.append(" [").append(enchantment.getFullname(level).getString());
				firstEnchantment.set(false);
			}
			else
				itemName.append(", ").append(enchantment.getFullname(level).getString());
		});
		if(!firstEnchantment.get()) //If an enchantment was gotten, append the end
			itemName.append("]");
		
		return itemName.toString();
	}
	
	private static String getItemNamesAndCount(ItemStack item1, String customName1, ItemStack item2, String customName2)
	{
		if(item1.isEmpty() && !item2.isEmpty())
			return item2.getCount() + "x " + getItemName(item2, customName2);
		if(item2.isEmpty() && !item1.isEmpty())
			return item1.getCount() + "x " + getItemName(item1, customName1);
		return item1.getCount() + "x " + getItemName(item1, customName1) + " and " + item2.getCount() + "x " + getItemName(item2, customName2);
	}
	
	@SubscribeEvent
	public void onTradeCarriedOut(PostTradeEvent event)
	{
		try {
			PlayerReference recipient = event.getTrader().getCoreSettings().getOwner();
			if(event.getTrader().getCoreSettings().getTeam() != null)
			{
				recipient = event.getTrader().getCoreSettings().getTeam().getOwner();
			}
			LinkedAccount account = AccountManager.getLinkedAccountFromPlayerID(recipient.id);
			if(account != null)
			{
				User linkedUser = this.getJDA().getUserById(account.discordID);
				if(AccountManager.currencyNotificationsEnabled(linkedUser))
				{
					if(event.getTrade() instanceof ItemTradeData)
					{
						ItemTradeData itemTrade = (ItemTradeData)event.getTrade();
						StringBuffer message = new StringBuffer();
						//Customer name
						message.append(event.getPlayerReference().lastKnownName());
						//Action (bought, sold, ???)
						switch(itemTrade.getTradeType())
						{
						case SALE: message.append(" bought "); break;
						case PURCHASE: message.append(" sold "); break;
						case BARTER: message.append( "bartered "); break;
							default: message.append(" ??? ");
						}
						if(itemTrade.isBarter())
						{
							//Item given
							message.append(getItemNamesAndCount(itemTrade.getBarterItem(0), "", itemTrade.getBarterItem(1), "")).append(" for ");
							//Item bought
							message.append(getItemNamesAndCount(itemTrade.getSellItem(0), itemTrade.getCustomName(0), itemTrade.getSellItem(1), itemTrade.getCustomName(1)));
						}
						else
						{
							//Item bought/sold
							if(itemTrade.isSale())
								message.append(getItemNamesAndCount(itemTrade.getSellItem(0), itemTrade.getCustomName(0), itemTrade.getSellItem(1), itemTrade.getCustomName(1)));
							else
								message.append(getItemNamesAndCount(itemTrade.getSellItem(0), "", itemTrade.getSellItem(1), ""));
							//Price
							message.append(" for ");
							if(event.getPricePaid().isFree() || event.getPricePaid().getRawValue() <= 0)
								message.append("free");
							else
								message.append(event.getPricePaid().getString());
						}
						//From trader name
						message.append(" from your ").append(event.getTrader().getName().getString());
						
						//Send the message directly to the linked user
						//Create as pending message to avoid message spamming them when a player buys a ton of the same item
						this.addPendingMessage(linkedUser, message.toString());
						//MessageUtil.sendPrivateMessage(linkedUser, message.toString());
						
						//Check if out of stock
						if(event.getTrader() instanceof IItemTrader)
						{
							if(itemTrade.stockCount((IItemTrader)event.getTrader()) < 1)
							{
								this.addPendingMessage(linkedUser, CurrencyMessages.M_NOTIFICATION_OUTOFSTOCK.get());
								//MessageUtil.sendPrivateMessage(linkedUser, "**This trade is now out of stock!**");
							}
						}
					}
					else
					{
						MinecraftForge.EVENT_BUS.post(new DiscordPostTradeEvent(event, (message) -> this.addPendingMessage(linkedUser, message)));
					}
				}
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	
	public void addPendingMessage(User user, String message)
	{
		String userId = user.getId();
		List<String> pendingMessages = this.pendingMessages.containsKey(userId) ? this.pendingMessages.get(userId) : Lists.newArrayList();
		pendingMessages.add(message);
		this.pendingMessages.put(userId, pendingMessages);
	}
	
	public void addPendingMessage(User user, List<String> messages)
	{
		String userId = user.getId();
		List<String> pendingMessages = this.pendingMessages.containsKey(userId) ? this.pendingMessages.get(userId) : Lists.newArrayList();
		messages.forEach(message -> pendingMessages.add(message));
		this.pendingMessages.put(userId, pendingMessages);
	}
	
	public void sendPendingMessages()
	{
		//LightmansConsole.LOGGER.info("Sending Pending Messages");
		this.pendingMessages.forEach((userId, messages)->{
			try {
				User user = this.getJDA().getUserById(userId);
				if(user != null)
				{
					MessageUtil.sendPrivateMessage(user, messages);
				}	
			} catch(Exception e) { e.printStackTrace(); }
		});
		this.pendingMessages.clear();
	}
	
	@SubscribeEvent
	public void onUniversalTraderRegistered(UniversalTradeCreateEvent event)
	{
		//Announce the creation of the trader 60s later
		new Timer().schedule(new AnnouncementTask(this, event), ANNOUCEMENT_DELAY);
	}
	
	@SubscribeEvent
	public void onServerStop(ServerStoppingEvent event)
	{
		//Cancel the timer
		this.timer.cancel();
		this.sendPendingMessages();
	}
	
	private static class NotifyTraderOwnerTask extends TimerTask
	{
		private final CurrencyListener cl;
		public NotifyTraderOwnerTask(CurrencyListener cl) { this.cl = cl; }
		@Override
		public void run() { this.cl.sendPendingMessages(); }
	}

	private static class AnnouncementTask extends TimerTask
	{
		
		private final CurrencyListener cl;
		private final UniversalTradeCreateEvent event;

		public AnnouncementTask(CurrencyListener cl, UniversalTradeCreateEvent event) {
			this.cl = cl;
			this.event = event;
		}
		
		@Override
		public void run() {
			try {
				if(this.event.getData() == null) //Abort if the trader was removed.
					return;
				if(event.getData().getCoreSettings().hasCustomName())
					cl.sendTextMessage(CurrencyMessages.M_NEWTRADER_NAMED.format(this.event.getData().getCoreSettings().getOwnerName(), event.getData().getCoreSettings().getCustomName()));
				else
					cl.sendTextMessage(CurrencyMessages.M_NEWTRADER.format(this.event.getData().getCoreSettings().getOwnerName()));
			} catch(Exception e) { e.printStackTrace(); }
		}
		
	}
	
}