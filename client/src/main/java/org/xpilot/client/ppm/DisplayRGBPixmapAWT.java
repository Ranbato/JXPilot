package org.xpilot.client.ppm;

import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;
import java.io.IOException;

// affiche une image PPM en couleur

class DisplayImage extends Canvas {

    Image img;

    public DisplayImage(Image img) {
        this.img = img;
        setSize(img.getWidth(this), img.getHeight(this));
    }

    @Override
    public void paint(Graphics gr) {
        gr.drawImage(img, 0, 0, this);
    }

}

public class DisplayRGBPixmapAWT extends Frame {

    // constructeur pour un argument Pixmap
    public DisplayRGBPixmapAWT(String name, ByteRGBPixmap rgb) {
        super(name);
        setLocation(50, 50);
        // fabrication des pixels couleur au format usuel AWT : ColorModel.RGBdefault
        int[] pixels = new int[rgb.size];
        for (int i = 0; i < pixels.length; i++) {
            byte red = rgb.r.data[i];
            byte green = rgb.g.data[i];
            byte blue = rgb.b.data[i];
            red = (byte) (i % 2 == 0 ? 0xff : 0);
            blue = (byte) (i % 2 == 1 ? 0xff : 0);
            System.out.println("DisplayRGB r:" + red + " g:" + green + " b:" + blue);
            pixels[i] = 0xFF000000 + (Pixmap.intValue(red) << 16)
                    | (Pixmap.intValue(green) << 8) | Pixmap.intValue(blue);
        }
        // construit une image avec ces pixels
        MemoryImageSource source = new MemoryImageSource(rgb.width, rgb.height, pixels, 0,
                rgb.width);
        Image img = Toolkit.getDefaultToolkit().createImage(source);
        add(new DisplayImage(img));
        pack();
        show();
    }

    // constructeur pour un argument fichier PPM
    public DisplayRGBPixmapAWT(String filename) throws IOException {
        this(filename, new ByteRGBPixmap(filename));
    }

    public static void main(String[] args) {
        try {
            new DisplayRGBPixmapAWT("c:/users/cbeust/Documents/a.ppm");
        } catch (IOException e) {
            System.err.println(e);
        }
    }

}