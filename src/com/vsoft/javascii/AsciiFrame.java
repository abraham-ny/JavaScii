package com.vsoft.javascii;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class AsciiFrame extends JFrame {

    private JLabel imagePreview;
    private JEditorPane asciiPane;
    private JProgressBar progressBar;
    private JButton chooseButton;
    private JButton convertButton;
    private JCheckBox colorCheck;
    private JSpinner qualitySpinner;

    private BufferedImage selectedImage;

    public AsciiFrame() {
        super("Image → ASCII Art Converter");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);
    }

    private void initUI() {

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        chooseButton = new JButton("Choose Image");
        convertButton = new JButton("Convert");
        convertButton.setEnabled(false);

        colorCheck = new JCheckBox("Color ASCII");
        qualitySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));

        topPanel.add(chooseButton);
        topPanel.add(new JLabel("Quality:"));
        topPanel.add(qualitySpinner);
        topPanel.add(colorCheck);
        topPanel.add(convertButton);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        // Image preview
        imagePreview = new JLabel("No image selected", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(300, 300));
        imagePreview.setBorder(BorderFactory.createTitledBorder("Image Preview"));

        // ASCII Viewer (HTML-supported)
        asciiPane = new JEditorPane("text/html", "");
        asciiPane.setEditable(false);

        JScrollPane asciiScroll = new JScrollPane(asciiPane);
        asciiScroll.setBorder(BorderFactory.createTitledBorder("ASCII Output"));

        // Layout split pane
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(imagePreview),
                asciiScroll
        );
        splitPane.setResizeWeight(0.4);

        // Layout main frame
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        // Listeners
        chooseButton.addActionListener(e -> chooseImage());
        convertButton.addActionListener(e -> convertImage());
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an image");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                selectedImage = ImageIO.read(file);

                // Scale preview
                Image scaled = selectedImage.getScaledInstance(
                        300, -1, Image.SCALE_SMOOTH
                );

                imagePreview.setIcon(new ImageIcon(scaled));
                imagePreview.setText("");
                convertButton.setEnabled(true);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to load image", "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void convertImage() {
        if (selectedImage == null) return;

        progressBar.setValue(0);

        int quality = (int) qualitySpinner.getValue();
        boolean color = colorCheck.isSelected();

        asciiPane.setText("<html><body><h3>Processing...</h3></body></html>");

        new Img2Ascii()
                .image(selectedImage)
                .quality(quality)
                .color(color)
                .convert(new Img2Ascii.Listener() {
                    @Override
                    public void onProgress(int percentage) {
                        progressBar.setValue(percentage);
                    }

                    @Override
                    public void onResponse(String asciiText) {
                        if (color) {
                            asciiPane.setText("<pre style='font-family: monospace; font-size: 10px;'>" +
                                    asciiText +
                                    "</pre>");
                        } else {
                            asciiPane.setText("<pre style='font-family: monospace; font-size: 10px; color: black;'>" +
                                    asciiText +
                                    "</pre>");
                        }
                        asciiPane.setCaretPosition(0);
                    }
                });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AsciiFrame::new);
    }
}
