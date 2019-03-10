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
import io.github.jevaengine.audio.IAudioClip;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.audio.NullAudioClip;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
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

public class ConnectionMenu implements IState
{
	private static final URI ENTRY_MAP = URI.create("file:///world/firstStationRework.jmp");
	private static final URI MENU_AUDIO = URI.create("file:///audio/menu/0.ogg");

	private IStateContext m_context;
	private Window m_window;

	private final Logger m_logger = LoggerFactory.getLogger(ConnectionMenu.class);

	private IAudioClip m_menuAudio = new NullAudioClip();
	private final String m_host;
	private final int m_port;

	public ConnectionMenu(String host, int port) {
		m_host = host;
		m_port = port;
	}

	@Override
	public void enter(IStateContext context)
	{
		try {
			m_menuAudio = context.getAudioClipFactory().create(MENU_AUDIO);
		} catch (IAudioClipFactory.AudioClipConstructionException ex) {
			m_logger.error("Unable to construct background audio.", ex);
		}

		m_menuAudio.play();
		m_menuAudio.repeat();
		m_context = context;

		try
		{
			m_window = context.getWindowFactory().create(URI.create("file:///ui/windows/connectionmenu.jwl"), new ConnectionMenuBehaviourInjector());
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
		m_menuAudio.stop();
		m_menuAudio.dispose();
		if(m_window != null)
		{
			m_context.getWindowManager().removeWindow(m_window);
			m_window.dispose();
		}
	}

	@Override
	public void update(int iDelta) { }
	
	public class ConnectionMenuBehaviourInjector extends WindowBehaviourInjector
	{
		@Override
		public void doInject() throws NoSuchControlException
		{
			final TextArea txtNickname = getControl(TextArea.class, "txtNickname");

			getControl(Button.class, "btnConnect").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					String nickname = txtNickname.getText();
					m_context.setState(new SynchronizeWithServer(nickname, new LoadingWorldHandler(), m_host, m_port));
				}
			});
		}
	}

	private class LoadingWorldHandler implements SynchronizeWithServer.ILoadingWorldHandler {
		@Override
		public void done(WorldClient client, String playerName, World world) {
			m_context.setState(new Playing(m_host, m_port, client, playerName, world));
		}
	}
}
