package com.vsoft.javascii;

import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class Img2Ascii {

    private String[] chars = {"@", "#", "+", "\\", ";", ":", ",", ".", "`", " "};
    private BufferedImage image;
    private boolean useColor = false;
    private int quality = 3;
    private int qualityColor = 6;

    private Listener listener;

    public Img2Ascii() {}

    public Img2Ascii image(BufferedImage img) {
        this.image = img;
        return this;
    }

    public Img2Ascii quality(int q) {
        this.quality = q;
        return this;
    }

    public Img2Ascii color(boolean b) {
        this.useColor = b;
        return this;
    }

    public void convert(Listener listener) {
        this.listener = listener;
        new Worker().execute();
    }

    private class Worker extends SwingWorker<String, Integer> {

        @Override
        protected String doInBackground() {

            int q = quality;
            if (useColor) {
                q = quality + qualityColor;
                if (q > 5 + qualityColor || q < 1 + qualityColor)
                    q = 3 + qualityColor;
            } else {
                if (q > 5 || q < 1)
                    q = 3;
            }

            int width = image.getWidth();
            int height = image.getHeight();
            StringBuilder sb = new StringBuilder();

            for (int y = 0; y < height; y += q) {
                for (int x = 0; x < width; x += q) {

                    int pixel = image.getRGB(x, y);
                    Color color = new Color(pixel);

                    int r = color.getRed();
                    int g = color.getGreen();
                    int b = color.getBlue();

                    if (useColor) {
                        // HTML colored output
                        sb.append("<span style=\"color: rgb(")
                          .append(r).append(",").append(g).append(",").append(b)
                          .append(")\">#</span>");
                    } else {
                        int brightness = r + g + b;
                        brightness = Math.round(brightness / (765f / (chars.length - 1)));
                        sb.append(chars[brightness]);
                    }
                }
                sb.append("\n");
                publish(y);
            }
            return sb.toString();
        }

        @Override
        protected void process(java.util.List<Integer> chunks) {
            int y = chunks.get(chunks.size() - 1);
            int percent = (int) (((double) y / image.getHeight()) * 100);
            listener.onProgress(percent);
        }

        @Override
        protected void done() {
            try {
                listener.onResponse(get()); // returns String
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface Listener {
        void onProgress(int percentage);
        void onResponse(String asciiText);
    }
}
