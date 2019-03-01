package com.projectstation.client.network.entity;

import com.jevaengine.spacestation.entity.character.ISpaceCharacterStatusResolver;
import com.jevaengine.spacestation.entity.character.ISpaceCharacterStatusResolverFactory;
import com.jevaengine.spacestation.entity.character.SpaceCharacterFactory;
import com.jevaengine.spacestation.entity.character.SpaceCharacterStatusResolverFactory;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptom;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptomDetails;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.config.IConfigurationFactory;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.dialogue.IDialogueRouteFactory;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.script.IScriptBuilderFactory;
import io.github.jevaengine.ui.style.IUIStyleFactory;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.scene.model.IActionSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;
import io.github.jevaengine.world.scene.model.particle.IParticleEmitterFactory;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ClientSpaceCharacterFactory extends SpaceCharacterFactory {
    @Inject
    public ClientSpaceCharacterFactory(IItemFactory itemFactory, IScriptBuilderFactory scriptBuilderFactory, IAudioClipFactory audioClipFactory, IAnimationSceneModelFactory animationSceneModelFactory, IConfigurationFactory configurationFactory, IDialogueRouteFactory dialogueRouteFactory, IUIStyleFactory styleFactory, IParticleEmitterFactory particleEmitterFactory) {
        super(itemFactory, scriptBuilderFactory, audioClipFactory, animationSceneModelFactory, configurationFactory, dialogueRouteFactory, styleFactory, particleEmitterFactory);
    }

    @Override
    protected ISpaceCharacterStatusResolverFactory createStatusResolverFactory(URI configContext, UsrCharacterDeclaration characterDecl) throws URISyntaxException, IUIStyleFactory.UIStyleConstructionException {
        return new ISpaceCharacterStatusResolverFactory() {
            @Override
            public ISpaceCharacterStatusResolver create(IRpgCharacter host, AttributeSet attributes, IActionSceneModel model) {
                return new ClientCharacterStatusResolver(attributes);
            }
        };
    }
}
