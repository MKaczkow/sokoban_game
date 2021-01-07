package elkaproj.ui;

import elkaproj.DebugWriter;
import elkaproj.config.IConfiguration;
import elkaproj.config.ILevel;
import elkaproj.config.ILevelPack;
import elkaproj.config.language.Language;
import elkaproj.game.GameController;
import elkaproj.game.IGameLifecycleHandler;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Main window class of the game. Holds all other components.
 */
public class GuiRootFrame extends JFrame implements ActionListener, IGameLifecycleHandler {

    public static final String STRING_WIN_DIALOG_TITLE_L10N_ID = "dialogs.win.title";
    public static final String STRING_WIN_DIALOG_CONTENTS_L10N_ID = "dialogs.win.message";
    public static final String STRING_AUTHORS_DIALOG_TITLE_L10N_ID = "dialogs.authors.title";
    public static final String STRING_AUTHORS_DIALOG_CONTENTS_L10N_ID = "dialogs.authors.message";

    public static final String COMMAND_EXIT = "PROZEkt_exit";
    public static final String COMMAND_PAUSE_RESUME = "PROZEkt_pause_resume";
    public static final String COMMAND_STOP = "PROZEkt_stop";
    public static final String COMMAND_RESET = "PROZEkt_reset";
    public static final String COMMAND_SCOREBOARD = "PROZEkt_highscores";
    public static final String COMMAND_AUTHORS = "PROZEkt_authors";
    public static final String COMMAND_CONFIRM_PLAYERNAME = "PROZEkt_confirm_playername";
    public static final String COMMAND_NEW_GAME = "PROZEkt_new_game";

    private final Language language;

    private final GuiStatusPanel statusPanel;

    private final GuiPlayerNameView playerNameView;
    private String playerName = null;

    private final GuiMainMenuView mainMenuView;

    private final GuiCanvas gameView;
    private final GameController gameController;

    private Component activeComponent = null;

    /**
     * Creates an initializes a new game window.
     * @param language UI language to use.
     * @param configuration Configuration for the game.
     * @param levelPack Level pack the player will play through.
     * @throws IOException Texture loading failed.
     */
    public GuiRootFrame(Language language, IConfiguration configuration, ILevelPack levelPack) throws IOException {
        super("@window.title");

        // set language and controller
        this.language = language;

        this.gameController = new GameController(configuration, levelPack);
        this.gameController.addLifecycleHandler(this);

        // set the listener so we can close the application
        this.addWindowListener(new GameFrameWindowAdapter(this));

        // set layout
        this.setLayout(new BorderLayout());

        // set size and location
        this.setSize(640, 480);
        this.setMinimumSize(new Dimension(640, 480));
        this.setLocationRelativeTo(null); // center on screen

        // add UI components
        this.setJMenuBar(new GuiMenuBar(this, this.gameController));

        this.playerNameView = new GuiPlayerNameView(this);
        this.setActiveView(this.playerNameView);

        this.mainMenuView = new GuiMainMenuView(this);
        this.gameView = new GuiCanvas(this.gameController, this.language.getValue("misc.paused"));

        this.statusPanel = new GuiStatusPanel(this.gameController, this.getSize(), this.language);
        this.add(this.statusPanel, BorderLayout.SOUTH);
    }

    private void localize(Component[] components) {
        for (Component component : components) {
            if (component instanceof Container) {
                this.localize(((Container) component).getComponents());
            }

            if (component instanceof JMenu) {
                int count = ((JMenu) component).getItemCount();
                Component[] items = new Component[count];
                for (int i = 0; i < count; i++) {
                    items[i] = ((JMenu) component).getItem(i);
                }
                this.localize(items);
            }

            if (component instanceof Frame) {
                String title = ((Frame) component).getTitle();
                if (title.startsWith("@"))
                    ((Frame) component).setTitle(this.language.getValue(title.substring(1)));
            }

            if (component instanceof AbstractButton) {
                String text = ((AbstractButton) component).getText();
                if (text.startsWith("@"))
                    ((AbstractButton) component).setText(this.language.getValue(text.substring(1)));
            }

            if (component instanceof JLabel) {
                String text = ((JLabel) component).getText();
                if (text.startsWith("@"))
                    ((JLabel) component).setText(this.language.getValue(text.substring(1)));
            }

            if (component instanceof JTextComponent) {
                String text = ((JTextComponent) component).getText();
                if (text.startsWith("@"))
                    ((JTextComponent) component).setText(this.language.getValue(text.substring(1)));
            }
        }
    }

    @Override
    public void paint(Graphics graphics) {
        this.localize(new Component[] { this });
        super.paint(graphics);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        DebugWriter.INSTANCE.logMessage("EVENT-UI-ACTION", "Event triggered: %s", actionEvent.getActionCommand());

        switch (actionEvent.getActionCommand()) {
            case COMMAND_EXIT:
                this.gameView.performShutdown();
                this.dispose();
                break;

            case COMMAND_CONFIRM_PLAYERNAME:
                this.playerName = this.playerNameView.getPlayerName();

                if (this.playerName != null) {
                    DebugWriter.INSTANCE.logMessage("PLAYER", "New player name (%d): '%s'", this.playerName.length(), this.playerName);

                    this.mainMenuView.setPlayerName(this.playerName);
                    this.setActiveView(this.mainMenuView);
                    this.forceUpdate();
                }

                break;

            case COMMAND_NEW_GAME:
                this.gameController.startGame();
                break;

            case COMMAND_STOP:
                this.gameController.stopGame(false);
                break;
            
            case COMMAND_AUTHORS:
                JOptionPane.showMessageDialog(this,
                        this.language.getValue(STRING_AUTHORS_DIALOG_CONTENTS_L10N_ID),
                        this.language.getValue(STRING_AUTHORS_DIALOG_TITLE_L10N_ID),
                        JOptionPane.INFORMATION_MESSAGE);
                break;

            case COMMAND_SCOREBOARD:

                break;

            case COMMAND_RESET:
                this.gameController.resetLevel();
                break;

            case COMMAND_PAUSE_RESUME:
                this.gameController.togglePause();
                break;
        }
    }

    @Override
    public void onGameStarted(ILevel currentLevel, int currentLives) {
        this.setActiveView(this.gameView);
        this.gameView.updateBufferStrategy();
    }

    @Override
    public void onGameStopped(int totalScore, boolean completed) {
        if (completed)
            JOptionPane.showMessageDialog(this,
                    String.format(this.language.getValue(STRING_WIN_DIALOG_CONTENTS_L10N_ID), totalScore),
                    this.language.getValue(STRING_WIN_DIALOG_TITLE_L10N_ID),
                    JOptionPane.INFORMATION_MESSAGE);

        this.setActiveView(this.mainMenuView);
    }

    private void setActiveView(Component component) {
        if (this.activeComponent != null)
            this.remove(this.activeComponent);

        this.add(component, BorderLayout.CENTER);
        this.activeComponent = component;
        this.forceUpdate();
        this.activeComponent.requestFocus();
    }

    private void forceUpdate() {
        this.repaint();
        this.revalidate();
    }

    private static class GameFrameWindowAdapter extends WindowAdapter {

        private final GuiRootFrame guiRootFrame;

        public GameFrameWindowAdapter(GuiRootFrame guiRootFrame) {
            this.guiRootFrame = guiRootFrame;
        }

        @Override
        public void windowClosing(WindowEvent windowEvent) {
            System.exit(0);
            this.guiRootFrame.gameView.performShutdown();
        }

        @Override
        public void windowActivated(WindowEvent windowEvent) {
            this.guiRootFrame.activeComponent.requestFocus();
        }
    }
}
