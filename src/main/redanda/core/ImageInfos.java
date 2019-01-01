package main.redanda.core;

import java.io.IOException;

/**
 *
 * @author pY4x3g
 */
public interface ImageInfos {

    public Infos getInfos(String path) throws IOException;

    public class Infos {

        public int heigth;
        public int width;
    }
}
