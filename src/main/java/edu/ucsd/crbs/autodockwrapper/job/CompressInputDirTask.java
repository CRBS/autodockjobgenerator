/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.job;

import edu.ucsd.crbs.autodockwrapper.io.CompressDirectory;
import edu.ucsd.crbs.autodockwrapper.io.CompressDirectoryImpl;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author churas
 */
public class CompressInputDirTask implements Runnable {

    
    private int _taskId;
    private String _inputsDir;
    
    public CompressInputDirTask(int taskId,final String inputsDir){
        _taskId = taskId;
        _inputsDir = inputsDir;
    }
    
    @Override
    public void run() {
        try {
            //System.out.println(Thread.currentThread().getId()+" Compressing "+_taskId+" and dir "+_inputsDir);
            CompressDirectory cd = new CompressDirectoryImpl();
            cd.compressDirectory(_taskId, _inputsDir);
           
            //System.out.println(Thread.currentThread().getId()+" sleeping 10 seconds before deleting files");
            
            //System.out.println(Thread.currentThread().getId()+" deleting "+_inputsDir+"/"+Integer.toString(_taskId));
            FileUtils.deleteDirectory(new File(_inputsDir+"/"+Integer.toString(_taskId)));
            
        }
        catch(IOException io){
            System.err.println(Thread.currentThread().getId()+" Unable to compress: "+_inputsDir+" "+io.getMessage());
        }
        catch(Exception ex){
            System.err.println(Thread.currentThread().getId()+" Caught exception"+ex.getMessage());
        }
    }
    
}
