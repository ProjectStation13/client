/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.projectstation.client.gamestates;

import com.jevaengine.spacestation.IState;
import com.jevaengine.spacestation.IStateContext;
import com.jevaengine.spacestation.StationProjectionFactory;
import com.jevaengine.spacestation.entity.IInteractableEntity;
import com.jevaengine.spacestation.entity.character.SpaceCharacter;
import com.jevaengine.spacestation.gamestates.MainMenu;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import com.jevaengine.spacestation.ui.*;
import com.jevaengine.spacestation.ui.HudFactory.Hud;
import com.jevaengine.spacestation.ui.InventoryHudFactory.InventoryHud;
import com.jevaengine.spacestation.ui.LoadoutHudFactory.LoadoutHud;
import com.jevaengine.spacestation.ui.playing.ConsoleInterfaceInteractionHandler;
import com.jevaengine.spacestation.ui.playing.PlayingWindowFactory;
import com.jevaengine.spacestation.ui.playing.PlayingWindowFactory.PlayingWindow;
import com.jevaengine.spacestation.ui.playing.WorldInteractionBehaviorInjector;
import com.projectstation.client.network.WorldClient;
import com.projectstation.client.network.entity.interaction.DoorInteractionHandler;
import com.projectstation.client.network.ui.ChatHudFactory;
import com.projectstation.client.network.ui.ClientHudFactory;
import com.projectstation.client.network.ui.ClientInventoryHudFactory;
import com.projectstation.client.network.ui.ClientLoadoutHudFactory;
import com.projectstation.network.command.server.ServerWorldVisit;
import com.projectstation.network.command.world.CharacterInteractedWith;
import com.projectstation.network.command.world.CharacterPerformedInteraction;
import com.projectstation.network.command.world.SetEntityVelocityCommand;
import com.projectstation.network.command.world.UseItemInHandsAtCommand;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.*;
import io.github.jevaengine.math.*;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter.NullRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.ISceneBuffer;
import io.github.jevaengine.world.scene.ISceneBufferFactory;
import io.github.jevaengine.world.scene.TopologicalOrthographicProjectionSceneBufferFactory;
import io.github.jevaengine.world.scene.camera.FollowCamera;
import io.github.jevaengine.world.scene.model.IActionSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public class Playing implements IState {

	private static final URI NICKNAME_FONT = URI.create("file:///ui/font/space/tiny/font.juif");
	private static final URI LEM_DISPLAY_WINDOW = URI.create("file:///ui/windows/dcpu/lem/layout.jwl");

	private static final float CAMERA_ZOOM = 2.5f;

	private IStateContext m_context;
	private final World m_world;
	private PlayingWindow m_playingWindow;

	private final Logger m_logger = LoggerFactory.getLogger(Playing.class);

	private SpaceCharacter m_player = null;

	private Hud m_hud;
	private LoadoutHud m_loadoutHud;
	private InventoryHud m_inventoryHud;

	private final String m_playerEntityName;
	private final WorldClient m_client;

	private IFont m_nicknameFont = new NullFont();
	private ChatHudFactory.ChatHud chatHud;

	public Playing(WorldClient client, String playerEntityName, World world) {
		m_world = world;
		m_client = client;
		m_playerEntityName = playerEntityName;
	}


	private WorldInteractionBehaviorInjector.IInteractionHandler[] createInteractionHandlers() {
		LemDisplayFactory lemDisplayFactory = new LemDisplayFactory(m_context.getWindowManager(), m_context.getWindowFactory(), LEM_DISPLAY_WINDOW);
		return new WorldInteractionBehaviorInjector.IInteractionHandler[] {
				new ConsoleInterfaceInteractionHandler(lemDisplayFactory),
				new DoorInteractionHandler(m_client)
		};

	}

	@Override
	public void enter(IStateContext context) {
		m_context = context;

		try {
			m_nicknameFont = context.getFontFactory().create(NICKNAME_FONT);
		} catch (IFontFactory.FontConstructionException ex) {
			m_logger.error("Unable to create nickname font, using null font for nicknames.", ex);
		}

		try {
			ISceneBufferFactory sceneBufferFactory = new TopologicalOrthographicProjectionSceneBufferFactory(new StationProjectionFactory().create());
			FollowCamera camera = new FollowCamera(sceneBufferFactory);
			//camera.addEffect(new NicknameEffect());
			camera.addEffect(new NicknameEffect2());
			camera.setZoom(CAMERA_ZOOM);

			SpaceCharacter playerEntityBuffer = m_world.getEntities().getByName(SpaceCharacter.class, m_playerEntityName);

			if (playerEntityBuffer != null) {
				m_player = playerEntityBuffer;
			} else {
				m_logger.error("Character entity was not placed in world.");
				return;
			}

			camera.attach(m_world);
			camera.setTarget(m_player);

			Vector2D resolution = context.getWindowManager().getResolution();
			m_hud = new ClientHudFactory(context.getWindowManager(), context.getWindowFactory()).create(m_client, m_player, m_player.getInventory(), m_player.getLoadout());
			m_hud.setTopMost(true);
			m_hud.setMovable(false);
			m_hud.center();
			m_hud.setLocation(new Vector2D(m_hud.getLocation().x, resolution.y - m_hud.getBounds().height));
			
			m_loadoutHud = new ClientLoadoutHudFactory(context.getWindowManager(), context.getWindowFactory()).create(m_client, m_player);
			m_loadoutHud.setMovable(false);
			m_loadoutHud.setTopMost(true);
			m_loadoutHud.setVisible(false);
			m_loadoutHud.center();
			m_loadoutHud.setLocation(new Vector2D(m_loadoutHud.getLocation().x, resolution.y - m_hud.getBounds().height - m_loadoutHud.getBounds().height));
			
			m_inventoryHud = new ClientInventoryHudFactory(context.getWindowManager(), context.getWindowFactory()).create(m_client, m_player.getLoadout(), m_player.getInventory(), m_player);
			m_inventoryHud.setMovable(false);
			m_inventoryHud.setTopMost(true);
			m_inventoryHud.setVisible(false);
			m_inventoryHud.setLocation(new Vector2D(m_loadoutHud.getLocation().x + m_loadoutHud.getBounds().width + 10,
												  m_loadoutHud.getLocation().y));

			m_hud.getObservers().add(new HudFactory.IHudObserver() {
				@Override
				public void movementSpeedChanged(boolean isRunning) { }

				@Override
				public void inventoryViewChanged(boolean isVisible) {
					m_loadoutHud.setVisible(isVisible);
					m_inventoryHud.setVisible(isVisible);
				}
			});

			CharacterStatusHudFactory.StatusHud hud = new CharacterStatusHudFactory(context.getWindowManager(), context.getWindowFactory()).create(m_player.getAttributes(), m_player.getStatusResolver());
			int y = context.getWindowManager().getResolution().y / 2 - hud.getBounds().height;
			hud.setMovable(false);
			hud.setTopMost(true);
			hud.setVisible(true);
			hud.setLocation(new Vector2D(context.getWindowManager().getResolution().x - hud.getBounds().width - 20, y));

			chatHud = new ChatHudFactory(context.getWindowManager(), context.getWindowFactory()).create();
			y = context.getWindowManager().getResolution().y - chatHud.getBounds().height - 20;
			chatHud.setMovable(false);
			chatHud.setTopMost(true);
			chatHud.setVisible(true);
			chatHud.setLocation(new Vector2D(20, y));
			chatHud.getObservers().add(new ChatHudFactory.IChatHudObserver() {
				@Override
				public void sendMessage(String message) {
					m_client.sendChatMessage(message);
				}
			});


			m_playingWindow = new PlayingWindowFactory(context.getWindowManager(), context.getWindowFactory()).create(camera, m_player, createInteractionHandlers(), new PlayerActionHandler());
			m_playingWindow.center();
			m_playingWindow.focus();
			
		} catch (WindowConstructionException e) {
			m_logger.error("Error occured constructing demo world or world view. Reverting to MainMenu.", e);
			m_context.setState(new ConnectionMenu());
		}
	}

	@Override
	public void leave() {
		if (m_playingWindow != null) {
			m_playingWindow.dispose();
		}
	}

	@Override
	public void update(int deltaTime) {
		m_world.update(deltaTime);
		m_client.update(deltaTime);

		for(String s : m_client.pollChatMessages()) {
			chatHud.appendMessage(s);
		}
	}

	private class NicknameEffect2 implements ISceneBuffer.ISceneBufferEffect {
		@Override
		public IRenderable getUnderlay(Vector2D translation, Rect2D bounds, Matrix3X3 projection) {
			return new NullGraphic();
		}

		@Override
		public IRenderable getOverlay(Vector2D translation, Rect2D bounds, Matrix3X3 projection) {
			return new IRenderable() {
				@Override
				public void render(Graphics2D g, int x, int y, float scale) {
					for(Map.Entry<String, String> e : m_client.getNicknameMapping().entrySet()) {
						IEntity entity = m_world.getEntities().getByName(IEntity.class, e.getKey());

						if(entity != null) {
							Vector3F projectLoc = entity.getBody().getLocation().add(new Vector3F(0, -0.6f, 0));
							Vector2D location = projection.dot(projectLoc).getXy().multiply(scale).round().add(translation);
							location = location.add(new Vector2F(x, y).round());
							location.x -= m_nicknameFont.getTextBounds(e.getValue(), scale).width / 2;
							m_nicknameFont.drawText(g, location.x, location.y, scale, e.getValue());
						}
					}
				}
			};
		}

		@Override
		public ISceneBuffer.ISceneComponentEffect[] getComponentEffect(Graphics2D g, int offsetX, int offsetY, float scale, Vector2D renderLocation, Matrix3X3 projection, ISceneBuffer.ISceneBufferEntry subject, Collection<ISceneBuffer.ISceneBufferEntry> beneath) {
			return new ISceneBuffer.ISceneComponentEffect[0];
		}
	}

	private class PlayerActionHandler implements WorldInteractionBehaviorInjector.IActionHandler {
		@Override
		public void handleUseItem(IRpgCharacter character, IItem item, IItem.ItemTarget target) {
			if(target.getTarget(IItem.class) != null) {
				m_client.send(new ServerWorldVisit(new UseItemInHandsAtCommand(character.getInstanceName(), target.getTargetLocation())));
			} else if(target.getTarget(IEntity.class) != null) {
				IEntity e = target.getTarget(IEntity.class);
				m_client.send(new ServerWorldVisit(new UseItemInHandsAtCommand(character.getInstanceName(), e.getInstanceName(), target.getTargetLocation())));
			} else {
				m_client.send(new ServerWorldVisit(new UseItemInHandsAtCommand(character.getInstanceName(), target.getTargetLocation())));
			}
		}

		@Override
		public void interactedWith(IRpgCharacter character, @Nullable IEntity subject) {
			if(subject != null)
				m_client.send(new ServerWorldVisit(new CharacterInteractedWith(subject.getInstanceName(), character.getInstanceName())));
			else
				m_client.send(new ServerWorldVisit(new CharacterPerformedInteraction(character.getInstanceName(), character.getBody().getLocation(), character.getBody().getDirection())));
		}
	}
}
