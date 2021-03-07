package com.ra4king.fps.renderers;

import java.io.InputStream;

/**
 * @author Roi Atalla
 */
public class Resources {
	public static final String RESOURCES_ROOT_PATH = "/res/";

	public static InputStream getInputStream(String path) {
		return Resources.class.getResourceAsStream(RESOURCES_ROOT_PATH + path);
	}
}
