/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Tyrael
 */
public class ImageInfosImageIO implements ImageInfos {

    public ImageInfosImageIO() {
    }

    @Override
    public Infos getInfos(String path) throws IOException {
        BufferedImage read = ImageIO.read(new File(path));
        Infos infos = new Infos();
        infos.width = read.getWidth();
        infos.heigth = read.getHeight();
        return infos;
    }
}
