package eu.scape_project.pt.mapred;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.ToolRunner;

import eu.scape_project.pt.proc.FileProcessor;
import eu.scape_project.pt.util.ArgsParser;

import eu.scape_project.pt.proc.PitProcessor;
import eu.scape_project.pt.proc.Processor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * A command-line interaction wrapper to execute cmd-line tools with MapReduce.
 * Code based on SimpleWrapper.
 * 
 * @author Rainer Schmidt [rschmidt13]
 * @author Matthias Rella [myrho]
 */
public class CLIWrapper extends Configured implements org.apache.hadoop.util.Tool {

    private static Log LOG = LogFactory.getLog(CLIWrapper.class);

    public static class CLIMapper extends Mapper<Object, Text, Text, Text> {
        //Mapper<Text, Buffer, Text, IntWritable> {

        /**
         * The Command-Line Processor. 
         * The same for all maps.
         */
        static Processor p = null;
        /**
         * Parser for the parameters in the command-lines (records).
         */
        static ArgsParser parser = null;
        /**
         * Workaround data structure to represent Toolspec Input Specifications
         */
        static HashMap<String, HashMap> mapInputs = null;

        /**
         * Sets up stuff which needs to be created only once and can be used in 
         * all maps this Mapper performs.
         * 
         * For per Job there can only be one Tool and one Action selected, 
         * this stuff is the processor and the input parameters parser.
         * @param context
         */
        @Override
        public void setup(Context context) {
            String strProc = context.getConfiguration().get(ArgsParser.PROCSTRING);

            if( strProc.equals(ArgsParser.PROC_TOOLSPEC ) ) {
                String strTool = 
                    context.getConfiguration().get(ArgsParser.TOOLSTRING);
                String strAction = 
                    context.getConfiguration().get(ArgsParser.ACTIONSTRING);
                p = new PitProcessor(strTool, strAction);
            }
            else if( strProc.equals( ArgsParser.PROC_TAVERNA ) ) 
                throw new UnsupportedOperationException(
                        "taverna processor not implemented");
            else
                throw new RuntimeException(
                        "processor (name: " + strProc + ") not found");


            p.initialize();
            // get parameters accepted by the processor.
            // this could be needed to validate input parameters 
            // mapInputs = p.getInputs();

            // get the parameters (the vars in the toolspec action command)
            // if mapInputs can be retrieved and parsing of the record 
            // as a command line would work:
            // parser = new ArgsParser();
            // for (Entry<String, HashMap> entry : mapInputs.entrySet()) {
            //    parser.setOption(entry.getKey(), entry.getValue());
            //}

        }

        /**
         * The map gets a key and value, the latter being a single command-line 
         * with execution parameters for pre-defined Processor (@see setup())
         * 
         * 1. Parse the input command-line and read parameters and arguments.
         * 2. Find input- and output-files. Input files are copied from their 
         *    remote location (eg. HDFS) to a local temporary location. A local 
         *    temporary location for the output-files is defined.
         *    Caveat: input and output-values are found by conventional keys 
         *    "input" and "output".
         * 3. Run the tool using generic Processor.
         * 4. Copy output-files (if needed) from the temp. local location to the 
         *    remote location which may be defined in the command-line parameter.
         * 
         * @param key 
         * @param value command-line with parameters and values for the tool
         * @param context Job context
         * @throws IOException
         * @throws InterruptedException
         */
        @Override
        public void map(Object key, Text value, Context context) 
                throws IOException, InterruptedException {

            LOG.info("MyMapper.map key:" + key.toString() + " value:" + value.toString());

            // Unix-style parsing (if mapInputs would be known in advance):
            /*
            String[] args = ArgsParser.makeCLArguments(value.toString());
            parser.parse(args);

            for (String strKey : mapInputs.keySet()) {
                if (parser.hasOption(strKey)) {
                    mapParams.put(strKey, parser.getValue(strKey));
                }
            }
            */

            // if mapInputs are not known, the paramters could be parsed that way:
            HashMap<String, String> mapParams = ArgsParser.readParameters( 
                  value.toString() );

            // parse parameter values for input- and output-files
            // FIXME that part should be done within the FilePreprocessor
            // and it should not rely on "input" and "output" conventions
            // maybe: a separate Preconditions-Specification where
            // input- and output-files are marked up
            ArrayList<String> inFiles = new ArrayList<String>();
            ArrayList<String> outFiles = new ArrayList<String>();

            String strInputFile = mapParams.get("input");
            String strOutputFile = mapParams.get("output");

            if (strInputFile != null ) {
                inFiles.add(strInputFile);
                // replace the input parameter with the tmp local location
                mapParams.put( "input", 
                        FileProcessor.getTempInputLocation(strInputFile));
            } 
            if (strOutputFile != null ) {
                outFiles.add(strOutputFile);
                // replace the output parameter with the tmpt local location
                mapParams.put("output", 
                        FileProcessor.getTempOutputLocation(strOutputFile));
            }

            // bring hdfs files to the exec-dir and use a hash 
            // FIXME maybe they are not hdfs-files ...
            // of the file's full path as identifier
            // prepares input files for local processing through cmd line tool

            FileSystem hdfs = FileSystem.get(new Configuration());
            FileProcessor fileProcessor = new FileProcessor(
                    inFiles.toArray(new String[0]), 
                    outFiles.toArray(new String[0]), 
                    hdfs);

            try {
                fileProcessor.resolvePrecondition();
            } catch (Exception e_pre) {
                LOG.error("Exception in preprocessing phase: " 
                        + e_pre.getMessage(), e_pre);
                e_pre.printStackTrace();
            }

            // run processor
            // TODO use sthg. like contextObject to manage type safety (?)

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                p.setStdout( bos );
                p.setContext(mapParams);
                p.execute();
            } catch (Exception e_exec) {
                LOG.error( "Exception in execution phase: " 
                        + e_exec.getMessage(), e_exec);
                e_exec.printStackTrace();
            }

            // bring output files in exec-dir back to the locations on hdfs 
            // as defined in the parameter value
            try {
                fileProcessor.resolvePostcondition();
            } catch (Exception e_post) {
                LOG.error("Exception in postprocessing phase: " 
                        + e_post.getMessage(), e_post);
                e_post.printStackTrace();
            }

            // write processor output to map context
            // use the first input file as key (workaround)
            // TODO fix that workaround
            context.write( new Text( inFiles.get(0)), new Text(bos.toByteArray()));

            /** STREAMING works but we'll integrate that later
            //Path inFile = new Path("hdfs://"+value.toString());
            //Path outFile = new Path("hdfs://"+value.toString()+".pdf");
            //Path fs_outFile = new Path("/home/rainer/tmp/"+inFile.getName()+".pdf");
            
            
            String[] cmds = {"ps2pdf", "-", "/home/rainer/tmp"+fn+".pdf"};
            //Process p = new ProcessBuilder(cmds[0],cmds[1],cmds[2]).start();
            Process p = new ProcessBuilder(cmds[0],cmds[1],cmds[1]).start();
            
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
            
             */
        }
    }

    public static class CLIReducer extends 
            Reducer<Text, IntWritable, Text, IntWritable> {

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) 
                throws IOException, InterruptedException {
        }
    }

    /**
     * Sets up, initializes and starts the Job.
     * 
     * @param args
     * @return
     * @throws Exception
     */
    @Override
    public int run(String[] args) throws Exception {

        Configuration conf = getConf();
        Job job = new Job(conf);

        job.setJarByClass(CLIWrapper.class);

        job.setOutputKeyClass(Text.class);
        // TODO Output Value Class may depend on the tool invoked
        job.setOutputValueClass(Text.class);

        job.setMapperClass(CLIMapper.class);


        //job.setReducerClass(MyReducer.class);

        job.setInputFormatClass(PtInputFormat.class);
        //job.setOutputFormatClass(FileOutputFormat.class);

        //job.setOutputFormatClass(MultipleOutputFormat.class);

        //FileInputFormat.addInputPath(job, new Path(args[0])); ArgsParser.INFILE
        //FileOutputFormat.setOutputPath(job, new Path(args[1])); ArgsParser.OUTDIR
        FileInputFormat.addInputPath(job, new Path(conf.get(ArgsParser.INFILE)));
        String outDir = (conf.get(ArgsParser.OUTDIR) == null) ? 
                "out/" + System.nanoTime() % 10000 
                : conf.get(ArgsParser.OUTDIR);
        conf.set(ArgsParser.OUTDIR, outDir);
        FileOutputFormat.setOutputPath(job, new Path(outDir));

        //add command to job configuration
        //conf.set(TOOLSPEC, args[2]);

        //job.setNumReduceTasks(Integer.parseInt(args[2]));

        //FileInputFormat.setInputPaths(job, s.toString());
        //FileOutputFormat.setOutputPath(job, new Path("output"));

        //FileInputFormat.setMaxInputSplitSize(job, 1000000);

        job.waitForCompletion(true);
        return 0;
    }

    public static void main(String[] args) throws Exception {

        int res = 1;
        CLIWrapper mr = new CLIWrapper();
        Configuration conf = new Configuration();

        try {
            ArgsParser pargs = new ArgsParser("i:o:t:a:p:x", args);
            //input file
            LOG.info("input: " + pargs.getValue("i"));
            //hadoop's output 
            LOG.info("output: " + pargs.getValue("o"));
            //tool to select
            LOG.info("tool: " + pargs.getValue("t"));
            //action to select
            LOG.info("action: " + pargs.getValue("a"));
            //defined parameter list
            //LOG.info("parameters: " + pargs.getValue("p"));
            LOG.info("processor: " + pargs.getValue("p"));

            conf.set(ArgsParser.INFILE, pargs.getValue("i"));
            //toolMap.initialize();
            //ToolSpec tool = toolMap.get(pargs.getValue("t"));
            //if(tool != null) conf.set(ArgsParser.TOOLSTRING, tool.toString());
            conf.set(ArgsParser.TOOLSTRING, pargs.getValue("t"));
            conf.set(ArgsParser.ACTIONSTRING, pargs.getValue("a"));
            if (pargs.hasOption("o")) {
                conf.set(ArgsParser.OUTDIR, pargs.getValue("o"));
            }
            if (pargs.hasOption("p")) {
                //conf.set(ArgsParser.PARAMETERLIST, pargs.getValue("p"));
                conf.set(ArgsParser.PROCSTRING, pargs.getValue("p"));
            }

            // TODO validate input parameters (eg. look for toolspec, action, ...)

            /*
            if(tool == null) {
            System.out.println("Cannot find tool: "+pargs.getValue("t"));
            System.exit(-1);
            }
             */
            //don't run hadoop
            if (pargs.hasOption("x")) {

                /*
                String t = System.getProperty("java.io.tmpdir");
                LOG.info("Using Temp. Directory:" + t);
                File execDir = new File(t);
                if(!execDir.exists()) {
                execDir.mkdir();
                }
                
                LOG.info("Is execDir a file: "+execDir.isFile() + " and a dir: "+execDir.isDirectory());
                File paper_ps = new File(execDir.toString()+"/paper.ps");
                LOG.info("Looking for this file: "+paper_ps);
                LOG.info("Is paper.ps a file: "+paper_ps.isFile());
                
                //LOG.info("trying ps2pdf in without args.....");
                String cmd = "/usr/bin/ps2pdf paper.ps paper.ps.pdf";
                String[] cmds = cmd.split(" ");
                System.out.println("cmds.length "+cmds.length);
                ProcessBuilder pb = new ProcessBuilder(cmds);
                pb.directory(execDir);
                Process p1 = pb.start();
                //LOG.info(".....");
                 */

                System.out.println("option x detected.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println(
                "usage: CLIWrapper -i inFile [-o outFile] [-p \"parameterList\"] -t cmd");
            LOG.info(e);
            System.exit(-1);
        }

        try {
            LOG.info("Running MapReduce ...");
            res = ToolRunner.run(conf, mr, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(res);
    }
}
