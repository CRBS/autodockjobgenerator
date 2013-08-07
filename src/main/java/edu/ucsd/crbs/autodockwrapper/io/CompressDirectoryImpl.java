/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import java.io.BufferedOutputStream;

import org.kamranzafar.jtar.*;

/**
 *
 * @author churas
 */
public class CompressDirectoryImpl implements CompressDirectory {

    static final int BUFFER = 8092;
    
    @Override
    public void compressDirectory(int taskId, final String inputsDirectory) throws IOException {


        String destinationPath = inputsDirectory+"/"+Integer.toString(taskId)+".tar.gz";

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destinationPath));
        GZIPOutputStream gout = new GZIPOutputStream(out);
        TarOutputStream tos = new TarOutputStream(gout);
        
        tarFolder("",inputsDirectory+"/"+Integer.toString(taskId),tos);
        tos.flush();
        tos.close();
        gout.close();
        out.close();
        
        

    }

    
    private void tarFolder(String parent, String path, TarOutputStream out) throws IOException {
        BufferedInputStream origin = null;
        File f = new File(path);
        String files[] = f.list();

        // is file
        if (files == null) {
            files = new String[1];
            files[0] = f.getName();
        }

        parent = ((parent == null) ? (f.isFile()) ? "" : f.getName() + "/" : parent + f.getName() + "/");

        for (int i = 0; i < files.length; i++) {
            File fe = f;
            byte data[] = new byte[BUFFER];

            if (f.isDirectory()) {
                fe = new File(f, files[i]);
            }

            if (fe.isDirectory()) {
                String[] fl = fe.list();
                if (fl != null && fl.length != 0) {
                    tarFolder(parent, fe.getPath(), out);
                } else {
                    TarEntry entry = new TarEntry(fe, parent + files[i] + "/");
                    out.putNextEntry(entry);
                }
                continue;
            }

            FileInputStream fi = new FileInputStream(fe);
            origin = new BufferedInputStream(fi);

            TarEntry entry = new TarEntry(fe, parent + files[i]);
            out.putNextEntry(entry);

            int count;

            while ((count = origin.read(data)) != -1) {
                out.write(data, 0, count);
            }

            out.flush();

            origin.close();
        }
    }
}
