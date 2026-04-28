import java.io.*;
import java.util.*;

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

    public List<Production> getAllProductions() {
        List<Production> list = new ArrayList<>();
        for (String lhs : nonTerminals) {
            for (List<String> rhs : productions.get(lhs)) {
                list.add(new Production(lhs, rhs));
            }
        }
        return list;
    }

    public List<List<String>> getProductions(String nt) {
        return productions.getOrDefault(nt, Collections.emptyList());
    }

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

    public void saveAugmentedGrammar(String filename, String grammarName, boolean append) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename, append));
        pw.println("\n=== Augmented Grammar: " + grammarName + " ===");
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