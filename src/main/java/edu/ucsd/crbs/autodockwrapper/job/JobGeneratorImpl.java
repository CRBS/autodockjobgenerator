/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.job;

import edu.ucsd.crbs.autodockwrapper.io.CompressDirectory;
import edu.ucsd.crbs.autodockwrapper.io.CompressDirectoryImpl;
import edu.ucsd.crbs.autodockwrapper.io.JobDirCreatorImpl;
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

/**
 * PROTOTYPE CODE!!!!!!!!!!!!!!
 * @author churas
 */
public class JobGeneratorImpl implements JobGenerator{

    public static final int SUB_JOBS_PER_JOB = 400;
    
    public static final String LIGAND_INPUTS = "inputs"+File.separator+"ligands"+File.separator;
    public static final String RECEPTOR_INPUTS = "inputs"+File.separator+"receptors"+File.separator;
    
    
    @Override
    public void createJobs(final String outputJobDir,List<String> ligands, List<String> receptors) throws IOException {
    
        LinkedList<Future> compressTasks = new LinkedList<Future>();
        
        CompressDirectory compressor = new CompressDirectoryImpl();
        int subJobCount = 1;
        int counter = 1;
        int taskId = 1;
        
        //Create a threadpool that is 75% size of number of processors on system
        //with a minimum size of 1.
        int threadPoolSize = (int)Math.round((double)Runtime.getRuntime().availableProcessors()*0.75);
        if (threadPoolSize < 1){
            threadPoolSize = 1;
        }
        System.out.println("Creating threadpool size of "+threadPoolSize+" to compress all the input folders");
        
        ExecutorService es = Executors.newFixedThreadPool(threadPoolSize);
        
        File sourceReceptorFile;
        File sourceLigandFile;
        File destReceptorFile;
        File destLigandFile;
        
        //create the first task directory
        File inputsDir = createTaskInputsDir(outputJobDir,taskId);
        
        String relativeLigandsInputsDir = "inputs/"+Integer.toString(taskId)+"/ligands/";
        String relativeReceptorsInputsDir = "inputs/"+Integer.toString(taskId)+"/receptors/";
        
                        
        String relativeOutputsDir = "outputs"+File.separator+Integer.toString(taskId);
        
        BufferedWriter bw = createConfigFile(inputsDir.getAbsolutePath(),taskId);
        
        long totalNumberSubJobs = receptors.size()*ligands.size();
        
        long numberOfBatchedJobs = Math.round(totalNumberSubJobs/SUB_JOBS_PER_JOB)+1;
        
        System.out.println("Generating "+numberOfBatchedJobs+" batch jobs containing "+totalNumberSubJobs+" sub jobs.");
        System.out.println("Please be patient this could take a while.  Each '.' is a batched job");
        
        for (String receptor : receptors){
            sourceReceptorFile = new File(receptor);
            destReceptorFile = new File(inputsDir.getAbsolutePath()+"/receptors/"+sourceReceptorFile.getName());
            
            for (String ligand : ligands){
                
                sourceLigandFile = new File(ligand);
                destLigandFile = new File(inputsDir.getAbsolutePath()+"/ligands/"+sourceLigandFile.getName());
                copyFileIfDoesNotExist(sourceReceptorFile,destReceptorFile);
                copyFileIfDoesNotExist(sourceLigandFile,destLigandFile);
                
                bw.write(Integer.toString(subJobCount)+":::--ligand "+relativeLigandsInputsDir+destLigandFile.getName()+
                        " --receptor "+relativeReceptorsInputsDir+destReceptorFile.getName()+"\n");

                subJobCount++;
                
                if (subJobCount >= SUB_JOBS_PER_JOB){
                    
                    System.out.print(".");
                    //need to tar up current inputs/# folder
                    bw.close();
                    
                    //tar up path
                    compressTasks.add(es.submit(new CompressInputDirTask(taskId,outputJobDir+"/inputs")));
                    
                    taskId++;
                    
                    //create new inputs folder
                    inputsDir = createTaskInputsDir(outputJobDir,taskId);
                    relativeLigandsInputsDir = "inputs/"+Integer.toString(taskId)+"/ligands/";
                    relativeReceptorsInputsDir = "inputs/"+Integer.toString(taskId)+"/receptors/";
                    relativeOutputsDir = "outputs"+File.separator+Integer.toString(taskId);
                    destReceptorFile = new File(inputsDir.getAbsolutePath()+"/receptors/"+sourceReceptorFile.getName());
                    subJobCount = 1;
                    
                    bw = createConfigFile(inputsDir.getAbsolutePath(),taskId);
                }
                counter++;
            }
        }
        bw.close();
        //tar up path
        compressTasks.add(es.submit(new CompressInputDirTask(taskId,outputJobDir+"/inputs")));
        System.out.println("\nWaiting for all the tasks compressing input directories to finish.");
        System.out.println("This could also take a while.");
        
        removeCompletedTasks(compressTasks);
        
        while(compressTasks.size() > 0){
        
            System.out.println("Looks like there are still:  "+compressTasks.size()+" tasks to complete.  Sleeping 20 seconds");
            try {
                Thread.sleep(20000);
            }
            catch(Exception ex){
                
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
        
        File inputsDir = new File(outputJobDir+File.separator+JobDirCreatorImpl.INPUTS_DIR_NAME+File.separator+Integer.toString(taskId));
        File ligandsDir = new File(inputsDir.getAbsoluteFile()+File.separator+"ligands");
        File receptorsDir = new File(inputsDir.getAbsoluteFile()+File.separator+"receptors");
        
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
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(inputsDir+File.separator+Integer.toString(taskId)+".autodock.sh.config"));
        return bw;
    }
    
}
