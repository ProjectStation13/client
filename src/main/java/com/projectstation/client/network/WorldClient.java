package com.projectstation.client.network;

import com.jevaengine.spacestation.ui.selectclass.CharacterClassDescription;
import com.projectstation.client.ClientStationEntityFactory;
import com.projectstation.client.network.entity.ClientNetworkEntityMappings;
import com.projectstation.network.command.server.NewChatMessage;
import com.projectstation.network.command.server.ServerSelectRole;
import com.projectstation.network.entity.IClientEntityNetworkAdapter;
import com.projectstation.network.*;
import com.projectstation.network.command.server.ServerGetTime;
import com.projectstation.network.entity.IEntityNetworkAdapter;
import com.projectstation.network.entity.IEntityNetworkAdapterFactory;
import com.projectstation.network.entity.EntityConfigurationDetails;
import com.projectstation.network.command.server.ServerJoinWorldRequest;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.IParallelEntityFactory;
import io.github.jevaengine.world.physics.IPhysicsWorldFactory;
import io.netty.channel.*;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WorldClient {
    private static final Logger logger = LoggerFactory.getLogger(WorldClient.class);

    private static final int TIME_RESET_INTERVAL = 5000; //Every 1 seconds
    private int lastTimeReset = 0;

    private World world;
    private final ClientNetworkEntityMappings netEntityMappings = new ClientNetworkEntityMappings();
    private final Map<String, IClientEntityNetworkAdapter> entityNetworkAdapters = new HashMap<>();

    private final Queue<QueuedMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private final NetworkMessageQueue<IServerVisit> writeQueue = new NetworkMessageQueue<>();
    private final HashSet<IClientPollable> pollRequests = new HashSet<>();
    private final HashMap<String, String> nicknameMapping = new HashMap<>();
    private final List<String> chatQueue = new ArrayList<>();

    private final IPhysicsWorldFactory physicsWorldFactory;
    private final IParallelEntityFactory parallelEntityFactory;
    private final IEffectMapFactory effectMapFactory;
    private final IItemFactory itemFactory;

    private final IEntityFactory entityFactory;

    private final WorldClientHandler clientHandler = new WorldClientHandler();
    private final VisitableWorldClientHandler visitable = new VisitableWorldClientHandler();

    private List<CharacterClassDescription> availableRoles = new ArrayList<>();
    private boolean isAwaitingRoleSelect = false;

    private long serverTimeDelta = 0;

    private String playerEntity;

    private Observers observers = new Observers();

    EventLoopGroup workerGroup = new NioEventLoopGroup();

    private String lastDisconnectReason = "Unknown.";

    public WorldClient(String nickname, IItemFactory itemFactory, IPhysicsWorldFactory physicsWorldFactory, IParallelEntityFactory parallelEntityFactory, IEffectMapFactory effectMapFactory, IEntityFactory entityFactory, String host, int port) {
        this.entityFactory = entityFactory;
        this.itemFactory = itemFactory;

        this.physicsWorldFactory = physicsWorldFactory;
        this.parallelEntityFactory = parallelEntityFactory;
        this.effectMapFactory = effectMapFactory;

        initNetwork(host, port);

        send(new ServerJoinWorldRequest(nickname));
        send(new ServerGetTime(System.nanoTime() / 1000000));
    }

    public void send(IServerVisit visit) {
        writeQueue.queue(visit);
    }

    public IObserverRegistry getObservers() {
        return observers;
    }

    public boolean isConnected() {
        return clientHandler.isConnected();
    }

    @Nullable
    public String getPlayerEntity() {
        return playerEntity;
    }

    @Nullable
    public World getWorld() {
        return world;
    }

    public List<String> pollChatMessages() {
        List<String> chatMessages = new ArrayList<>(chatQueue);
        chatQueue.clear();

        return chatMessages;
    }

    public void sendChatMessage(String message) {
        clientHandler.send(new NewChatMessage(message));
        clientHandler.flush();
    }

    public CharacterClassDescription[] getAvailableRoles() {
        return availableRoles.toArray(new CharacterClassDescription[0]);
    }

    public boolean isAwaitingRoleSelect() {
        return isAwaitingRoleSelect;
    }

    public void selectRole(CharacterClassDescription role) {
        clientHandler.send(new ServerSelectRole(role));
        clientHandler.flush();
        isAwaitingRoleSelect = false;
    }

    public Map<String, String> getNicknameMapping() {
        return Collections.unmodifiableMap(nicknameMapping);
    }

    private void initNetwork(String host, int port) {
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
                            p.addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));

                            p.addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    clientHandler);
                        }
                    });

            ChannelFuture channel = b.connect(host, port);
            channel.await();

            if(!channel.isSuccess())
                logger.error("Unable to establish connection", channel.cause());

        } catch (InterruptedException ex) {
            logger.error("Unable to establish connection", ex);
            Thread.currentThread().interrupt();
        }

        if(!clientHandler.isConnected()) {
            lastDisconnectReason = "Unable to establish a connection with the host. The host may not be accessible or available or the server may be full.";
        }
    }

    public void stop() {
        workerGroup.shutdownGracefully();
    }

    public void update(int deltaTime) {

        lastTimeReset += deltaTime;

        clientHandler.poll();

        NetworkMessageQueue<IServerVisit> delta = new NetworkMessageQueue<>();
        Set<IClientEntityNetworkAdapter> oldPr = new HashSet(pollRequests);
        pollRequests.clear();
        for(IClientEntityNetworkAdapter e : oldPr) {
            try {
                for(IServerVisit v : e.poll(deltaTime))
                    delta.queue(v);

            } catch (NetworkPollException ex) {
                logger.error("Unable to poll delta of entity.", ex);
            }
        }

        if(!delta.isEmpty()) {
            while (!delta.isEmpty()) {
                clientHandler.send(delta.poll());
            }
            clientHandler.flush();
        }

        while (!messageQueue.isEmpty()) {
            QueuedMessage msg = messageQueue.remove();

            try {
                List<IServerVisit> response = msg.visit.visit(visitable);

                if(!response.isEmpty()) {
                    for (IServerVisit v : response) {
                        msg.ctx.write(v);
                    }

                    msg.ctx.flush();
                }

            } catch (VisitException ex) {
                logger.error("World visit message failed.", ex);
            }
        }


        if(lastTimeReset >= TIME_RESET_INTERVAL) {
            lastTimeReset = 0;
            clientHandler.send(new ServerGetTime(System.nanoTime() / 1000000));
            clientHandler.flush();
        }
    }

    private void registerNetworkEntity(IEntity e) {
        EntityConfigurationDetails details = ClientStationEntityFactory.getConfig(e);

        IEntityNetworkAdapterFactory factory = netEntityMappings.get(e.getClass());
        if (factory != null) {
            if (details == null) {
                logger.info("Synchronized entity has no configuration details.");
                details = new EntityConfigurationDetails(entityFactory.lookup(e.getClass()));
            }

            IClientEntityNetworkAdapter net = createNetworkAdapter(e.getClass(), e, details, new NetworkEntityAdapterHost(e.getInstanceName()));
            if (net != null) {
                entityNetworkAdapters.put(e.getInstanceName(), net);
                pollRequests.add(net);
            } else {
                logger.error("Unable to create network adapter for entity " + e.getInstanceName() +", Not synchronizing.");
            }
        }
    }

    private <T extends IEntity> IClientEntityNetworkAdapter createNetworkAdapter(Class<T> cls, Object entity, EntityConfigurationDetails config, IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost host) {
        return netEntityMappings.get(cls).create((T)entity, config, host);
    }


    private void unregisterNetworkEntity(IEntity e) {
        entityNetworkAdapters.remove(e.getInstanceName());
    }

    public String getDisconnectReason() {
        return lastDisconnectReason;
    }

    private class NetworkEntityAdapterHost implements IEntityNetworkAdapterFactory.IEntityNetworkAdapterHost {
        private final String name;

        public NetworkEntityAdapterHost(String name) {
            this.name = name;
        }

        @Override
        public void poll() {
            if(entityNetworkAdapters.containsKey(name))
                pollRequests.add(entityNetworkAdapters.get(name));
        }

        @Override
        public boolean isOwner() {
            return visitable.isOwner(name);
        }
    }

    private class VisitableWorldClientHandler implements IClientWorldHandler {

        private final Set<String> ownedEntities = new HashSet<>();

        @Override
        public void setEntityNickname(String instanceName, String nickname) {
            nicknameMapping.put(instanceName, nickname);
        }

        @Override
        public void setPlayerEntity(String name) {
            playerEntity = name;
            observers.raise(WorldClientListener.class).playerEntitySet(name);
        }

        public void setWorld(World world) {
            WorldClient.this.world = world;
            world.getObservers().add(new WorldObserver());
            observers.raise(WorldClientListener.class).worldSet(world);
        }

        public boolean hasWorld() {
            return WorldClient.this.world != null;
        }

        public World getWorld() {
            if(!hasWorld())
                throw new RuntimeException("Attempted to access client world when it is not present.");

            return WorldClient.this.world;
        }

        @Override
        public void requestRoleSelect(List<CharacterClassDescription> available) {
            availableRoles = new ArrayList<>(available);
            isAwaitingRoleSelect = true;
        }

        @Override
        public IEntityFactory getEntityFactory() {
            return entityFactory;
        }

        @Override
        public IPhysicsWorldFactory getPhysicsWorldFactory() {
            return physicsWorldFactory;
        }

        @Override
        public IEffectMapFactory getEffectMapFactory() {
            return effectMapFactory;
        }

        @Override
        public IParallelEntityFactory getParallelEntityFactory() {
            return parallelEntityFactory;
        }

        @Override
        public IItemFactory getItemFactory() {
            return itemFactory;
        }

        @Override
        public void setServerTime(long time) {

            serverTimeDelta = time - System.nanoTime() / 1000000;
        }

        @Override
        public long getServerTime() {
            return serverTimeDelta + System.nanoTime() / 1000000;
        }

        @Override
        public void giveOwnership(String entityName) {
            ownedEntities.add(entityName);
        }

        @Override
        public void revokeOwnership(String entityName) {
            ownedEntities.remove(entityName);
        }

        @Override
        public boolean isOwner(String entityName) {
            return ownedEntities.contains(entityName);
        }

        @Override
        public IEntityNetworkAdapter getAdapter(String entityName) {
            return entityNetworkAdapters.get(entityName);
        }

        @Override
        public void disconnect(String reason) {
            clientHandler.disconnect(reason);
        }

        @Override
        public void recieveChatMessage(String message) {
            chatQueue.add(message);
        }
    }

    public interface WorldClientListener {
        void worldSet(World world);
        void playerEntitySet(String name);
    }

    class WorldClientHandler extends SimpleChannelInboundHandler<IClientVisit> {

        private ChannelHandlerContext ctx = null;

        public void disconnect(String reason) {
            if(ctx != null) {
                lastDisconnectReason = reason;
                ctx.disconnect();
            }
        }

        public void send(IServerVisit msg) {
            if(ctx == null)
                return;

            ctx.write(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(!channelFuture.isSuccess())
                        logger.error("Error transmitting message", channelFuture.cause());
                }
            });
        }

        public void flush() {
            if(ctx != null)
                ctx.flush();
        }

        public void poll() {
            if(ctx == null || writeQueue.isEmpty())
                return;

            while(!writeQueue.isEmpty()) {

                IServerVisit msg = writeQueue.poll();
                send(msg);
            }

            ctx.flush();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            if(ctx == this.ctx)
                this.ctx = null;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            this.ctx = ctx;
        }

        @Override
        protected void messageReceived(ChannelHandlerContext channelHandlerContext, IClientVisit clientVisit) throws Exception {
            messageQueue.add(new QueuedMessage(channelHandlerContext, clientVisit));
        }

        public boolean isConnected() {
            return ctx != null && !ctx.isRemoved();
        }
    }

    private class WorldObserver implements World.IWorldObserver {
        @Override
        public void addedEntity(IEntity e) {
            registerNetworkEntity(e);
        }

        @Override
        public void removedEntity(Vector3F location, IEntity e) {
            unregisterNetworkEntity(e);
        }
    }

    private static class QueuedMessage {
        public ChannelHandlerContext ctx;
        public IClientVisit visit;

        public QueuedMessage(ChannelHandlerContext ctx, IClientVisit visit) {
            this.ctx = ctx;
            this.visit = visit;
        }
    }
}
