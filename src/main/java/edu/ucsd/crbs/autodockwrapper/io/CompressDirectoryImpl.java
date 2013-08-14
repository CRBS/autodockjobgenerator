/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import edu.ucsd.crbs.autodockwrapper.Constants;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import java.io.BufferedOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import org.kamranzafar.jtar.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this class compress a directory and its contents using tar and 
 * gzip.  
 * @author churas
 */
public class CompressDirectoryImpl implements CompressDirectory {

    final static Logger logger = LoggerFactory.getLogger(CompressDirectoryImpl.class);
    
    
    private static final String EMPTY_STRING = "";
    
    /**
     * This method compresses the folder <b>taskId</b> under the <b>inputsDirectory</b>
     * using Java Tar and Gzip compression to create a <b>inputsDirectory/taskId{@value edu.ucsd.crbs.autodockwrapper.Constants#TAR_GZ_SUFFIX}</b>
     * file.  
     * @param taskId name of directory to compress under <b>inputsDirectory</b>
     * @param inputsDirectory Base directory where the <b>taskId</b> directory resides
     * @throws IOException If there is a problem during compression.
     * @throws IllegalArgumentException if the taskId is negative or inputsDirectory is null
     */
    @Override
    public void compressDirectory(int taskId, final String inputsDirectory) throws IOException {

        if (taskId < 0){
            throw new IllegalArgumentException("taskId method parameter cannot be negative");
        }
        
        if (inputsDirectory == null){
            throw new IllegalArgumentException("inputsDirectory method parameter cannot be null");
        }
        
        String sourceDir = inputsDirectory+File.separator+Integer.toString(taskId);
        
        String destinationPath = sourceDir+Constants.TAR_GZ_SUFFIX;

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destinationPath));
        GZIPOutputStream gout = new GZIPOutputStream(out);
        TarOutputStream tos = new TarOutputStream(gout);
        
        logger.debug("Compressing path {}",sourceDir);
        
        tarDirectory(sourceDir,tos);
        tos.flush();
        tos.close();
        gout.close();
        out.close();
    }

    
    /**
     * Given a directory path this method compresses the path using JTar library.<p/>
     * 
     * The base directory of the resulting tarball will be set to the directory
     * name of the <b>path</b> passed in.
     * @param path Directory to compress
     * @param out  TarOutputStream to write the data to
     * @throws IOException If there is an error during the write
     */
    private void tarDirectory(final String path, 
                           TarOutputStream out) throws IOException {

        TarEntry entry;
        FileInputStream fin;
        
        File basePath = new File(path);
        
        //Grab the path excluding the name of the directory passed in
        String prefixPathToRemove = basePath.getParentFile().getCanonicalPath()+File.separator;
        
        //Using commons IO get a list of all files and directories
        for (File f : FileUtils.listFilesAndDirs(basePath, 
                                                        FileFilterUtils.trueFileFilter(), 
                                                        FileFilterUtils.trueFileFilter())){
            
            
            if (f.isDirectory()){
                //for a directory make a new entry removing the path prefix set in prefixPathToRemove and adding a File.separator
                entry = new TarEntry(f,f.getCanonicalPath().replace(prefixPathToRemove, EMPTY_STRING)+File.separator);
                out.putNextEntry(entry);
            }
            else {
                //for a file make a new entry removing the path prefix set in pathPrefixToRemove
                entry  = new TarEntry(f,f.getCanonicalPath().replace(prefixPathToRemove, EMPTY_STRING));
                out.putNextEntry(entry);
                
                //copy the contents of the file to the TarOutputStream
                fin = new FileInputStream(f);
                IOUtils.copy(fin, out);
                out.flush();
                fin.close();
            }
            
        }
    }
}
