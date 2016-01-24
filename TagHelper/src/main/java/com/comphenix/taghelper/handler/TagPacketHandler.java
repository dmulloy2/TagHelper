package com.comphenix.taghelper.handler;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.taghelper.ReceiveNameTagEvent;

public class TagPacketHandler extends PacketAdapter {
	private PluginManager pluginManager;

	/**
	 * Construct a packet handler with the given parent plugin.
	 * 
	 * @param plugin parent plugin.
	 */
	public TagPacketHandler(Plugin plugin, PluginManager pluginManager) {
		super(plugin, PacketType.Play.Server.PLAYER_INFO);
		this.pluginManager = pluginManager;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		PacketContainer packet = event.getPacket();
		if (packet.getPlayerInfoAction().read(0) != PlayerInfoAction.ADD_PLAYER) {
			return;
		}

		List<PlayerInfoData> newPlayerInfo = new ArrayList<PlayerInfoData>();
		for (PlayerInfoData playerInfo : packet.getPlayerInfoDataLists().read(0)) {
			Player player;
			if (playerInfo == null || playerInfo.getProfile() == null || (player = plugin.getServer().getPlayer(playerInfo.getProfile().getUUID())) == null) {
				// Unknown Player
				newPlayerInfo.add(playerInfo);
				continue;
			}

			WrappedGameProfile oldProfile = playerInfo.getProfile();

			ReceiveNameTagEvent tagEvent = new ReceiveNameTagEvent(event.getPlayer(), player, oldProfile.getName());
			pluginManager.callEvent(tagEvent);

			if (tagEvent.isModified()) {
				String tag = tagEvent.getTrimmedTag();
				if (tag == null)
					tag = "";

				WrappedGameProfile profile = new WrappedGameProfile(oldProfile.getUUID(), tag);
				PlayerInfoData newData = new PlayerInfoData(profile, playerInfo.getPing(), playerInfo.getGameMode(), playerInfo.getDisplayName());
				newPlayerInfo.add(newData);
			} else {
				newPlayerInfo.add(playerInfo);
			}
		}

		packet.getPlayerInfoDataLists().write(0, newPlayerInfo);
	}
}