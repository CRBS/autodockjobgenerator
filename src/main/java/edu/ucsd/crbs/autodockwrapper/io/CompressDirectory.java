/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.IOException;

/**
 *
 * @author churas
 */
public interface CompressDirectory {
    
    
    public void compressDirectory(int taskId,final String inputsDir) throws IOException;
    
}
