package me.michal.radiacjaAleJAVA.Tasks.Things;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;

import java.io.*;
import java.net.URI;
import java.net.URL;

public class Updater {
    private static URL url;
    private static File updateFolder;
    private static File file;
    private static Player player;

    public Updater() {}

    public void updatePlugin(File file, Player p) {
        Updater.updateFolder = Bukkit.getUpdateFolderFile();
        Updater.file = file;
        Updater.player = p;


        if (getLatestVersion() == null) {
            return;
        }
        String fileToGet = "radiacja-alejava-" + getLatestVersion() + ".jar";

        try {
            Updater.url = URI.create("https://github.com/Lach1423/Radiacja/raw/refs/heads/main/" + fileToGet).toURL();
        } catch (Exception ex) {
            player.sendMessage(ChatColor.RED + "Invalid link file");
            player.sendMessage(Updater.url.toString());
        }
        saveFile();
    }

    private String getLatestVersion() {
        String nv;
        try {
            URL url = URI.create("https://raw.githubusercontent.com/Lach1423/Radiacja/refs/heads/main/version").toURL();

            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            nv = br.readLine();
            nv = nv.substring(nv.lastIndexOf(" ") + 1);
            br.close();

        } catch (Exception err) {
            player.sendMessage(ChatColor.RED + "zły version.txt");

            return null;
        }
        return nv;
    }

    private void saveFile() {
        final File folder = updateFolder;

        deleteOldFile();
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                player.sendMessage("Failed to create" + folder);
            }
        }
        downloadFile();

        player.sendMessage("Downloaded");
    }

    private void deleteOldFile() {
        File[] list = listFilesOrError(updateFolder);
        for (File x : list) {
            if (x.getName().equals(file.getName())) {
                if (x.delete()) {
                    player.sendMessage("usunięto");
                } else {
                    player.sendMessage("nie usunięto");
                }
            }
        }
    }

    private File[] listFilesOrError(File folder) {
        File[] contents = folder.listFiles();
        if (contents == null) {
            player.sendMessage("no bitches/files");
            return new File[0];
        } else {
            return contents;
        }
    }

    private void downloadFile() {
        BufferedInputStream in = null;
        FileOutputStream fout = null;

        try {
            int byteSize = 1024;
            final int fileLength = url.openConnection().getContentLength();
            in = new BufferedInputStream(url.openStream());
            fout = new FileOutputStream(new File(updateFolder, file.getName()));

            final byte[] data = new byte[byteSize];
            int count;
            long downloaded = 0;

            while ((count = in.read(data, 0, byteSize)) != -1) {
                downloaded += count;
                fout.write(data, 0, count);
                final int percent = (int) ((downloaded * 100) / fileLength);
                if (((percent % 10) == 0)) {
                    player.sendMessage("Downloading: " + percent + "%");
                }
            }
        } catch (Exception ex) {
            player.sendMessage(ChatColor.RED + "Failure in downloading");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException ex) {
                player.sendMessage("failed to close in");
            }
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (final IOException ex) {
                player.sendMessage("failed to close fo");
            }
        }

    }
}
