/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.job;

import edu.ucsd.crbs.autodockwrapper.io.CompressDirectory;
import edu.ucsd.crbs.autodockwrapper.io.CompressDirectoryImpl;
import edu.ucsd.crbs.autodockwrapper.io.JobDirCreatorImpl;
import edu.ucsd.crbs.autodockwrapper.Constants;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public class JobGeneratorImpl implements JobGenerator{
    
    final static Logger logger = LoggerFactory.getLogger(JobGeneratorImpl.class);
    
    @Override
    public void createJobs(final String outputJobDir,List<String> ligands, List<String> receptors) throws IOException {
    
        LinkedList<Future> compressTasks = new LinkedList<Future>();
        
        CompressDirectory compressor = new CompressDirectoryImpl();
        int subJobCount = 1;
        int counter = 1;
        int taskId = 1;
        
        //Create a threadpool that is 75% size of number of processors on system
        //with a minimum size of 1.
        int threadPoolSize = (int)Math.round((double)Runtime.getRuntime().availableProcessors()*
                                              Constants.PERCENT_OF_CORES_TO_USE_FOR_COMPRESSION);
        if (threadPoolSize < 1){
            threadPoolSize = 1;
        }
        logger.debug("Creating threadpool size of {} to compress all the input directories",threadPoolSize);
        ExecutorService es = Executors.newFixedThreadPool(threadPoolSize);
        
        File sourceReceptorFile;
        File sourceLigandFile;
        File destReceptorFile;
        File destLigandFile;
        
        //create the first task directory
        File inputsDir = createTaskInputsDir(outputJobDir,taskId);
        
        String relativeLigandsInputsDir = Constants.INPUTS_DIR_NAME+
                                          File.separator+
                                          Integer.toString(taskId)+
                                          File.separator+
                                          Constants.LIGANDS_DIR_NAME+
                                          File.separator;
        
        String relativeReceptorsInputsDir = Constants.INPUTS_DIR_NAME+
                                            File.separator+
                                            Integer.toString(taskId)+
                                            File.separator+
                                            Constants.RECEPTORS_DIR_NAME+
                                            File.separator;
        
        BufferedWriter bw = createConfigFile(inputsDir.getAbsolutePath(),taskId);
        
        long totalNumberSubJobs = receptors.size()*ligands.size();
        
        long numberOfBatchedJobs = Math.round(totalNumberSubJobs/Constants.SUB_JOBS_PER_JOB)+1;

        logger.info("Generating {} batch jobs containing {} sub jobs",numberOfBatchedJobs,totalNumberSubJobs);
        
        for (String receptor : receptors){
            sourceReceptorFile = new File(receptor);
            destReceptorFile = new File(inputsDir.getAbsolutePath()+
                                        File.separator+
                                        Constants.RECEPTORS_DIR_NAME+
                                        File.separator+
                                        sourceReceptorFile.getName());
            
            for (String ligand : ligands){
                
                sourceLigandFile = new File(ligand);
                destLigandFile = new File(inputsDir.getAbsolutePath()+
                                          File.separator+
                                          Constants.LIGANDS_DIR_NAME+
                                          File.separator+
                                          sourceLigandFile.getName());
                
                copyFileIfDoesNotExist(sourceReceptorFile,destReceptorFile);
                copyFileIfDoesNotExist(sourceLigandFile,destLigandFile);
                
                bw.write(Integer.toString(subJobCount)+
                         Constants.CONFIG_SUBTASK_ID_DELIM+
                         Constants.LIGANDS_FLAG+relativeLigandsInputsDir+
                          destLigandFile.getName()+
                         Constants.RECEPTORS_FLAG+relativeReceptorsInputsDir+
                         destReceptorFile.getName()+Constants.NEW_LINE);

                subJobCount++;
                
                if (subJobCount >= Constants.SUB_JOBS_PER_JOB){
                    
                    //need to tar up current inputs/# folder
                    bw.close();
                    logger.debug("Completed creation of new task: {}",taskId);
                    //tar up path
                    compressTasks.add(es.submit(new CompressInputDirTask(taskId,
                                                outputJobDir+File.separator+
                                                Constants.INPUTS_DIR_NAME)));
                    
                    taskId++;
                    
                    //create new inputs folder
                    inputsDir = createTaskInputsDir(outputJobDir,taskId);
                    relativeLigandsInputsDir = Constants.INPUTS_DIR_NAME+
                                               File.separator+
                                               Integer.toString(taskId)+
                                               File.separator+
                                               Constants.LIGANDS_DIR_NAME+
                                               File.separator;
                    
                    relativeReceptorsInputsDir = Constants.INPUTS_DIR_NAME+
                                                 File.separator+
                                                 Integer.toString(taskId)+
                                                 File.separator+
                                                 Constants.RECEPTORS_DIR_NAME+
                                                 File.separator;
                    
                    destReceptorFile = new File(inputsDir.getAbsolutePath()+
                                                File.separator+
                                                Constants.RECEPTORS_DIR_NAME+
                                                File.separator+
                                                sourceReceptorFile.getName());
                    subJobCount = 1;
                    
                    bw = createConfigFile(inputsDir.getAbsolutePath(),taskId);
                }
                counter++;
            }
        }
        bw.close();
        //tar up path
        compressTasks.add(es.submit(new CompressInputDirTask(taskId,outputJobDir+
                                    File.separator+Constants.INPUTS_DIR_NAME)));
        
        logger.debug("Waiting for all the tasks compressing input directories to finish.");
        
        removeCompletedTasks(compressTasks);
        
        while(compressTasks.size() > 0){

            logger.debug("{} directory compression tasks remain.  Sleeping 20 seconds",compressTasks.size());
            try {
                Thread.sleep(20000);
            }
            catch(Exception ex){
                logger.debug("Sleep interrupted exception caught",ex);
            }
            removeCompletedTasks(compressTasks);
        }
        
        es.shutdown();
    }
    
    private void removeCompletedTasks(List<Future> tasks){
        Future f;
        Iterator<Future> itr = tasks.iterator();
        while(itr.hasNext()){
            f = itr.next();
            if (f.isDone() || f.isCancelled()){
                itr.remove();
            }
        }
    }
    
    private File createTaskInputsDir(final String outputJobDir,int taskId) throws IOException{
        
        File inputsDir = new File(outputJobDir+File.separator+Constants.INPUTS_DIR_NAME+File.separator+Integer.toString(taskId));
        File ligandsDir = new File(inputsDir.getAbsoluteFile()+File.separator+Constants.LIGANDS_DIR_NAME);
        File receptorsDir = new File(inputsDir.getAbsoluteFile()+File.separator+Constants.RECEPTORS_DIR_NAME);
        
        FileUtils.forceMkdir(ligandsDir);
        FileUtils.forceMkdir(receptorsDir);
        
        return inputsDir;
    }
    
    private void copyFileIfDoesNotExist(File src, File dest) throws IOException {
        if (!dest.exists()){
            FileUtils.copyFile(src, dest);
        }
    }
    
    private BufferedWriter createConfigFile(final String inputsDir,int taskId) throws IOException{
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(inputsDir+File.separator+Integer.toString(taskId)+Constants.AUTO_DOCK_CONFIG_SUFFIX));
        return bw;
    }
    
}
