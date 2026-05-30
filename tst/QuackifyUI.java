import apply.StaticQuackify;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A GUI application for the Quackify music player.
 * Provides a visual interface for managing playlists and playback.
 */
public class QuackifyUI {

    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;
    private static final int FONT_SIZE_TITLE = 16;
    private static final int FONT_SIZE_INFO = 12;
    private static final int FONT_SIZE_LIST = 14;
    private static final int LED_SIZE = 12;
    private static final int CONFETTI_HEIGHT = 200;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 30;
    private static final int LEFT_PANEL_WIDTH = 260;
    private static final int INSET_SIZE = 4;
    private static final int BORDER_PADDING = 8;
    private static final int CELL_HEIGHT = 28;
    private static final int INFO_DISPLAY_TIME = 4000;
    private static final int CONFETTI_DURATION = 1000;
    private static final int CONFETTI_SLEEP = 100;
    private static final int CONFETTI_COUNT = 80;
    private static final int CONFETTI_MIN_SIZE = 4;
    private static final int CONFETTI_MAX_SIZE = 24;
    private static final int ALPHA_VALUE = 200;
    private static final int MAX_COLOR_VALUE = 256;

    private static final String[] AUDIO_URLS = {
        "https://github.com/csvistool/1332_misc/raw/refs/heads/main/audio/song_1.wav",
        "https://github.com/csvistool/1332_misc/raw/refs/heads/main/audio/song_2.wav",
        "https://github.com/csvistool/1332_misc/raw/refs/heads/main/audio/song_3.wav",
        "https://github.com/csvistool/1332_misc/raw/refs/heads/main/audio/song_4.wav",
        "https://github.com/csvistool/1332_misc/raw/refs/heads/main/audio/song_5.wav",
        "https://github.com/csvistool/1332_misc/raw/refs/heads/main/audio/song_6.wav",
        "https://github.com/csvistool/1332_misc/raw/refs/heads/main/audio/song_7.wav"
    };

    private static final Map<String, Integer> SONG_AUDIO_MAP = new HashMap<>();
    private static int nextAudioIndex = ThreadLocalRandom.current().nextInt(AUDIO_URLS.length);
    private static Clip currentClip = null;

    private static final Map<Integer, byte[]> AUDIO_CACHE = new HashMap<>();

    /**
     * Main entry point for the Quackify UI application.
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        preloadAudioCache();
        SwingUtilities.invokeLater(QuackifyUI::createAndShowGUI);
    }

    /**
     * Pre-loads all audio files into memory cache in background.
     */
    private static void preloadAudioCache() {
        new Thread(() -> {
            for (int i = 0; i < AUDIO_URLS.length; i++) {
                try {
                    URL url = new URL(AUDIO_URLS[i]);
                    byte[] audioData = url.openStream().readAllBytes();
                    synchronized (AUDIO_CACHE) {
                        AUDIO_CACHE.put(i, audioData);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to cache audio " + i + ": " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Gets the audio index for a song. Each unique song is assigned a persistent
     * audio clip index (0-6) that stays the same even if the playlist changes.
     * @param song the song name
     * @return the audio index for the song
     */
    private static int getAudioIndexForSong(String song) {
        if (!SONG_AUDIO_MAP.containsKey(song)) {
            SONG_AUDIO_MAP.put(song, nextAudioIndex);
            nextAudioIndex = (nextAudioIndex + 1) % AUDIO_URLS.length;
        }
        return SONG_AUDIO_MAP.get(song);
    }

    /**
     * Plays the audio clip associated with the given song in a loop.
     * @param song the song to play audio for
     */
    private static void playAudioForSong(String song) {
        stopAudio();
        int audioIndex = getAudioIndexForSong(song);

        new Thread(() -> {
            try {
                byte[] audioData;
                synchronized (AUDIO_CACHE) {
                    audioData = AUDIO_CACHE.get(audioIndex);
                }

                AudioInputStream audioStream;
                if (audioData != null) {
                    // Use cached data (instant)
                    audioStream = AudioSystem.getAudioInputStream(
                            new java.io.ByteArrayInputStream(audioData));
                } else {
                    // Fallback: download if not cached yet
                    URL url = new URL(AUDIO_URLS[audioIndex]);
                    audioStream = AudioSystem.getAudioInputStream(url);
                }

                Clip newClip = AudioSystem.getClip();
                newClip.open(audioStream);

                // Synchronized block to safely swap clips
                synchronized (QuackifyUI.class) {
                    stopAudio(); // Stop again in case another clip started
                    currentClip = newClip;
                    currentClip.loop(Clip.LOOP_CONTINUOUSLY);
                    currentClip.start();
                }
            } catch (Exception e) {
                System.err.println("Could not play audio: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Stops the currently playing audio clip.
     */
    private static void stopAudio() {
        synchronized (QuackifyUI.class) {
            if (currentClip != null) {
                currentClip.stop();
                currentClip.close();
                currentClip = null;
            }
        }
    }

    /**
     * Creates and displays the main GUI window.
     */
    private static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = initializeFrame();
        StaticQuackify quackify = Main.getQuackifyInstance();
        UIComponents components = createUIComponents(quackify);
        layoutComponents(frame, components);
        Runnable refresh = createRefreshRunnable(frame, quackify, components);
        setupButtonListeners(frame, quackify, components, refresh);
        configurePlaylistView(components.playlistView, components.currentIdx);
        setPlayIndicator(components.playLight, components.playStatusLabel, false);

        frame.setVisible(true);
    }

    /**
     * Initializes and configures the main application frame.
     * @return the configured JFrame
     */
    private static JFrame initializeFrame() {
        JFrame frame = new JFrame("Quackify - CS 1332 Music Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setLayout(new BorderLayout());

        try {
            java.net.URL iconURL = new java.net.URL("https://csvistool.com/favicon.png");
            ImageIcon icon = new ImageIcon(iconURL);
            frame.setIconImage(icon.getImage());
        } catch (Exception ignored) {
        }

        frame.setLocationRelativeTo(null);
        return frame;
    }

    /**
     * Creates all UI components needed for the application.
     * @param quackify the Quackify instance to initialize with
     * @return a UIComponents object containing all created components
     */
    private static UIComponents createUIComponents(StaticQuackify quackify) {
        UIComponents comp = new UIComponents();

        comp.listModel = new DefaultListModel<>();
        comp.playlistView = new JList<>(comp.listModel);
        comp.listScroll = new JScrollPane(comp.playlistView);
        comp.listScroll.setBorder(BorderFactory.createTitledBorder("Playlist"));

        comp.currentLabel = new JLabel("Now Playing: <none>");
        comp.currentLabel.setFont(comp.currentLabel.getFont().deriveFont(Font.BOLD,
                (float) FONT_SIZE_TITLE));

        comp.infoLabel = new JLabel(" ");
        comp.infoLabel.setFont(comp.infoLabel.getFont().deriveFont(Font.PLAIN,
                (float) FONT_SIZE_INFO));
        comp.infoLabel.setBorder(BorderFactory.createEmptyBorder(2, BORDER_PADDING,
                BORDER_PADDING, BORDER_PADDING));
        comp.infoLabel.setForeground(Color.DARK_GRAY);

        comp.playLight = new JLabel();
        comp.playLight.setOpaque(true);
        comp.playLight.setBackground(Color.RED);
        comp.playLight.setPreferredSize(new Dimension(LED_SIZE, LED_SIZE));

        comp.playStatusLabel = new JLabel("Music Stopped");
        comp.playStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        comp.sizeLabel = new JLabel("Playlist Size: 0");
        comp.sizeLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        comp.confetti = new ConfettiPanel();
        comp.confetti.setOpaque(false);
        comp.confetti.setPreferredSize(new Dimension(FRAME_WIDTH, CONFETTI_HEIGHT));

        comp.songField = new JTextField();
        comp.addBtn = new JButton("Add Song");
        comp.removeBtn = new JButton("Remove Song");
        comp.playBtn = new JButton("Play");
        comp.stopBtn = new JButton("Stop");
        comp.nextBtn = new JButton("Next");
        comp.randomSongBtn = new JButton("Random Song");
        comp.undoBtn = new JButton("Undo");
        comp.redoBtn = new JButton("Redo");
        comp.reverseBtn = new JButton("Reverse Playlist");

        comp.currentIdx = new int[] {-1};
        try {
            String cur = quackify.currentSong();
            comp.currentIdx[0] = comp.listModel.indexOf(cur);
        } catch (Exception ignore) {
            comp.currentIdx[0] = -1;
        }

        return comp;
    }

    /**
     * Lays out all components in the frame.
     * @param frame the main frame to add components to
     * @param comp the UIComponents containing all widgets
     */
    private static void layoutComponents(JFrame frame, UIComponents comp) {
        JPanel controls = createControlPanel(comp);
        JPanel leftWrapper = new JPanel(new BorderLayout());
        leftWrapper.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 0));
        leftWrapper.add(controls, BorderLayout.NORTH);

        JPanel topPanel = createTopPanel(comp);

        frame.add(leftWrapper, BorderLayout.WEST);
        frame.add(comp.listScroll, BorderLayout.CENTER);
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(comp.confetti, BorderLayout.SOUTH);
    }

    /**
     * Creates the control panel with all buttons.
     * @param comp the UIComponents containing buttons and text field
     * @return the configured control panel
     */
    private static JPanel createControlPanel(UIComponents comp) {
        JPanel controls = new JPanel();
        controls.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        controls.add(comp.songField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        controls.add(comp.addBtn, c);

        c.gridx = 1;
        c.gridy = 1;
        controls.add(comp.removeBtn, c);

        c.gridx = 0;
        c.gridy = 2;
        controls.add(comp.playBtn, c);

        c.gridx = 1;
        c.gridy = 2;
        controls.add(comp.stopBtn, c);

        c.gridx = 0;
        c.gridy = 3;
        controls.add(comp.nextBtn, c);

        c.gridx = 1;
        c.gridy = 3;
        controls.add(comp.randomSongBtn, c);

        c.gridx = 0;
        c.gridy = 4;
        controls.add(comp.undoBtn, c);

        c.gridx = 1;
        c.gridy = 4;
        controls.add(comp.redoBtn, c);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        controls.add(comp.reverseBtn, c);

        Dimension btnSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
        Component[] buttons = {
            comp.addBtn, comp.removeBtn, comp.playBtn, comp.stopBtn, comp.nextBtn,
            comp.randomSongBtn, comp.undoBtn, comp.redoBtn, comp.reverseBtn
        };
        for (Component button : buttons) {
            if (button instanceof JButton) {
                ((JButton) button).setPreferredSize(btnSize);
            }
        }

        controls.setBorder(BorderFactory.createTitledBorder("Controls"));
        controls.setBackground(new Color(0, 0, 0, 0));
        return controls;
    }

    /**
     * Creates the top panel with current song label and status indicators.
     * @param comp the UIComponents containing labels
     * @return the configured top panel
     */
    private static JPanel createTopPanel(UIComponents comp) {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(INSET_SIZE,
                INSET_SIZE, INSET_SIZE, INSET_SIZE));
        topPanel.add(comp.currentLabel, BorderLayout.NORTH);
        topPanel.add(comp.infoLabel, BorderLayout.SOUTH);

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setOpaque(false);

        JPanel playStatusRow = new JPanel(new FlowLayout(FlowLayout.RIGHT,
                INSET_SIZE, INSET_SIZE));
        playStatusRow.setOpaque(false);
        playStatusRow.add(comp.playLight);
        playStatusRow.add(comp.playStatusLabel);
        statusPanel.add(playStatusRow);

        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT,
                INSET_SIZE, INSET_SIZE));
        sizeRow.setOpaque(false);
        sizeRow.add(comp.sizeLabel);
        statusPanel.add(sizeRow);

        topPanel.add(statusPanel, BorderLayout.EAST);
        return topPanel;
    }

    /**
     * Creates the refresh runnable that updates the playlist display.
     * @param frame the main frame for error dialogs
     * @param quackify the Quackify instance
     * @param comp the UIComponents to update
     * @return the refresh runnable
     */
    private static Runnable createRefreshRunnable(JFrame frame, StaticQuackify quackify,
                                                  UIComponents comp) {
        return () -> {
            try {
                comp.listModel.clear();
                int n = quackify.size();
                int i = 0;
                for (String s : quackify.getPlaylist()) {
                    if (i++ >= n) {
                        break;
                    }
                    comp.listModel.addElement(s);
                }
                comp.sizeLabel.setText("Playlist Size: " + n);
                setPlayIndicator(comp.playLight, comp.playStatusLabel, quackify.isPlaying());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, "Error: " + ex.getMessage(), true);
            }
        };
    }

    /**
     * Sets up all button action listeners.
     * @param frame the main frame for error dialogs
     * @param quackify the Quackify instance
     * @param comp the UIComponents containing buttons and other widgets
     * @param refresh the refresh runnable to update the display
     */
    private static void setupButtonListeners(JFrame frame, StaticQuackify quackify,
                                             UIComponents comp, Runnable refresh) {
        comp.songField.addActionListener(e -> comp.addBtn.doClick());
        setupPlaylistModificationListeners(frame, quackify, comp, refresh);
        setupPlaybackControlListeners(frame, quackify, comp);
        setupUndoRedoListeners(frame, quackify, comp, refresh);
    }

    /**
     * Sets up listeners for playlist modification buttons (add, remove, reverse).
     * @param frame the main frame for error dialogs
     * @param quackify the Quackify instance
     * @param comp the UIComponents containing buttons and other widgets
     * @param refresh the refresh runnable to update the display
     */
    private static void setupPlaylistModificationListeners(JFrame frame, StaticQuackify quackify,
                                                           UIComponents comp, Runnable refresh) {
        comp.addBtn.addActionListener(e -> {
            String song = comp.songField.getText().trim();
            try {
                quackify.addSong(song);
                refresh.run();
                comp.songField.setText("");
                comp.songField.requestFocusInWindow();
                try {
                    if (quackify.isPalindrome()) {
                        comp.confetti.splash();
                        showInfo(comp.infoLabel, "Added: " + song
                                + " - Palindrome playlist!", false);
                    } else {
                        showInfo(comp.infoLabel, "Added: " + song, false);
                    }
                } catch (Exception ex) {
                    showInfo(comp.infoLabel, "Added: " + song, false);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });

        comp.removeBtn.addActionListener(e -> {
            String song = comp.songField.getText().trim();
            try {
                quackify.removeSong(song);
                refresh.run();
                try {
                    if (quackify.isPalindrome()) {
                        comp.confetti.splash();
                        showInfo(comp.infoLabel, "Removed: " + song
                                + " - Palindrome playlist!", false);
                    } else {
                        showInfo(comp.infoLabel, "Removed: " + song, false);
                    }
                } catch (Exception ex) {
                    showInfo(comp.infoLabel, "Removed: " + song, false);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });

        comp.reverseBtn.addActionListener(e -> {
            try {
                quackify.reverse();
                refresh.run();
                try {
                    String cur = quackify.currentSong();
                    comp.currentIdx[0] = findNextIndex(comp.listModel, cur, 0);
                } catch (Exception ex) {
                    comp.currentIdx[0] = -1;
                }
                comp.playlistView.repaint();
                try {
                    if (quackify.isPalindrome()) {
                        comp.confetti.splash();
                        showInfo(comp.infoLabel, "Reversed - Palindrome playlist!",
                                false);
                    } else {
                        showInfo(comp.infoLabel, "Reversed", false);
                    }
                } catch (Exception ex) {
                    showInfo(comp.infoLabel, "Reversed", false);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });
    }

    /**
     * Sets up listeners for playback control buttons (play, stop, next, random).
     * @param frame the main frame for error dialogs
     * @param quackify the Quackify instance
     * @param comp the UIComponents containing buttons and other widgets
     */
    private static void setupPlaybackControlListeners(JFrame frame, StaticQuackify quackify,
                                                      UIComponents comp) {
        comp.playBtn.addActionListener(e -> {
            try {
                quackify.play();
                try {
                    String cur = quackify.currentSong();
                    comp.currentLabel.setText("Now Playing: " + cur);
                    comp.currentIdx[0] = findNextIndex(comp.listModel, cur, 0);
                    playAudioForSong(cur);
                    setPlayIndicator(comp.playLight, comp.playStatusLabel, quackify.isPlaying());
                } catch (Exception ex) {
                    comp.currentLabel.setText("Now Playing: <none>");
                    comp.currentIdx[0] = -1;
                }
                comp.playlistView.repaint();
                showInfo(comp.infoLabel, "Playback started", false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });

        comp.stopBtn.addActionListener(e -> {
            try {
                quackify.stop();
                stopAudio();
                setPlayIndicator(comp.playLight, comp.playStatusLabel, quackify.isPlaying());
                comp.currentLabel.setText("Now Playing: <none>");
                comp.currentIdx[0] = -1;
                comp.playlistView.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });

        comp.nextBtn.addActionListener(e -> {
            try {
                String next = quackify.nextSong();
                comp.currentLabel.setText("Now Playing: " + next);
                comp.currentIdx[0] = findNextIndex(comp.listModel, next, comp.currentIdx[0] + 1);
                playAudioForSong(next);
                comp.playlistView.repaint();
                showInfo(comp.infoLabel, "Advanced to next song", false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });

        comp.randomSongBtn.addActionListener(e -> {
            try {
                String song = quackify.randomSong();
                comp.currentLabel.setText("Now Playing: " + song);
                comp.currentIdx[0] = findNextIndex(comp.listModel, song, comp.currentIdx[0] + 1);
                playAudioForSong(song);
                comp.playlistView.repaint();
                showInfo(comp.infoLabel, "Jumped to a random song", false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });
    }

    /**
     * Sets up listeners for undo and redo buttons.
     * @param frame the main frame for error dialogs
     * @param quackify the Quackify instance
     * @param comp the UIComponents containing buttons and other widgets
     * @param refresh the refresh runnable to update the display
     */
    private static void setupUndoRedoListeners(JFrame frame, StaticQuackify quackify,
                                               UIComponents comp, Runnable refresh) {
        comp.undoBtn.addActionListener(e -> {
            try {
                quackify.undo();
                try {
                    refresh.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    showInfo(comp.infoLabel, ex.getMessage(), true);
                }
                try {
                    String cur = quackify.currentSong();
                    comp.currentIdx[0] = findNextIndex(comp.listModel, cur, 0);
                } catch (Exception ex) {
                    comp.currentIdx[0] = -1;
                }
                comp.playlistView.repaint();
                showInfo(comp.infoLabel, "Undo", false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });

        comp.redoBtn.addActionListener(e -> {
            try {
                quackify.redo();
                try {
                    refresh.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    showInfo(comp.infoLabel, ex.getMessage(), true);
                }
                try {
                    String cur = quackify.currentSong();
                    comp.currentIdx[0] = findNextIndex(comp.listModel, cur, 0);
                } catch (Exception ex) {
                    comp.currentIdx[0] = -1;
                }
                comp.playlistView.repaint();
                showInfo(comp.infoLabel, "Redo", false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                showInfo(comp.infoLabel, ex.getMessage(), true);
            }
        });
    }

    /**
     * Configures the playlist view with custom rendering and selection behavior.
     * @param playlistView the JList to configure
     * @param currentIdx array holding the current playing song index
     */
    private static void configurePlaylistView(JList<String> playlistView, int[] currentIdx) {
        playlistView.setFont(playlistView.getFont().deriveFont(
                (float) FONT_SIZE_LIST));
        playlistView.setFixedCellHeight(CELL_HEIGHT);

        playlistView.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
            }

            @Override
            public void addSelectionInterval(int index0, int index1) {
            }
        });

        playlistView.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, false, false);
                if (currentIdx[0] == index) {
                    lbl.setBackground(new Color(200, 255, 200));
                    lbl.setOpaque(true);
                } else {
                    lbl.setOpaque(false);
                }
                lbl.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                return lbl;
            }
        });
    }

    /**
     * Displays a temporary information message in the info label.
     * @param infoLabel the label to display the message in
     * @param message the message to display
     * @param isError whether this is an error message (affects color)
     */
    private static void showInfo(JLabel infoLabel, String message,
                                 boolean isError) {
        SwingUtilities.invokeLater(() -> {
            infoLabel.setText(message);
            infoLabel.setForeground(isError ? Color.RED.darker()
                    : Color.DARK_GRAY);
        });

        Timer t = new Timer(INFO_DISPLAY_TIME,
                e -> SwingUtilities.invokeLater(() -> infoLabel.setText(" ")));
        t.setRepeats(false);
        t.start();
    }

    /**
     * Updates the play indicator light and label.
     * @param light the LED indicator label
     * @param label the status text label
     * @param playing whether music is currently playing
     */
    private static void setPlayIndicator(JLabel light, JLabel label,
                                         boolean playing) {
        SwingUtilities.invokeLater(() -> {
            light.setBackground(playing ? Color.GREEN.brighter() : Color.RED);
            label.setText(playing ? "Music Playing" : "Music Stopped");
        });
    }

    /**
     * Find the next index of value in listModel starting at fromIndex
     * (inclusive). Returns -1 if not found.
     * This helper advances through the list so duplicate song names will
     * move the highlight forward.
     * @param model the list model to search
     * @param value the value to find
     * @param fromIndex the starting index (inclusive)
     * @return the index of the value, or -1 if not found
     */
    private static int findNextIndex(DefaultListModel<String> model,
                                     String value, int fromIndex) {
        if (value == null || model == null || model.size() == 0) {
            return -1;
        }
        int n = model.size();
        int start = Math.max(0, fromIndex % n);
        for (int i = 0; i < n; i++) {
            int idx = (start + i) % n;
            if (value.equals(model.get(idx))) {
                return idx;
            }
        }
        return -1;
    }


    /**
     * Helper class to hold all UI components together.
     */
    private static class UIComponents {
        private DefaultListModel<String> listModel;
        private JList<String> playlistView;
        private JScrollPane listScroll;
        private JLabel currentLabel;
        private JLabel infoLabel;
        private JLabel playLight;
        private JLabel playStatusLabel;
        private JLabel sizeLabel;
        private ConfettiPanel confetti;
        private JTextField songField;
        private JButton addBtn;
        private JButton removeBtn;
        private JButton playBtn;
        private JButton stopBtn;
        private JButton nextBtn;
        private JButton randomSongBtn;
        private JButton undoBtn;
        private JButton redoBtn;
        private JButton reverseBtn;
        private int[] currentIdx;
    }

    /**
     * A panel that displays animated confetti effects.
     */
    static class ConfettiPanel extends JPanel {
        private volatile boolean active = false;

        /**
         * Triggers a confetti animation effect.
         */
        public void splash() {
            if (active) {
                return;
            }
            active = true;
            Thread t = new Thread(() -> {
                long end = System.currentTimeMillis() + CONFETTI_DURATION;
                while (System.currentTimeMillis() < end) {
                    repaint();
                    try {
                        Thread.sleep(CONFETTI_SLEEP);
                    } catch (InterruptedException ignored) {
                        // Ignore interruption
                    }
                }
                active = false;
                repaint();
            });
            t.setDaemon(true);
            t.start();
        }

        /**
         * Paints the confetti on the panel.
         * @param g the Graphics object to paint with
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!active) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            for (int i = 0; i < CONFETTI_COUNT; i++) {
                int red = ThreadLocalRandom.current().nextInt(MAX_COLOR_VALUE);
                int green = ThreadLocalRandom.current().nextInt(MAX_COLOR_VALUE);
                int blue = ThreadLocalRandom.current().nextInt(MAX_COLOR_VALUE);
                g2.setColor(new Color(red, green, blue, ALPHA_VALUE));
                int x = ThreadLocalRandom.current().nextInt(Math.max(1,
                        getWidth()));
                int y = ThreadLocalRandom.current().nextInt(Math.max(1,
                        getHeight()));
                int size = ThreadLocalRandom.current().nextInt(CONFETTI_MIN_SIZE,
                        CONFETTI_MAX_SIZE);
                g2.fillOval(x, y, size, size);
            }
            g2.dispose();
        }
    }
}
