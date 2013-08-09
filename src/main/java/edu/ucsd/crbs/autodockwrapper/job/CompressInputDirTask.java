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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class CompressInputDirTask implements Runnable {

    final static Logger logger = LoggerFactory.getLogger(CompressInputDirTask.class);
    
    private int _taskId;
    private String _inputsDir;
    
    public CompressInputDirTask(int taskId,final String inputsDir){
        _taskId = taskId;
        _inputsDir = inputsDir;
    }
    
    @Override
    public void run() {
        try {
            logger.debug("Compressing directory for task {}",_taskId);
            CompressDirectory cd = new CompressDirectoryImpl();
            cd.compressDirectory(_taskId, _inputsDir);
           
            logger.debug("Deleting directory for task {}",_taskId);

            FileUtils.deleteDirectory(new File(_inputsDir+File.separator+Integer.toString(_taskId)));
        }
        catch(IOException io){
            logger.error("Caught IOException, Unable to compress",io);
        }
        catch(Exception ex){
            logger.error("Caught Exception, Unable to compress",ex);
        }
    }
    
}
