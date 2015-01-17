/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redPandaLib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.redPandaLib.core.Channel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.redPandaLib.core.Test;
import org.redPandaLib.crypt.AESCrypt;
import org.redPandaLib.crypt.AddressFormatException;
import org.redPandaLib.crypt.Sha256Hash;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author robin
 */
public class ExportImport {

    public static void writeXML(String path, long myIdentity, ArrayList<Channel> channels, HashMap<Long, String> identity2Name, String password) {

//        identity2Name.put(4578787L, "Robin");
//        identity2Name.put(1231534554L, "Hans");
//        identity2Name.put(-131525787L, "Meier");
//
//
//        channels.add(SpecialChannels.MAIN);
//        channels.add(SpecialChannels.SPAM);

        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("redPanda");
            doc.appendChild(rootElement);

            // staff elements
            Element myIdElement = doc.createElement("my-identity");
            rootElement.appendChild(myIdElement);

            Element longIdElement = doc.createElement("long");
            myIdElement.appendChild(longIdElement);
            longIdElement.appendChild(doc.createTextNode("" + myIdentity));

            // staff elements
            Element channelsElement = doc.createElement("channels");
            rootElement.appendChild(channelsElement);

            // set attribute to staff element
            Attr attr = doc.createAttribute("count");
            attr.setValue("" + channels.size());
            channelsElement.setAttributeNode(attr);

            // shorten way
            // staff.setAttribute("id", "1");

            for (Channel c : channels) {

                Element channelElement = doc.createElement("channel");
                channelsElement.appendChild(channelElement);

                Attr channelAttribut = doc.createAttribute("id");
                channelAttribut.setValue("" + c.getId());
                channelElement.setAttributeNode(channelAttribut);

                // firstname elements
                Element firstname = doc.createElement("name");
                firstname.appendChild(doc.createTextNode(c.getName()));
                channelElement.appendChild(firstname);

                // lastname elements
                Element lastname = doc.createElement("key");
                lastname.appendChild(doc.createTextNode(c.exportForHumans()));
                channelElement.appendChild(lastname);


            }

            Element identitiesElement = doc.createElement("identities");
            rootElement.appendChild(identitiesElement);

            for (long identity : identity2Name.keySet()) {

                Element channelElement = doc.createElement("identity");
                identitiesElement.appendChild(channelElement);

                // firstname elements
                Element firstname = doc.createElement("long");
                firstname.appendChild(doc.createTextNode("" + identity));
                channelElement.appendChild(firstname);

                // lastname elements
                Element lastname = doc.createElement("name");
                lastname.appendChild(doc.createTextNode(identity2Name.get(identity)));
                channelElement.appendChild(lastname);


            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            //StreamResult result = new StreamResult(new File("C://tmp/file.xml"));
            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);
            //transformer.transform(source, result);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(bos);
            transformer.transform(source, result);
            byte[] array = bos.toByteArray();
            //byte[] encode = AESCrypt.encode("hansMeier".getBytes(), array);

            System.out.println("xml--- \n\n" + new String(array));



            Sha256Hash create = Sha256Hash.create(password.getBytes());
            byte[] pass = create.getBytes();


            IvParameterSpec iv = new IvParameterSpec(pass, 0, 16);
            ByteArrayOutputStream encodedBytes = new ByteArrayOutputStream();
            AESCrypt.encode(array, encodedBytes, pass, iv);
            //Files.write(encodedBytes.toByteArray(), new File(path));
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            fileOutputStream.write(encodedBytes.toByteArray());
            fileOutputStream.close();


            System.out.println("File saved!");

        } catch (Exception ex) {
            Logger.getLogger(ExportImport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void readXML(String path, String password) throws Exception {


        File file = new File(path);
        byte[] read = read(file);

        Sha256Hash create = Sha256Hash.create(password.getBytes());
        byte[] pass = create.getBytes();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(read);


        IvParameterSpec iv = new IvParameterSpec(pass, 0, 16);
        ByteArrayOutputStream encodedBytes = new ByteArrayOutputStream();
        byte[] decode = AESCrypt.decode(byteArrayInputStream, pass, iv);


        ByteArrayInputStream finalBytes = new ByteArrayInputStream(decode);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(finalBytes);
        doc.getDocumentElement().normalize();

        System.out.println("root of xml file: " + doc.getDocumentElement().getNodeName());
        NodeList nodeMyId = doc.getElementsByTagName("my-identity");
        Node item = nodeMyId.item(0);
        if (item.getNodeType() == Node.ELEMENT_NODE) {
            String value = getValue("long", (Element) item);
            System.out.println("my-id: " + value);
            Test.localSettings.identity = Long.parseLong(value);
        }


        System.out.println("==========================");

        NodeList nodes = doc.getElementsByTagName("channel");

        System.out.println("==========================");

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String name = getValue("name", element);
                String key = getValue("key", element);
                System.out.println("name: " + name);
                System.out.println("key: " + key);
                try {
                    Main.importChannelFromHuman(key, name);
                } catch (AddressFormatException ex) {
                    Logger.getLogger(ExportImport.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        NodeList nodesIdentities = doc.getElementsByTagName("identity");

        System.out.println("==========================");

        for (int i = 0; i < nodesIdentities.getLength(); i++) {
            Node node = nodesIdentities.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String idLong = getValue("long", element);
                String nick = getValue("name", element);
                System.out.println("long: " + idLong);
                System.out.println("nick: " + nick);

                long longId = Long.parseLong(idLong);

                Test.localSettings.identity2Name.remove(longId);
                Test.localSettings.identity2Name.put(longId, nick);

            }
        }


    }

    private static String getValue(String tag, Element element) {
        NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodes.item(0);
        return node.getNodeValue();
    }

    public static byte[] read(File file) throws IOException {

        if (file.length() > 1024 * 1024 * 10) {
            return null;
        }


        byte[] buffer = new byte[(int) file.length()];
        InputStream ios = null;
        try {
            ios = new FileInputStream(file);
            if (ios.read(buffer) == -1) {
                throw new IOException("EOF reached while trying to read the whole file");
            }
        } finally {
            try {
                if (ios != null) {
                    ios.close();
                }
            } catch (IOException e) {
            }
        }

        return buffer;
    }
}
