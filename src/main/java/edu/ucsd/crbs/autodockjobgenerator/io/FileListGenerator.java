package edu.ucsd.crbs.autodockjobgenerator.io;

import java.io.IOException;
import java.util.List;

/**
 * Implementing classes allow users to get a list of Files when given
 * a path to a file list or to a directory from which files can be extracted.
 * @author churas
 */
public interface FileListGenerator {
    
    
    /**
     * Given a path to a file or directory return a list of file paths
     * for files under that location
     * @param path String containing a path to a file containing a file list or the path is to a directory
     * @return List of file paths derived from <b>path</b> parameter
     * @throws IOException 
     */
    List<String> getFileList(final String path) throws IOException;
}
