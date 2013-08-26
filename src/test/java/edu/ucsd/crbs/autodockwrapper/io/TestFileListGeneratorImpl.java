/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsd.crbs.autodockwrapper.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Christopher Churas <churas@ncmir.ucsd.edu>
 */
public class TestFileListGeneratorImpl {
    
    
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void testGetFileListWithNullPath() throws Exception {
        
        try {
            FileListGeneratorImpl listGen = new FileListGeneratorImpl();
            listGen.getFileList(null);
            fail("Expected exception");
        }
        catch(IllegalArgumentException ex){
            assertTrue(ex.getMessage().contains("Path passed into method cannot be null"));
        }
    }
    
    @Test
    public void testGetFileListWithPathNeitherFileNorDirectory() throws Exception {
        File baseDir = folder.newFolder();
        try {
            FileListGeneratorImpl listGen = new FileListGeneratorImpl();
            listGen.getFileList(baseDir.getCanonicalPath()+File.separator+"DOESNOTEXIST");
            fail("Expected exception");
        }
        catch(IOException ex){
            assertTrue(ex.getMessage().contains(baseDir.getCanonicalPath()+File.separator+"DOESNOTEXIST is not a directory or a file"));
        }
    }
    
    @Test
    public void testGetFileListOnEmptyDirectory() throws Exception {
        File baseDir = folder.newFolder();
        FileListGeneratorImpl listGen = new FileListGeneratorImpl();
        List<String> result = listGen.getFileList(baseDir.getCanonicalPath());
        
        assertTrue(result != null);
        assertTrue(result.isEmpty() == true);
        
    }
    
    @Test
    public void testGetFileListWithPathPointingToFile() throws Exception {
        File baseDir = folder.newFolder();
        File fileOfFiles = new File(baseDir+File.separator+"foo");
        FileWriter fw = new FileWriter(fileOfFiles);
        IOUtils.write("1\n2\n33\n", fw);
        fw.flush();
        fw.close();
        
        FileListGeneratorImpl listGen = new FileListGeneratorImpl();
        List<String> result = listGen.getFileList(fileOfFiles.getCanonicalPath());
        
        assertTrue(result.size() == 3);
        assertTrue(result.get(0).equals("1"));
        assertTrue(result.get(1).equals("2"));
        assertTrue(result.get(2).equals("33"));
    }

    @Test
    public void testGetFileListFromDirectoryWithSomeFiles() throws Exception {
         File baseDir = folder.newFolder();
         
         File fileOne = new File(baseDir.getCanonicalPath()+File.separator+"one.txt");
         fileOne.createNewFile();
         File fileTwo = new File(baseDir.getCanonicalPath()+File.separator+"two.pdbqt");
         fileTwo.createNewFile();
         
         File subDir = new File(baseDir.getCanonicalPath()+File.separator+"subdir");
         FileUtils.forceMkdir(subDir);
         File fileThree = new File(subDir.getCanonicalPath()+File.separator+"three.pdbqt");
         fileThree.createNewFile();
         
         FileListGeneratorImpl listGen = new FileListGeneratorImpl();
        List<String> result = listGen.getFileList(baseDir.getCanonicalPath());
        
        assertTrue(result.size() == 2);
         
        assertTrue((result.get(0).contains("three.pdbqt") && result.get(1).contains("two.pdbqt")) ||
                   (result.get(1).contains("three.pdbqt") && result.get(0).contains("two.pdbqt")));
        
        File chkFile = null;
        for (String s : result){
            chkFile = new File(s);
            assertTrue(chkFile.exists() == true);
        }
    }
    
    
    
    
}
