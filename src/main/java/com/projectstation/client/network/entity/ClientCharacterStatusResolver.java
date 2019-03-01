package com.projectstation.client.network.entity;

import com.jevaengine.spacestation.entity.character.ISpaceCharacterStatusResolver;
import com.jevaengine.spacestation.entity.character.SpaceCharacterAttribute;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptom;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptomDetails;
import com.projectstation.network.IClientSpaceCharacterStatusResolver;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.scene.model.IActionSceneModel;

import java.util.ArrayList;
import java.util.List;

public class ClientCharacterStatusResolver implements IClientSpaceCharacterStatusResolver {
    private List<ISymptom> symptoms = new ArrayList<>();
    private final AttributeSet attributeSet;

    public ClientCharacterStatusResolver(AttributeSet attributeSet) {
        this.attributeSet = attributeSet;
    }

    public void setEffectiveHitpoints(float hitpoints) {
        this.attributeSet.get(SpaceCharacterAttribute.EffectiveHitpoints).set(hitpoints);
    }

    @Override
    public List<ISymptomDetails> getSymptoms() {
        return new ArrayList<>(symptoms);
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public IObserverRegistry getObservers() {
        return new Observers();
    }

    @Override
    public void update(int deltaTime) {

    }

    @Override
    public void addSymptom(ISymptom symtom) {
        this.symptoms.add(symtom);
    }

    @Override
    public void removeSymptom(String name) {
        ISymptom r = null;

        for(ISymptom s : symptoms)
        {
            if(s.getName().equals(name)) {
                r = s;
                break;
            }
        }

        if(r != null) {
            symptoms.remove(r);
        }
    }

    @Override
    public IActionSceneModel decorate(IActionSceneModel subject) {
        return subject;
    }
}
