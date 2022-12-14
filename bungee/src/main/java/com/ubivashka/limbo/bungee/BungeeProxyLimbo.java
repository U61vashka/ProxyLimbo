package com.ubivashka.limbo.bungee;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.ubivashka.limbo.ProxyLimbo;
import com.ubivashka.limbo.bungee.commmand.CommandRegistry;
import com.ubivashka.limbo.bungee.config.PluginConfig;
import com.ubivashka.limbo.bungee.listener.LimboInteractionListener;
import com.ubivashka.limbo.bungee.player.DefaultBungeeLimboPlayer;
import com.ubivashka.limbo.bungee.task.LimboPlayersTask;
import com.ubivashka.limbo.container.Container;
import com.ubivashka.limbo.container.SetContainer;
import com.ubivashka.limbo.nbt.resolver.CompoundTagDataResolver;
import com.ubivashka.limbo.nbt.resolver.SimpleCompoundTagDataResolver;
import com.ubivashka.limbo.nbt.type.CompoundTag;
import com.ubivashka.limbo.player.LimboPlayer;
import com.ubivashka.limbo.protocol.packet.UpdatePlayerPositionPacket;
import com.ubivashka.limbo.server.LimboServer;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeProxyLimbo extends Plugin implements ProxyLimbo, Listener {
    private final SimpleCompoundTagDataResolver compoundTagDataResolver = new SimpleCompoundTagDataResolver();
    private final Container<LimboServer> limboServerContainer = new SetContainer<>();
    private final Map<UUID, LimboPlayer> limboPlayers = new HashMap<>();
    private PluginConfig config;

    @Override
    public void onEnable() {
        ProxyLimbo.LIMBO_SINGLETON_HOLDER.setObject(this);

        config = new PluginConfig(this);

        ProxyServer.getInstance().getPluginManager().registerListener(this, new LimboInteractionListener(this));
        Protocolize.protocolRegistration()
                .registerPacket(UpdatePlayerPositionPacket.MAPPINGS, Protocol.PLAY, PacketDirection.CLIENTBOUND, UpdatePlayerPositionPacket.class);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new LimboPlayersTask(this), 5, 5, TimeUnit.SECONDS);
        new CommandRegistry(this);
    }

    @Override
    public CompoundTagDataResolver<CompoundTag> getTagDataResolver() {
        return compoundTagDataResolver;
    }

    @Override
    public Container<LimboServer> getLimboServerContainer() {
        return limboServerContainer;
    }

    @Override
    public Map<UUID, LimboPlayer> getLimboPlayers() {
        return Collections.unmodifiableMap(limboPlayers);
    }

    @Override
    public LimboPlayer findLimboPlayer(UUID uniqueId) {
        if (limboPlayers.containsKey(uniqueId))
            return limboPlayers.get(uniqueId);
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uniqueId);
        if (player == null)
            return null;
        return limboPlayers.put(uniqueId, new DefaultBungeeLimboPlayer(player));
    }

    @Override
    public LimboPlayer fetchLimboPlayer(Object object) {
        if (object instanceof LimboPlayer)
            return (LimboPlayer) object;

        if (object instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) object;
            if (limboPlayers.containsKey(player.getUniqueId()))
                return limboPlayers.get(player.getUniqueId());
            LimboPlayer limboPlayer = new DefaultBungeeLimboPlayer(player);
            limboPlayers.put(player.getUniqueId(), limboPlayer);
            return limboPlayer;
        }
        return null;
    }

    public void removeLimboPlayer(UUID uniqueId) {
        if (limboPlayers.containsKey(uniqueId))
            limboPlayers.get(uniqueId).setCurrentLimbo(null);

        limboPlayers.remove(uniqueId);
    }

    public PluginConfig getConfig() {
        return config;
    }
}

