package dicograph.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


/**
 * Created by Fynn Leitow on 09.01.18.
 */
public class Parameters {
    private String[] args;
    private Options options;
    private CommandLine input;


    // Editing-Parameters with Default values:
    private int timeOut = 120;
    private static final int maxCost = 20; // from parameter k
    private int softThreshold = 3; // If no succesful edit found, discard edits with forbiddenSub-score <= this value - atm for my bad tests.
    private int hardThreshold = 0; // Exclude edges with a forbiddenSubgraph-score <= this threshold from the edge-edit-set.
    // prev: Force stop if forbiddenSub-Score <= this value during first run and use brute-force/branching/ILP in second run to complete.
    private boolean skipPaths = true;
    private boolean skipExistingVertices = false;
    private double weightMultiplier = 0.0;
    private int solutionGap = -1; // only one solution by default.
    private boolean requireGlobal = false; // only accept local edit if global also improves?

    // When to start brute force:
    private int primeStartThreshold = 5;

    // methods
    private boolean lazy = true;


    public Parameters(String[] args) {

        this.args = args;
        options = new Options();

        // globals:
        options.addOption("md","Just compute the modular decomposition.");
        options.addOption("v","verbose - only the editgraph or MDTree as .dot");
        options.addOption("test", true, "Test run...");

        Option input = new Option("i",true,"Input file: .dot, .txt (Matrix) or .jtxt (JGraph)");
        input.setArgName("infile");
        options.addOption(input);

        Option output = new Option("o", true,"Output file: .dot, .txt (Matrix) or .jtxt (JGraph)");
        output.setArgName("outfile");
        options.addOption(output);
        // default: same as input, with timestamp.

        Option log = new Option("log",true,"Choose log level: warning/info/fine/off");
        log.setArgName("value");
        options.addOption(log);

        //methods:
        options.addOption("glazy", "Default: determnistic lazy greedy method");
        options.addOption("gprime","Step 2 when prime threshold reached");
        options.addOption("gsoft", "Step 2 when #edges with subgraph-score > soft-trh is < prime-thr ^2");
        options.addOption("ghard","Step 2 when subgraph-score < hard-trh");
        options.addOption("ilp", "Use MD and ILP");
        options.addOption("ilpglobal","Use ILP withoud MD");



        options.addOption("h", false, "show help.");

    }

    public void parse() throws ParseException{
        CommandLineParser parser = new DefaultParser();
        if(args.length == 0) {
            help();
            return;
        }
        input = parser.parse(options, args);

        if (input.hasOption("h")) {
            help();
            return;
        }

        if (input.hasOption("test")) {
            // use my default testing options
        } else{
            if (!isMDOnly()){
                // init for Editing
                if(isPrime() || isHard() || isSoft() || isIlpMD() || isIlpOnly()){
                    lazy = input.hasOption("glazy");
                }
                if(options.hasOption("gap")){
                    solutionGap = Integer.valueOf( input.getOptionValue("gap"));
                }
                if(options.hasOption("t")){
                    timeOut = Integer.valueOf( input.getOptionValue("t"));
                }
            }
        }


    }

    private void help(){
        HelpFormatter helpF = new HelpFormatter();
        String usage = "dmdedit -i <infile> [-options] or dmdedit -test <n m k> [-options]";
        String header = "Global flags: -i, -o, -log, -v, -md, -test\n" +
                "General editing flags: -t -gap\n" +
                "Editing methods (If several, chooses best solution):  \n" +
                "  -glazy, -gprime, -gsoft, -ghard; -ilp, -ilpglobal\n" +
                "Other parameters adjust the greedy methods.\n\n";
        String footer = "\nRefer to thesis for details.";
        helpF.printHelp(usage,header,options,footer,false);
    }

    public String[] getArgs() {
        return args;
    }

    public Options getOptions() {
        return options;
    }

    public boolean isMDOnly(){
        return input.hasOption("md");
    }

    public boolean isVerbose(){
        return input.hasOption("v");
    }


    public static int getMaxCost() {
        return maxCost;
    }

    public int getSoftThreshold() {
        return softThreshold;
    }

    public int getHardThreshold() {
        return hardThreshold;
    }

    public boolean isSkipPaths() {
        return skipPaths;
    }

    public boolean isSkipExistingVertices() {
        return skipExistingVertices;
    }

    public double getWeightMultiplier() {
        return weightMultiplier;
    }

    public int getSolutionGap() {
        return solutionGap;
    }

    public boolean isRequireGlobal() {
        return requireGlobal;
    }

    public int getPrimeStartThreshold() {
        return primeStartThreshold;
    }

    public boolean isLazy() {
        return lazy;
    }

    public boolean isPrime() {
        return input.hasOption("gprime");
    }

    public boolean isSoft() {
        return input.hasOption("gsoft");
    }

    public boolean isHard() {
        return input.hasOption("ghard");
    }

    public boolean isIlpMD() {
        return input.hasOption("ilp");
    }

    public boolean isIlpOnly() {
        return input.hasOption("ilpglobal");
    }
}
