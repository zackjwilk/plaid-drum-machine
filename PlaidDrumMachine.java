import javax.swing.*;
import java.awt.*;
import java.util.Random;
import javax.sound.sampled.*;
import java.io.File;

public class PlaidDrumMachine extends JFrame {
    private static final int GRID_SIZE = 8;
    private PlaidPanel drawingPanel;
    private JTextArea infoTextArea;
    private AudioPlayer audioPlayer;
    private Timer beatTimer;
    private int currentBeat = 0;

    private static Random random = new Random();

    private final String[] SAMPLE_PATHS = {
            "assets/samples/kick.wav", "assets/samples/snare.wav", "assets/samples/ride.wav", "assets/samples/cowbell.wav"
    };

    static class LineProperties {
        Color color;
        int thickness;
        float transparency;
        int[] gridPositions;
        AudioClip audioClip;

        public LineProperties(Color color, int thickness, float transparency, AudioClip audioClip) {
            this.color = color;
            this.thickness = thickness;
            this.transparency = transparency;
            this.audioClip = audioClip;
            this.gridPositions = generateGridPositions();
        }

        private int[] generateGridPositions() {
            int numPositions = 2 + random.nextInt(5); // 2-6 grid positions
            int[] positions = new int[numPositions];
            for (int i = 0; i < numPositions; i++) {
                positions[i] = random.nextInt(GRID_SIZE);
            }
            return positions;
        }
    }

    static class AudioClip {
        private Clip clip;

        public AudioClip(String filePath) {
            try {
                File audioFile = new File(filePath);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                clip = AudioSystem.getClip();
                clip.open(audioStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void play(float volume, float pitch) {
            if (clip == null) return;

            // reset clip to beginning
            clip.setFramePosition(0);

            FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float normalizedVolume = (1 - volume) * -20; // convert transparency to vol (-20dB to 0dB)
            volumeControl.setValue(normalizedVolume);

            clip.start();
        }
    }

    class AudioPlayer {
        private AudioClip[] audioClips;
        private LineProperties[] horizontalLines;
        private LineProperties[] verticalLines;

        public AudioPlayer(LineProperties[] horizontalLines, LineProperties[] verticalLines) {
            this.horizontalLines = horizontalLines;
            this.verticalLines = verticalLines;

            // load audio clips
            audioClips = new AudioClip[SAMPLE_PATHS.length];
            for (int i = 0; i < SAMPLE_PATHS.length; i++) {
                audioClips[i] = new AudioClip(SAMPLE_PATHS[i]);
            }
        }

        public void playBeat(int beat) {
            for (LineProperties line : horizontalLines) {
                if (contains(line.gridPositions, beat)) {
                    playSound(line);
                }
            }

            for (LineProperties line : verticalLines) {
                if (contains(line.gridPositions, beat)) {
                    playSound(line);
                }
            }
        }

        private void playSound(LineProperties line) {
            // audio clip based on line thickness
            int clipIndex = Math.min(SAMPLE_PATHS.length - 1, line.thickness / 4);
            AudioClip clip = audioClips[clipIndex];

            // pitch based on color brightness
            float brightness = (line.color.getRed() + line.color.getGreen() + line.color.getBlue()) / (3f * 255);

            // play with volume based on transparency, pitch based on color
            clip.play(line.transparency, brightness);
        }

        private boolean contains(int[] array, int value) {
            for (int num : array) {
                if (num == value) return true;
            }
            return false;
        }
    }

    class PlaidPanel extends JPanel {
        private Color backgroundColor;
        private LineProperties[] horizontalLineProps;
        private LineProperties[] verticalLineProps;
        private int cellSize;

        public PlaidPanel() {
            setPreferredSize(new Dimension(400, 400));
        }

        public void generateNewPattern() {
            // rand bg color determines BPM
            backgroundColor = new Color(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)
            );
            setBackground(backgroundColor);

            // calc BPM from background color
            int bpm = calculateBPM(backgroundColor);
            updateBeatTimer(bpm);

            horizontalLineProps = generateLineProperties(true);
            verticalLineProps = generateLineProperties(false);

            audioPlayer = new AudioPlayer(horizontalLineProps, verticalLineProps);

            repaint();
            updateInfoText(bpm);
        }

        private int calculateBPM(Color bgColor) {
            // convert bg color to BPM (80-100)
            int avgColor = (bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue()) / 3;
            return 80 + (avgColor * 100 / 255);
        }

        private void updateBeatTimer(int bpm) {
            if (beatTimer != null) beatTimer.stop();

            // calc ms per beat
            int msPerBeat = 60000 / bpm;

            // create timer
            beatTimer = new Timer(msPerBeat, e -> {
                currentBeat = (currentBeat + 1) % GRID_SIZE;
                audioPlayer.playBeat(currentBeat);
                repaint();
            });
            beatTimer.start();
        }

        private LineProperties[] generateLineProperties(boolean isHorizontal) {
            int numLineTypes = 2 + random.nextInt(4);
            LineProperties[] lineProps = new LineProperties[numLineTypes];

            for (int i = 0; i < numLineTypes; i++) {
                Color color = new Color(
                        random.nextInt(256),
                        random.nextInt(256),
                        random.nextInt(256)
                );

                int thickness = 1 + random.nextInt(20);
                float transparency = 0.1f + random.nextFloat() * 0.8f;

                // determine sample
                String samplePath = SAMPLE_PATHS[Math.min(SAMPLE_PATHS.length - 1, thickness / 4)];
                AudioClip audioClip = new AudioClip(samplePath);

                lineProps[i] = new LineProperties(color, thickness, transparency, audioClip);
            }

            return lineProps;
        }

        private void updateInfoText(int bpm) {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("bpm: ").append(bpm).append("\n");
            logMessage.append("background color: ").append(String.format("#%06x", backgroundColor.getRGB() & 0xFFFFFF)).append("\n");

            logMessage.append("\nhorizontal line types:\n");
            for (LineProperties prop : horizontalLineProps) {
                logMessage.append(String.format("  - thickness: %dpx, color: #%06x, transparency: %d%%\n",
                        prop.thickness,
                        prop.color.getRGB() & 0xFFFFFF,
                        Math.round(prop.transparency * 100)
                ));
            }

            logMessage.append("\nvertical line types:\n");
            for (LineProperties prop : verticalLineProps) {
                logMessage.append(String.format("  - thickness: %dpx, color: #%06x, transparency: %d%%\n",
                        prop.thickness,
                        prop.color.getRGB() & 0xFFFFFF,
                        Math.round(prop.transparency * 100)
                ));
            }

            infoTextArea.setText(logMessage.toString());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            int width = getWidth();
            int height = getHeight();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // calc cell size for square grid
            cellSize = width/(GRID_SIZE);

            // draw plaid pattern
            paintPlaidPattern(g2d, width, height);

            // draw beat indicator
            paintCrosshairAndBeat(g2d, width, height);
        }

        private void paintPlaidPattern(Graphics2D g2d, int width, int height) {
            // draw horizontal lines
            for (LineProperties prop : horizontalLineProps) {
                g2d.setStroke(new BasicStroke(prop.thickness));
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, prop.transparency));
                g2d.setColor(prop.color);

                int numLines = height / (prop.thickness * 8 + 10);
                if (numLines < 2) numLines = 2;

                int spacing = height / numLines;

                for (int j = 0; j < numLines; j++) {
                    int y = (j * spacing) + (spacing / 2);
                    g2d.drawLine(0, y, width, y);
                }
            }

            // draw vertical lines
            for (LineProperties prop : verticalLineProps) {
                g2d.setStroke(new BasicStroke(prop.thickness));
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, prop.transparency));
                g2d.setColor(prop.color);

                int numLines = width / (prop.thickness * 8 + 10);
                if (numLines < 2) numLines = 2;

                int spacing = width / numLines;

                for (int j = 0; j < numLines; j++) {
                    int x = (j * spacing) + (spacing / 2);
                    g2d.drawLine(x, 0, x, height);
                }
            }

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        private void paintCrosshairAndBeat(Graphics2D g2d, int width, int height) {
            int negRed = 255 - backgroundColor.getRed();
            int negGreen = 255 - backgroundColor.getGreen();
            int negBlue = 255 - backgroundColor.getBlue();
            Color inverseBackgroundColor = new Color(negRed, negGreen, negBlue, 100);
            g2d.setColor(inverseBackgroundColor); // inverse background color slight transparency

            int row = currentBeat;
            int col = currentBeat;

            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(col*cellSize, 0, col*cellSize, height);
            g2d.drawLine(col*cellSize+cellSize, 0, col*cellSize+cellSize, height);
            g2d.drawLine(0, row*cellSize, width, row*cellSize);
            g2d.drawLine(0, row*cellSize+cellSize, width, row*cellSize+cellSize);

            g2d.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);
        }
    }

    public PlaidDrumMachine() {
        setTitle("plaid drum machine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        drawingPanel = new PlaidPanel();
        infoTextArea = new JTextArea(15, 20);
        infoTextArea.setEditable(false);
        infoTextArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        infoTextArea.setMargin(new Insets(7, 7, 7, 7));
        JScrollPane scrollPane = new JScrollPane(infoTextArea);

        JButton generateButton = new JButton("generate");
        generateButton.addActionListener(e -> drawingPanel.generateNewPattern());

        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(scrollPane, BorderLayout.CENTER);
        sidePanel.add(generateButton, BorderLayout.SOUTH);

        mainPanel.add(drawingPanel, BorderLayout.CENTER);
        mainPanel.add(sidePanel, BorderLayout.EAST);

        add(mainPanel);
        drawingPanel.generateNewPattern();

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PlaidDrumMachine().setVisible(true);
        });
    }
}