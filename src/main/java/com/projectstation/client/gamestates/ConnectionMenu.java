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
import com.projectstation.client.network.WorldClient;
import io.github.jevaengine.FutureResult;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IParallelWorldFactory;
import io.github.jevaengine.world.IWorldFactory.WorldConstructionException;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntityFactory;
import io.github.jevaengine.world.entity.IParallelEntityFactory;
import io.github.jevaengine.world.physics.IPhysicsWorldFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ConnectionMenu implements IState
{
	private static final String HOST = "127.0.0.1";
	private static final int PORT = 7345;

	private static final URI ENTRY_MAP = URI.create("file:///world/firstStationRework.jmp");

	private IStateContext m_context;
	private Window m_window;

	private final IWindowFactory m_windowFactory;
	private final IParallelWorldFactory m_worldFactory;
	private final IAudioClipFactory m_audioClipFactory;
	private final ISpriteFactory m_spriteFactory;
	private final IEntityFactory m_entityFactory;
	private final Logger m_logger = LoggerFactory.getLogger(ConnectionMenu.class);


	private final IPhysicsWorldFactory m_physicsWorldFactory;
	private final IParallelEntityFactory m_parallelEntityFactory;
	private final IEffectMapFactory m_effectMapFactory;


	public ConnectionMenu(IPhysicsWorldFactory physicsWorldFactory, IParallelEntityFactory parallelEntityFactory, IEffectMapFactory effectMapFactory, IEntityFactory entityFactory, IWindowFactory windowFactory, IParallelWorldFactory worldFactory, IAudioClipFactory audioClipFactory, ISpriteFactory spriteFactory)
	{
		m_physicsWorldFactory = physicsWorldFactory;
		m_parallelEntityFactory = parallelEntityFactory;
		m_effectMapFactory = effectMapFactory;

		m_entityFactory = entityFactory;
		m_windowFactory = windowFactory;
		m_worldFactory = worldFactory;
		m_audioClipFactory = audioClipFactory;
		m_spriteFactory = spriteFactory;
	}
	
	@Override
	public void enter(IStateContext context)
	{
		m_context = context;

		try
		{
			m_window = m_windowFactory.create(URI.create("file:///ui/windows/connectionmenu.jwl"), new MainMenuBehaviourInjector());
			context.getWindowManager().addWindow(m_window);
			m_window.center();
		} catch (WindowConstructionException e)
		{
			m_logger.error("Error constructing demo menu window", e);
		}
		
		
	}

	@Override
	public void leave()
	{
		if(m_window != null)
		{
			m_context.getWindowManager().removeWindow(m_window);
			m_window.dispose();
		}
	}

	@Override
	public void update(int iDelta) { }
	
	public class MainMenuBehaviourInjector extends WindowBehaviourInjector
	{
		@Override
		public void doInject() throws NoSuchControlException
		{
			getControl(Button.class, "btnConnect").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_context.setState(new SynchronizeWithServer(m_physicsWorldFactory, m_parallelEntityFactory, m_effectMapFactory, m_entityFactory, m_windowFactory, m_worldFactory, m_audioClipFactory, m_spriteFactory, new LoadingWorldHandler(), HOST, PORT));
				}
			});
		}
	}

	private class LoadingWorldHandler implements SynchronizeWithServer.ILoadingWorldHandler {
		@Override
		public void done(WorldClient client, String playerName, World world) {
			m_context.setState(new Playing(m_windowFactory, m_worldFactory, m_audioClipFactory, m_spriteFactory, client, playerName, world));
		}
	}
}
