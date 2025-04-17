package me.michal.radiacjaAleJAVA.Tasks.Things;


import me.michal.radiacjaAleJAVA.RadiacjaAleJAVA;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.block.SignChangeEvent;

import java.io.*;
import java.net.URI;
import java.net.URL;

public class Updater {
    private static RadiacjaAleJAVA plugin;
    private static SignChangeEvent e;
    private static URL url;
    private static File updateFolder;
    private static File file;

    public Updater(RadiacjaAleJAVA plugin) {
        Updater.plugin = plugin;
    }

    public void updatePlugin(SignChangeEvent event, File file) {
        Updater.e = event;
        Updater.updateFolder = Bukkit.getUpdateFolderFile();
        Updater.file = file;


        if (getLatestVersion() == null) {
            return;
        }
        String fileToGet = "radiacja-alejava-" + getLatestVersion() + ".jar";

        try {
            Updater.url = URI.create("https://github.com/Lach1423/Radiacja/raw/refs/heads/main/" + fileToGet).toURL();
        } catch (Exception ex) {
            event.setLine(1, "Invalid link file");
            plugin.setColor(e, ChatColor.RED);
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
            e.setLine(1, "zły version.txt");
            plugin.setColor(e, ChatColor.RED);
            return null;
        }
        return nv;
    }

    private void saveFile() {
        final File folder = updateFolder;

        deleteOldFile();
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                e.getPlayer().sendMessage("Failed to create" + folder);
            }
        }
        downloadFile();

        e.setLine(2, "Downloaded");
    }

    private void deleteOldFile() {
        File[] list = listFilesOrError(updateFolder);
        for (File x : list) {
            if (x.getName().equals(file.getName())) {
                if (x.delete()) {
                    e.setLine(0, "usunięto");
                } else {
                    e.setLine(0, "nie usunięto");
                }
            }
        }
    }

    private File[] listFilesOrError(File folder) {
        File[] contents = folder.listFiles();
        if (contents == null) {
            e.setLine(1, "no bitches/files");
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
                    e.setLine(1, "Downloading: " + percent + "%");
                }
            }
        } catch (Exception ex) {
            e.setLine(1, "Failure in");
            e.setLine(2, "downloading");
            plugin.setColor(e, ChatColor.RED);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException ex) {
                e.setLine(1, "failed to close in");
                plugin.setColor(e, ChatColor.RED);
            }
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (final IOException ex) {
                e.setLine(1, "failed to close fo");
                plugin.setColor(e, ChatColor.RED);
            }
        }

    }
}
