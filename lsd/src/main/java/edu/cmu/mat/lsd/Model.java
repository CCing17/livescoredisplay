package edu.cmu.mat.lsd;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import edu.cmu.mat.lsd.hcmp.HcmpClient;
import edu.cmu.mat.lsd.hcmp.HcmpListener;
import edu.cmu.mat.lsd.hcmp.TimeMap;
import edu.cmu.mat.lsd.menus.listeners.DisplayMenuListener;
import edu.cmu.mat.lsd.tools.DeleteTool;
import edu.cmu.mat.lsd.tools.MoveTool;
import edu.cmu.mat.lsd.tools.NewBarlineTool;
import edu.cmu.mat.lsd.tools.NewSectionTool;
import edu.cmu.mat.lsd.tools.NewSystemTool;
import edu.cmu.mat.lsd.tools.Tool;
import edu.cmu.mat.parsers.exceptions.CompilerException;
import edu.cmu.mat.scores.Barline;
import edu.cmu.mat.scores.Score;
import edu.cmu.mat.scores.Section;

public class Model implements DisplayMenuListener {
	private Controller _controller;
	private Gson _gson;

	private File _library;
	private File _init_file;

	@Expose
	private int _windowX = 100;
	@Expose
	private int _windowY = 100;
	@Expose
	private int _windowWidth = 100;
	@Expose
	private int _windowHeight = 100;

	private List<Score> _scores = new ArrayList<Score>();
	private int _currentScore = -1;

	@Expose
	private String _currentScoreName = "";

	@Expose
	private int _currentView = VIEW_NOTATION;

	private Tool _currentTool = null;

	private HcmpClient _hcmp = new HcmpClient();
	private static final String IP_ADDRESS = "192.168.1.127";
	private static final String PORT_PULL = "5544";
	private static final String PORT_PUBLISH = "5566";

	public final Tool NEW_SYSTEM_TOOL = new NewSystemTool();
	public final Tool NEW_BARLINE_TOOL = new NewBarlineTool();
	public final Tool NEW_SECTION_TOOL = new NewSectionTool(this);
	public final Tool MOVE_TOOL = new MoveTool();
	public final Tool DELETE_TOOL = new DeleteTool();

	public final static int VIEW_NOTATION = 0;
	public final static int VIEW_SECTIONS = 1;
	public final static int VIEW_ARRANGMENT = 2;
	public final static int VIEW_DISPLAY = 3;

	public Model(Controller controller) {
		_controller = controller;
		_gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.create();

		_hcmp.start(IP_ADDRESS, PORT_PULL, PORT_PUBLISH);

		// XXX: Hack, set %USER%/hcmp or something as the default library.
		File path = new File("C:\\Users\\deadh_000\\library");
		try {
			onSetPath(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getWindowX() {
		return _windowX;
	}

	public int getWindowY() {
		return _windowY;
	}

	public int getWindowWidth() {
		return _windowWidth;
	}

	public int getWindowHeight() {
		return _windowHeight;
	}

	public List<Score> getScoreList() {
		return _scores;
	}

	public String getCurrentScoreName() {
		return _currentScoreName;
	}

	public Score getCurrentScore() {
		return _scores.get(_currentScore);
	}

	public int getCurrentView() {
		return _currentView;
	}

	public Tool getCurrentTool() {
		return _currentTool;
	}

	public void setWindowX(int windowX) {
		_windowX = windowX;
	}

	public void setWindowY(int windowY) {
		_windowY = windowY;
	}

	public void setWindowWidth(int windowWidth) {
		_windowWidth = windowWidth;
	}

	public void setWindowHeight(int windowHeight) {
		_windowHeight = windowHeight;
	}

	public void setCurrentScore(String currentScoreName) {
		_currentScoreName = currentScoreName;

		_currentScore = -1;
		for (int i = 0; i < _scores.size(); i++) {
			if (_scores.get(i).getName().equals(_currentScoreName)) {
				_currentScore = i;
				break;
			}
		}

		_controller.scoreUpdated();
	}

	public void setCurrentView(int view) {
		_currentView = view;

		_controller.viewUpdated();
	}

	public void setCurrentTool(Tool tool) {
		_currentTool = tool;

		_controller.toolUpdated();
	}

	public void onNewScore() {
		// TODO Auto-generated method stub

	}

	public void onNewArrangement() {
		// TODO Auto-generated method stub
	}

	public void onSetPath(File path) throws IOException {
		save();

		_library = path;
		_init_file = new File(_library.getAbsolutePath() + File.separator
				+ "init.json");
		load();
		loadScores();

		_controller.viewUpdated();
		_controller.libraryPathUpdated();
	}

	public void quit() {
		save();
		_hcmp.stop();
		_controller.programQuit();
		System.exit(0);
	}

	public void save() {
		for (Score score : _scores) {
			File score_directory = new File(_library, score.getName());
			score.saveTo(score_directory);
		}

		if (_init_file == null) {
			return;
		}

		String init_text = _gson.toJson(this);
		System.out.println("Write: " + init_text);
		try {
			FileWriter writer = new FileWriter(_init_file);
			writer.write(init_text);
			writer.close();
		} catch (IOException e) {
			// Notify that an error occurred here.
			e.printStackTrace();
		}
	}

	private void load() {
		try {
			FileReader reader = new FileReader(_init_file);
			String json_text = "";
			int c;
			while ((c = reader.read()) != -1) {
				json_text += (char) c;
			}
			reader.close();

			System.out.println("Read: " + json_text);

			Model model = _gson.fromJson(json_text, this.getClass());
			_windowX = model._windowX;
			_windowY = model._windowY;
			_windowWidth = model._windowWidth;
			_windowHeight = model._windowHeight;
			_currentView = model._currentView;
			_currentScoreName = model._currentScoreName;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Read in arrangement to load
		// Read in what mode to be in
	}

	private void loadScores() throws IOException {
		File[] scores = _library.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return !file.getName().equals("init.json");
			}
		});

		_scores.clear();
		for (File score : scores) {
			try {
				Score newScore = Score.fromDirectory(score);
				if (newScore.getName().equals(_currentScoreName)) {
					_currentScore = _scores.size();
				}
				_scores.add(newScore);
			} catch (CompilerException e) {
				e.printStackTrace();
			}
		}
	}

	public void saveArrangment(DefaultListModel<String> list_model) {
		Score score = getCurrentScore();
		score.saveArrangment(list_model);
	}

	public void loadArrangment(DefaultListModel<String> list_model) {
		Score score = getCurrentScore();
		score.loadArrangment(list_model);
	}

	public void sendArrangement() {
		_hcmp.sendArrangement(getCurrentScore());
	}

	public void loadSections(DefaultListModel<String> list_model) {
		list_model.removeAllElements();
		Score score = getCurrentScore();
		for (Section section : score.getSections()) {
			list_model.addElement(section.getName());
		}
	}

	public void addSection(String name, Barline start_barline,
			Barline end_barline) {
		getCurrentScore().addSection(start_barline, end_barline).setName(name);
		_controller.modelUpdated();
	}

	public HcmpClient getHcmp() {
		return _hcmp;
	}
}