package edu.cmu.mat.lsd.menus.listeners;

import java.io.File;
import java.io.IOException;

public interface DisplayMenuListener {

	void onNewScore(String score_name, File[] files);

	void onNewArrangement();

	void onSetPath(File file) throws IOException;

	void quit();
}
