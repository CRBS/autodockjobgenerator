/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.job;

import edu.ucsd.crbs.autodockwrapper.io.JobDirCreatorImpl;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
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
    
        
        int subJobCount = 1;
        int counter = 1;
        int taskId = 1;
        
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
                    
                    //need to tar up current inputs/# folder
                    System.out.println("Task: "+Integer.toString(taskId));
                    taskId++;
                    
                    //create new inputs folder
                    inputsDir = createTaskInputsDir(outputJobDir,taskId);
                    relativeLigandsInputsDir = "inputs/"+Integer.toString(taskId)+"/ligands/";
                    relativeReceptorsInputsDir = "inputs/"+Integer.toString(taskId)+"/receptors/";
                    relativeOutputsDir = "outputs"+File.separator+Integer.toString(taskId);
                    destReceptorFile = new File(inputsDir.getAbsolutePath()+"/receptors/"+sourceReceptorFile.getName());
                    subJobCount = 1;
                    bw.close();
                    bw = createConfigFile(inputsDir.getAbsolutePath(),taskId);
                }
                counter++;
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
