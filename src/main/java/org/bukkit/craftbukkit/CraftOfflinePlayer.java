package org.bukkit.craftbukkit;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SerializableAs("Player")
public class CraftOfflinePlayer implements OfflinePlayer, ConfigurationSerializable {
    private final GameProfile profile;
    private final CraftServer server;
    private final net.minecraft.world.storage.SaveHandler storage;

    protected CraftOfflinePlayer(CraftServer server, GameProfile profile) {
        this.server = server;
        this.profile = profile;
        this.storage = (net.minecraft.world.storage.SaveHandler) (server.console.worlds.get(0).getSaveHandler());
    }

    public GameProfile getProfile() {
        return profile;
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public String getName() {
        Player player = getPlayer();
        if (player != null) {
            return player.getName();
        }

        // This might not match lastKnownName but if not it should be more correct
        if (profile.getName() != null) {
            return profile.getName();
        }

        NBTTagCompound data = getBukkitData();

        if (data != null) {
            if (data.hasKey("lastKnownName")) {
                return data.getString("lastKnownName");
            }
        }

        return null;
    }

    public UUID getUniqueId() {
        return profile.getId();
    }

    public Server getServer() {
        return server;
    }

    public boolean isOp() {
        return server.getHandle().func_152596_g(profile);
    }

    public void setOp(boolean value) {
        if (value == isOp()) {
            return;
        }

        if (value) {
            server.getHandle().func_152605_a(profile);
        } else {
            server.getHandle().func_152610_b(profile);
        }
    }

    public boolean isBanned() {
        if (getName() == null) {
            return false;
        }

        return server.getBanList(BanList.Type.NAME).isBanned(getName());
    }

    public void setBanned(boolean value) {
        if (getName() == null) {
            return;
        }

        if (value) {
            server.getBanList(BanList.Type.NAME).addBan(getName(), null, null, null);
        } else {
            server.getBanList(BanList.Type.NAME).pardon(getName());
        }
    }

    public boolean isWhitelisted() {
        return server.getHandle().func_152607_e(profile); // Cauldron
    }

    public void setWhitelisted(boolean value) {
        if (value) {
            server.getHandle().func_152601_d(profile);
        } else {
            server.getHandle().func_152597_c(profile);
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        result.put("UUID", profile.getId().toString());

        return result;
    }

    public static OfflinePlayer deserialize(Map<String, Object> args) {
        // Backwards comparability
        if (args.get("name") != null) {
        return Bukkit.getServer().getOfflinePlayer((String) args.get("name"));
    }

        return Bukkit.getServer().getOfflinePlayer(UUID.fromString((String) args.get("UUID")));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[UUID=" + profile.getId() + "]";
    }

    public Player getPlayer() {
        for (Object obj : server.getHandle().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
            if (player.getUniqueID().equals(getUniqueId())) {
                return (player.playerNetServerHandler != null) ? player.playerNetServerHandler.getPlayerB() : null; // Cauldron
            }
        }
        EntityPlayer player = net.minecraftforge.common.util.FakePlayerFactory.fakePlayers.get(profile);
        return player != null ? (Player)player.getBukkitEntity() : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof OfflinePlayer)) {
            return false;
        }

        OfflinePlayer other = (OfflinePlayer) obj;
        if ((this.getUniqueId() == null) || (other.getUniqueId() == null)) {
            return false;
        }

        return this.getUniqueId().equals(other.getUniqueId());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.getUniqueId() != null ? this.getUniqueId().hashCode() : 0);
        return hash;
    }

    private NBTTagCompound getData() {
        return storage.getPlayerData(getUniqueId().toString());
    }

    private NBTTagCompound getBukkitData() {
        NBTTagCompound result = getData();

        if (result != null) {
            if (!result.hasKey("bukkit")) {
                result.setTag("bukkit", new net.minecraft.nbt.NBTTagCompound());
            }
            result = result.getCompoundTag("bukkit");
        }

        return result;
    }

    private File getDataFile() {
        return new File(storage.getPlayerDir(), getUniqueId() + ".dat");
    }

    public long getFirstPlayed() {
        Player player = getPlayer();
        if (player != null) return player.getFirstPlayed();

        net.minecraft.nbt.NBTTagCompound data = getBukkitData();

        if (data != null) {
            if (data.hasKey("firstPlayed")) {
                return data.getLong("firstPlayed");
            } else {
                File file = getDataFile();
                return file.lastModified();
            }
        } else {
            return 0;
        }
    }

    public long getLastPlayed() {
        Player player = getPlayer();
        if (player != null) return player.getLastPlayed();

        net.minecraft.nbt.NBTTagCompound data = getBukkitData();

        if (data != null) {
            if (data.hasKey("lastPlayed")) {
                return data.getLong("lastPlayed");
            } else {
                File file = getDataFile();
                return file.lastModified();
            }
        } else {
            return 0;
        }
    }

    public boolean hasPlayedBefore() {
        return getData() != null;
    }

    public Location getBedSpawnLocation() {
        net.minecraft.nbt.NBTTagCompound data = getData();
        if (data == null) return null;

        if (data.hasKey("SpawnX") && data.hasKey("SpawnY") && data.hasKey("SpawnZ")) {
            String spawnWorld = data.getString("SpawnWorld");
            if (spawnWorld.equals("")) {
                spawnWorld = server.getWorlds().get(0).getName();
            }
            return new Location(server.getWorld(spawnWorld), data.getInteger("SpawnX"), data.getInteger("SpawnY"), data.getInteger("SpawnZ"));
        }
        return null;
    }

    public void setMetadata(String metadataKey, MetadataValue metadataValue) {
        server.getPlayerMetadata().setMetadata(this, metadataKey, metadataValue);
    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return server.getPlayerMetadata().getMetadata(this, metadataKey);
    }

    public boolean hasMetadata(String metadataKey) {
        return server.getPlayerMetadata().hasMetadata(this, metadataKey);
    }

    public void removeMetadata(String metadataKey, Plugin plugin) {
        server.getPlayerMetadata().removeMetadata(this, metadataKey, plugin);
    }
}
