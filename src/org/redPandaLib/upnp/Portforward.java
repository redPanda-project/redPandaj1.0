///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package org.redPandaLib.upnp;
//
//import org.teleal.cling.UpnpService;
//import org.teleal.cling.UpnpServiceImpl;
//import org.teleal.cling.support.igd.PortMappingListener;
//import org.teleal.cling.support.model.PortMapping;
//
///**
// *
// * @author sony
// */
//public class Portforward {
//
//    public static void start(final int port, final String ip) {
//        new Thread() {
//            @Override
//            public void run() {
//                final String orgName = Thread.currentThread().getName();
//                Thread.currentThread().setName(orgName + " - Portforward");
//                PortMapping desiredMapping =
//                        new PortMapping(
//                        port,
//                        ip,
//                        PortMapping.Protocol.TCP,
//                        "My Port Mapping");
//
//                UpnpService upnpService =
//                        new UpnpServiceImpl(
//                        new PortMappingListener(desiredMapping));
//                upnpService.getControlPoint().search();
//
//
//                // Let's wait 10 seconds for them to respond
//                System.out.println("Waiting 10 seconds before shutting down...");
//                try {
//                    Thread.sleep(10000);
//                } catch (InterruptedException ex) {
//                    ex.printStackTrace();
//                }
//
//                // Release all resources and advertise BYEBYE to other UPnP devices
//                System.out.println("Stopping Cling...");
//                upnpService.shutdown();
//
//            }
//        }.start();
//    }
//}
