/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.job;

import edu.ucsd.crbs.autodockwrapper.io.CompressDirectory;
import edu.ucsd.crbs.autodockwrapper.io.CompressDirectoryImpl;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compresses paths passed in via constructor using the {@link Runnable} interface
 * allowing jobs to be run in background threads.
 * 
 * @author churas
 */
public class CompressInputDirTask implements Runnable {

    final static Logger logger = LoggerFactory.getLogger(CompressInputDirTask.class);
    
    private int _taskId;
    private String _inputsDir;
    private CompressDirectory _cd;

    /**
     * Constructor
     * @param cd CompressDirectory object that does the compression
     * @param taskId id of task to compress
     * @param inputsDir Inputs folder where task to comrpess resides
     */
    public CompressInputDirTask(CompressDirectory cd,int taskId,final String inputsDir){
        _taskId = taskId;
        _inputsDir = inputsDir;
        _cd = cd;
    }
    
    /**
     * Compresses path set in constructor using CompressDirectory object also
     * set in the constructor.  
     */
    @Override
    public void run() {
        try {
            logger.debug("Compressing directory for task {}",_taskId);
            _cd.compressDirectory(_taskId, _inputsDir);
           
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
