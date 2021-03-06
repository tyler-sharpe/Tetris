package com.github.tylersharpe.tetris.swing;

import com.github.tylersharpe.tetris.*;
import com.github.tylersharpe.tetris.audio.AudioFileNotFound;
import com.github.tylersharpe.tetris.audio.TetrisAudioSystem;
import com.github.tylersharpe.tetris.event.TetrisEvent;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MasterTetrisFrame extends JFrame {

  // Prevents tooltips from disappearing while mouse is over them
  static { ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE); }

  static final Font ARIAL_HEADER = new Font("Arial", Font.BOLD, 17);
  static final Font ARIAL_DESCRIPTION = new Font("Arial", Font.PLAIN, 13);
  private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

  private TetrisAudioSystem audioSystem;
  private final TetrisGame game;
  private final ScoreRepository scoreRepository = new ScoreRepository();

  private final BoardPanel boardPanel;
  @SuppressWarnings("FieldCanBeLocal")
  private final BlockDisplayPanel nextBlockPanel;
  private final BlockDisplayPanel holdPanel;
  private final MenuPanel menuPanel;
  private final SettingsPanel settingsPanel;
  private ScorePanel scorePanel;

  // Tracks progress of Asynchronous UI effects
  private Future<?> clearTask;
  private Future<?> flashLabelTask;

  private final KeyAdapter keyHandler = new KeyAdapter() {

    final Set<Integer> pressedKeyCodes = new HashSet<>();

    public void keyPressed(KeyEvent e) {

      int code = e.getKeyCode();
      pressedKeyCodes.add(code);

      switch (code) {

        case KeyEvent.VK_LEFT:

          if (pressedKeyCodes.contains(KeyEvent.VK_S)) {
            game.superSlideActiveBlockLeft();
            audioSystem.playSuperSlideSound();
          } else {
            game.moveActiveBlockLeft();
          }

          break;

        case KeyEvent.VK_RIGHT:

          if (pressedKeyCodes.contains(KeyEvent.VK_S)) {
            game.superSlideActiveBlockRight();
            audioSystem.playSuperSlideSound();
          } else {
            game.moveActiveBlockRight();
          }

          break;

        case KeyEvent.VK_DOWN:

          game.moveActiveBlockDown();
          break;

        case KeyEvent.VK_UP:

          if (game.rotateActiveBlock(Rotation.CLOCKWISE)) {
            audioSystem.playClockwiseRotationSound();
          }
          break;

        case KeyEvent.VK_F:

          if (game.rotateActiveBlock(Rotation.COUNTER_CLOCKWISE)) {
            audioSystem.playCounterClockwiseRotationSound();
          }
          break;

        case KeyEvent.VK_D: // Hold set

          Block activeBlock = game.getActiveBlock();

          if (game.getHoldBlock().isEmpty() && !activeBlock.isHoldBlock()) {
            activeBlock.tagAsHoldBlock();
            audioSystem.playHoldSound();
            game.setHoldBlock(activeBlock);
            Block nextBlock = game.getConveyor().next();
            game.spawn(nextBlock);
          }

          break;

        case KeyEvent.VK_E: // Hold release

          if (game.getHoldBlock().isPresent()) {
            Block heldPiece = game.getHoldBlock().get();
            game.spawn(heldPiece);
            game.clearHoldBlock();
            audioSystem.playReleaseSound();
          }

          break;

        case KeyEvent.VK_SPACE:

          game.dropCurrentBlock();
          audioSystem.playBlockPlacementSound();
          game.tryMoveActiveBlockDown();
          break;
      }

      repaint();
    }

    public void keyReleased(KeyEvent e) {
      pressedKeyCodes.remove(e.getKeyCode());
    }

  };

  public MasterTetrisFrame() {
    try {
      this.audioSystem = TetrisAudioSystem.getInstance();
    } catch (AudioFileNotFound ex) {
      JOptionPane.showMessageDialog(null, "Audio files were not found. Please re-build the archive", "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

    this.game = new TetrisGame();
    this.game.getFallTimer().setInitialDelay(0);
    this.game.getFallTimer().addActionListener(e -> repaint());

    this.game.getGameTimer().addActionListener(e -> {
      scorePanel.timeLabel.repaint();
      scorePanel.timeProgressBar.repaint();
    });

    for (TetrisEvent gameOverEvent : List.of(TetrisEvent.SPAWN_FAIL, TetrisEvent.TIME_ATTACK_FAIL)) {
      this.game.subscribe(gameOverEvent, e -> onGameOver());
    }
    this.game.subscribe(TetrisEvent.GAME_WON, e -> onWin());

    this.game.subscribe(TetrisEvent.LINES_CLEARED, event -> {
      int lines = (int) event;

      audioSystem.playClearLineSound(lines);
      scorePanel.totalLinesLabel.repaint();
      scorePanel.linesClearedProgressBar.repaint();
    });

    this.game.subscribe(TetrisEvent.LEVEL_CHANGED, event -> {
      int newLevel = (int) event;

      scorePanel.levelLabel.setText("Level: " + newLevel);
      scorePanel.timeProgressBar.repaint();

      audioSystem.startSoundtrack(newLevel);

      if (newLevel > 1) {
        flashLabelTask = THREAD_POOL.submit(() -> scorePanel.levelLabel.flash(Color.YELLOW));
      }
    });

    this.game.subscribe(TetrisEvent.SCORE_CHANGED, score -> scorePanel.scoreLabel.repaint());

    this.boardPanel = new BoardPanel();

    this.nextBlockPanel = new BlockDisplayPanel("Next") {
      @Override
      public Collection<ColoredSquare> getCurrentColors() {
        Block nextBlock = game.getConveyor().peek();
        return nextBlock == null ? List.of() : nextBlock.getPreviewPanelSquares();
      }
    };

    this.holdPanel = new BlockDisplayPanel("Hold") {
      @Override
      public Collection<ColoredSquare> getCurrentColors() {
        return game.getHoldBlock().map(Block::getPreviewPanelSquares).orElse(Collections.emptyList());
      }
    };

    this.menuPanel = new MenuPanel();
    this.settingsPanel = new SettingsPanel();
    this.scorePanel = new ScorePanel();

    LinkedHashMap<String, String> controls = new LinkedHashMap<>(); // LinkedHashMap because order matters for display
    controls.put("Up:",          "Rotate CW");
    controls.put("'F':",         "Rotate CCW");
    controls.put("Down:",        "Shift down");
    controls.put("Left:",        "Shift left");
    controls.put("Right:",       "Shift right");
    controls.put("'S' + left:",  "Super-slide left");
    controls.put("'S' + right:", "Super-slide right");
    controls.put("Spacebar:",    "Instant drop");
    controls.put("'D':",         "Set hold");
    controls.put("'E':",         "Release hold");
    controls.put("Alt + 'S':",   "Start game");
    controls.put("Alt + 'P':",   "Pause game");
    controls.put("Alt + 'R':",   "Resume game");
    controls.put("Alt + 'G':",   "Give up");
    controls.put("Alt + 'L':",   "View leaderboard");

    JPanel keys = new JPanel(new GridLayout(controls.size(), 1));
    JPanel actions = new JPanel(new GridLayout(controls.size(), 1));
    controls.forEach((key, action) -> {
      keys.add(new JLabel(key));
      actions.add(new JLabel(action));
    });

    JPanel controlsPanel = new JPanel(new BorderLayout());
    controlsPanel.setBorder(new TitledBorder("Controls"));
    controlsPanel.add(keys, BorderLayout.WEST);
    controlsPanel.add(actions, BorderLayout.EAST);

    JPanel holdContainer = new JPanel(new BorderLayout());
    holdContainer.add(holdPanel, BorderLayout.NORTH);
    holdContainer.add(controlsPanel, BorderLayout.CENTER);
    add(holdContainer, BorderLayout.WEST);

    add(boardPanel, BorderLayout.CENTER);

    JPanel infoPanel = new JPanel(new BorderLayout());
    infoPanel.add(nextBlockPanel, BorderLayout.NORTH);
    infoPanel.add(scorePanel, BorderLayout.CENTER);
    infoPanel.add(settingsPanel, BorderLayout.SOUTH);
    add(infoPanel, BorderLayout.EAST);

    add(menuPanel, BorderLayout.SOUTH);

    SwingUtility.setIcon(this, "/images/game-icon.png");
    setTitle("Tetris");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    int masterWidth = (holdPanel.getColumns() * BlockDisplayPanel.DEFAULT_BLOCK_DIMENSION) +
                      (boardPanel.getColumns() * BlockDisplayPanel.DEFAULT_BLOCK_DIMENSION) +
                      (nextBlockPanel.getColumns() * BlockDisplayPanel.DEFAULT_BLOCK_DIMENSION);

    int masterHeight = BlockDisplayPanel.DEFAULT_BLOCK_DIMENSION * (boardPanel.getRows());

    setSize(masterWidth, masterHeight);
    setResizable(false); // I don't want to mess with trying to make this work right

    setLocationRelativeTo(null);
  }

  private void onStart() {

    game.reset();

    if (clearTask != null && !clearTask.isDone()) {
      clearTask.cancel(true);
    }

    if (flashLabelTask != null && !flashLabelTask.isDone()) {
      flashLabelTask.cancel(true);
    }

    settingsPanel.difficultyCombobox.setEnabled(false);
    settingsPanel.timeAttackCheckbox.isSelected();
    settingsPanel.specialsButton.setEnabled(false);
    settingsPanel.timeAttackCheckbox.setEnabled(false);
    settingsPanel.ghostSquaresCheckbox.setEnabled(false);
    settingsPanel.musicCheckbox.setEnabled(false);
    settingsPanel.soundEffectsCheckbox.setEnabled(false);

    menuPanel.startButton.setEnabled(false);
    menuPanel.pauseButton.setEnabled(true);
    menuPanel.resumeButton.setEnabled(false);
    menuPanel.giveUpButton.setEnabled(true);
    menuPanel.leaderboardButton.setEnabled(false);

    boardPanel.enableKeyHandler();

    holdPanel.repaint();

    scorePanel.timeProgressBar.setVisible(settingsPanel.timeAttackCheckbox.isSelected());
    scorePanel.totalLinesLabel.repaint();
  }

  private void onPause() {

    game.getFallTimer().stop();
    game.getGameTimer().stop();

    settingsPanel.ghostSquaresCheckbox.setEnabled(true);
    settingsPanel.musicCheckbox.setEnabled(true);
    settingsPanel.soundEffectsCheckbox.setEnabled(true);

    audioSystem.stopCurrentSoundtrack();
    audioSystem.playPauseSound();

    boardPanel.disableKeyHandler();

    menuPanel.resumeButton.setEnabled(true);
    menuPanel.pauseButton.setEnabled(false);
    menuPanel.giveUpButton.setEnabled(true);
    menuPanel.leaderboardButton.setEnabled(true);
  }

  private void onResume() {

    game.getFallTimer().start();
    game.getGameTimer().start();

    settingsPanel.ghostSquaresCheckbox.setEnabled(false);
    settingsPanel.musicCheckbox.setEnabled(false);
    settingsPanel.soundEffectsCheckbox.setEnabled(false);

    audioSystem.resumeCurrentSoundtrack();

    boardPanel.enableKeyHandler();

    menuPanel.resumeButton.setEnabled(false);
    menuPanel.pauseButton.setEnabled(true);
    menuPanel.giveUpButton.setEnabled(true);
    menuPanel.leaderboardButton.setEnabled(true);
  }

  private void onWin() {

    settingsPanel.difficultyCombobox.setEnabled(true);
    settingsPanel.specialsButton.setEnabled(true);
    settingsPanel.timeAttackCheckbox.setEnabled(true);
    settingsPanel.ghostSquaresCheckbox.setEnabled(true);
    settingsPanel.musicCheckbox.setEnabled(true);
    settingsPanel.soundEffectsCheckbox.setEnabled(true);

    menuPanel.startButton.setEnabled(true);
    menuPanel.pauseButton.setEnabled(false);
    menuPanel.resumeButton.setEnabled(false);
    menuPanel.giveUpButton.setEnabled(false);
    menuPanel.leaderboardButton.setEnabled(true);

    audioSystem.stopCurrentSoundtrack();
    audioSystem.playVictoryFanfare();

    boardPanel.disableKeyHandler();
    clearTask = THREAD_POOL.submit(boardPanel::jumpClear);
    scorePanel.levelLabel.setText("You Win!!!");
    flashLabelTask = THREAD_POOL.submit(() -> scorePanel.levelLabel.flash(Color.YELLOW));
  }

  private void onGameOver() {

    game.getFallTimer().stop();
    game.getGameTimer().stop();

    audioSystem.stopCurrentSoundtrack();
    audioSystem.playGameOverSound();

    menuPanel.startButton.setEnabled(true);
    menuPanel.pauseButton.setEnabled(false);
    menuPanel.resumeButton.setEnabled(false);
    menuPanel.giveUpButton.setEnabled(false);
    menuPanel.leaderboardButton.setEnabled(true);

    settingsPanel.ghostSquaresCheckbox.setEnabled(true);
    settingsPanel.musicCheckbox.setEnabled(true);
    settingsPanel.soundEffectsCheckbox.setEnabled(true);
    settingsPanel.timeAttackCheckbox.setEnabled(true);
    settingsPanel.difficultyCombobox.setEnabled(true);
    settingsPanel.specialsButton.setEnabled(true);

    boardPanel.disableKeyHandler();

    scorePanel.levelLabel.setText("Game Over!!!");
    flashLabelTask = THREAD_POOL.submit(() -> scorePanel.levelLabel.flash(Color.RED));

    clearTask = THREAD_POOL.submit(boardPanel::spiralClear);
  }

  private class BoardPanel extends ColorGrid {

    private static final int SPIRAL_SLEEP_INTERVAL = 7;
    private static final int CLEAR_SLEEP_INTERVAL = 79;

    BoardPanel() {
      super(TetrisGame.VERTICAL_DIMENSION - 3, TetrisGame.HORIZONTAL_DIMENSION, BlockDisplayPanel.DEFAULT_BLOCK_DIMENSION);
      setFocusable(true);
      setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
    }

    void enableKeyHandler() {
      addKeyListener(keyHandler);
    }

    void disableKeyHandler() {
      removeKeyListener(keyHandler);
    }

    void spiralClear() {
      try {
        game.persistActiveBlockColors();
        game.clearActiveBlock();

        Collection<ColoredSquare> spiralSquares = new LinkedHashSet<>();

        int nextLeftCol = 0,
            nextRightCol = TetrisGame.HORIZONTAL_DIMENSION - 1,
            nextTopRow = 3,
            nextBottomRow = TetrisGame.VERTICAL_DIMENSION - 1;

        int maxSquares = TetrisGame.VERTICAL_DIMENSION * TetrisGame.HORIZONTAL_DIMENSION;
        maxSquares -= (3 * TetrisGame.HORIZONTAL_DIMENSION); // Knock off invisible rows at top

        while (spiralSquares.size() < maxSquares) {

          // All cells in the next leftmost column
          for (int row = nextTopRow; row <= nextBottomRow; row++) {
            spiralSquares.add(new ColoredSquare(row, nextLeftCol));
          }
          nextLeftCol++;

          // All cells in the next bottom row
          for (int col = nextLeftCol; col <= nextRightCol; col++) {
            spiralSquares.add(new ColoredSquare(nextBottomRow, col));
          }
          nextBottomRow--;

          // All cells in the next rightmost column
          for (int row = nextBottomRow; row >= nextTopRow; row--) {
            spiralSquares.add(new ColoredSquare(row, nextRightCol));
          }
          nextRightCol--;

          // All cells in the next top row
          for (int col = nextRightCol; col >= nextLeftCol; col--) {
            spiralSquares.add(new ColoredSquare(nextTopRow, col));
          }
          nextTopRow++;
        }

        // Run 1 loop to paint in all unoccupied squares
        for (ColoredSquare spiralSquare : spiralSquares) {
          if (game.isOpenAndInBounds(spiralSquare.getRow(), spiralSquare.getColumn())) {
            game.setColor(spiralSquare.getRow(), spiralSquare.getColumn(), spiralSquare.getColor());
          }
          repaint();
          Thread.sleep(SPIRAL_SLEEP_INTERVAL);
        }

        // Run a second loop to erase all of them
        for (ColoredSquare spiralSquare : spiralSquares) {
          game.clearSquare(spiralSquare.getRow(), spiralSquare.getColumn());
          repaint();
          Thread.sleep(SPIRAL_SLEEP_INTERVAL);
        }

          menuPanel.leaderboardButton.bindDisabledStateToFrame(new ScoreResultsFrame(scoreRepository, game));
      } catch (InterruptedException e) {
        // Will happen if new game is started before spiral clear is finished
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    void jumpClear() {
      try {

        // Fill all rows bottom to top
        for (int row = TetrisGame.VERTICAL_DIMENSION - 1; row >= 3; row --) {
          for (int col = 0; col < TetrisGame.HORIZONTAL_DIMENSION; col++) {
            if (game.isOpenAndInBounds(row, col)) {
              game.setColor(row, col, Utility.getRandomColor());
            }
          }
          repaint();
          Thread.sleep(CLEAR_SLEEP_INTERVAL);
        }

        // Clear all rows top to bottom.
        for (int row = 3; row < TetrisGame.VERTICAL_DIMENSION; row ++) {
          for (int col = 0; col < TetrisGame.HORIZONTAL_DIMENSION; col++) {
            game.clearSquare(row, col);
          }
          repaint();
          Thread.sleep(CLEAR_SLEEP_INTERVAL);
        }

        menuPanel.leaderboardButton.bindDisabledStateToFrame(new ScoreResultsFrame(scoreRepository, game));
      } catch (InterruptedException e) {
        // Will happen if we start a new game before task is done
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Collection<ColoredSquare> getCurrentColors() {
      return game.getColoredSquares();
    }

    @Override
    protected int getYCoordinate(ColoredSquare square) {
      return (square.getRow() - 3) * getUnitHeight(); // Adjusts for 3 invisible squares at top
    }

  }

  private class ScorePanel extends JPanel {

    private final GridLayout layout;

    private final JLabel scoreLabel = new JLabel("Score: 0", JLabel.CENTER) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setText("Score: " + game.getScore());
      }
    };

    private final JLabel totalLinesLabel = new JLabel("Lines: 0 / " + game.getDifficulty().getLinesPerLevel(), JLabel.CENTER) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setText("Lines: " + game.getCurrentLevelLinesCleared() + " / " + game.getDifficulty().getLinesPerLevel());
      }
    };

    private final FlashableLabel levelLabel = new FlashableLabel("Level: 1", JLabel.CENTER);

    private final JLabel timeLabel = new JLabel("Time: 00:00", JLabel.CENTER) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        String timeLabel = "Time: " + Utility.formatSeconds(game.getCurrentLevelTime());
        if (game.isTimeAttack()) {
          timeLabel += " / " + Utility.formatSeconds(game.getDifficulty().getTimeAttackSecondsPerLevel());
        }
        setText(timeLabel);
      }
    };

    private final ProgressBar linesClearedProgressBar = new ProgressBar(11, Color.GREEN) {
      @Override
      protected double getCurrentPercentage() {
        return 100.0 * (game.getCurrentLevelLinesCleared() * 1.0 / game.getDifficulty().getLinesPerLevel());
      }
    };

    private final ProgressBar timeProgressBar = new ProgressBar(11, Color.YELLOW) {
      @Override
      protected double getCurrentPercentage() {
        int currentTime = game.getCurrentLevelTime();
        int totalTime = game.getDifficulty().getTimeAttackSecondsPerLevel();
        int timeRemaining = totalTime - currentTime;

        if (timeRemaining <= 10) {
          this.barColor = Color.RED;
        } else if (timeRemaining <= 20) {
          this.barColor = Color.YELLOW;
        } else {
          this.barColor = Color.GREEN;
        }

        return 100.0 * currentTime / totalTime;
      }
    };

    ScorePanel() {
      setBorder(new TitledBorder("Score"));

      layout = new GridLayout(6, 1);
      setLayout(layout);

      scoreLabel.setFont(ARIAL_HEADER);
      totalLinesLabel.setFont(ARIAL_HEADER);
      levelLabel.setFont(ARIAL_HEADER);
      timeLabel.setFont(ARIAL_HEADER);

      add(scoreLabel);
      add(levelLabel);
      add(totalLinesLabel);
      add(SwingUtility.nestInPanel(linesClearedProgressBar));
      add(timeLabel);

      timeProgressBar.setVisible(false);
      add(SwingUtility.nestInPanel(timeProgressBar));
    }

  }

  private class SettingsPanel extends JPanel {

    private final JCheckBox ghostSquaresCheckbox;
    private final JCheckBox musicCheckbox;
    private final JCheckBox soundEffectsCheckbox;
    private final JCheckBox timeAttackCheckbox;
    private final JComboBox<Difficulty> difficultyCombobox;
    private final TetrisButton specialsButton;

    SettingsPanel() {

      musicCheckbox = new JCheckBox("Music", true);
      musicCheckbox.setToolTipText("Controls whether music is played during game play");
      musicCheckbox.addItemListener(e -> audioSystem.setSoundtrackEnabled(musicCheckbox.isSelected()));

      soundEffectsCheckbox = new JCheckBox("Sound Effects", true);
      soundEffectsCheckbox.setToolTipText("Controls whether sound effects (rotation, drop, etc.) are played");
      soundEffectsCheckbox.addItemListener(e -> audioSystem.setEffectsEnabled(soundEffectsCheckbox.isSelected()));

      ghostSquaresCheckbox = new JCheckBox("Ghost Squares", true);
      ghostSquaresCheckbox.setToolTipText("Controls whether block placement squares are shown as the block falls");
      ghostSquaresCheckbox.addItemListener(e -> {
        game.setGhostSquaresEnabled(ghostSquaresCheckbox.isSelected());
        boardPanel.repaint();
      });

      timeAttackCheckbox = new JCheckBox("Time Attack Mode", false);
      timeAttackCheckbox.addItemListener(e -> game.setTimeAttack(timeAttackCheckbox.isSelected()));
      timeAttackCheckbox.setToolTipText(
        "<html>" +
          "<p>Limits available time per level as well as grants a point bonus per level cleared:</p>" +
          "<ul>" +
            "<li>On easy, you are given <b>" + Difficulty.EASY.getTimeAttackSecondsPerLevel() + "</b> seconds per level and <b>+" + Difficulty.EASY.getTimeAttackBonus() + "</b> bonus points are awarded per level cleared</li>" +
            "<li>On medium, you are given <b>" + Difficulty.MEDIUM.getTimeAttackSecondsPerLevel() + "</b> seconds per level and <b>+" + Difficulty.MEDIUM.getTimeAttackBonus() + "</b> bonus points are awarded per level cleared</li>" +
            "<li>On hard, you are given <b>" + Difficulty.HARD.getTimeAttackSecondsPerLevel() + "</b> seconds per level and <b>+" + Difficulty.HARD.getTimeAttackBonus() + "</b> bonus points are awarded per level cleared</li>" +
          "</ul>" +
       "</html>"
      );

      difficultyCombobox = new JComboBox<>(Difficulty.values());
      difficultyCombobox.addActionListener(e -> game.setDifficulty(getSelectedDifficulty()));
      difficultyCombobox.setSelectedIndex(0);

      specialsButton = new TetrisButton("Special Pieces...");
      specialsButton.addActionListener(e ->
        specialsButton.bindDisabledStateToFrame(new SpecialPiecesFrame())
      );

      setLayout(new BorderLayout());
      setBorder(new TitledBorder("Settings"));

      List<JCheckBox> checkboxes = List.of(ghostSquaresCheckbox, musicCheckbox, soundEffectsCheckbox, timeAttackCheckbox)
              .stream()
              .filter(Component::isVisible)// Sound checkboxes will be invisible if we are running the no-sound distribution
              .collect(Collectors.toList());

      JPanel checkboxPanel = new JPanel(new GridLayout(checkboxes.size(), 1));
      for (JCheckBox checkbox : checkboxes) {
        checkboxPanel.add(checkbox);
        checkbox.setFocusable(false);
      }

      JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      diffPanel.add(new JLabel("Difficulty:  "));
      diffPanel.add(difficultyCombobox);
      difficultyCombobox.setToolTipText(
        "<html>" +
          "<p>Sets the game difficulty. The difficulty affects the following game parameters:</p>" +
          "<ul>" +
            "<li>" +
              "Number of lines required to complete each level:" +
              "<ul>" +
                "<li>" + Difficulty.EASY.getLinesPerLevel() + " on easy</li>" +
                "<li>" + Difficulty.MEDIUM.getLinesPerLevel() + " on medium</li>" +
                "<li>" + Difficulty.HARD.getLinesPerLevel() + " on hard</li>" +
              "</ul>" +
            "</li>" +
            "<li>" +
              "Bonus points awarded upon game completion:" +
              "<ul>" +
                "<li>" + Difficulty.EASY.getWinBonus() + " on easy</li>" +
                "<li>" + Difficulty.MEDIUM.getWinBonus() + " on medium</li>" +
                "<li>" + Difficulty.HARD.getWinBonus() + " on hard</li>" +
              "</ul>" +
            "</li>" +
            "<li>The likelihood of different block types appearing. Harder difficulties will cause 'easier' blocks to appear less often</li>" +
            "<li>" +
              "Initial block speed:" +
              "<ul>" +
                "<li>Initial fall delay of " + Difficulty.EASY.getInitialTimerDelay() + " milliseconds on easy</li>" +
                "<li>Initial fall delay of " + Difficulty.MEDIUM.getInitialTimerDelay() + " milliseconds on medium</li>" +
                "<li>Initial fall delay of " + Difficulty.HARD.getInitialTimerDelay() + " milliseconds on hard</li>" +
              "</ul>" +
            "</li>" +
            "<p>The block falling speed increases at a rate of " + Difficulty.TIMER_SPEEDUP + " milliseconds per level, regardless of difficulty</p>" +
          "</ul>" +
        "</html>"
      );

      add(checkboxPanel, BorderLayout.NORTH);
      add(diffPanel, BorderLayout.CENTER);
      add(specialsButton, BorderLayout.SOUTH);
    }

    Difficulty getSelectedDifficulty() {
      return (Difficulty) difficultyCombobox.getSelectedItem();
    }

  }

  private class MenuPanel extends JPanel {

    final TetrisButton startButton = new TetrisButton("Start");
    final TetrisButton pauseButton = new TetrisButton("Pause");
    final TetrisButton resumeButton = new TetrisButton("Resume");
    final TetrisButton giveUpButton = new TetrisButton("Give Up");
    final TetrisButton leaderboardButton = new TetrisButton("Leaderboard");

    private MenuPanel() {

      setLayout(new FlowLayout());

      startButton.setMnemonic('s');
      startButton.setEnabled(true);
      startButton.addActionListener(e -> onStart());
      add(startButton);

      pauseButton.setMnemonic('p');
      pauseButton.setEnabled(false);
      pauseButton.addActionListener(e -> onPause());
      add(pauseButton);

      resumeButton.setMnemonic('r');
      resumeButton.setEnabled(false);
      resumeButton.addActionListener(e -> onResume());
      add(resumeButton);

      leaderboardButton.setMnemonic('l');
      leaderboardButton.setEnabled(true);
      leaderboardButton.addActionListener(e ->
        leaderboardButton.bindDisabledStateToFrame(new LeaderBoardFrame(scoreRepository))
      );
      add(leaderboardButton);

      giveUpButton.setMnemonic('g');
      giveUpButton.setEnabled(false);
      giveUpButton.addActionListener(e -> onGameOver());
      add(giveUpButton);
    }

  }

  private class SpecialPiecesFrame extends JFrame {

    SpecialPiecesFrame() {
      TetrisButton closeButton = new TetrisButton("Close");
      closeButton.addActionListener(e -> dispose());

      Collection<BlockType> specialBlocks = BlockType.getSpecialBlocks();
      JPanel blockPanels = new JPanel(new GridLayout(1, specialBlocks.size()));
      for (BlockType specialType : specialBlocks) {

        BlockDisplayPanel display = new BlockDisplayPanel("\"" + specialType + "\"", new Block(specialType));
        BlockEnabledToggleButton blockEnabledToggleButton = new BlockEnabledToggleButton(specialType);

        JLabel pointBonus = new JLabel("+" + specialType.getBonusPointsPerLine() + " points per line");
        pointBonus.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel toggleControlPanel = new JPanel(new BorderLayout());
        toggleControlPanel.add(blockEnabledToggleButton, BorderLayout.NORTH);
        toggleControlPanel.add(pointBonus, BorderLayout.SOUTH);

        JPanel blockPanel = new JPanel(new BorderLayout());
        blockPanel.add(display, BorderLayout.CENTER);
        blockPanel.add(toggleControlPanel, BorderLayout.SOUTH);
        blockPanels.add(blockPanel);
      }

      add(blockPanels, BorderLayout.CENTER);
      add(SwingUtility.nestInPanel(closeButton), BorderLayout.SOUTH);

      SwingUtility.setIcon(this, "/images/star.png");
      setTitle("Special Pieces");
      setResizable(false);
      pack();
      setLocationRelativeTo(null);
      setVisible(true);
    }

    private class BlockEnabledToggleButton extends JButton {

      private final BlockType blockType;
      private boolean active;

      private BlockEnabledToggleButton(BlockType blockType) {
        this.blockType = blockType;
        setActiveState(game.getConveyor().isEnabled(blockType));
        setFocusable(false);
        addMouseMotionListener(new MouseAdapter() {
          public void mouseMoved(MouseEvent e) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
          }
        });
        addActionListener(e -> toggle());
        setPreferredSize(new Dimension(getWidth(), 28));
      }

      private void toggle() {
        setActiveState(!active);
        if (active) {
          game.getConveyor().enableBlockType(settingsPanel.getSelectedDifficulty(), blockType);
        } else {
          game.getConveyor().disableBlockType(blockType);
        }
      }

      void setActiveState(boolean active) {
        this.active = active;
        setBackground(active ? Color.YELLOW : Color.LIGHT_GRAY);
        setText(active ? "Active" : "Inactive");
      }
    }
  }

}
