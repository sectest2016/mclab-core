/**
 * 
 */
package natlab.Static.mc4;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;


import natlab.Static.callgraph.FunctionCollection;
import natlab.Static.mc4.builtin.Mc4BuiltinQuery;
import natlab.options.Options;
import natlab.toolkits.filehandling.FunctionFinder;
import natlab.toolkits.filehandling.genericFile.ZippedFile;


/**
 * Mc4 Main and entry point.
 * @author ant6n
 *
 */
public class Mc4 {    
    public static final String outDir = "natlab_out";
    public static boolean DEBUG = true;
    public static boolean PRINT_STACK_ON_ERROR = true;
    public static boolean EXIT_ON_ERROR = true;
    public static void debug(String message){
        if (DEBUG) System.out.println(message);
    }
    public static void error(String message){
        System.err.println("mc4 error: "+message);
        if (PRINT_STACK_ON_ERROR) Thread.dumpStack();
        if (EXIT_ON_ERROR) System.exit(1);
    }
    public static FunctionFinder functionFinder;
    
    
    //TODO get rid of this
    public static void fiddle() throws Exception{
        URL location = Mc4.class.getProtectionDomain().getCodeSource().getLocation();
        System.out.println(location);
        System.out.println(location.getFile());
        
        System.out.println(natlab.Main.class.getResource("Main.class"));
        
        System.out.println(
                new ZippedFile("C:\\classes\\COMP 621.ZIP","ass2/ass2.pdf"));                
    }
    

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    
        //for now just open a file if no option is given .. eclipse is a pain
		Options options = new Options();
		options.parse(args);
		
		
		//try to do all benchmarks
		String bFolderString = "C:\\classes\\mclab\\Benchmarks\\matlabBenchmarks";
		File bFolder = new File(bFolderString);
		for (File dir : bFolder.listFiles()){
		    File[] list = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.matches("drv_.*m");
                }
            });
		    if (list == null || list.length == 0) continue;
		    String main = list[0].toString();
		    System.err.println("reading "+main);
		    args = new String[]{"-m",main};
		    options = new Options();
	        options.parse(args);
	        main(options);
		}
		System.exit(0);
		  
		
		
		
		//if no files are given, we will use some internal test files
		if (options.getFiles().size() == 0){
			//try to get a file from the project folder
		    //where it is depends on whether we call from Project or natlab dir
		    File thisDir = new File(System.getProperty("user.dir"));
		    String main = (thisDir.getName().equals("McLab"))?
		            "languages/Natlab/src/natlab/Static/mc4/test/drv_mbrt.m"
		            :"src/natlab/Static/mc4/test/drv_mbrt.m";
            args = new String[]{main};
			options.parse(args);
		} 
		main(options);
	}

	/**
	 * compiles matlab files into fortran, using the Options as argument.
	 * This is basically the entry point for mc4.
	 * @param options A Natlab Options object defines the command line arguments to Mc4
	 */
	public static void main(Options options){	   	    
        //object that resolves function names to files      
        functionFinder = new FunctionFinder(options, new Mc4BuiltinQuery());
        
	    //collect all need matlab files
	    FunctionCollection functions = new FunctionCollection(options);
	    	    
	    //inline all
	    //functions.inlineAll();
	    
	    //print result for now
	    //System.out.println(functions.getAsInlinedFunction().getPrettyPrinted());
	   
	    if (true){
	        FileWriter fstream = null;
	        try{
	            ast.Function function = functions.getAsInlinedFunction();
	            if (!(new File(outDir).exists())){ (new File(outDir)).mkdir(); }
	            File file = new File(outDir+"/"+function.getName()+".m");
	            fstream = new FileWriter(file);
	            fstream.write(function.getPrettyPrinted());
                fstream.close();	            
	        } catch(Exception e){
	            System.err.println("output failed: "+e.getMessage());
	        }
	    }
	}
}



