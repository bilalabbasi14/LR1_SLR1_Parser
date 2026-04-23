import java.io.*;
import java.util.*;

/**
 * Grammar.java
 * Represents a Context-Free Grammar (CFG).
 * Handles reading from file, augmentation, and computing FIRST/FOLLOW sets.
 */
public class Grammar {

    // Original start symbol
    private String startSymbol;

    // Augmented start symbol (e.g., "ExprPrime")
    private String augmentedStart;

    // All productions: maps LHS -> list of RHS alternatives (each RHS is a list of symbols)
    private LinkedHashMap<String, List<List<String>>> productions;

    // Ordered list of non-terminals (augmented start first)
    private List<String> nonTerminals;

    // Set of terminals (including "$")
    private Set<String> terminals;

    // FIRST sets
    private Map<String, Set<String>> firstSets;

    // FOLLOW sets
    private Map<String, Set<String>> followSets;

    // Epsilon representation
    public static final String EPSILON = "epsilon";

    public Grammar() {
        productions = new LinkedHashMap<>();
        nonTerminals = new ArrayList<>();
        terminals = new LinkedHashSet<>();
        firstSets = new HashMap<>();
        followSets = new HashMap<>();
    }

    // -----------------------------------------------------------------------
    // File reading
    // -----------------------------------------------------------------------

    /**
     * Reads a CFG from a file.
     * Format: NonTerminal -> prod1 | prod2 | ...
     */
    public void readFromFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        boolean firstProduction = true;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Split on "->"
            int arrowIdx = line.indexOf("->");
            if (arrowIdx == -1) {
                System.err.println("[Warning] Skipping malformed line: " + line);
                continue;
            }

            String lhs = line.substring(0, arrowIdx).trim();
            String rhsPart = line.substring(arrowIdx + 2).trim();

            if (firstProduction) {
                startSymbol = lhs;
                firstProduction = false;
            }

            if (!productions.containsKey(lhs)) {
                productions.put(lhs, new ArrayList<>());
                nonTerminals.add(lhs);
            }

            // Split alternatives by "|"
            String[] alternatives = rhsPart.split("\\|");
            for (String alt : alternatives) {
                List<String> symbols = new ArrayList<>();
                String[] tokens = alt.trim().split("\\s+");
                for (String tok : tokens) {
                    if (!tok.isEmpty()) {
                        symbols.add(tok);
                    }
                }
                if (!symbols.isEmpty()) {
                    productions.get(lhs).add(symbols);
                }
            }
        }
        br.close();
    }

    // -----------------------------------------------------------------------
    // Augmentation
    // -----------------------------------------------------------------------

    /**
     * Augments the grammar: adds S' -> S (original start).
     */
    public void augment() {
        // Create augmented start symbol name
        augmentedStart = startSymbol + "Prime";

        // Build new productions map with augmented start first
        LinkedHashMap<String, List<List<String>>> newProductions = new LinkedHashMap<>();
        List<String> newNonTerminals = new ArrayList<>();

        // Add S' -> S
        List<List<String>> augProd = new ArrayList<>();
        List<String> augRHS = new ArrayList<>();
        augRHS.add(startSymbol);
        augProd.add(augRHS);
        newProductions.put(augmentedStart, augProd);
        newNonTerminals.add(augmentedStart);

        // Add original productions
        newProductions.putAll(productions);
        newNonTerminals.addAll(nonTerminals);

        productions = newProductions;
        nonTerminals = newNonTerminals;

        // Collect all terminals
        collectTerminals();
    }

    /**
     * Collects all terminal symbols from the grammar.
     */
    private void collectTerminals() {
        terminals.clear();
        Set<String> ntSet = new HashSet<>(nonTerminals);

        for (List<List<String>> alts : productions.values()) {
            for (List<String> rhs : alts) {
                for (String sym : rhs) {
                    if (!ntSet.contains(sym) && !sym.equals(EPSILON)) {
                        terminals.add(sym);
                    }
                }
            }
        }
        terminals.add("$");
    }

    // -----------------------------------------------------------------------
    // FIRST sets
    // -----------------------------------------------------------------------

    /**
     * Computes FIRST sets for all non-terminals.
     */
    public void computeFirstSets() {
        // Initialize
        for (String nt : nonTerminals) {
            firstSets.put(nt, new LinkedHashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String nt : nonTerminals) {
                for (List<String> rhs : productions.get(nt)) {
                    Set<String> added = firstOfSequence(rhs);
                    if (firstSets.get(nt).addAll(added)) {
                        changed = true;
                    }
                }
            }
        }
    }

    /**
     * Returns FIRST set of a sequence of symbols.
     */
    public Set<String> firstOfSequence(List<String> symbols) {
        Set<String> result = new LinkedHashSet<>();

        if (symbols.isEmpty() || (symbols.size() == 1 && symbols.get(0).equals(EPSILON))) {
            result.add(EPSILON);
            return result;
        }

        boolean allCanBeEmpty = true;
        for (String sym : symbols) {
            Set<String> symFirst = firstOf(sym);
            // Add everything except epsilon
            for (String s : symFirst) {
                if (!s.equals(EPSILON)) result.add(s);
            }
            if (!symFirst.contains(EPSILON)) {
                allCanBeEmpty = false;
                break;
            }
        }

        if (allCanBeEmpty) result.add(EPSILON);
        return result;
    }

    /**
     * Returns FIRST set of a single symbol.
     */
    public Set<String> firstOf(String sym) {
        Set<String> result = new LinkedHashSet<>();
        if (sym.equals(EPSILON)) {
            result.add(EPSILON);
        } else if (terminals.contains(sym)) {
            result.add(sym);
        } else if (firstSets.containsKey(sym)) {
            result.addAll(firstSets.get(sym));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // FOLLOW sets
    // -----------------------------------------------------------------------

    /**
     * Computes FOLLOW sets for all non-terminals.
     */
    public void computeFollowSets() {
        for (String nt : nonTerminals) {
            followSets.put(nt, new LinkedHashSet<>());
        }

        // FOLLOW(augmented start) contains $
        followSets.get(augmentedStart).add("$");

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String nt : nonTerminals) {
                for (List<String> rhs : productions.get(nt)) {
                    for (int i = 0; i < rhs.size(); i++) {
                        String B = rhs.get(i);
                        if (!isNonTerminal(B)) continue;

                        // Compute FIRST of the suffix after B
                        List<String> suffix = rhs.subList(i + 1, rhs.size());
                        Set<String> firstSuffix = firstOfSequence(suffix.isEmpty()
                                ? Collections.singletonList(EPSILON) : suffix);

                        // Add FIRST(suffix) - {epsilon} to FOLLOW(B)
                        for (String s : firstSuffix) {
                            if (!s.equals(EPSILON)) {
                                if (followSets.get(B).add(s)) changed = true;
                            }
                        }

                        // If epsilon in FIRST(suffix), add FOLLOW(nt) to FOLLOW(B)
                        if (firstSuffix.contains(EPSILON)) {
                            if (followSets.get(B).addAll(followSets.get(nt))) changed = true;
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Utility / Accessors
    // -----------------------------------------------------------------------

    public boolean isNonTerminal(String sym) {
        return nonTerminals.contains(sym);
    }

    public boolean isTerminal(String sym) {
        return terminals.contains(sym);
    }

    public String getStartSymbol() { return startSymbol; }
    public String getAugmentedStart() { return augmentedStart; }
    public List<String> getNonTerminals() { return nonTerminals; }
    public Set<String> getTerminals() { return terminals; }
    public Map<String, Set<String>> getFirstSets() { return firstSets; }
    public Map<String, Set<String>> getFollowSets() { return followSets; }

    /**
     * Returns all productions as a flat list of (LHS, RHS) pairs.
     */
    public List<Production> getAllProductions() {
        List<Production> list = new ArrayList<>();
        for (String lhs : nonTerminals) {
            for (List<String> rhs : productions.get(lhs)) {
                list.add(new Production(lhs, rhs));
            }
        }
        return list;
    }

    /**
     * Returns all RHS alternatives for a given non-terminal.
     */
    public List<List<String>> getProductions(String nt) {
        return productions.getOrDefault(nt, Collections.emptyList());
    }

    /**
     * Pretty-prints the (augmented) grammar.
     */
    public void printGrammar() {
        System.out.println("=== Augmented Grammar ===");
        for (String nt : nonTerminals) {
            List<List<String>> alts = productions.get(nt);
            StringBuilder sb = new StringBuilder(nt + " -> ");
            for (int i = 0; i < alts.size(); i++) {
                sb.append(String.join(" ", alts.get(i)));
                if (i < alts.size() - 1) sb.append(" | ");
            }
            System.out.println(sb);
        }
        System.out.println();
    }

    /**
     * Prints FIRST and FOLLOW sets.
     */
    public void printFirstFollowSets() {
        System.out.println("=== FIRST Sets ===");
        for (String nt : nonTerminals) {
            System.out.println("FIRST(" + nt + ") = " + firstSets.get(nt));
        }
        System.out.println();
        System.out.println("=== FOLLOW Sets ===");
        for (String nt : nonTerminals) {
            System.out.println("FOLLOW(" + nt + ") = " + followSets.get(nt));
        }
        System.out.println();
    }

    /**
     * Saves augmented grammar to a file.
     */
    public void saveAugmentedGrammar(String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename));
        pw.println("=== Augmented Grammar ===");
        for (String nt : nonTerminals) {
            List<List<String>> alts = productions.get(nt);
            StringBuilder sb = new StringBuilder(nt + " -> ");
            for (int i = 0; i < alts.size(); i++) {
                sb.append(String.join(" ", alts.get(i)));
                if (i < alts.size() - 1) sb.append(" | ");
            }
            pw.println(sb);
        }
        pw.close();
    }

    // -----------------------------------------------------------------------
    // Inner class: Production
    // -----------------------------------------------------------------------

    /**
     * Represents a single production rule LHS -> RHS.
     */
    public static class Production {
        public final String lhs;
        public final List<String> rhs;

        public Production(String lhs, List<String> rhs) {
            this.lhs = lhs;
            this.rhs = Collections.unmodifiableList(new ArrayList<>(rhs));
        }

        @Override
        public String toString() {
            if (rhs.isEmpty() || (rhs.size() == 1 && rhs.get(0).equals(EPSILON))) {
                return lhs + " -> " + EPSILON;
            }
            return lhs + " -> " + String.join(" ", rhs);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Production)) return false;
            Production p = (Production) o;
            return lhs.equals(p.lhs) && rhs.equals(p.rhs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lhs, rhs);
        }
    }
}