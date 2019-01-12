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
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.world.IEffectMapFactory;
import io.github.jevaengine.world.IParallelWorldFactory;
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

	private final Logger m_logger = LoggerFactory.getLogger(SynchronizeWithServer.class);

	private WorldClient m_client;
	private final String m_nickName;
	private final String m_host;
	private final int m_port;
	private final ILoadingWorldHandler m_handler;

	public SynchronizeWithServer(String nickName, ILoadingWorldHandler handler, String host, int port)
	{
		m_nickName = nickName;
		m_host = host;
		m_port = port;
		m_handler = handler;
	}
	
	@Override
	public void enter(IStateContext context)
	{
		m_client = new WorldClient(m_nickName, context.getItemFactory(), context.getPhysicsWorldFactory(), context.getParallelEntityFactory(), context.getEffectMapFactory(), context.getEntityFactory(), m_host, m_port);
		m_context = context;
		try
		{
			final LoadingBehaviourInjector behavior = new LoadingBehaviourInjector();
			
			m_window = context.getWindowFactory().create(URI.create("file:///ui/windows/loading.jwl"), behavior);
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
		m_client.update(iDelta);
		if(m_client.getWorld() != null && m_client.getPlayerEntity() != null &&
				m_client.getWorld().getEntities().getByName(IRpgCharacter.class, m_client.getPlayerEntity()) != null)
		{
			m_handler.done(m_client, m_client.getPlayerEntity(), m_client.getWorld());
		}

		if(!m_client.isConnected()) {
			m_logger.error("Cannot synchronize with server, unable to establish connection.");
			String reason = m_client.getDisconnectReason();
			m_context.setState(new DisconnectedErrorDisplay(reason));
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
