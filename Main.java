import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static final int GAME_WIDTH = 1280;
    static final int GAME_HEIGHT = 720;
    static final Path SAVE_FILE = Path.of(System.getProperty("user.home"), ".novel-game-save.properties");
    static JFrame frame;
    static CardLayout cardLayout;
    static JPanel mainPanel;
    static GamePanel gamePanel;
    static int dialogueFontSize = 20;
    static int textDelayMs = 35;
    static final int INSTANT_TEXT_DELAY = 10;
    static int masterVolume = 80;
    static final Color INK = new Color(10, 13, 29);
    static final Color SURFACE = new Color(20, 25, 49, 225);
    static final Color ACCENT = new Color(255, 179, 104);
    static final Color MUTED = new Color(177, 190, 222);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    static void createWindow() {
        frame = new JFrame("Novel Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        gamePanel = new GamePanel();
        mainPanel.add(createTitlePanel(), "TITLE");
        mainPanel.add(gamePanel, "GAME");
        mainPanel.add(createSettingsPanel(), "SETTINGS");
        mainPanel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        frame.setContentPane(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(640, 400));
        setupKeys();
        showTitle();
        frame.setVisible(true);
    }

    static JPanel createTitlePanel() {
        JPanel panel = new AtmospherePanel();
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("INTERACTIVE STORY  /  01");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 12));
        eyebrow.setForeground(ACCENT);
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("NOVEL GAME");
        title.setFont(new Font("Serif", Font.BOLD, 54));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startButton = createMenuButton("はじめから", true);
        JButton loadButton = createMenuButton("ロード", false);
        JButton settingsButton = createMenuButton("設定", false);
        JButton exitButton = createMenuButton("終了", false);
        for (JButton button : new JButton[] {startButton, loadButton, settingsButton, exitButton}) {
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            Dimension buttonSize = new Dimension(300, 54);
            button.setMinimumSize(buttonSize);
            button.setPreferredSize(buttonSize);
            button.setMaximumSize(buttonSize);
        }
        menu.setPreferredSize(new Dimension(360, 400));
        menu.add(eyebrow);
        menu.add(Box.createVerticalStrut(12));
        menu.add(title);
        menu.add(Box.createVerticalStrut(32));
        menu.add(startButton);
        menu.add(Box.createVerticalStrut(10));
        menu.add(loadButton);
        menu.add(Box.createVerticalStrut(10));
        menu.add(settingsButton);
        menu.add(Box.createVerticalStrut(10));
        menu.add(exitButton);
        panel.add(menu);
        startButton.addActionListener(e -> showGame());
        loadButton.addActionListener(e -> { if (loadGame()) showGame(); });
        settingsButton.addActionListener(e -> showSettings());
        exitButton.addActionListener(e -> frame.dispose());
        return panel;
    }

    static JPanel createSettingsPanel() {
        JPanel panel = new AtmospherePanel();

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("SETTINGS");
        title.setFont(new Font("Serif", Font.BOLD, 44));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel fontSizeLabel = new JLabel("\u4f1a\u8a71\u30c6\u30ad\u30b9\u30c8\u306e\u5927\u304d\u3055");
        fontSizeLabel.setForeground(MUTED);
        fontSizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JSlider fontSizeSlider = new JSlider(18, 32, dialogueFontSize);
        fontSizeSlider.setMaximumSize(new Dimension(260, 50));
        fontSizeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel textSpeedLabel = new JLabel("\u6587\u5b57\u9001\u308a\u306e\u901f\u5ea6\uff08\u9045\u3044 \u2190 \u2192 \u901f\u3044\uff09");
        textSpeedLabel.setForeground(MUTED);
        textSpeedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JSlider textSpeedSlider = new JSlider(INSTANT_TEXT_DELAY, 150, textDelayMs);
        textSpeedSlider.setInverted(true);
        textSpeedSlider.setMaximumSize(new Dimension(260, 50));
        textSpeedSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
        String speedPreviewText = "\u3053\u306e\u6587\u7ae0\u3067\u6587\u5b57\u30b5\u30a4\u30ba\u3068\u6587\u5b57\u9001\u308a\u901f\u5ea6\u3092\u78ba\u8a8d\u3067\u304d\u307e\u3059\u3002";
        JLabel speedPreview = new JLabel("");
        speedPreview.setForeground(new Color(255, 220, 150));
        speedPreview.setFont(new Font("SansSerif", Font.PLAIN, dialogueFontSize));
        speedPreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        int[] previewCharacters = {0};
        Timer speedPreviewTimer = new Timer(textDelayMs, e -> {
            if (previewCharacters[0] < speedPreviewText.length()) {
                previewCharacters[0]++;
                speedPreview.setText(speedPreviewText.substring(0, previewCharacters[0]));
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        speedPreviewTimer.start();
        textSpeedSlider.addChangeListener(e -> {
            textDelayMs = textSpeedSlider.getValue();
            gamePanel.textTimer.setDelay(textDelayMs);
            gamePanel.textTimer.setInitialDelay(textDelayMs);
            speedPreviewTimer.setDelay(textDelayMs);
            speedPreviewTimer.setInitialDelay(textDelayMs);
            if (textDelayMs == INSTANT_TEXT_DELAY) {
                previewCharacters[0] = speedPreviewText.length();
                speedPreview.setText(speedPreviewText);
                speedPreviewTimer.stop();
                gamePanel.finishTextAnimation();
            } else {
                previewCharacters[0] = 0;
                speedPreview.setText("");
                speedPreviewTimer.restart();
            }
        });
        fontSizeSlider.addChangeListener(e -> {
            dialogueFontSize = fontSizeSlider.getValue();
            speedPreview.setFont(new Font("SansSerif", Font.PLAIN, dialogueFontSize));
            gamePanel.repaint();
            previewCharacters[0] = speedPreviewText.length();
            speedPreview.setText(speedPreviewText);
            speedPreviewTimer.stop();
        });

        JLabel volumeLabel = new JLabel("\u97f3\u91cf: " + masterVolume + "%");
        volumeLabel.setForeground(MUTED);
        volumeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JSlider volumeSlider = new JSlider(0, 100, masterVolume);
        volumeSlider.setMaximumSize(new Dimension(260, 50));
        volumeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
        volumeSlider.addChangeListener(e -> {
            masterVolume = volumeSlider.getValue();
            volumeLabel.setText("\u97f3\u91cf: " + masterVolume + "%");
        });

        JPanel previewBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                super.paintComponent(g);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        previewBox.setOpaque(false);
        previewBox.setLayout(new BoxLayout(previewBox, BoxLayout.Y_AXIS));
        previewBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewBox.setMinimumSize(new Dimension(900, 120));
        previewBox.setPreferredSize(new Dimension(900, 120));
        previewBox.setMaximumSize(new Dimension(900, 120));
        previewBox.add(Box.createVerticalStrut(47));
        previewBox.add(speedPreview);

        JButton backButton = createMenuButton("タイトルへ戻る", false);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(240, 50));
        backButton.addActionListener(e -> showTitle());

        menu.add(title);
        menu.add(Box.createVerticalStrut(20));
        menu.add(previewBox);
        menu.add(Box.createVerticalStrut(18));
        menu.add(fontSizeLabel);
        menu.add(Box.createVerticalStrut(10));
        menu.add(fontSizeSlider);
        menu.add(Box.createVerticalStrut(25));
        menu.add(textSpeedLabel);
        menu.add(Box.createVerticalStrut(10));
        menu.add(textSpeedSlider);
        menu.add(Box.createVerticalStrut(25));
        menu.add(volumeLabel);
        menu.add(Box.createVerticalStrut(10));
        menu.add(volumeSlider);
        menu.add(Box.createVerticalStrut(25));
        menu.add(backButton);
        panel.add(menu);
        return panel;
    }

    static void showTitle() { cardLayout.show(mainPanel, "TITLE"); }

    static void showSettings() { cardLayout.show(mainPanel, "SETTINGS"); }

    static void showGame() {
        cardLayout.show(mainPanel, "GAME");
        gamePanel.requestFocusInWindow();
    }

    static void setupKeys() {
        JRootPane root = frame.getRootPane();
        InputMap input = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = root.getActionMap();
        input.put(KeyStroke.getKeyStroke("H"), "toggleDialogue");
        input.put(KeyStroke.getKeyStroke("I"), "toggleInventory");
        input.put(KeyStroke.getKeyStroke("F5"), "saveGame");
        input.put(KeyStroke.getKeyStroke("F9"), "loadGame");
        actions.put("toggleDialogue", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (gamePanel.isShowing()) gamePanel.toggleDialogue();
            }
        });
        actions.put("toggleInventory", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (gamePanel.isShowing()) gamePanel.toggleInventory();
            }
        });
        actions.put("saveGame", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (gamePanel.isShowing()) saveGame();
            }
        });
        actions.put("loadGame", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (gamePanel.isShowing()) loadGame();
            }
        });
    }

    static void saveGame() {
        Properties data = new Properties();
        data.setProperty("dialogueVisible", Boolean.toString(gamePanel.dialogueVisible));
        data.setProperty("characterName", gamePanel.characterName);
        data.setProperty("message", gamePanel.message);
        data.setProperty("visibleCharacters", Integer.toString(gamePanel.visibleCharacters));
        try (Writer writer = Files.newBufferedWriter(SAVE_FILE, StandardCharsets.UTF_8)) {
            data.store(writer, "Novel Game Save");
            showSaveMessage("\u30bb\u30fc\u30d6\u3057\u307e\u3057\u305f\u3002");
        } catch (IOException exception) {
            showSaveMessage("\u30bb\u30fc\u30d6\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002");
        }
    }

    static boolean loadGame() {
        if (!Files.exists(SAVE_FILE)) {
            showSaveMessage("\u30bb\u30fc\u30d6\u30c7\u30fc\u30bf\u304c\u3042\u308a\u307e\u305b\u3093\u3002");
            return false;
        }
        Properties data = new Properties();
        try (Reader reader = Files.newBufferedReader(SAVE_FILE, StandardCharsets.UTF_8)) {
            data.load(reader);
            gamePanel.dialogueVisible = Boolean.parseBoolean(data.getProperty("dialogueVisible", "true"));
            gamePanel.characterName = data.getProperty("characterName", gamePanel.characterName);
            gamePanel.message = data.getProperty("message", gamePanel.message);
            gamePanel.visibleCharacters = Math.max(0, Math.min(
                Integer.parseInt(data.getProperty("visibleCharacters", "0")),
                gamePanel.message.length()
            ));
            if (gamePanel.visibleCharacters < gamePanel.message.length()) {
                gamePanel.textTimer.start();
            }
            gamePanel.repaint();
            showSaveMessage("\u30ed\u30fc\u30c9\u3057\u307e\u3057\u305f\u3002");
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            showSaveMessage("\u30ed\u30fc\u30c9\u306b\u5931\u6557\u3057\u307e\u3057\u305f\u3002");
            return false;
        }
    }

    static void showSaveMessage(String message) {
        JOptionPane.showMessageDialog(frame, message, "NOVEL GAME", JOptionPane.INFORMATION_MESSAGE);
    }

    static void showBacklog() {
        StringBuilder text = new StringBuilder();
        for (String line : gamePanel.backlog) {
            text.append(line).append(System.lineSeparator());
        }
        JOptionPane.showMessageDialog(frame, text.toString(), "\u30d0\u30c3\u30af\u30ed\u30b0", JOptionPane.INFORMATION_MESSAGE);
    }

    static void confirmReturnToTitle() {
        int choice = JOptionPane.showConfirmDialog(
            frame,
            "\u30bf\u30a4\u30c8\u30eb\u306b\u623b\u308a\u307e\u3059\u304b\uff1f",
            "\u30bf\u30a4\u30c8\u30eb\u3078\u623b\u308b",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            showTitle();
        }
    }

    static JButton createMenuButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setForeground(primary ? INK : Color.WHITE);
        button.setBackground(primary ? ACCENT : new Color(255, 255, 255, 20));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                boolean hover = b.getModel().isRollover();
                g2.setColor(primary ? (hover ? new Color(255, 199, 132) : ACCENT)
                    : (hover ? new Color(255, 255, 255, 45) : new Color(255, 255, 255, 20)));
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 14, 14);
                if (!primary) {
                    g2.setColor(new Color(255, 255, 255, hover ? 100 : 55));
                    g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
                }
                g2.dispose();
                super.paint(g, c);
            }
        });
        return button;
    }

    static class AtmospherePanel extends JPanel {
        AtmospherePanel() {
            super(new GridBagLayout());
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, new Color(17, 22, 49), getWidth(), getHeight(), new Color(54, 28, 72)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 179, 104, 28));
            g2.fillOval(getWidth() - 400, -180, 500, 500);
            g2.setColor(new Color(128, 174, 255, 22));
            g2.fillOval(-190, getHeight() - 270, 520, 520);
            g2.dispose();
        }
    }

    static class GamePanel extends JPanel {
        boolean dialogueVisible = true;
        boolean inventoryVisible = false;
        BufferedImage backgroundImage;
        BufferedImage characterImage;
        BufferedImage eventImage;
        String characterName = "\u30ad\u30e3\u30e9\u30af\u30bf\u30fc";
        String message = "\u3053\u3053\u306b\u30bb\u30ea\u30d5\u3092\u8868\u793a\u3057\u307e\u3059\u3002";
        List<String> backlog = new ArrayList<>();
        List<String> items = new ArrayList<>();
        String selectedItem;
        int visibleCharacters = 0;
        Timer textTimer;

        GamePanel() {
            setBackground(Color.BLACK);
            setFocusable(true);
            backlog.add(characterName + "\u3000" + message);
            items.add("\u53e4\u3044\u9375");
            items.add("\u6708\u306e\u6b20\u7247");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (!handleDialogueMenuClick(e.getX(), e.getY())) {
                        finishTextAnimation();
                    }
                }
            });
            textTimer = new Timer(textDelayMs, e -> advanceTextAnimation());
            startTextAnimation();
        }

        void toggleDialogue() {
            dialogueVisible = !dialogueVisible;
            repaint();
        }

        void toggleInventory() {
            inventoryVisible = !inventoryVisible;
            repaint();
        }

        boolean handleDialogueMenuClick(int mouseX, int mouseY) {
            double scale = Math.min(getWidth() / (double) GAME_WIDTH,
                getHeight() / (double) GAME_HEIGHT);
            int x = (int) ((mouseX - (getWidth() - GAME_WIDTH * scale) / 2) / scale);
            int y = (int) ((mouseY - (getHeight() - GAME_HEIGHT * scale) / 2) / scale);
            if (inventoryVisible) {
                int itemIndex = (y - 230) / 42;
                if (x >= 300 && x <= 700 && y >= 230 && itemIndex >= 0 && itemIndex < items.size()) {
                    selectedItem = items.get(itemIndex);
                    repaint();
                } else {
                    toggleInventory();
                }
                return true;
            }
            if (!dialogueVisible) return false;
            if (y < 640 || y > 680) return false;

            if (x >= 850 && x < 905) {
                saveGame();
                return true;
            } else if (x >= 910 && x < 965) {
                loadGame();
                return true;
            } else if (x >= 970 && x < 1045) {
                showBacklog();
                return true;
            } else if (x >= 1050 && x < 1110) {
                confirmReturnToTitle();
                return true;
            } else if (x >= 1120 && x < 1185) {
                toggleInventory();
                return true;
            }
            return false;
        }

        void startTextAnimation() {
            visibleCharacters = 0;
            if (textDelayMs == INSTANT_TEXT_DELAY) {
                finishTextAnimation();
            } else {
                textTimer.start();
            }
        }

        void advanceTextAnimation() {
            if (textDelayMs == INSTANT_TEXT_DELAY) {
                finishTextAnimation();
                return;
            }
            if (visibleCharacters < message.length()) {
                visibleCharacters++;
                repaint();
            }
            if (visibleCharacters >= message.length()) {
                textTimer.stop();
            }
        }

        void finishTextAnimation() {
            if (visibleCharacters < message.length()) {
                visibleCharacters = message.length();
                textTimer.stop();
                repaint();
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            double scale = Math.min(getWidth() / (double) GAME_WIDTH, getHeight() / (double) GAME_HEIGHT);
            int drawWidth = (int) (GAME_WIDTH * scale);
            int drawHeight = (int) (GAME_HEIGHT * scale);
            g2.translate((getWidth() - drawWidth) / 2, (getHeight() - drawHeight) / 2);
            g2.scale(scale, scale);
            drawBackground(g2);
            drawCharacter(g2);
            drawEventImage(g2);
            if (dialogueVisible) drawDialogue(g2);
            if (inventoryVisible) drawInventory(g2);
            g2.dispose();
        }

        void drawBackground(Graphics2D g2) {
            if (backgroundImage != null) {
                g2.drawImage(backgroundImage, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(22, 29, 59), 0, GAME_HEIGHT, new Color(86, 46, 83)));
                g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                g2.setColor(new Color(255, 208, 142, 40));
                g2.fillOval(875, 62, 220, 220);
                g2.setColor(new Color(10, 13, 29, 90));
                int[] x = {0, 0, 260, 425, 600, 790, 980, 1280, 1280};
                int[] y = {500, 720, 720, 540, 720, 490, 720, 540, 720};
                g2.fillPolygon(x, y, x.length);
                g2.setColor(new Color(255, 255, 255, 28));
                for (int i = 0; i < 42; i++) {
                    int sx = (i * 149) % GAME_WIDTH;
                    int sy = 35 + (i * 83) % 390;
                    int size = i % 3 + 1;
                    g2.fillOval(sx, sy, size, size);
                }
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setFont(new Font("Serif", Font.ITALIC, 20));
                g2.drawString("A story begins beneath the quiet moon.", 72, 104);
            }
        }

        void drawCharacter(Graphics2D g2) {
            if (characterImage != null && eventImage == null) {
                g2.drawImage(characterImage, 700, 80, 450, 640, null);
            }
        }

        void drawEventImage(Graphics2D g2) {
            if (eventImage != null) g2.drawImage(eventImage, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
        }

        void drawDialogue(Graphics2D g2) {
            g2.setColor(new Color(7, 10, 25, 218));
            g2.fillRoundRect(50, 482, 1180, 198, 22, 22);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.drawRoundRect(50, 482, 1179, 197, 22, 22);
            g2.setColor(ACCENT);
            g2.fillRoundRect(76, 504, 236, 43, 12, 12);
            g2.setColor(INK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            g2.drawString(characterName, 94, 533);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, dialogueFontSize));
            g2.drawString(message.substring(0, Math.min(visibleCharacters, message.length())), 85, 586);

            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(MUTED);
            g2.drawString("\u30bb\u30fc\u30d6", 850, 665);
            g2.drawString("\u30ed\u30fc\u30c9", 910, 665);
            g2.drawString("\u30d0\u30c3\u30af\u30ed\u30b0", 970, 665);
            g2.drawString("\u30bf\u30a4\u30c8\u30eb", 1050, 665);
            g2.drawString("\u30a2\u30a4\u30c6\u30e0", 1120, 665);
            g2.setColor(ACCENT);
            g2.drawString("\u25bc", 1190, 630);
        }

        void drawInventory(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            g2.setColor(new Color(12, 18, 40, 242));
            g2.fillRoundRect(260, 145, 760, 365, 24, 24);
            g2.setColor(new Color(255, 255, 255, 75));
            g2.drawRoundRect(260, 145, 759, 364, 24, 24);
            g2.setColor(ACCENT);
            g2.setFont(new Font("Serif", Font.BOLD, 30));
            g2.drawString("ITEMS", 305, 205);
            g2.setColor(MUTED);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            if (items.isEmpty()) {
                g2.drawString("\u30a2\u30a4\u30c6\u30e0\u306f\u3042\u308a\u307e\u305b\u3093\u3002", 305, 265);
            } else {
                for (int i = 0; i < items.size(); i++) {
                    boolean selected = items.get(i).equals(selectedItem);
                    g2.setColor(selected ? ACCENT : MUTED);
                    g2.fillOval(310, 244 + i * 42, 9, 9);
                    g2.setColor(selected ? ACCENT : Color.WHITE);
                    g2.drawString(items.get(i), 334, 255 + i * 42);
                }
            }
            g2.setColor(new Color(255, 255, 255, 45));
            g2.drawLine(305, 330, 975, 330);
            g2.setColor(ACCENT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("\u8a73\u7d30", 305, 365);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g2.drawString(selectedItem == null ? "\u30a2\u30a4\u30c6\u30e0\u3092\u30af\u30ea\u30c3\u30af\u3057\u3066\u8a73\u7d30\u3092\u8868\u793a\u3057\u307e\u3059\u3002" : itemDescription(selectedItem), 305, 400);
            g2.setColor(MUTED);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g2.drawString("\u30af\u30ea\u30c3\u30af\u307e\u305f\u306f I \u30ad\u30fc\u3067\u9589\u3058\u308b", 305, 470);
        }

        String itemDescription(String item) {
            if ("\u53e4\u3044\u9375".equals(item)) {
                return "\u9306\u3073\u3066\u3044\u308b\u304c\u3001\u307e\u3060\u4f55\u304b\u3092\u958b\u3051\u3089\u308c\u305d\u3046\u3060\u3002";
            }
            if ("\u6708\u306e\u6b20\u7247".equals(item)) {
                return "\u6de1\u3044\u5149\u3092\u653e\u3064\u3001\u534a\u900f\u660e\u306a\u6b20\u7247\u3002";
            }
            return "\u8a73\u7d30\u306f\u307e\u3060\u5206\u304b\u3089\u306a\u3044\u3002";
        }
    }
}
