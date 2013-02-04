package eu.scape_project.pt.invoke;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.OutputStream;
import java.util.Map;
import org.apache.commons.io.IOUtils;

/*
 * Class to invoke tools as native processes. Supports IO via files and streams.
 * 
 * Deprecated: Using xa-pit instead.
 * 
 * @author Rainer Schmidt [rschmidt13]
 */ 
public class ToolInvoker {
	
	private static Log LOG = LogFactory.getLog(ToolInvoker.class);
	
    /*
	private ToolSpec toolSpec = null;
	
	public int execute() throws IOException, InterruptedException {
		
		File execDir = PtFileUtil.getExecDir();
		String newExecDir = toolSpec.getContext().get(ToolSpec.EXEC_DIR);
		
		//has the execution dir been overwritten?
		if(newExecDir != null) {
			execDir = new File(newExecDir);
		}
		//otherwise use the default value
		else {
			toolSpec.getContext().put(ToolSpec.EXEC_DIR, execDir.toString());
		}
			
		LOG.info("Is execDir a file: "+execDir.isFile() + " and a dir: "+execDir.isDirectory());
		
		
		//TODO check if a simple split(" ") is sufficient for complexer parameter lists
		//Perhaps a specific delimiter for parameters will be required 
		String[] cmds = toolSpec.getCmd().split(Pattern.quote(" "));
		
		ProcessBuilder processBuilder = new ProcessBuilder(cmds);
		processBuilder.directory(execDir);
		Process p = processBuilder.proc();
		p.waitFor();
		return p.exitValue();
	}	
	
	/** STREAMING works but we'll integrate that later
	//Path inFile = new Path("hdfs://"+value.toString());
	//Path outFile = new Path("hdfs://"+value.toString()+".pdf");
	//Path fs_outFile = new Path("/home/rainer/tmp/"+inFile.getName()+".pdf");

	 

	
	//opening file
	FSDataInputStream hdfs_in = hdfs.open(inFile);
	FSDataOutputStream hdfs_out = hdfs.create(outFile);
	//FileOutputStream fs_out = new FileOutputStream(fs_outFile.toString());
		    	
	//pipe(process.getErrorStream(), System.err);
	
	OutputStream p_out = p.getOutputStream();
	InputStream p_in = p.getInputStream();
	//TODO copy outstream and send to log file
	
	byte[] buffer = new byte[1024];
	int bytesRead = -1;
	
	System.out.println("streaming data to process");
	Thread toProc = pipe(hdfs_in, new PrintStream(p_out), '>');
	
	System.out.println("streaming data to hdfs");()
	Thread toHdfs = pipe(p_in, new PrintStream(hdfs_out), 'h'); 
	
	//pipe(process.getErrorStream(), System.err);
	
	toProc.join();	    	
	toHdfs.join();
	
	*/
	
  	private Thread pipe(final InputStream src, final PrintStream dest, final char debugToken) throws IOException {
		System.out.println("Starting piping "+ debugToken);
	    Thread t = new Thread(new Runnable() {
	        public void run() {
	            try {
	                byte[] buffer = new byte[1024];
	                for (int n = 0; n != -1; n = src.read(buffer)) {
	                	System.out.print(debugToken);
	                    dest.write(buffer, 0, n);
	                    System.out.println("/"+debugToken);
	                }
	                //reader at EOF, flush writer, and close streams
	                System.out.print("EOF, flushing, and closing \n");
	                dest.flush();
	                src.close();
	                dest.close();
	                return;
	            } catch (IOException e) { // just exit
	            	System.out.println(e);
	            	return;
	            }
	        }
	    });
	    t.start();
	    return t;
	}

    /**
     * Replaces input parameters in the given command and executes it locally. 
     * 
     * @param cmd String of command path with variables like ${"key"}
     * @param inputs Map of input parameters (keys and values)
     * @throws IOException 
     */
    public int runCommand( String cmd ) throws IOException {
        return runCommand( cmd, null, null );
    }

    /**
     * Replaces input parameters in the given command and executes it locally. 
     * Passes in Inputstream stdin and writes command's standard output to stdout.
     * 
     * @param cmd String of command path with variables like ${"key"}
     * @param inputs Map of input parameters (keys and values)
     * @param stdin InputStream to read command's standard input from
     * @param stdout OutputStream to write command's standard output to
     * @throws IOException 
     */
	public int runCommand( String cmd, InputStream in, OutputStream out ) 
            throws IOException {

		// Build the command:
        //String[] cmd_template = cmd.split(" ");
        //replaceAll(cmd_template, inputs );

		//ProcessBuilder pb = new ProcessBuilder(cmd_template);
		LOG.debug("Executing: "+cmd);
		
		//pb.redirectErrorStream(true);
		Process proc = Runtime.getRuntime().exec(cmd);//pb.start();
		try {
			//copy input stream to output stream
			if( in != null ) {
				IOUtils.copyLarge(in,  proc.getOutputStream() );
				proc.getOutputStream().close();
				System.out.println("Data in...");
			}
			
            InputStream procStdout = proc.getInputStream();
			// Copy the OutputStream:
            if( out != null ) {
                IOUtils.copy( procStdout, out );
            }
            else {
                //StringWriter sw = new StringWriter();
                IOUtils.copy( procStdout , System.out );
                //System.out.println(sw.toString());
            }

			// Wait for completion:
			// FIXME Needs time-out. 			
			proc.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            proc.destroy();
        }
        return proc.exitValue();
	}
	


}