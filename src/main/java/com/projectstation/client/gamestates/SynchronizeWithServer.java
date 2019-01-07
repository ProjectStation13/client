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
import io.github.jevaengine.IInitializationMonitor;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.ui.*;
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

/**
 *
 * @author Jeremy
 */
public class SynchronizeWithServer implements IState
{
	private IStateContext m_context;
	private Window m_window;
	
	private final IWindowFactory m_windowFactory;
	private final IParallelWorldFactory m_worldFactory;
	private final IAudioClipFactory m_audioClipFactory;
	private final ISpriteFactory m_spriteFactory;
	private final IEntityFactory m_entityFactory;
	
	private final Logger m_logger = LoggerFactory.getLogger(SynchronizeWithServer.class);

	private final ILoadingWorldHandler m_handler;

	private final WorldClient client;


	private final IPhysicsWorldFactory m_physicsWorldFactory;
	private final IParallelEntityFactory m_parallelEntityFactory;
	private final IEffectMapFactory m_effectMapFactory;

	public SynchronizeWithServer(IPhysicsWorldFactory physicsWorldFactory, IParallelEntityFactory parallelEntityFactory, IEffectMapFactory effectMapFactory, IEntityFactory entityFactory, IWindowFactory windowFactory, IParallelWorldFactory worldFactory, IAudioClipFactory audioClipFactory, ISpriteFactory spriteFactory, ILoadingWorldHandler handler, String host, int port)
	{
		m_physicsWorldFactory = physicsWorldFactory;
		m_parallelEntityFactory = parallelEntityFactory;
		m_effectMapFactory = effectMapFactory;

		m_entityFactory = entityFactory;
		m_handler = handler;
		m_windowFactory = windowFactory;
		m_worldFactory = worldFactory;
		m_audioClipFactory = audioClipFactory;
		m_spriteFactory = spriteFactory;

		client = new WorldClient(physicsWorldFactory, parallelEntityFactory, effectMapFactory, entityFactory, host, port);

	}
	
	@Override
	public void enter(IStateContext context)
	{
		m_context = context;
		try
		{
			final LoadingBehaviourInjector behavior = new LoadingBehaviourInjector();
			
			m_window = m_windowFactory.create(URI.create("file:///ui/windows/loading.jwl"), behavior);
			context.getWindowManager().addWindow(m_window);
			m_window.center();

		} catch (WindowConstructionException e)
		{
			m_logger.error("Error constructing loading window", e);
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
	public void update(int iDelta)
	{
		client.update(iDelta);
		if(client.getWorld() != null && client.getPlayerEntity() != null &&
				client.getWorld().getEntities().getByName(IRpgCharacter.class, client.getPlayerEntity()) != null)
		{
			m_handler.done(client, client.getPlayerEntity(), client.getWorld());
		}

		if(!client.isConnected()) {
			m_logger.error("Cannot synchronize with server, unable to establish connection.");
			m_context.setState(new ConnectionMenu(m_physicsWorldFactory, m_parallelEntityFactory, m_effectMapFactory, m_entityFactory, m_windowFactory, m_worldFactory, m_audioClipFactory, m_spriteFactory));
		}
	}

	public interface ILoadingWorldHandler
	{
		void done(WorldClient client, String playerEntity, World world);
	}
	
	public class LoadingBehaviourInjector extends WindowBehaviourInjector
	{	
		private ValueGuage m_progressGuage;
		
		@Override
		public void doInject() throws NoSuchControlException
		{
			m_progressGuage = getControl(ValueGuage.class, "loadingProgressBar");
		}
		
		public void setProgress(float progress)
		{
			m_progressGuage.setValue(progress);
		}
	}

}
