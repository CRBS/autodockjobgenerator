/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.job;

import java.io.IOException;

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public interface JobGenerator extends java.lang.Runnable {
    
    public void createJobs() throws IOException;
    
    public long getTotalJobs();
}
