package edu.ucsd.crbs.autodockwrapper.io;

import edu.ucsd.crbs.autodockwrapper.Constants;
import edu.ucsd.crbs.autodockwrapper.io.FileListGenerator;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

/**
 * Instances of this class return via the {@link #getFileList} method a list
 * of file paths using the path which can be a file containing a list of files
 * or a directory.
 * @author churas
 */
public class FileListGeneratorImpl implements FileListGenerator {

    
    
    /**
     * If <b>path</p> is a file this method returns the contents of that file.  If <b>path</b>
     * is a directory this method returns a list of all files ending in {@value edu.ucsd.crbs.autodockwrapper.Constants#PDBQT_SUFFIX}
     * under that directory path when recursively searched.  
     * @param path Path to a file or directory
     * @return List of files
     * @throws IOException if there is an error opening the path or directory
     */
    @Override
    public List<String> getFileList(final String path) throws IOException {
        File sourceFileObj = new File(path);
        
        if (sourceFileObj.isDirectory()){
            Collection<File> moleculeFiles = FileUtils.listFiles(sourceFileObj, FileFilterUtils.suffixFileFilter(Constants.PDBQT_SUFFIX),FileFilterUtils.trueFileFilter());
            LinkedList<String> fileList = new LinkedList<String>();
            for(File f : moleculeFiles){
                fileList.add(f.getCanonicalPath());
            }
            return fileList;
        }
        if (sourceFileObj.isFile()){
            return IOUtils.readLines(new FileReader(sourceFileObj));
        }
        throw new IOException(path+" is not a directory or a file");
    }
    
}
