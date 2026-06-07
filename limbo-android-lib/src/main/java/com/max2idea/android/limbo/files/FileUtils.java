/*
Copyright (C) Max Kastanas 2012

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package com.max2idea.android.limbo.files;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.limbo.emu.lib.R;
import com.max2idea.android.limbo.main.Config;
import com.max2idea.android.limbo.main.LimboApplication;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * @author dev
 */
public class FileUtils {
    private final static String TAG = "FileUtils";
    private static final Object fdsLock = new Object();
    private static final HashMap<Integer, FileInfo> fds = new HashMap<>();

    public static String getNativeLibDir(@NonNull Context context) {
        return context.getApplicationInfo().nativeLibraryDir;
    }

    public static String getFullPathFromDocumentFilePath(String filePath) {

        filePath = filePath.replaceAll("%3A", "^3A");
        int index = filePath.lastIndexOf("^3A");
        if (index > 0)
            filePath = filePath.substring(index + 3);
        if (!filePath.startsWith("/"))
            filePath = "/" + filePath;

        //remove any spaces encoded by the ASF
        try {
            filePath = URLDecoder.decode(filePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return filePath;
    }

    public static String decodeDocumentFilePath(String filePath) {
        if (filePath != null && filePath.startsWith("/content//")) {
            filePath = filePath.replace("/content//", "content://");
            filePath = filePath.replaceAll("\\^\\^\\^", "%");
        }
        return filePath;
    }

    //TODO: we should pass the modes from the backend and translate them
    // instead of blindly using "rw". ie ISOs should be read only.
    public static int get_fd(String path) {
        synchronized (fdsLock) {
            int fd = 0;
            if (path == null)
                return 0;
            if (path.startsWith("/content//") || path.startsWith("content://")) {
                String npath = decodeDocumentFilePath(path);
                try {
                    Uri uri = Uri.parse(npath);
                    String mode = "rw";
                    if (path.toLowerCase().endsWith(".iso"))
                        mode = "r";
                    ParcelFileDescriptor pfd = LimboApplication.getInstance().getContentResolver().openFileDescriptor(uri, mode);
                    fd = pfd.getFd();
                    fds.put(fd, new FileInfo(path, npath, pfd));
                    Log.d(TAG, "Opening Content Uri: " + npath + ", FD: " + fd);
                } catch (Exception e) {
                    String msg = LimboApplication.getInstance().getString(R.string.CouldNotOpenDocFile) + " "
                            + FileUtils.getFullPathFromDocumentFilePath(npath)
                            + "\n" + LimboApplication.getInstance().getString(R.string.PleaseReassingYourDiskFiles);
                    Log.e(TAG, msg);
                    e.printStackTrace();
                }
            } else {
                try {
                    int mode = ParcelFileDescriptor.MODE_READ_WRITE;
                    if (path.toLowerCase().endsWith(".iso"))
                        mode = ParcelFileDescriptor.MODE_READ_ONLY;
                    File file = new File(path);
                    if (!file.exists())
                        file.createNewFile();
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, mode);
                    fd = pfd.getFd();
                    fds.put(fd, new FileInfo(path, path, pfd));
                    Log.d(TAG, "Opening File: " + path + ", FD: " + fd);
                } catch (Exception e) {
                    Log.e(TAG, "Could not open File: " + path + ", FD: " + fd);
                    if (Config.debug)
                        e.printStackTrace();
                }
            }
            return fd;
        }
    }

    /**
     * Closing File Descriptors
     *
     * @param fd File Descriptro to be closed
     * @return Returns 0 if fd is closed successfully otherwise -1
     */
    public static int close_fd(int fd) {
        if (!Config.closeFileDescriptors) {
            return 0;
        }
        synchronized (fds) {
            if (FileUtils.fds.containsKey(fd)) {
                FileInfo info = FileUtils.fds.get(fd);
                try {
                    ParcelFileDescriptor pfd = info.pfd;
                    if(Config.syncFilesOnClose) {
                        try {
                            pfd.getFileDescriptor().sync();
                        } catch (IOException e) {
                            if (Config.debug) {
                                Log.w(TAG, "Syncing DocumentFile: " + info.path + ": " + fd + " : " + e);
                                e.printStackTrace();
                            }
                        }
                    }
                    pfd.close();
                    FileUtils.fds.remove(fd);
                    return 0;
                } catch (IOException e) {
                    Log.e(TAG, "Error Closing DocumentFile: " + info.path + ": " + fd + " : " + e);
                    if (Config.debug)
                        e.printStackTrace();
                }
            } else {
                ParcelFileDescriptor pfd = null;

                try {
                    String path = "";
                    FileInfo info = FileUtils.fds.get(fd);
                    if (info != null) {
                        pfd = info.pfd;
                        path = info.path;
                    }
                    if (pfd == null)
                        pfd = ParcelFileDescriptor.fromFd(fd);
                    if(Config.syncFilesOnClose) {
                        try {
                            pfd.getFileDescriptor().sync();
                        } catch (IOException e) {
                            if (Config.debug) {
                                Log.w(TAG, "Error Syncing File: " + path + ": " + fd + " : " + e);
                                e.printStackTrace();
                            }
                        }
                    }
                    pfd.close();
                    return 0;
                } catch (Exception e) {
                    Log.e(TAG, "Error Closing File FD: " + fd + " : " + e);
                    if (Config.debug)
                        e.printStackTrace();
                }
            }
            return -1;
        }
    }


    @NonNull
    public static String LoadFile(Context context, String fileName, boolean loadFromRawFolder) throws IOException {
        // Create a InputStream to read the file into
        InputStream iS;
        if (loadFromRawFolder) {
            // get the resource id from the file name
            int rID = context.getResources().getIdentifier(LimboApplication.getInstance().getClass().getPackage().getName() + ":raw/" + fileName,
                    null, null);
            // get the file as a stream
            iS = context.getResources().openRawResource(rID);
        } else {
            // get the file as a stream
            iS = context.getResources().getAssets().open(fileName);
        }

        ByteArrayOutputStream oS = new ByteArrayOutputStream();
        byte[] buffer = new byte[iS.available()];
        int bytesRead = 0;
        while ((bytesRead = iS.read(buffer)) > 0) {
            oS.write(buffer);
        }
        oS.close();
        iS.close();

        // return the output stream as a String
        return oS.toString();
    }

    public static class FileInfo {
        public String path;
        public String npath;
        public ParcelFileDescriptor pfd;

        public FileInfo(String path, String npath, ParcelFileDescriptor pfd) {
            this.npath = npath;
            this.path = path;
            this.pfd = pfd;
        }
    }
}
