package edu.ucsd.crbs.autodockwrapper.job;

import edu.ucsd.crbs.autodockwrapper.Constants;
import edu.ucsd.crbs.autodockwrapper.io.CompressDirectory;
import edu.ucsd.crbs.autodockwrapper.io.CompressDirectoryImpl;
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
 *
 * Instances of this class generate the vina jobs.  Please consider this implementation
 * in an alpha state.  
 * See {@link #createJobs(java.lang.String, java.util.List, java.util.List) createJobs}
 * for more information.
 * 
 * @author churas
 */
public class JobGeneratorImpl implements JobGenerator,Runnable{

   

   
    
    final static Logger logger = LoggerFactory.getLogger(JobGeneratorImpl.class);
    
    private ExecutorService _es;
    private final String _outputJobDir;
    private List<String> _ligands;
    private List<String> _receptors;
    private List<Future> _taskList;
    private long _totalJobCount;
    
    private static final String ONE_SPACE = " ";
    
    public JobGeneratorImpl(ExecutorService es,List<Future> taskList,final String outputJobDir,List<String> ligands, List<String> receptors){
        _es = es;
        _ligands = ligands;
        _receptors = receptors;
        _outputJobDir = outputJobDir;
        _taskList = taskList;
        _totalJobCount = 0;
    }
    
    
    /**
     * This method creates the jobs which contain batches of {@link edu.ucsd.crbs.autodockwrapper.Constants#SUB_JOBS_PER_JOB}
     * jobs.  For each job generates this code will create a compressed file under the inputs directory
     * within each compressed file are a configuration file containing the sub jobs and ligand
     * and receptor files needed by that job to run.  These ligand and receptor files are copied
     * from the paths passed in as parameters to this method.<P/>
     * 
     * Example Input Directory<P/>
     * 
     * {@value edu.ucsd.crbs.autodockwrapper.Constants#INPUTS_DIR_NAME}/(task id)/ligands <BR/>
     * {@value edu.ucsd.crbs.autodockwrapper.Constants#INPUTS_DIR_NAME}/(task id)/receptors <BR/>
     * <P/>
     * 
     * 
     * @param outputJobDir Path to directory where job files should be placed
     * @param ligands List of Strings containing full paths to ligand model (pdbqt) files
     * @param receptors List of Strings containing full paths to receptor model (pdbqt) files
     * @throws IOException If there is an IO error anywhere in the process
     */
     
    @Override
    public void createJobs() throws IOException {
    
        
        CompressDirectory compressor = new CompressDirectoryImpl();
        int subJobCount = 1;
        int counter = 1;
        int taskId = 1;
        
        File sourceReceptorFile;
        File sourceLigandFile;
        File destReceptorFile;
        File destLigandFile;
        
        //create the first task directory
        File inputsDir = createTaskInputsDir(_outputJobDir,taskId);
        
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
        
        long totalNumberSubJobs = _receptors.size()*_ligands.size();
        
        long numberOfBatchedJobs = Math.round(totalNumberSubJobs/Constants.SUB_JOBS_PER_JOB)+1;

        setTotalJobs(numberOfBatchedJobs);
        
        logger.info("Generating {} batch jobs containing {} sub jobs",numberOfBatchedJobs,totalNumberSubJobs);
        
        for (String receptor : _receptors){
            sourceReceptorFile = new File(receptor);
            destReceptorFile = new File(inputsDir.getAbsolutePath()+
                                        File.separator+
                                        Constants.RECEPTORS_DIR_NAME+
                                        File.separator+
                                        sourceReceptorFile.getName());
            
            for (String ligand : _ligands){
                
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
                          destLigandFile.getName()+ONE_SPACE+
                         Constants.RECEPTORS_FLAG+relativeReceptorsInputsDir+
                         destReceptorFile.getName()+Constants.NEW_LINE);

                subJobCount++;
                
                if (subJobCount >= Constants.SUB_JOBS_PER_JOB){
                    
                    //need to tar up current inputs/# folder
                    bw.close();
                    logger.debug("Completed creation of new task: {}",taskId);
                    //tar up path
                    _taskList.add(_es.submit(new CompressInputDirTask(compressor,
                                                taskId,
                                                _outputJobDir+File.separator+
                                                Constants.INPUTS_DIR_NAME)));
                    
                    taskId++;
                    
                    //create new inputs folder
                    inputsDir = createTaskInputsDir(_outputJobDir,taskId);
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
        
        //tar up last path
        _taskList.add(_es.submit(new CompressInputDirTask(compressor,taskId,_outputJobDir+
                                    File.separator+Constants.INPUTS_DIR_NAME)));
    }
    
    @Override
    public void run() {
        try {
            createJobs();
        }
        catch(Exception ex){
            logger.error("Caught Exception: "+ex.getMessage(), ex);
        }
    }
    
    @Override
    public synchronized long getTotalJobs() {
        return _totalJobCount;
    }
    
    private synchronized void setTotalJobs(long totalJobCount) {
        _totalJobCount = totalJobCount;
    }
    
    /**
     * This method creates the following paths under the output directory of the job:<P>
     * 
     * {@value edu.ucsd.crbs.autodockwrapper.Constants#INPUTS_DIR_NAME}/(task id)/ligands <BR/>
     * {@value edu.ucsd.crbs.autodockwrapper.Constants#INPUTS_DIR_NAME}/(task id)/receptors <BR/>
     * 
     * @param outputJobDir base directory under which to place the above paths
     * @param taskId task id for the job
     * @return File object to the base inputs directory 
     * @throws IOException If there is an error
     */
    private File createTaskInputsDir(final String outputJobDir,int taskId) throws IOException{
        
        File inputsDir = new File(outputJobDir+File.separator+Constants.INPUTS_DIR_NAME+File.separator+Integer.toString(taskId));
        File ligandsDir = new File(inputsDir.getAbsoluteFile()+File.separator+Constants.LIGANDS_DIR_NAME);
        File receptorsDir = new File(inputsDir.getAbsoluteFile()+File.separator+Constants.RECEPTORS_DIR_NAME);
        
        FileUtils.forceMkdir(ligandsDir);
        FileUtils.forceMkdir(receptorsDir);
        
        return inputsDir;
    }
    
    /**
     * This method will copy the source file to destination if the destination file
     * does not already exist
     * @param src Source file
     * @param dest Destination file
     * @throws IOException If there is an error
     */
    private void copyFileIfDoesNotExist(File src, File dest) throws IOException {
        if (!dest.exists()){
            FileUtils.copyFile(src, dest);
        }
    }
    
    /**
     * Creates a {@link java.io.BufferedWriter} pointing to the configuration file
     * that will contain the list of sub jobs.
     * @param inputsDir Path to inputs directory
     * @param taskId Task id of job
     * @return BufferedWriter upon success.
     * @throws IOException If there is an IO error.
     */
    private BufferedWriter createConfigFile(final String inputsDir,int taskId) throws IOException{
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(inputsDir+File.separator+Integer.toString(taskId)+Constants.AUTO_DOCK_CONFIG_SUFFIX));
        return bw;
    }
    
}
