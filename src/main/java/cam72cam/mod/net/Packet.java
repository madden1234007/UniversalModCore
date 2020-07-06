package cam72cam.mod.net;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.world.World;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

import java.util.function.Supplier;

public abstract class Packet {
    private static MinecraftServer server;
    static {
        // Probably should not do this...
        ServerTickCallback.EVENT.register(server -> Packet.server = server);
    }

    private TagCompound data;
    private Player player;
    private Supplier<World> world;

    public static void register(Supplier<Packet> sup, PacketDirection dir) {
        Identifier ident = sup.get().getIdent();
        switch (dir) {
            case ClientToServer:
                ServerSidePacketRegistry.INSTANCE.register(ident, (ctx, buffer) -> {
                    Packet packet = sup.get();
                    packet.data = new TagCompound(buffer.readCompoundTag());
                    try {
                        TagSerializer.deserialize(packet.data, packet);
                    } catch (SerializationException e) {
                        ModCore.catching(e);
                    }
                    packet.player = new Player(ctx.getPlayer());
                    packet.world = () -> packet.player.getWorld();
                    ctx.getTaskQueue().execute(packet::handle);
                });
                break;
            case ServerToClient:
                if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
                    return;
                }
                ClientSidePacketRegistry.INSTANCE.register(ident, (ctx, buffer) -> {
                    Packet packet = sup.get();
                    packet.data = new TagCompound(buffer.readCompoundTag());
                    try {
                        TagSerializer.deserialize(packet.data, packet);
                    } catch (SerializationException e) {
                        ModCore.catching(e);
                    }
                    packet.world = () -> World.get(ctx.getPlayer().getEntityWorld());
                    packet.player = new Player(ctx.getPlayer());
                    ctx.getTaskQueue().execute(packet::handle);
                });
                break;
        }
    }

    protected Identifier getIdent() {
        return new Identifier(ModCore.MODID, getClass().getName().replace("$", "_").toLowerCase());
    }

    protected abstract void handle();

    protected final World getWorld() {
        return world.get();
    }

    protected final Player getPlayer() {
        return player;
    }

    protected PacketByteBuf getBuffer() {
        PacketByteBuf buff = new PacketByteBuf(Unpooled.buffer());
        data = new TagCompound();
        try {
            TagSerializer.serialize(data, this);
        } catch (SerializationException e) {
            ModCore.catching(e);
        }
        buff.writeCompoundTag(data.internal);
        return buff;
    }


    public void sendToAllAround(World world, Vec3d pos, double distance) {
        PlayerStream.around(world.internal, pos.internal, distance).forEach(x -> ((ServerPlayerEntity)x).networkHandler.sendPacket(new CustomPayloadS2CPacket(getIdent(), getBuffer())));
    }

    public void sendToServer() {
        ClientSidePacketRegistry.INSTANCE.sendToServer(new CustomPayloadC2SPacket(getIdent(), getBuffer()));
    }

    public void sendToAll() {
        if (server == null) {
            return;
        }
        server.getPlayerManager().sendToAll(new CustomPayloadS2CPacket(getIdent(), getBuffer()));
    }

    public void sendToPlayer(Player player) {
        if (server == null) {
            return;
        }
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player.internal, new CustomPayloadS2CPacket(getIdent(), getBuffer()));
    }
}
