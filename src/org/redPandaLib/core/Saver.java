/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redPandaLib.core.messages.RawMsg;

/**
 *
 * @author robin
 */
public class Saver implements SaverInterface {

    public static final String SAVE_DIR = "data";

    public static String getPrefix() {
        //return "-" + Integer.toString(Test.MY_PORT);
        return "";
    }

    public void saveMsgs(ArrayList<RawMsg> msgs) {
        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            File mkdirs = new File(SAVE_DIR);
            mkdirs.mkdir();

            File file = new File(SAVE_DIR + "/msgs" + getPrefix() + ".dat");



            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(msgs.clone());
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (IOException ex) {
            Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

    }

    public ArrayList<RawMsg> loadMsgs() {
        FileInputStream fileInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            File file = new File(SAVE_DIR + "/msgs" + getPrefix() + ".dat");

            fileInputStream = new FileInputStream(file);
            objectInputStream = new ObjectInputStream(fileInputStream);
            Object readObject = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();

            return (ArrayList<RawMsg>) readObject;

        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        System.out.println("could not load msgs.dat");

        return new ArrayList<RawMsg>();
    }

    public void savePeerss(ArrayList<Peer> peers) {
        ArrayList<PeerSaveable> arrayList = new ArrayList<PeerSaveable>();

        for (Peer peer : peers) {
            arrayList.add(peer.toSaveable());
        }
        //arrayList = (ArrayList<PeerSaveable>) arrayList.clone();//hack?

        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;

        try {
            File file = new File(SAVE_DIR + "/peers" + getPrefix() + ".dat");

            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(arrayList);
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (IOException ex) {
            Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

    }

    public ArrayList<Peer> loadPeers() {
        try {
            File file = new File(SAVE_DIR + "/peers" + getPrefix() + ".dat");

            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object readObject = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();

            ArrayList<PeerSaveable> pp = (ArrayList<PeerSaveable>) readObject;
            ArrayList<Peer> arrayList = new ArrayList<Peer>();


            for (PeerSaveable p : pp) {
                arrayList.add(p.toPeer());
            }


            return arrayList;


        } catch (ClassNotFoundException ex) {
        } catch (IOException ex) {
        } catch (ClassCastException ex) {
        }

        System.out.println("could not load peers.dat");

        return new ArrayList<Peer>();
    }

    public void saveIdentities(ArrayList<Channel> identities) {

        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            File file = new File(SAVE_DIR + "/identities.dat");

            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(identities);
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (IOException ex) {
            Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

    }

    public ArrayList<Channel> loadIdentities() {
        try {
            File file = new File(SAVE_DIR + "/identities.dat");

            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object readObject = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();

            return (ArrayList<Channel>) readObject;


        } catch (ClassNotFoundException ex) {
        } catch (IOException ex) {
        }

        System.out.println("could not load identities.dat");

        return new ArrayList<Channel>();
    }

    @Override
    public void saveLocalSettings(LocalSettings localSettings) {
        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;

        try {

            File mkdirs = new File(SAVE_DIR);
            mkdirs.mkdir();

            File file = new File(SAVE_DIR + "/localSettings" + getPrefix() + ".dat");

            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(localSettings);
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (IOException ex) {
            Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    @Override
    public LocalSettings loadLocalSettings() {
        try {
            File file = new File(SAVE_DIR + "/localSettings" + getPrefix() + ".dat");

            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object readObject = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();

            return (LocalSettings) readObject;


        } catch (ClassNotFoundException ex) {
        } catch (IOException ex) {
        }

        System.out.println("could not load objects.dat");

        return new LocalSettings();
    }

    @Override
    public void saveTrustedPeers(ArrayList<PeerTrustData> peertrusts) {
        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            File mkdirs = new File(SAVE_DIR);
            mkdirs.mkdir();

            File fileTmp = new File(SAVE_DIR + "/trustData-tmp" + getPrefix() + ".dat");



            fileTmp.createNewFile();
            fileOutputStream = new FileOutputStream(fileTmp);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(peertrusts.clone());
            objectOutputStream.close();
            fileOutputStream.close();
            
            File originFile = new File(SAVE_DIR + "/trustData" + getPrefix() + ".dat");
            originFile.delete();
            fileTmp.renameTo(originFile);

        } catch (IOException ex) {
            Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Saver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    @Override
    public ArrayList<PeerTrustData> loadTrustedPeers() {
        try {
            File file = new File(SAVE_DIR + "/trustData" + getPrefix() + ".dat");

            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object readObject = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();

            return (ArrayList<PeerTrustData>) readObject;

        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("could not load trustData.dat");

        return new ArrayList<PeerTrustData>();
    }
}
