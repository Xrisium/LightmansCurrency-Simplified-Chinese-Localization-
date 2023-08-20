package io.github.lightman314.lightmanscurrency.network.message.trader;

import io.github.lightman314.lightmanscurrency.common.menus.providers.TerminalMenuProvider;
import io.github.lightman314.lightmanscurrency.common.menus.validation.IValidatedMenu;
import io.github.lightman314.lightmanscurrency.common.menus.validation.MenuValidator;
import io.github.lightman314.lightmanscurrency.common.menus.validation.types.SimpleValidator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class MessageOpenNetworkTerminal {

	public MessageOpenNetworkTerminal() { }

	public static void handle(MessageOpenNetworkTerminal message, Supplier<Context> supplier) {
		supplier.get().enqueueWork(() ->
		{
			ServerPlayer player = supplier.get().getSender();
			if(player != null)
			{
				MenuValidator validator = SimpleValidator.NULL;
				if(player.containerMenu instanceof IValidatedMenu menu)
					validator = menu.getValidator();
				TerminalMenuProvider.OpenMenu(player, validator);
			}
		});
		supplier.get().setPacketHandled(true);
	}

}