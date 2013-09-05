package edu.ucsd.crbs.autodockjobgenerator.io;

import java.io.IOException;

/**
 * Interface to compress a directory path.
 * @author churas
 */
public interface CompressDirectory {
    
    /**
     * This method should compress the path specified in the <b>inputsDir</b> and
     * write out a compressed file with the name <b>taskId</b>.(compression)
     * @param taskId
     * @param inputsDir
     * @throws IOException 
     */
    public void compressDirectory(int taskId,final String inputsDir) throws IOException;
    
}
