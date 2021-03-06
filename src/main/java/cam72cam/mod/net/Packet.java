package cam72cam.mod.net;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.world.World;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class Packet {
    private static final SimpleNetworkWrapper net = NetworkRegistry.INSTANCE.newSimpleChannel("cam72cam.mod");
    private static Map<String, Supplier<Packet>> types = new HashMap<>();

    static {
        net.registerMessage(new Packet.Handler<>(), Message.class, 0, Side.CLIENT);
        net.registerMessage(new Packet.Handler<>(), Message.class, 1, Side.SERVER);
    }

    MessageContext ctx;
    private TagCompound data;

    public static void register(Supplier<Packet> sup, PacketDirection dir) {
        types.put(sup.get().getClass().toString(), sup);
    }

    protected abstract void handle();

    protected final World getWorld() {
        return getPlayer().getWorld();
    }

    protected final Player getPlayer() {
        return ctx.side == Side.CLIENT ? MinecraftClient.getPlayer() : new Player(ctx.getServerHandler().player);
    }

    public void sendToAllAround(World world, Vec3d pos, double distance) {
        net.sendToAllAround(new Message(this),
                new NetworkRegistry.TargetPoint(world.getId(), pos.x, pos.y, pos.z, distance));
    }

    public void sendToServer() {
        net.sendToServer(new Message(this));
    }

    public void sendToAll() {
        net.sendToAll(new Message(this));
    }

    public static class Message implements IMessage {
        Packet packet;

        public Message() {
            // FORGE REFLECTION
        }

        public Message(Packet pkt) {
            this.packet = pkt;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            TagCompound data = new TagCompound(ByteBufUtils.readTag(buf));
            String cls = data.getString("cam72cam.mod.pktid");
            packet = types.get(cls).get();
            packet.data = data;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            TagCompound data = new TagCompound();
            data.setString("cam72cam.mod.pktid", packet.getClass().toString());
            try {
                TagSerializer.serialize(data, packet);
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
            ByteBufUtils.writeTag(buf, data.internal);
        }
    }

    public static class Handler<T extends Message> implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
            return null;
        }

        private void handle(T message, MessageContext ctx) {
            message.packet.ctx = ctx;
            try {
                TagSerializer.deserialize(message.packet.data, message.packet, message.packet.getWorld());
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
            message.packet.handle();
        }
    }
}
