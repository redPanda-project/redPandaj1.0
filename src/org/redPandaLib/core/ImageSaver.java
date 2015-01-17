/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author robin
 */
public class ImageSaver {

    String path;

    public ImageSaver(String path) {
        this.path = path;
    }

    public void saveImage(byte[] b, String name) {
        File file = new File(path + name);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);


            try {
                fileOutputStream.write(b);
                fileOutputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(ImageSaver.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ImageSaver.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
