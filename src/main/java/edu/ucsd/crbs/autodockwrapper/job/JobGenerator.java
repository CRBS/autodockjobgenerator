/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.job;

import java.io.IOException;
import java.util.List;

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public interface JobGenerator {
    
    public void createJobs(final String outputJobDir,List<String> ligands,List<String> receptors) throws IOException;
}
