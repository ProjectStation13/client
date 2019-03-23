package com.projectstation.client.gamestates;

import com.jevaengine.spacestation.IState;
import com.jevaengine.spacestation.IStateContext;
import com.jevaengine.spacestation.StationProjectionFactory;
import com.jevaengine.spacestation.ui.selectclass.CharacterClassDescription;
import com.jevaengine.spacestation.ui.selectclass.CharacterClassSelectWindowFactory;
import com.projectstation.client.network.WorldClient;
import com.projectstation.client.network.ui.ChatHudFactory;
import com.projectstation.client.network.ui.observing.ObservingWindowFactory;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwaitCharacterAssignment implements IState
{
    private static final float CAMERA_ZOOM = 2.5f;
    private IStateContext m_context;
    private Window m_window;

    private final Logger m_logger = LoggerFactory.getLogger(SynchronizeWithServer.class);

    private WorldClient m_client;
    private final String m_host;
    private final int m_port;
    private final World m_world;
    private ObservingWindowFactory.ObservingWindow m_observingWindow;
    private final Vector3F m_requestedObserveLocation;
    private ChatHudFactory.ChatHud m_chatHud;
    private CharacterClassSelectWindowFactory.CharacterSelectClassWindow m_classSelect;

    public AwaitCharacterAssignment(String host, int port, WorldClient client, World world) {
        this(host, port, client, world, new Vector3F());
    }

    public AwaitCharacterAssignment(String host, int port, WorldClient client, World world, Vector3F observeLocation)
    {
        m_requestedObserveLocation = new Vector3F(observeLocation);
        m_world = world;
        m_host = host;
        m_port = port;
        m_client = client;
    }

    private void createSelectClass() {
        if(m_classSelect != null)
            return;

        try {
            m_classSelect = new CharacterClassSelectWindowFactory(m_context.getWindowManager(), m_context.getWindowFactory()).create(m_context.getEntityFactory(), m_client.getAvailableRoles());
            m_classSelect.setTopMost(true);
            m_classSelect.setVisible(true);

            m_classSelect.getObservers().add(new CharacterClassSelectWindowFactory.ICharacterClassSelectObserver() {
                @Override
                public void selectedClass(CharacterClassDescription desc) {
                    m_client.selectRole(desc);
                    m_classSelect.setVisible(false);
                }
            });
        } catch (IWindowFactory.WindowConstructionException e) {
            m_logger.error("Error creating class select window.", e);
        }
    }

    private Vector3F getObserveStart() {
        if(!m_requestedObserveLocation.isZero())
            return m_requestedObserveLocation;

        IEntity[] entities = m_world.getEntities().all();

        if(entities.length == 0)
            return new Vector3F();

        Vector3F average = new Vector3F(0,0,2);

        for(IEntity e : entities) {
            average.x += e.getBody().getLocation().x;
            average.y += e.getBody().getLocation().y;
        }

        average.x /= entities.length;
        average.y /= entities.length;

        return average;
    }

    @Override
    public void enter(IStateContext context)
    {

        ISceneBufferFactory sceneBufferFactory = new TopologicalOrthographicProjectionSceneBufferFactory(new StationProjectionFactory().create());
        ControlledCamera camera = new ControlledCamera(sceneBufferFactory);
        camera.attach(m_world);
        camera.move(getObserveStart().difference(camera.getLookAt()));

        camera.setZoom(CAMERA_ZOOM);
        m_context = context;

        try {
            m_observingWindow = new ObservingWindowFactory(context.getWindowManager(), context.getWindowFactory()).create(camera);
            m_observingWindow.center();
            m_observingWindow.focus();

            int y = 0;
            m_chatHud = new ChatHudFactory(context.getWindowManager(), context.getWindowFactory()).create();
            y = context.getWindowManager().getResolution().y - m_chatHud.getBounds().height - 20;
            m_chatHud.setMovable(false);
            m_chatHud.setTopMost(true);
            m_chatHud.setVisible(true);
            m_chatHud.setLocation(new Vector2D(20, y));
            m_chatHud.getObservers().add(new ChatHudFactory.IChatHudObserver() {
                @Override
                public void sendMessage(String message) {
                    m_client.sendChatMessage(message);
                }
            });

        } catch (
            IWindowFactory.WindowConstructionException e) {
            m_logger.error("Error occured constructing demo world or world view. Reverting to MainMenu.", e);
            m_context.setState(new ConnectionMenu(m_host, m_port));
        }
    }

    @Override
    public void leave()
    {
        if(m_observingWindow != null)
        {
            m_observingWindow.dispose();
            m_chatHud.dispose();
        }

        if(m_classSelect != null) {
            m_classSelect.dispose();
        }
    }

    @Override
    public void update(int iDelta)
    {
        m_client.update(iDelta);
        if(m_client.getWorld() != null && m_client.getPlayerEntity() != null &&
                m_client.getWorld().getEntities().getByName(IRpgCharacter.class, m_client.getPlayerEntity()) != null)
        {
            m_context.setState(new Playing(m_host, m_port, m_client, m_client.getPlayerEntity(), m_world));
        }

        if(m_client.isAwaitingRoleSelect())
            createSelectClass();
        else if(m_classSelect != null) {
            m_classSelect.dispose();
            m_classSelect = null;
        }

        if(!m_client.isConnected()) {
            m_logger.error("Cannot synchronize with server, unable to establish connection.");
            String reason = m_client.getDisconnectReason();
            m_context.setState(new DisconnectedErrorDisplay(m_host, m_port, reason));
        }
    }
}
