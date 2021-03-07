package com.ra4king.fps.actors;

import com.ra4king.opengl.util.math.Vector3;

/**
 * @author Roi Atalla
 */
public interface Actor {
	void update(long deltaTime);

	Vector3 getPosition();
}
