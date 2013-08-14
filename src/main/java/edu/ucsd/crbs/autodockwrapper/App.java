package edu.ucsd.crbs.autodockwrapper;

import joptsimple.OptionParser;

import java.io.File;
import joptsimple.OptionSet;
import edu.ucsd.crbs.autodockwrapper.io.*;
import edu.ucsd.crbs.autodockwrapper.job.JobGenerator;
import edu.ucsd.crbs.autodockwrapper.job.JobGeneratorImpl;
import java.io.FileReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main entry class for AutoDockWrapper. AutoDockWrapper is a
 * program that generates Auto Dock Vina jobs suitable for running via Panfish
 * on HPC resources.<P/>
 * 
 * This program invokes helper classes to do four main tasks.  The links
 * below describe these tasks.<P/>
 * 
 * {@link edu.ucsd.crbs.autodockwrapper.io.JobDirCreatorImpl}<BR/>
 * {@link edu.ucsd.crbs.autodockwrapper.io.AutoDockScriptCreatorImpl}<BR/>
 * {@link edu.ucsd.crbs.autodockwrapper.io.PanfishAutoDockScriptCreatorImpl}<BR/>
 * {@link edu.ucsd.crbs.autodockwrapper.job.JobGeneratorImpl}<BR/>
 * 
 *
 * @author Christopher Churas
 */
public class App {

    final static Logger logger = LoggerFactory.getLogger(App.class);
    
    static final String LIGANDS_ARG = "ligands";
    static final String RECEPTORS_ARG = "receptors";
    static final String OUTPUT_ARG = "outputjobdir";
    static final String HELP_ARG = "h";

    /**
     * The default arguments passed to Auto Dock jobs: {@value}
     */
    public static final String DEFAULT_ARGS = "--center_x 41.1100 --center_y 34.9382 --center_z 35.8160 --size_x 25.0000 --size_y 25.0000 --size_z 25.0000 --cpu 2";
    
    /**
     * The default path to Auto Dock vina binary: {@value}
     */
    public static final String DEFAULT_VINA_BIN = "$PANFISH_BASEDIR/home/churas/bin/autodock_vina_1_1_2/bin/vina";
    

    /**
     * Command line application that generates Auto Dock Vina jobs
     */
    public static void main(String[] args) {
        try {

            OptionParser parser = new OptionParser() {

                {
                    accepts(LIGANDS_ARG).withRequiredArg().ofType(File.class).describedAs("list of ligand files");
                    accepts(RECEPTORS_ARG).withRequiredArg().ofType(File.class).describedAs("list of receptor files");
                    accepts(OUTPUT_ARG).withRequiredArg().ofType(String.class).describedAs("Output Job Directory");
                    accepts(HELP_ARG, "show help").forHelp();
                }
            };

            OptionSet optionSet = parser.parse(args);

            if (optionSet.has(HELP_ARG)) {
                parser.printHelpOn(System.out);
                System.exit(0);
            }
            
            if (!optionSet.has(LIGANDS_ARG) || 
                !optionSet.has(RECEPTORS_ARG) ||
                !optionSet.has(OUTPUT_ARG)){
                logger.error("Missing one or more required arguments");
                parser.printHelpOn(System.err);
                System.exit(1);
            }
            
            File ligandFile = (File)optionSet.valueOf(LIGANDS_ARG);
            File receptorFile =(File)optionSet.valueOf(RECEPTORS_ARG);
            String outputJobDir = (String)optionSet.valueOf(OUTPUT_ARG);
            
            logger.info("Generating job in directory: {}",outputJobDir);
            
            //create the job directory
            JobDirCreator jdc = getJobDirCreator();
            jdc.createJobDirectories(outputJobDir);
            
            
            //write out the autodock.sh file into outputjobdir
            AutoDockScriptCreator adsc = getAutoDockScriptCreator();
            adsc.createAutoDockScript(outputJobDir, DEFAULT_ARGS,DEFAULT_VINA_BIN);
            
              //write out the panfish auto dock script file into outputjobdir
            PanfishAutoDockScriptCreator padc = getPanfishAutoDockScriptCreator();
            padc.createPanfishAutoDockScript(outputJobDir);
            
            
            ExecutorService es = getExecutorService();
            List<Future> taskList = Collections.synchronizedList(new LinkedList<Future>());
            //generate jobs
            JobGenerator jg = getJobGenerator(es,taskList,outputJobDir, 
                    IOUtils.readLines(new FileReader(ligandFile)), 
                    IOUtils.readLines(new FileReader(receptorFile)));
            
            long startTime = System.currentTimeMillis();
            taskList.add(es.submit(jg));
            
            
            threadSleep(20000);
            long totalJobs = jg.getTotalJobs();
            long percentComplete;
            
            //okay we submitted the jobs now we can let the user know the status
            //by looking at the count of tasks remaining
            int removeCount = 0;
            while(taskList.size() > 0){
               percentComplete = getPercentComplete(removeCount,totalJobs);
               System.out.println(percentComplete+"% ("+removeCount+" of "+totalJobs+") job(s) created.  "+estimatedTimeRemaining(removeCount,totalJobs,startTime));
               threadSleep(20000);
               removeCount += removeCompletedTasks(taskList);
            }
            
            es.shutdown();
            
            System.out.println("Job generation complete.  Total Time: "+
                     Math.round((double)(System.currentTimeMillis() - startTime)/1000.0)+
                    " seconds.");
            File outputJobDirFile = new File(outputJobDir);
            System.out.println("Job created in: "+
                    outputJobDirFile.getCanonicalPath());
            System.out.println("Be sure to adjust VINA and ARGUMENTS lines in: "+
                    outputJobDirFile.getCanonicalPath()+File.separator+
                    Constants.AUTO_DOCK_SCRIPT+ " file before running");
            
            System.out.println("To run via panfish invoke:");
            
            System.out.println("cd "+outputJobDirFile.getCanonicalPath()+
                               ";./panfish_autodock.sh");
            
            System.out.println();
            
            System.out.println("To run directly in serial fashion invoke: ");
            
            System.out.println("cd "+outputJobDirFile.getCanonicalPath()+
                    ";for Y in `seq 1 "+Long.toString(totalJobs)+
                    "` ; do echo \"Running job $Y\";export SGE_TASK_ID=$Y;./autodock.sh ; done");
                    
            
            
            
        } catch (Exception ex) {
            logger.error("Caught Exception.  Exiting..", ex);
            System.exit(2);
        }
    }
    
    static void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
        }
    }
    
    static String estimatedTimeRemaining(long numberCompleted,long totalCount,long startTime){
        long curTime = System.currentTimeMillis();
        if (numberCompleted <= 0){
            return "Unknown time remaining.";
        }
        long elapsedTime = curTime - startTime;
        
        //using time elapsed and # jobs completed get a total time estimate.
        double estTimeMillis = (double)(elapsedTime*totalCount)/(double)numberCompleted;
        
        //use total time estimate to calculate time remaining
        double estMillisRemaining = estTimeMillis*(((double)totalCount-(double)numberCompleted)/(double)totalCount);
        return Long.toString(Math.round(estMillisRemaining/1000.0))+" seconds remaining.";
    }
    
    static long getPercentComplete(long numberCompleted, long totalCount){
        return Math.round(((double)numberCompleted/(double)totalCount)*100.0);
    }
    
    
    static JobDirCreator getJobDirCreator(){
        return new JobDirCreatorImpl();
    }
    
    static AutoDockScriptCreator getAutoDockScriptCreator(){
        return new AutoDockScriptCreatorImpl();
    }
    
    static PanfishAutoDockScriptCreator getPanfishAutoDockScriptCreator(){
        return new PanfishAutoDockScriptCreatorImpl();
    }
    
    static JobGenerator getJobGenerator(ExecutorService es,List<Future> taskList,final String outputJobDir,List<String> ligands,List<String> receptors){
        return new JobGeneratorImpl(es,taskList,outputJobDir,ligands,receptors);
    }
    
     /**
     * This method creates an {@link java.util.concurrent.ExecutorService} with a thread pool size
     * set to {@link edu.ucsd.crbs.autodockwrapper.Constants#PERCENT_OF_CORES_TO_USE_FOR_COMPRESSION} X
     * {@link java.lang.Runtime.getRuntime()#availableProcessors() Runtime.getRuntime().availableProcessors()} or 1 if value is less then 1.
     * @return ExecutorService
     */
    static ExecutorService getExecutorService(){
        //Create a threadpool that is 90% size of number of processors on system
        //with a minimum size of 1.
        int threadPoolSize = (int)Math.round((double)Runtime.getRuntime().availableProcessors()*
                                              Constants.PERCENT_OF_CORES_TO_USE_FOR_COMPRESSION);
        if (threadPoolSize < 1){
            threadPoolSize = 1;
        }
        logger.debug("Creating threadpool size of {} to compress all the input directories",threadPoolSize);
        return Executors.newFixedThreadPool(threadPoolSize);
    }
    
    static int removeCompletedTasks(List<Future> taskList){
        Future f;
        int removeCount = 0;
        Iterator<Future> itr = taskList.iterator();
        while(itr.hasNext()){
            f = itr.next();
            if (f.isDone() || f.isCancelled()){
                removeCount++;
                itr.remove();
            }
        }
        return removeCount;
    }
}
