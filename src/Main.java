import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Grammar grammar = new Grammar();
            grammar.readFromFile("grammar.txt"); // Ensure this file exists [cite: 243]
            grammar.augment(); // [cite: 23]
            grammar.computeFirstSets();
            grammar.computeFollowSets();
            grammar.printGrammar();
            grammar.printFirstFollowSets();

            // Part 1: SLR(1) [cite: 31]
            SLRParser slr = new SLRParser(grammar);
            ParsingTable slrTable = slr.buildTable();
            slrTable.print(grammar.getAllProductions());

            // Part 2: LR(1) [cite: 128]
            LR1Parser lr1 = new LR1Parser(grammar);
            ParsingTable lr1Table = lr1.buildTable();
            lr1Table.print(grammar.getAllProductions());

            // Parsing Example [cite: 218]
            String input = "id + id * id";
            System.out.println("Parsing input: " + input);
            parse(input, slrTable, grammar);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void parse(String inputStr, ParsingTable table, Grammar grammar) {
        Stack stack = new Stack();
        Tree parseTree = new Tree();
        stack.push("", 0); // Initial state [cite: 105]

        List<String> tokens = new ArrayList<>(Arrays.asList(inputStr.split("\\s+")));
        tokens.add("$");
        int ip = 0;

        System.out.println(String.format("%-5s | %-25s | %-20s | %-15s", "Step", "Stack", "Input", "Action"));
        int step = 1;

        while (true) {
            int s = stack.topState();
            String a = tokens.get(ip);
            ParsingTable.Action action = table.getAction(s, a);

            System.out.print(String.format("%-5d | %-25s | %-20s | ", step++, stack.toDisplayString(), String.join(" ", tokens.subList(ip, tokens.size()))));

            if (action == null) {
                System.out.println("Error: Rejected");
                break;
            }

            if (action.type == ParsingTable.ActionType.SHIFT) {
                System.out.println("Shift " + action.value);
                stack.push(a, action.value);
                parseTree.shift(a);
                ip++;
            } else if (action.type == ParsingTable.ActionType.REDUCE) {
                Grammar.Production prod = grammar.getAllProductions().get(action.value);
                System.out.println("Reduce " + prod);

                int n = prod.rhs.size();
                if (prod.rhs.get(0).equals(Grammar.EPSILON)) n = 0;

                stack.popN(n);
                int stateTop = stack.topState();
                int gotoState = table.getGoto(stateTop, prod.lhs);
                stack.push(prod.lhs, gotoState);

                parseTree.reduce(prod.lhs, n, n == 0);
            } else if (action.type == ParsingTable.ActionType.ACCEPT) {
                System.out.println("Accept");
                parseTree.print();
                break;
            }
        }
    }
}