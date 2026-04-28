import java.io.*;
import java.util.*;

public class Main {

    // Paths relative to where you run: java Main (i.e., from inside src/)
    static final String INPUT_DIR  = "../input/";
    static final String OUTPUT_DIR = "../output/";

    // Grammar files to process (skipping input_valid/invalid — those are strings, not grammars)
    static final String[] GRAMMAR_FILES = {
            "grammar1.txt",
            "grammar2.txt",
            "grammar3.txt",
            "grammar_with_conflict.txt"
    };

    // Input strings file (one token string per line)
    static final String VALID_INPUTS_FILE   = INPUT_DIR + "input_valid.txt";
    static final String INVALID_INPUTS_FILE = INPUT_DIR + "input_invalid.txt";

    public static void main(String[] args) throws IOException {

        // Create output directory if it doesn't exist
        new File(OUTPUT_DIR).mkdirs();

        // Collect all input strings
        List<String> validInputs   = readLines(VALID_INPUTS_FILE);
        List<String> invalidInputs = readLines(INVALID_INPUTS_FILE);
        List<String> allInputs     = new ArrayList<>();
        allInputs.addAll(validInputs);
        allInputs.addAll(invalidInputs);

        // Comparison summary across all grammars
        PrintWriter comparisonWriter = new PrintWriter(new FileWriter(OUTPUT_DIR + "comparison.txt"));
        comparisonWriter.println("=== SLR(1) vs LR(1) Comparison ===\n");
        comparisonWriter.printf("%-35s | %-12s | %-12s | %-20s%n",
                "Grammar", "SLR States", "LR1 States", "SLR Conflicts?");
        comparisonWriter.println("-".repeat(85));

        // Parse trees collected across all grammars
        PrintWriter treeWriter = new PrintWriter(new FileWriter(OUTPUT_DIR + "parse_trees.txt"));
        treeWriter.println("=== Parse Trees ===\n");

        // SLR trace collected across all grammars
        PrintWriter slrTraceWriter = new PrintWriter(new FileWriter(OUTPUT_DIR + "slr_trace.txt"));
        slrTraceWriter.println("=== SLR(1) Parsing Traces ===\n");

        // LR1 trace collected across all grammars
        PrintWriter lr1TraceWriter = new PrintWriter(new FileWriter(OUTPUT_DIR + "lr1_trace.txt"));
        lr1TraceWriter.println("=== LR(1) Parsing Traces ===\n");

        // Process each grammar file
        for (String grammarFile : GRAMMAR_FILES) {
            String grammarName = grammarFile.replace(".txt", "");
            System.out.println("\n========================================");
            System.out.println("Processing: " + grammarFile);
            System.out.println("========================================");

            try {
                Grammar grammar = new Grammar();
                grammar.readFromFile(INPUT_DIR + grammarFile);
                grammar.augment();
                grammar.computeFirstSets();
                grammar.computeFollowSets();

                // Print to terminal
                grammar.printGrammar();
                grammar.printFirstFollowSets();

                // Save augmented grammar (appends grammar name as header)
                String augFile = OUTPUT_DIR + "augmented_grammar.txt";
                try (PrintWriter pw = new PrintWriter(new FileWriter(augFile,
                        !grammarFile.equals(GRAMMAR_FILES[0])))) { // overwrite on first, append after
                    pw.println("=== " + grammarName + " ===");
                }
                grammar.saveAugmentedGrammar(augFile, grammarName,
                        !grammarFile.equals(GRAMMAR_FILES[0])); // pass append flag

                SLRParser slr = new SLRParser(grammar);
                ParsingTable slrTable = slr.buildTable();

                Items slrItems = new Items(grammar);
                // Re-use the canonical collection already built inside slr
                List<Set<Items.LR0Item>> slrCollection = slr.getCanonicalCollection();

                // Print to terminal
                slrItems.printLR0Collection(slrCollection);
                slrTable.print(grammar.getAllProductions());

                // Save SLR items (append per grammar)
                slrItems.saveLR0Collection(slrCollection,
                        OUTPUT_DIR + "slr_items.txt",
                        grammarName,
                        !grammarFile.equals(GRAMMAR_FILES[0]));

                // Save SLR table (append per grammar)
                slrTable.save(OUTPUT_DIR + "slr_parsing_table.txt",
                        grammar.getAllProductions(),
                        grammarName,
                        !grammarFile.equals(GRAMMAR_FILES[0]));

                LR1Parser lr1 = new LR1Parser(grammar);
                ParsingTable lr1Table = lr1.buildTable();

                Items lr1Items = new Items(grammar);
                List<Set<Items.LR1Item>> lr1Collection = lr1.getCanonicalCollection();

                // Print to terminal
                lr1Items.printLR1Collection(lr1Collection);
                lr1Table.print(grammar.getAllProductions());

                // Save LR1 items (append per grammar)
                lr1Items.saveLR1Collection(lr1Collection,
                        OUTPUT_DIR + "lr1_items.txt",
                        grammarName,
                        !grammarFile.equals(GRAMMAR_FILES[0]));

                // Save LR1 table (append per grammar)
                lr1Table.save(OUTPUT_DIR + "lr1_parsing_table.txt",
                        grammar.getAllProductions(),
                        grammarName,
                        !grammarFile.equals(GRAMMAR_FILES[0]));

                comparisonWriter.printf("%-35s | %-12d | %-12d | %-20s%n",
                        grammarName,
                        slrCollection.size(),
                        lr1Collection.size(),
                        slrTable.hasConflicts() ? "YES (conflicts)" : "No");

                slrTraceWriter.println("\n--- Grammar: " + grammarName + " ---\n");
                lr1TraceWriter.println("\n--- Grammar: " + grammarName + " ---\n");
                treeWriter.println("\n--- Grammar: " + grammarName + " ---\n");

                for (String input : allInputs) {
                    System.out.println("\nParsing (SLR): \"" + input + "\"");
                    slrTraceWriter.println("Input: \"" + input + "\"");
                    parseToWriter(input, slrTable, grammar, slrTraceWriter, treeWriter, "SLR");

                    System.out.println("Parsing (LR1): \"" + input + "\"");
                    lr1TraceWriter.println("Input: \"" + input + "\"");
                    parseToWriter(input, lr1Table, grammar, lr1TraceWriter, null, "LR1");
                }

            } catch (Exception e) {
                System.err.println("Error processing " + grammarFile + ": " + e.getMessage());
                comparisonWriter.println(grammarFile + " -> ERROR: " + e.getMessage());
            }
        }

        comparisonWriter.close();
        treeWriter.close();
        slrTraceWriter.close();
        lr1TraceWriter.close();

        System.out.println("\n\nAll output files saved to: " + OUTPUT_DIR);
    }


    public static void parseToWriter(String inputStr, ParsingTable table, Grammar grammar,
                                     PrintWriter traceWriter, PrintWriter treeWriter,
                                     String parserLabel) {
        Stack stack = new Stack();
        Tree parseTree = new Tree();
        stack.push("", 0);

        List<String> tokens = new ArrayList<>(Arrays.asList(inputStr.split("\\s+")));
        tokens.add("$");
        int ip = 0;

        String header = String.format("%-5s | %-30s | %-25s | %-15s", "Step", "Stack", "Input", "Action");
        traceWriter.println(header);
        traceWriter.println("-".repeat(header.length()));

        // Also print to terminal
        System.out.println(header);

        int step = 1;
        boolean accepted = false;

        while (true) {
            int s = stack.topState();
            String a = tokens.get(ip);
            ParsingTable.Action action = table.getAction(s, a);

            String stackStr  = stack.toDisplayString();
            String inputLeft = String.join(" ", tokens.subList(ip, tokens.size()));
            String actionStr;

            if (action == null) {
                actionStr = "Error: Rejected";
                String row = String.format("%-5d | %-30s | %-25s | %-15s", step, stackStr, inputLeft, actionStr);
                traceWriter.println(row);
                System.out.println(row);
                break;
            }

            if (action.type == ParsingTable.ActionType.SHIFT) {
                actionStr = "Shift " + action.value;
                String row = String.format("%-5d | %-30s | %-25s | %-15s", step++, stackStr, inputLeft, actionStr);
                traceWriter.println(row);
                System.out.println(row);
                stack.push(a, action.value);
                parseTree.shift(a);
                ip++;

            } else if (action.type == ParsingTable.ActionType.REDUCE) {
                Grammar.Production prod = grammar.getAllProductions().get(action.value);
                actionStr = "Reduce " + prod;
                String row = String.format("%-5d | %-30s | %-25s | %-15s", step++, stackStr, inputLeft, actionStr);
                traceWriter.println(row);
                System.out.println(row);

                int n = prod.rhs.size();
                if (prod.rhs.get(0).equals(Grammar.EPSILON)) n = 0;
                stack.popN(n);
                int stateTop = stack.topState();
                int gotoState = table.getGoto(stateTop, prod.lhs);
                stack.push(prod.lhs, gotoState);
                parseTree.reduce(prod.lhs, n, n == 0);

            } else if (action.type == ParsingTable.ActionType.ACCEPT) {
                actionStr = "Accept";
                String row = String.format("%-5d | %-30s | %-25s | %-15s", step, stackStr, inputLeft, actionStr);
                traceWriter.println(row);
                System.out.println(row);
                accepted = true;

                // Save parse tree only from SLR (avoid duplicates)
                if (treeWriter != null) {
                    treeWriter.println("Input: \"" + inputStr + "\"");
                    try {
                        // print tree to a temp string then write
                        parseTree.print();
                        parseTree.save("../output/_tmp_tree.txt");
                        // read temp and write into treeWriter
                        BufferedReader br = new BufferedReader(new FileReader("../output/_tmp_tree.txt"));
                        String line;
                        while ((line = br.readLine()) != null) treeWriter.println(line);
                        br.close();
                        new File("../output/_tmp_tree.txt").delete();
                    } catch (IOException ignored) {}
                    treeWriter.println();
                }
                break;
            }
        }
        traceWriter.println();
    }

    static List<String> readLines(String filename) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not read " + filename + " (" + e.getMessage() + ")");
        }
        return lines;
    }
}