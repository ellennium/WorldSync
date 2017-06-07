package io.grappl.worldsync;

import io.grappl.client.api.*;
import io.grappl.client.api.Protocol;
import io.grappl.client.impl.ApplicationState;
import io.grappl.client.impl.GrapplBuilder;
import io.grappl.client.impl.error.RelayServerNotFoundException;
import io.grappl.worldsync.gui.MainGUI;
import jdk.nashorn.internal.scripts.JO;

import javax.swing.*;
import java.io.*;
import java.util.UUID;

/**
 * The main class. This runs when the program starts.
 */
public class ServerSync {

    public static final String APP_NAME = "WorldSync";

    public static void main(String[] args) {
        /*
            Create log
            Start MainGUI
         */

        // TODO: Create log
        new MainGUI();
    }

    // Class will use Grappl's authentication system, so need to rewrite all of that unless something comes up

    // Get list of ServerDatas from the core server after logging in
    public static void setUpServerLocally(ServerData serverData) { // ServerData -> Server
        /*
            Create folder

            If new server
                Download the minecraft server jar
                Prompt the user to accept mojang's terms of service

            If not new server
                Download the files to the local location
                Find the minecraft server jar somehow

            Start server
            Get port
            Open Grappl

         */

        System.out.println("---------------------------------------------");
        System.out.println("---------------------------------------------");

        // Create folder
        String dataFolder = Utility.getAppdataFolder() + "servers\\";
        String serverFolder = dataFolder + serverData.getServerName() + "\\";
        File serverFolderFile = new File(serverFolder);
        serverFolderFile.mkdirs(); // Create all folders necessary to store the server
        boolean newServer = serverData.getBootTimes() == 0;

        System.out.println("Directory of server: " + serverFolder);

        if(newServer) {
            boolean check = true;

            File eulaText = new File(serverFolder + "/eula.txt");
            try {
                FileInputStream fileInputStream = new FileInputStream(eulaText);
                String input = new DataInputStream(fileInputStream).readLine();
                String[] fork = input.split("\\=");
                if(fork[1].equalsIgnoreCase("TRUE")) {
                    check = false;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int reply = -1;

            if(check)
                reply = JOptionPane.showConfirmDialog(null, "To run the server, you must agree to Mojang's EULA. Do you?");

            if(!check || reply == JOptionPane.YES_OPTION) {
                File mcServerFile = new File(serverFolder + "/mc_server.jar");

                if(!mcServerFile.exists()) {
                    final String mcServerURL = "https://s3.amazonaws.com/Minecraft.Download/versions/1.11.2/minecraft_server.1.11.2.jar";
                    Utility.download(mcServerURL, serverFolder+"mc_server.jar");
                    System.out.println("DOWNLOADER: Minecraft Server jar not found on disk- downloaded from AWS, version 1.11.2");
                } else {
                    System.out.println("DOWNLOADER: Server .jar found to be already present on disk. Not redownloading.");
                }

//                File eulaText = new File(serverFolder + "/eula.txt");
                try {
                    PrintStream eulaPrintStream = new PrintStream(new FileOutputStream(eulaText));
                    eulaPrintStream.println("eula=TRUE");
                    eulaPrintStream.flush();
                    eulaPrintStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(null, "You cannot run the server without accepting.");
            }
        }

        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", "cd " + serverFolder +" && java -jar mc_server.jar");
        builder.redirectErrorStream(true);
        try {
            Process p = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final int serverPort = 25565;

        GrapplBuilder grapplBuilder = new GrapplBuilder(new ApplicationState(), Protocol.TCP);
        grapplBuilder.atLocalPort(serverPort);
        Grappl theGrappl = grapplBuilder.build();
        try {
            theGrappl.connect("n.grappl.io");
            System.out.println("GRAPPL: Connected to relay server, all ports opened, public at > " + theGrappl.getExternalServer().toString() + " < ");
            JOptionPane.showMessageDialog(null, "Grappl connection open on: " + theGrappl.getExternalServer());
        } catch (RelayServerNotFoundException e) {
            e.printStackTrace();
        }
    }
    // Server object will send ServerUpdate messages to core to update the servers status on occasion
}
