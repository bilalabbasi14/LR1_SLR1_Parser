import java.util.*;
import java.io.IOException;
public class Items {

    private Grammar grammar;

    // All productions as a flat list (for index-based reduce actions)
    private List<Grammar.Production> allProductions;

    public Items(Grammar grammar) {
        this.grammar = grammar;
        this.allProductions = grammar.getAllProductions();
    }

    public static class LR0Item {
        public final Grammar.Production production;
        public final int dotPos;

        public LR0Item(Grammar.Production production, int dotPos) {
            this.production = production;
            this.dotPos = dotPos;
        }

        public String symbolAfterDot() {
            List<String> rhs = production.rhs;
            if (rhs.isEmpty() || (rhs.size() == 1 && rhs.get(0).equals(Grammar.EPSILON))) return null;
            if (dotPos >= rhs.size()) return null;
            return rhs.get(dotPos);
        }

        public boolean isComplete() {
            return symbolAfterDot() == null;
        }

        @Override
        public String toString() {
            List<String> rhs = production.rhs;
            StringBuilder sb = new StringBuilder(production.lhs + " -> ");
            if (rhs.isEmpty() || (rhs.size() == 1 && rhs.get(0).equals(Grammar.EPSILON))) {
                sb.append("• ");
            } else {
                for (int i = 0; i < rhs.size(); i++) {
                    if (i == dotPos) sb.append("• ");
                    sb.append(rhs.get(i)).append(" ");
                }
                if (dotPos == rhs.size()) sb.append("•");
            }
            return sb.toString().trim();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LR0Item)) return false;
            LR0Item other = (LR0Item) o;
            return dotPos == other.dotPos && production.equals(other.production);
        }

        @Override
        public int hashCode() {
            return Objects.hash(production, dotPos);
        }
    }

    public static class LR1Item {
        public final Grammar.Production production;
        public final int dotPos;
        public final String lookahead;

        public LR1Item(Grammar.Production production, int dotPos, String lookahead) {
            this.production = production;
            this.dotPos = dotPos;
            this.lookahead = lookahead;
        }

        public String symbolAfterDot() {
            List<String> rhs = production.rhs;
            if (rhs.isEmpty() || (rhs.size() == 1 && rhs.get(0).equals(Grammar.EPSILON))) return null;
            if (dotPos >= rhs.size()) return null;
            return rhs.get(dotPos);
        }

        public boolean isComplete() {
            return symbolAfterDot() == null;
        }

        @Override
        public String toString() {
            List<String> rhs = production.rhs;
            StringBuilder sb = new StringBuilder("[" + production.lhs + " -> ");
            if (rhs.isEmpty() || (rhs.size() == 1 && rhs.get(0).equals(Grammar.EPSILON))) {
                sb.append("•");
            } else {
                for (int i = 0; i < rhs.size(); i++) {
                    if (i == dotPos) sb.append("• ");
                    sb.append(rhs.get(i));
                    if (i < rhs.size() - 1) sb.append(" ");
                }
                if (dotPos == rhs.size()) sb.append(" •");
            }
            sb.append(", ").append(lookahead).append("]");
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LR1Item)) return false;
            LR1Item other = (LR1Item) o;
            return dotPos == other.dotPos
                    && production.equals(other.production)
                    && lookahead.equals(other.lookahead);
        }

        @Override
        public int hashCode() {
            return Objects.hash(production, dotPos, lookahead);
        }
    }

    public Set<LR0Item> closure0(Set<LR0Item> items) {
        Set<LR0Item> closure = new LinkedHashSet<>(items);
        Queue<LR0Item> worklist = new LinkedList<>(items);

        while (!worklist.isEmpty()) {
            LR0Item item = worklist.poll();
            String B = item.symbolAfterDot();
            if (B == null || !grammar.isNonTerminal(B)) continue;

            // Add B -> • gamma for all productions B -> gamma
            for (List<String> rhs : grammar.getProductions(B)) {
                Grammar.Production prod = new Grammar.Production(B, rhs);
                LR0Item newItem = new LR0Item(prod, 0);
                if (closure.add(newItem)) {
                    worklist.add(newItem);
                }
            }
        }
        return closure;
    }

    public Set<LR0Item> goto0(Set<LR0Item> items, String X) {
        Set<LR0Item> moved = new LinkedHashSet<>();
        for (LR0Item item : items) {
            String sym = item.symbolAfterDot();
            if (sym != null && sym.equals(X)) {
                moved.add(new LR0Item(item.production, item.dotPos + 1));
            }
        }
        if (moved.isEmpty()) return Collections.emptySet();
        return closure0(moved);
    }

    public List<Set<LR0Item>> buildCanonicalLR0() {
        List<Set<LR0Item>> collection = new ArrayList<>();

        // Initial item: S' -> • S
        Grammar.Production startProd = new Grammar.Production(
                grammar.getAugmentedStart(),
                Collections.singletonList(grammar.getStartSymbol()));
        Set<LR0Item> initial = new LinkedHashSet<>();
        initial.add(new LR0Item(startProd, 0));
        Set<LR0Item> I0 = closure0(initial);
        collection.add(I0);

        // All grammar symbols
        Set<String> allSymbols = new LinkedHashSet<>();
        allSymbols.addAll(grammar.getNonTerminals());
        allSymbols.addAll(grammar.getTerminals());

        Queue<Set<LR0Item>> worklist = new LinkedList<>();
        worklist.add(I0);

        while (!worklist.isEmpty()) {
            Set<LR0Item> I = worklist.poll();
            for (String X : allSymbols) {
                Set<LR0Item> gotoSet = goto0(I, X);
                if (!gotoSet.isEmpty() && !containsSet0(collection, gotoSet)) {
                    collection.add(gotoSet);
                    worklist.add(gotoSet);
                }
            }
        }
        return collection;
    }

    private boolean containsSet0(List<Set<LR0Item>> collection, Set<LR0Item> target) {
        for (Set<LR0Item> s : collection) {
            if (s.equals(target)) return true;
        }
        return false;
    }

    public int indexOf0(List<Set<LR0Item>> collection, Set<LR0Item> target) {
        for (int i = 0; i < collection.size(); i++) {
            if (collection.get(i).equals(target)) return i;
        }
        return -1;
    }

    public Set<LR1Item> closure1(Set<LR1Item> items) {
        Set<LR1Item> closure = new LinkedHashSet<>(items);
        Queue<LR1Item> worklist = new LinkedList<>(items);

        while (!worklist.isEmpty()) {
            LR1Item item = worklist.poll();
            String B = item.symbolAfterDot();
            if (B == null || !grammar.isNonTerminal(B)) continue;

            // Build beta + lookahead sequence to compute FIRST
            List<String> rhs = item.production.rhs;
            List<String> betaA = new ArrayList<>();
            // Symbols after B in the production
            if (!rhs.isEmpty() && !(rhs.size() == 1 && rhs.get(0).equals(Grammar.EPSILON))) {
                for (int k = item.dotPos + 1; k < rhs.size(); k++) {
                    betaA.add(rhs.get(k));
                }
            }
            betaA.add(item.lookahead);

            Set<String> lookaheads = grammar.firstOfSequence(betaA);
            lookaheads.remove(Grammar.EPSILON);

            for (List<String> prodRHS : grammar.getProductions(B)) {
                Grammar.Production prod = new Grammar.Production(B, prodRHS);
                for (String la : lookaheads) {
                    LR1Item newItem = new LR1Item(prod, 0, la);
                    if (closure.add(newItem)) {
                        worklist.add(newItem);
                    }
                }
            }
        }
        return closure;
    }

    public Set<LR1Item> goto1(Set<LR1Item> items, String X) {
        Set<LR1Item> moved = new LinkedHashSet<>();
        for (LR1Item item : items) {
            String sym = item.symbolAfterDot();
            if (sym != null && sym.equals(X)) {
                moved.add(new LR1Item(item.production, item.dotPos + 1, item.lookahead));
            }
        }
        if (moved.isEmpty()) return Collections.emptySet();
        return closure1(moved);
    }

    public List<Set<LR1Item>> buildCanonicalLR1() {
        List<Set<LR1Item>> collection = new ArrayList<>();

        // Initial item: [S' -> • S, $]
        Grammar.Production startProd = new Grammar.Production(
                grammar.getAugmentedStart(),
                Collections.singletonList(grammar.getStartSymbol()));
        Set<LR1Item> initial = new LinkedHashSet<>();
        initial.add(new LR1Item(startProd, 0, "$"));
        Set<LR1Item> I0 = closure1(initial);
        collection.add(I0);

        Set<String> allSymbols = new LinkedHashSet<>();
        allSymbols.addAll(grammar.getNonTerminals());
        allSymbols.addAll(grammar.getTerminals());

        Queue<Set<LR1Item>> worklist = new LinkedList<>();
        worklist.add(I0);

        while (!worklist.isEmpty()) {
            Set<LR1Item> I = worklist.poll();
            for (String X : allSymbols) {
                Set<LR1Item> gotoSet = goto1(I, X);
                if (!gotoSet.isEmpty() && !containsSet1(collection, gotoSet)) {
                    collection.add(gotoSet);
                    worklist.add(gotoSet);
                }
            }
        }
        return collection;
    }

    private boolean containsSet1(List<Set<LR1Item>> collection, Set<LR1Item> target) {
        for (Set<LR1Item> s : collection) {
            if (s.equals(target)) return true;
        }
        return false;
    }

    public int indexOf1(List<Set<LR1Item>> collection, Set<LR1Item> target) {
        for (int i = 0; i < collection.size(); i++) {
            if (collection.get(i).equals(target)) return i;
        }
        return -1;
    }


    public void printLR0Collection(List<Set<LR0Item>> collection) {
        System.out.println("=== Canonical Collection of LR(0) Item Sets ===");
        for (int i = 0; i < collection.size(); i++) {
            System.out.println("I" + i + ":");
            for (LR0Item item : collection.get(i)) {
                System.out.println("  " + item);
            }
            System.out.println();
        }
        System.out.println("Total states: " + collection.size());
        System.out.println();
    }

    public void printLR1Collection(List<Set<LR1Item>> collection) {
        System.out.println("=== Canonical Collection of LR(1) Item Sets ===");
        for (int i = 0; i < collection.size(); i++) {
            System.out.println("I" + i + ":");
            for (LR1Item item : collection.get(i)) {
                System.out.println("  " + item);
            }
            System.out.println();
        }
        System.out.println("Total states: " + collection.size());
        System.out.println();
    }

    public void saveLR0Collection(List<Set<LR0Item>> collection, String filename, String grammarName, boolean append) throws IOException {
        java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(filename, append));
        pw.println("\n=== LR(0) Items: " + grammarName + " ===\n");
        for (int i = 0; i < collection.size(); i++) {
            pw.println("I" + i + ":");
            for (LR0Item item : collection.get(i)) {
                pw.println("  " + item);
            }
            pw.println();
        }
        pw.println("Total states: " + collection.size());
        pw.close();
    }

    public void saveLR1Collection(List<Set<LR1Item>> collection, String filename, String grammarName, boolean append) throws IOException {
        java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(filename, append));
        pw.println("\n=== LR(1) Items: " + grammarName + " ===\n");
        for (int i = 0; i < collection.size(); i++) {
            pw.println("I" + i + ":");
            for (LR1Item item : collection.get(i)) {
                pw.println("  " + item);
            }
            pw.println();
        }
        pw.println("Total states: " + collection.size());
        pw.close();
    }

    // Accessors
    public List<Grammar.Production> getAllProductions() { return allProductions; }


    public int productionIndex(Grammar.Production prod) {
        for (int i = 0; i < allProductions.size(); i++) {
            if (allProductions.get(i).equals(prod)) return i;
        }
        return -1;
    }
}