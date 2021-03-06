package dicograph.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.nio.file.FileSystems;
import java.util.logging.Level;

/*
 *   This source file is part of the program for editing directed graphs
 *   into cographs using modular decomposition.
 *   Copyright (C) 2018 Fynn Leitow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

public class Parameters {
    private final String[] args;
    private final Options options;
    private CommandLine input;

    // Editing-Parameters with Default values:
    private long timeOut = 3600;
    private int hardThreshold = 0; // Exclude edges with a forbiddenSubgraph-score <= this threshold from the edge-edit-set.
    // prev: Force stop if forbiddenSub-Score <= this value during first run and use brute-force/branching/ILP in second run to complete.
    private double weightMultiplier = 1.0;
    private int solutionGap = 0; // all best solutions by default.

    private int lazyreach = -1;
    private int lazyRestart = 5;
    private int editStart = -1;

    // When to start brute force:
    private int bruteForceThreshold = 10;
    // when to stop brute force:
    private int bruteForceGap = 0; // max difference of subset-sizes that are solutions
    private int bruteForceLimit = 0; // max size of the subset (if set > 0)
    private int maxResults = 10; // stops Brute force or ILP after finding this many possible solutions



    // methods
    private boolean lazy = true;

    private double maximumMemory = -1;
    private int noThreads = -1;


    public Parameters(String[] args) {

        this.args = args;
        options = new Options();

        // globals:
        options.addOption("h", false, "show help.");
        options.addOption("md","Just compute the modular decomposition.");
        options.addOption("v","if not, console outputs only the editgraph or MDTree as .dot");

        Option test = new Option("test", true, "m test runs run on random cographs with n vertices disturbed by k edits.");
        test.setArgName("m n k");
        test.setArgs(3);
        options.addOption(test);

        Option cographs = new Option("cograph",true,"For test: Cographs do SE, PA and ORD (=1-SE-PA) joins with the given probability (Default: 1/3 each)");
        cographs.setArgName("SE PA");
        cographs.setArgs(2);
        options.addOption(cographs);

        Option input = new Option("i",true,"Input file: .dot, .txt (Matrix) or .jtxt (JGraph)");
        input.setArgName("infile");
        options.addOption(input);

        Option output = new Option("o", true,"Output file: .dot or .txt (Matrix)");
        output.setArgName("outfile");
        options.addOption(output);
        // default: same as input, with timestamp.

        Option log = new Option("log",true,"Log level: warning/info/fine/finer/finest/off");
        log.setArgName("level");
        options.addOption(log);

        // methods:
        options.addOption("lazy", "Use lazy greedy method");
        options.addOption("gforce","Use Brute Force in Step 2. Exit step 1 when bf- or hard thr reached");
        options.addOption("gilp", "Use ILP in step 2. Exit step 1 when bf- or hard thr reached");
        options.addOption("ilp", "Use MD and ILP");
        options.addOption("ilpglobal","Use ILP withoud MD");


        // method type parameters:
        options.addOption("hth", true,"Methods: Step 2 when subgraph-score < this hard-trh. Default: stop at Brute-Force-TH instead. For lazy: Don't apply lzreach when...");
        options.addOption("glscore","Methods: Runs Step 2 only on edges with high global edge-score");


        // method tweaking:
        options.addOption("gap",true,"Accept solutions with: cost <= best cost + gap. Default: 0; -1 exits after first");
        options.addOption("t",true, "Time limit of an ILP/Brute-Force-Computation in s. Default: 1h");

        options.addOption("bfth",true,"Step 2 when number of primes < brute-force-TH. Default: 10");
        options.addOption("bfgap",true,"Exit module-bf when subset-size > best solution + bfgap. Default: 0; -1 exits after first.");
        options.addOption("bflimit",true,"Abort module-bf when subset-size > this. Default: size of the prime");
        options.addOption("bfsize", true, "Abort module-bf or ILP when number of found solutions >= this. Default: 10");

        options.addOption("pskip", "Skipping (u,v) if u,v path in graph of the edit-edge-set");
        options.addOption("vskip", "Skips (u,v) if u,v in vertex set of edit-edge-set's graph");
        options.addOption("sth", true,"Soft threshold: No edit found in 1st lazy run -> discards edges with subgraph-score <= this");

        options.addOption("lzreach", true,"Number of next possible edits to be included during lazy run. Default: sqrt(no. of prime's children) + 2.");
        options.addOption("lzrestart",true, "Restarts lazy method when no of primes improved by lzre. Default: 9" );

        options.addOption("mem",true,"Maximum memory to be used by CPlex-Solver.");
        options.addOption("threads",true,"Number of Threads to be used by CPlex-Solver. Default: automatic.");

        options.addOption("start", true, "Number of first edges to be edited without using lazyreach.");

        Option weightm = new Option("wm",true,"Weight multiplier. Default: 1.0; Set lower if no solution, higher if too expensive");
        weightm.setArgName("double");
        options.addOption(weightm);

    }

    public void parse(){
        CommandLineParser parser = new DefaultParser();
        boolean err = false;
        try {
            input = parser.parse(options, args);
        } catch ( ParseException e){
            System.err.println( e.getMessage());
            err = true;
        }

        if ( err || input.hasOption("h") || args.length == 0) {
            help();
            return;
        }

        if (!isMDOnly()){
            // set params
            if(isBruteForce() || isGreedyPlusILP()|| isIlpMD() || isIlpOnly()){
                lazy = input.hasOption("lazy");
            }
            if(input.hasOption("gap")){
                solutionGap = Integer.parseInt( input.getOptionValue("gap"));
            }
            if(input.hasOption("t")){
                timeOut = Long.parseLong( input.getOptionValue("t"));
            }
            if(input.hasOption("hth")){
                hardThreshold = Integer.parseInt( input.getOptionValue("hth"));
            }
            if(input.hasOption("bfth")){
                bruteForceThreshold = Integer.parseInt( input.getOptionValue("bfth"));
            }
            if(input.hasOption("bfgap")){
                bruteForceGap = Integer.parseInt( input.getOptionValue("bfgap"));
            }
            if(input.hasOption("bflimit")){
                bruteForceLimit = Integer.parseInt( input.getOptionValue("bflimit"));
            }
            if(input.hasOption("wm")){
                weightMultiplier = Double.parseDouble( input.getOptionValue("wm"));
            }
            if(input.hasOption("bfsize")){
                maxResults = Integer.parseInt( input.getOptionValue("bfsize"));
            }
            if(input.hasOption("lzreach")){
                lazyreach = Integer.parseInt( input.getOptionValue("lzreach"));
            }
            if(input.hasOption("lzrestart")){
                lazyRestart = Integer.parseInt( input.getOptionValue("lzrestart"));
            }
            if(input.hasOption("mem")){
                maximumMemory = Double.parseDouble( input.getOptionValue("mem"));
            }
            if(input.hasOption("threads")){
                noThreads = Integer.parseInt( input.getOptionValue("threads"));
            }
            if(input.hasOption("start")){
                editStart = Integer.parseInt( input.getOptionValue("start"));
            }
        }
    }

    private void help(){
        HelpFormatter helpF = new HelpFormatter();
        String usage = "dmdedit -i <infile> [-options] or dmdedit -test <m n k> [-options]";
        String header = "Global flags: -i, -o, -log, -v, -md, -test\n" +
                "General editing flags: -t -gap\n" +
                "Editing methods (If several, chooses best solution):  \n" +
                "  -lazy, -gforce, -gilp; -ilp, -ilpglobal\n" +
                "Parameters for lazy greedy method:\n" +
                " -lzreach -hth -sth\n" +
                "Behaviour of 2-step greedy methods:\n" +
                " -hth (step 1), - glscore (step 2)\n\n";

        String footer = "\nRefer to thesis for details.";
        helpF.printHelp(usage,header,options,footer,false);
    }

    public boolean isValid(){
        return input != null;
    }

    public boolean isMDOnly(){
        return input.hasOption("md");
    }

    public boolean isVerbose(){
        return input.hasOption("v");
    }

    public boolean isTest(){
        return input.hasOption("test");
    }

    public String[] getTestParams(){
        return input.getOptionValues("test");
    }

    public String[] getCographProbabilities(){
        return input.getOptionValues("cograph");
    }

    public String getInFileAbsPath(){
        if(input.hasOption("i")){
            String sep = FileSystems.getDefault().getSeparator();
            String path = input.getOptionValue("i");
            if(path.startsWith(sep) || path.contains(':' + sep)) // windows C:\path\file
                return path;
            else
                return System.getProperty("user.dir") + sep + path;
        } else {
            throw new IllegalArgumentException("Error: Input file missing!");
        }
    }

    public String getOutFileAbsPath(){
        if(input.hasOption("o")){
            String path = input.getOptionValue("o");
            if(path.startsWith("/"))
                return path;
            else
                return System.getProperty("user.dir") + "/" + path;
        } else {
            return "";
        }
    }

    public Level getLogLevel(){
        if(input.hasOption("log")) {
            String name = input.getOptionValue("log");
            switch (name) {
                case "warning":
                    return Level.WARNING;
                case "info":
                    return Level.INFO;
                case "fine":
                    return Level.FINE;
                case "finer":
                    return Level.FINER;
                case "finest":
                    return Level.FINEST;
                case "off":
                    return Level.OFF;
                // else use defaults.
            }
        }
        if (isMDOnly()) {
            return Level.FINER; // MD Default
        } else {
            return Level.INFO; // Editing Default
        }
    }

    // methods
    public boolean isLazy() {
        return lazy;
    }

    public boolean isBruteForce() {
        return input.hasOption("gforce");
    }

    public boolean isGreedyPlusILP(){
        return input.hasOption("gilp");
    }

    public boolean isIlpMD() {
        return input.hasOption("ilp");
    }

    public boolean isIlpOnly() {
        return input.hasOption("ilpglobal");
    }

    // type
    public boolean isStopOnlyAtHardThreshold() {
        return input.hasOption("hth");
    }
    public int getHardThreshold() {
        return hardThreshold;
    }

    public boolean isUseGlobal(){
        return input.hasOption("glscore");
    }

    // tweaks
    public int getSolutionGap() {
        return solutionGap;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public int getBruteForceThreshold() {
        return bruteForceThreshold;
    }

    public int getBruteForceGap(){
        return bruteForceGap;
    }

    public int getBruteForceLimit() {
        return bruteForceLimit;
    }

    public boolean isSkipPaths() {
        return input.hasOption("pskip"); // default: false
    }

    public boolean isSkipExistingVertices() {
        return input.hasOption("vskip"); // default: false
    }

    public double getWeightMultiplier() {
        return weightMultiplier;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public int getLazyreach() {
        return lazyreach;
    }

    public int getLazyRestart() {
        return lazyRestart;
    }

    public double getMaximumMemory() {
        return maximumMemory;
    }

    public int getNoThreads() {
        return noThreads;
    }

    public int getEditStart() {
        return editStart;
    }
}
