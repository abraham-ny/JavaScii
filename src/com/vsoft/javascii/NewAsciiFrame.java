package com.vsoft.javascii;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class NewAsciiFrame extends JFrame {

    private JLabel imagePreview;
    private JEditorPane asciiPane;
    private JProgressBar progressBar;
    private JButton chooseButton;
    private JButton convertButton;
    private JButton exportButton;
    private JCheckBox colorCheck;
    private JSpinner qualitySpinner;
    private JSlider zoomSlider;

    private BufferedImage selectedImage;
    private String lastAsciiRaw = "";    // plain ASCII (no color)
    private String lastAsciiHtml = "";   // HTML colored
    private String lastAsciiAnsi = "";   // ANSI colored for terminals

    public NewAsciiFrame() {
        super("JavaScii 2");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);
    }

    private void initUI() {

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        chooseButton = new JButton("Choose Image");
        convertButton = new JButton("Convert");
        exportButton = new JButton("Export ASCII");
        exportButton.setEnabled(false);

        convertButton.setEnabled(false);

        colorCheck = new JCheckBox("Color ASCII");
        qualitySpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));

        topPanel.add(chooseButton);
        topPanel.add(new JLabel("Quality:"));
        topPanel.add(qualitySpinner);
        topPanel.add(colorCheck);
        topPanel.add(convertButton);
        topPanel.add(exportButton);

        // ZOOM SLIDER
        zoomSlider = new JSlider(6, 30, 12);
        zoomSlider.setPreferredSize(new Dimension(220, 40));
        zoomSlider.setMajorTickSpacing(6);
        zoomSlider.setPaintTicks(true);

        topPanel.add(new JLabel("Zoom:"));
        topPanel.add(zoomSlider);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        // Image preview
        imagePreview = new JLabel("No image selected", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(320, 320));
        imagePreview.setBorder(BorderFactory.createTitledBorder("Image Preview"));

        // ASCII Viewer
        asciiPane = new JEditorPane("text/html", "");
        asciiPane.setEditable(false);

        JScrollPane asciiScroll = new JScrollPane(asciiPane);
        asciiScroll.setBorder(BorderFactory.createTitledBorder("ASCII Output"));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(imagePreview),
                asciiScroll
        );
        splitPane.setResizeWeight(0.35);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        // Listeners
        chooseButton.addActionListener(e -> chooseImage());
        convertButton.addActionListener(e -> convertImage());
        zoomSlider.addChangeListener(e -> updateZoom());
        exportButton.addActionListener(e -> exportAscii());
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an image");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                selectedImage = ImageIO.read(file);

                Image scaled = selectedImage.getScaledInstance(
                        300, -1, Image.SCALE_SMOOTH
                );

                imagePreview.setIcon(new ImageIcon(scaled));
                imagePreview.setText("");
                convertButton.setEnabled(true);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to load image", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void convertImage() {
        if (selectedImage == null) return;

        progressBar.setValue(0);
        asciiPane.setText("<html><body><h3>Processing...</h3></body></html>");

        int quality = (int) qualitySpinner.getValue();
        boolean color = colorCheck.isSelected();

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
                    public void onResponse(String asciiHtml) {

                        lastAsciiHtml = "<pre style='font-family: monospace; font-size:" +
                                zoomSlider.getValue() + "px;'>" +
                                asciiHtml + "</pre>";

                        asciiPane.setText(lastAsciiHtml);
                        asciiPane.setCaretPosition(0);

                        createRawAscii(asciiHtml);
                        createAnsiAscii(asciiHtml);

                        exportButton.setEnabled(true);
                    }
                });
    }

    // Convert HTML-ascii back into plain ASCII text for TXT export
    private void createRawAscii(String htmlAscii) {
        lastAsciiRaw = htmlAscii.replaceAll("<[^>]+>", "");
    }

    // Convert HTML spans to ANSI colors
    private void createAnsiAscii(String htmlAscii) {
        String ansi = htmlAscii;

        ansi = ansi.replaceAll("<span style=\"color: rgb\\(", "\u001B[38;2;");
        ansi = ansi.replaceAll("\\)\">#", "m#\u001B[0m");

        ansi = ansi.replaceAll("<[^>]+>", ""); // remove any other HTML
        lastAsciiAnsi = ansi;
    }

    private void updateZoom() {
        if (lastAsciiHtml != null && !lastAsciiHtml.isEmpty()) {
            asciiPane.setText(
                    lastAsciiHtml.replaceAll("font-size:\\d+px", "font-size:" + zoomSlider.getValue() + "px")
            );
        }
    }

    private void exportAscii() {
        if (lastAsciiRaw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to export yet.");
            return;
        }

        Object[] options = {"Plain TXT", "HTML (keep colors)", "ANSI TXT (terminal colors)"};

        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose export format:",
                "Export ASCII",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File saveFile = chooser.getSelectedFile();

        try (FileWriter fw = new FileWriter(saveFile)) {

            switch (choice) {
                case 0 -> fw.write(lastAsciiRaw);    // plain txt
                case 1 -> fw.write(lastAsciiHtml);   // html
                case 2 -> fw.write(lastAsciiAnsi);   // ANSI terminal text
            }

            JOptionPane.showMessageDialog(this, "Saved successfully!");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NewAsciiFrame::new);
    }
}
