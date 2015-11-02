package edu.cmu.mat.lsd.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicToolBarUI;

import edu.cmu.mat.lsd.Model;
import edu.cmu.mat.lsd.components.JArrow;
import edu.cmu.mat.lsd.components.JBlock;
import edu.cmu.mat.lsd.components.JCursor;
import edu.cmu.mat.lsd.hcmp.HcmpListener;
import edu.cmu.mat.lsd.hcmp.TimeMap;
import edu.cmu.mat.scores.Barline;
import edu.cmu.mat.scores.Block;
import edu.cmu.mat.scores.PlaybackEvent;
import edu.cmu.mat.scores.Score;
import edu.cmu.mat.scores.System;
//hi this is Stella
public class DisplayPanel implements Panel, HcmpListener {
	private Model _model;
	private Score _score;

	private JScrollPane _scroller;
	private JPanel _panel = new JPanel();
	private JPanel _centering = new JPanel();
	private JPanel _margin = new JPanel();
	private JLayeredPane _layers = new JLayeredPane();
	private JBlock _upper_block;
	private JBlock _lower_block;
	private JCursor _cursor;
	private JArrow _arrow;
	private boolean _is_arrow_visible = false;
	
	private JToolBar _toolbar = new JToolBar("DisplayTools");
	private final JButton _play_button = new JButton("Play");
	private final JButton _stop_button = new JButton("Stop");
	private final JButton _magnify_button = new JButton("+");
	private final JButton _reduce_button = new JButton("-");

	private List<Block> _blocks;
	private Barline _previous_jump_to = null;

	private TimeMap _time_map;
	private Timer _play_timer;
	
	private int _playback_id = 0;
	private List<PlaybackEvent> _playback_events = new ArrayList<PlaybackEvent>();
	private int _events_index = 0;
	private int _current_block_index = 0;
	private static final int PAGE_LEFT = 8;
	private int _panelHeight;

	public DisplayPanel(Model model, int panelHeight) {
		_model = model;
		_panelHeight = panelHeight;
		_model.getHcmp().setListener(this);
		_score = _model.getCurrentScore();
		
		int blockHeight = (_panelHeight - 20) / 2;
		//java.lang.System.out.format("Jblock Height: %d", blockHeight);
		_upper_block = new JBlock(blockHeight);
		_lower_block = new JBlock(blockHeight);
		
		_score.addAllHeights(blockHeight); // instantiate the list of heights

		_panel.setLayout(new BoxLayout(_panel, BoxLayout.Y_AXIS));
		_layers.add(_panel, -1);
		_panel.setLocation(0, 0);
		
		_arrow = new JArrow(_panel);
		_arrow.setOpaque(false);
		_arrow.setVisible(_is_arrow_visible);
		_layers.add(_arrow, 0);
		
		_cursor = new JCursor(_panel);
		_cursor.setOpaque(false);
		_layers.add(_cursor, 0);

		
		_stop_button.setEnabled(false);
		
		_play_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				handleNewTime(TimeMap.Create(new Date().getTime(), 0, 0.004));
				start();
				_play_button.setEnabled(false);
				_stop_button.setEnabled(true);
			}
		});
		
		_stop_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				handleStop();
				onUpdateView();
				_stop_button.setEnabled(false);
				_play_button.setEnabled(true);
			}
		});
		
		_magnify_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (_score.magnify()) {
					onUpdateSize();
				}
			}
		});
		
		_reduce_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (_score.reduce()) {
					onUpdateSize();
				}
			}
		});
		
		_toolbar.add(_play_button);
		_toolbar.add(_stop_button);
		
		_toolbar.addSeparator();
		_toolbar.add(_magnify_button);
		_toolbar.add(_reduce_button);
		
		_toolbar.setVisible(true);

		JPanel _envelop = new JPanel(new BorderLayout());
		
		_scroller = new JScrollPane(_envelop);
	    _centering.setLayout(new GridBagLayout());
//		
		_centering.add(_layers);
		
		_envelop.add(_toolbar, BorderLayout.PAGE_START);
		_envelop.add(_centering, BorderLayout.CENTER);
		_layers.setVisible(true);
		
		
		
		onUpdateView();
	}

	public JComponent getContainer() {
		return _scroller;
	}

	public void onUpdateLibraryPath() {
	}

	public void onUpdateModel() {
	}

	public void onUpdateScore() {
	}

	public void onUpdateView() {
		if (_model.getCurrentView() == Model.VIEW_DISPLAY) {
			 _score.updateCurrentHeight();
			 String[] annie = new String[] {"A,0,36","A,0,36","B,36,48","B,36,48","A,0,36"};
			 String[] boy = new String[] {"A,0,20","A,0,20","B,20,32","C,52,16","D,68,8","D,68,8","E,76,16"};
			 String[] wings = new String[] {"A,0,16","B,16,32","B,16,32","C,48,60","D,108,148"};
			 handleNewArrangement(boy);
			 handleNewPosition(0); 
//			 handleNewTime(TimeMap.Create(new Date().getTime(), 0, 0.004)); 
//			 handlePlay();
			 
			_scroller.revalidate();
			_scroller.repaint();
		} else {
			handleStop();
		}
	}
	
	public void onUpdateSize() {
		_blocks = _score.createBlockList(_playback_events, _upper_block.getHeight());
		_current_block_index = getCurrentBlockIndex();
		
		_is_arrow_visible = false;
		
		initializeBlocks(_current_block_index);
		
		// If only one block left to be displayed, remove the lower block;
		if (_current_block_index >= _blocks.size() - 1) {
			_panel.remove(_lower_block);
		}
		
		int width = _score.getCurrentWidth();//_blocks.get(_current_block_index).getWidth();
		
		int height = _layers.getHeight();
		
		_layers.setPreferredSize(new Dimension(width, height));
		_panel.setSize(_layers.getPreferredSize());

		_cursor.setSize(_layers.getPreferredSize());
		moveCursor();
		_arrow.setSize(_layers.getPreferredSize());
		_arrow.setVisible(_is_arrow_visible);
		redraw();
	}

	public void onProgramQuit() {
	}

	public void onUpdateTool() {
	}

	@Override
	public Boolean handleNewPosition(int beat) {
		setTime(beat);
		return true;
	}

	@Override
	public Boolean handleNewTime(TimeMap time_map) {
		_time_map = time_map;
		if (_play_timer != null) {
			restart();
		}
		return true;
	}

	@Override
	public Boolean handlePlay() {
		start();
		return true;
	}

	@Override
	public Boolean handlePause() {
		pause();
		return true;
	}

	@Override
	public Boolean handleStop() {
		pause();
		resetTime();
		return true;
	}

	@Override
	public Boolean handleNewArrangement(String[] arrangement_string) {
		List<PlaybackEvent> new_events = _score
				.createPlaybackEvents(arrangement_string);

		if (new_events == null) {
			java.lang.System.err.println("Could not parse new arrangement!");
			return false;
		}

		_playback_events = new_events;
		_blocks = _score.createBlockList(_playback_events, _upper_block.getHeight());
		_current_block_index = 0;
		_is_arrow_visible = false;
		
		_panel.removeAll();

		initializeBlocks(0);
		
		int width = _score.getCurrentWidth();//_blocks.get(0).getWidth();
		int height = _upper_block.getHeight() + _lower_block.getHeight() + 10;
		
		
		JPanel margin = new JPanel();
		_margin.setPreferredSize(new Dimension(width, 10));
		
		_panel.add(_upper_block);
		_panel.add(margin);
		if (_blocks.size() > 1) _panel.add(_lower_block);
		
		

		_layers.setPreferredSize(new Dimension(width, height));
		_panel.setSize(_layers.getPreferredSize());

		_cursor.setSize(_layers.getPreferredSize());
		moveCursor();
		_arrow.setSize(_layers.getPreferredSize());
		_arrow.setVisible(_is_arrow_visible);
		redraw();

		if (_play_timer != null) {
			restart();
		}
		return true;
	}
	
	private void initializeBlocks(int index) {
		_upper_block.setWidth(_score.getCurrentWidth());
		_lower_block.setWidth(_score.getCurrentWidth());
		
		Block curr = _blocks.get(index);
		//curr.makeImage(_score.getCurrentHeight());
		
		if (index % 2 == 0) _upper_block.setBlock(curr);
		else _lower_block.setBlock(curr);
			
		if (_blocks.size() > index + 1) {
			Block next = _blocks.get(index + 1);
			//next.makeImage(_score.getCurrentHeight());
			if (index % 2 == 0) _lower_block.setBlock(next);
			else _upper_block.setBlock(next);
		}
		
		
	}
	

	private void fireNextEvent(final int id) {
		if (id != _playback_id) {
			return;
		}

		long delay = 0;
		long time = new Date().getTime();
		for (;; _events_index++) {
			delay = (long) (_time_map.from(_events_index * 4) - time);
			if (delay >= 0) {
				break;
			}
		}

		delay = Math.max(delay, 0);
		_play_timer = new Timer((int) delay, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (_events_index < _playback_events.size()) updateBlock();
				drawArrow();
				moveCursor();
				if (_events_index < _playback_events.size()) {
					fireNextEvent(id);
				} 
			}
		});

		_play_timer.setRepeats(false);
		_play_timer.start();
	}

	private void resetTime() {
		setTime(0);
	}

	private void setTime(int beat) {
		int so_far = 0;
		for (PlaybackEvent event : _playback_events) {
			so_far += event.getDuration();
			if (so_far > beat) {
				_events_index = _playback_events.indexOf(event);
				break;
			}
		}
		//moveCursor();
	}
	
	private int getCurrentBlockIndex(){
		for (int blockIndex = 0; blockIndex < _blocks.size(); blockIndex ++) {
			Block current = _blocks.get(blockIndex);
			if (! _score.outOfBlock(current, _playback_events.get(_events_index).getStart())) {
				return blockIndex;
			}
		}
		java.lang.System.err.print("Error! Unable to find current block after resizing\n");
		return -1;
	}

	private void updateBlock() {
		Block current_block = _blocks.get(_current_block_index);
		if (_current_block_index < _blocks.size() - 1) {
			if (_score.outOfBlock(current_block, _playback_events.get(_events_index).getStart())) {
				_current_block_index += 1;
				if (_score.outOfBlock(_blocks.get(_current_block_index), _playback_events.get(_events_index).getStart())) {
					java.lang.System.err.print("Error! Current event out of current and next block.\n");
				}
			}
		}
		current_block = _blocks.get(_current_block_index);
		if (!current_block.isBlockFlipped()) {
			if (_current_block_index < _blocks.size() - 1) {
				Block next_block = _blocks.get(_current_block_index + 1);
				//next_block.makeImage(_score.getCurrentHeight());
				if (_current_block_index % 2 == 0) _lower_block.setBlock(next_block);
				else _upper_block.setBlock(next_block);
				
				current_block.flipToNextBlock();
			}
			else current_block.flipToNextBlock();
		}
		
		
		
		
		redraw();
	}
	
	private boolean isCloseTo(int d, int current_events_index, Barline to) {
		int end = current_events_index + d;
		if (end >= _playback_events.size()) {
			end = _playback_events.size() - 1;
		}
		for (int i = current_events_index; i < end; i++) {
			if (_playback_events.get(i).getEnd() == to) {
				return true;
			}
		}
		return false;
	}
	
	private void drawArrow() {
		if (_events_index < _playback_events.size()) {
			PlaybackEvent current_event = _playback_events.get(_events_index);
			System current_system = current_event.getStart().getParent();
			Block current_block = _blocks.get(_current_block_index);
			Barline next_jump_from = current_block.getNextJumpFrom();
			Barline next_jump_to = current_block.getNextJumpTo();
			
			if (_previous_jump_to != null && current_event.getStart() == _previous_jump_to) {
				_is_arrow_visible = false;
			}
			
			if (!_is_arrow_visible) {
				
				//show arrow on the system
				//if (next_jump_from != null && current_system == next_jump_from.getParent()) 
				
				//show arrow before 2 measures
				if (next_jump_from != null && isCloseTo(2, _events_index, next_jump_from)) {
					_is_arrow_visible = true;
					
					
					int from_x = next_jump_from.getOffset() - PAGE_LEFT;
					int to_x = next_jump_to.getOffset() - PAGE_LEFT;
					int from_y = current_block.getYOffset(next_jump_from.getParent())
							+ getJBlock(true).getY();
					int to_y;
					if (_score.outOfBlock(current_block, next_jump_to)) {
						Block next_block = _blocks.get(_current_block_index + 1);
						
						to_y = next_block.getYOffset(next_jump_to.getParent())
							+ getJBlock(false).getY();
					}
					else to_y = current_block.getYOffset(next_jump_to.getParent())
							+ getJBlock(true).getY();
					// Current y values are referring to bottom of system
					
					if (from_y < to_y) {
						to_y -= next_jump_to.getParent().getInnerHeight();
					}
					else if (from_y > to_y) {
						from_y -= current_system.getInnerHeight();
					}
						
					_previous_jump_to = next_jump_to;
					_arrow.setPosition(from_x, from_y, to_x, to_y);
				}
	
				_arrow.setVisible(_is_arrow_visible);
			}
			
			if (current_event.getEnd() == next_jump_from) {
				current_block.makeJump(next_jump_from, next_jump_to);
			}
			
		}
		
	}
	
	// Get the current or the other JBlock using block index
	private JBlock getJBlock(boolean isCurrent) {
		if (_current_block_index % 2 == 0) {
			if (isCurrent) return _upper_block;
			else return _lower_block;
		}
		else {
			if (isCurrent) return _lower_block;
			else return _upper_block;
		}
	}
	
	private void moveCursor() {
		if (_playback_events.isEmpty()) {
			return;
		}

		if (_events_index < 0) {
			resetTime();
			return;
		}
		if (_events_index >= _playback_events.size()) {
			PlaybackEvent end_event = _playback_events.get(_playback_events.size()-1);
			Barline end_bar = end_event.getEnd();
			
			Block end_block = _blocks.get(_current_block_index);
			
			int x = end_bar.getOffset();
			int y = end_block.getYOffset(end_bar.getParent())
					+ getJBlock(true).getY();

			_cursor.setPosition(x, y-5); // y-5: keep cursor inside block
			
		}
		else {
			PlaybackEvent current_event = _playback_events.get(_events_index);
			Barline current_bar = current_event.getStart();
			
			Block current_block = _blocks.get(_current_block_index);
	
			int x = current_bar.getOffset();
			int y = current_block.getYOffset(current_bar.getParent())
					+ getJBlock(true).getY();
	
			_cursor.setPosition(x, y-5); // y-5: keep cursor inside block
			
			
		}
		redraw();
	}

	private void start() {
		_playback_id++;
		fireNextEvent(_playback_id);
	}

	private void pause() {
		_playback_id++;
		if (_play_timer != null) {
			_play_timer.stop();
			_play_timer = null;
		}
	}

	private void restart() {
		pause();
		start();
	}

	private void redraw() {
		_layers.revalidate();
		_layers.repaint();

		_scroller.revalidate();
		_scroller.repaint();
	}
}
