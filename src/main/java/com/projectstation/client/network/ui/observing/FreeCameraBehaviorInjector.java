/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.projectstation.client.network.ui.observing;

import com.jevaengine.spacestation.entity.Infrastructure;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Matrix3X3;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.scene.ISceneBuffer;
import io.github.jevaengine.world.scene.camera.ControlledCamera;
import io.github.jevaengine.world.scene.camera.ICamera;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.search.RadialSearchFilter;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jeremy
 */
public class FreeCameraBehaviorInjector extends WindowBehaviourInjector {
	private final ControlledCamera m_camera;

	public FreeCameraBehaviorInjector(ControlledCamera camera) {
		m_camera = camera;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView worldView = getControl(WorldView.class, "worldView");
		final Timer logicTimer = new Timer();
		final CameraController cameraController = new CameraController(worldView);

		addControl(logicTimer);
		worldView.setCamera(m_camera);

		logicTimer.getObservers().add(cameraController);
		worldView.getObservers().add(cameraController);
		getObservers().add(cameraController);
	}

	private class CameraController implements io.github.jevaengine.ui.Window.IWindowFocusObserver, Timer.ITimerObserver, WorldView.IWorldViewInputObserver, Window.IWindowInputObserver {

		private Vector3F m_cameraMovement = new Vector3F();
		private final WorldView m_worldView;

		public CameraController(WorldView worldView) {
			m_worldView = worldView;
		}


		@Override
		public void update(int deltaTime) {
			if (!m_cameraMovement.isZero()) {
				m_camera.move(m_cameraMovement.normalize().multiply(deltaTime / 200.0F * (1.0f / m_camera.getZoom()) * 30));
			}
		}


		@Override
		public void onFocusChanged(boolean hasFocus) {
			if (!hasFocus) {
				m_cameraMovement = new Vector3F();
			}
		}

		@Override
		public void keyEvent(InputKeyEvent e) {
			if (e.type == InputKeyEvent.KeyEventType.KeyDown) {
				switch (e.keyCode) {
					case KeyEvent.VK_UP:
						m_cameraMovement.y = -1;
						break;
					case KeyEvent.VK_RIGHT:
						m_cameraMovement.x = 1;
						break;
					case KeyEvent.VK_DOWN:
						m_cameraMovement.y = 1;
						break;
					case KeyEvent.VK_LEFT:
						m_cameraMovement.x = -1;
						break;
				}
			} else if (e.type == InputKeyEvent.KeyEventType.KeyUp) {
				switch (e.keyCode) {
					case KeyEvent.VK_UP:
					case KeyEvent.VK_DOWN:
						m_cameraMovement.y = 0;
						break;
					case KeyEvent.VK_RIGHT:
					case KeyEvent.VK_LEFT:
						m_cameraMovement.x = 0;
						break;
				}
			}
		}

		@Override
		public void onKeyEvent(InputKeyEvent event) {
			keyEvent(event);
		}

		@Override
		public void onMouseEvent(InputMouseEvent event) {

		}

		@Override
		public void mouseEvent(InputMouseEvent event) {

		}
	}
}
