import java.util.*;
import java.io.*;

/**
 * ParsingTable.java
 * Stores the ACTION and GOTO tables for SLR(1) or LR(1) parsers.
 * Detects and reports shift/reduce and reduce/reduce conflicts.
 */
public class ParsingTable {

    public enum ActionType { SHIFT, REDUCE, ACCEPT, ERROR }

    /** A single cell in the ACTION table. */
    public static class Action {
        public final ActionType type;
        public final int value; // state to shift to, OR production index to reduce by

        public Action(ActionType type, int value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return switch (type) {
                case SHIFT  -> "s" + value;
                case REDUCE -> "r" + value;
                case ACCEPT -> "acc";
                case ERROR  -> "";
            };
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Action)) return false;
            Action a = (Action) o;
            return type == a.type && value == a.value;
        }

        @Override
        public int hashCode() { return Objects.hash(type, value); }
    }

    private final int numStates;
    private final List<String> terminals;       // ACTION columns (ordered)
    private final List<String> nonTerminals;    // GOTO columns (ordered, excl. aug start)

    // ACTION[state][terminal] = set of actions (set size > 1 => conflict)
    private final Map<Integer, Map<String, List<Action>>> actionTable;

    // GOTO[state][nonTerminal] = target state, -1 = undefined
    private final Map<Integer, Map<String, Integer>> gotoTable;

    // Conflict log
    private final List<String> conflicts;

    private final String parserName; // "SLR(1)" or "LR(1)"


    public ParsingTable(int numStates, List<String> terminals,
                        List<String> nonTerminals, String parserName) {
        this.numStates    = numStates;
        this.terminals    = new ArrayList<>(terminals);
        this.nonTerminals = new ArrayList<>(nonTerminals);
        this.parserName   = parserName;
        this.actionTable  = new HashMap<>();
        this.gotoTable    = new HashMap<>();
        this.conflicts    = new ArrayList<>();

        // Initialize all cells to empty
        for (int i = 0; i < numStates; i++) {
            actionTable.put(i, new LinkedHashMap<>());
            gotoTable.put(i, new LinkedHashMap<>());
        }
    }


    /**
     * Sets an ACTION entry. Detects conflicts automatically.
     */
    public void setAction(int state, String terminal, Action action) {
        Map<String, List<Action>> row = actionTable.get(state);
        if (!row.containsKey(terminal)) {
            row.put(terminal, new ArrayList<>());
        }
        List<Action> existing = row.get(terminal);

        if (!existing.contains(action)) {
            if (!existing.isEmpty()) {
                // Conflict detected
                for (Action prev : existing) {
                    String conflictType;
                    if (prev.type == ActionType.SHIFT && action.type == ActionType.REDUCE) {
                        conflictType = "Shift/Reduce";
                    } else if (prev.type == ActionType.REDUCE && action.type == ActionType.SHIFT) {
                        conflictType = "Shift/Reduce";
                    } else {
                        conflictType = "Reduce/Reduce";
                    }
                    String msg = conflictType + " conflict in state " + state
                            + " on terminal '" + terminal + "': "
                            + prev + " vs " + action;
                    if (!conflicts.contains(msg)) {
                        conflicts.add(msg);
                    }
                }
            }
            existing.add(action);
        }
    }

    /**
     * Sets a GOTO entry.
     */
    public void setGoto(int state, String nonTerminal, int targetState) {
        gotoTable.get(state).put(nonTerminal, targetState);
    }



    /**
     * Returns the primary (first) action for a given state and terminal.
     * Returns null if no action (error).
     */
    public Action getAction(int state, String terminal) {
        Map<String, List<Action>> row = actionTable.get(state);
        if (row == null) return null;
        List<Action> actions = row.get(terminal);
        if (actions == null || actions.isEmpty()) return null;
        return actions.get(0); // use first (shift-preference in conflicts)
    }

    /**
     * Returns all actions for conflict display.
     */
    public List<Action> getAllActions(int state, String terminal) {
        Map<String, List<Action>> row = actionTable.get(state);
        if (row == null) return Collections.emptyList();
        return row.getOrDefault(terminal, Collections.emptyList());
    }

    /**
     * Returns the GOTO target state, or -1 if undefined.
     */
    public int getGoto(int state, String nonTerminal) {
        Map<String, Integer> row = gotoTable.get(state);
        if (row == null) return -1;
        return row.getOrDefault(nonTerminal, -1);
    }


    public boolean hasConflicts() { return !conflicts.isEmpty(); }
    public List<String> getConflicts() { return Collections.unmodifiableList(conflicts); }


    /**
     * Pretty-prints the parsing table to stdout.
     */
    public void print(List<Grammar.Production> productions) {
        System.out.println("=== " + parserName + " Parsing Table ===");

        // Determine column widths
        int stateW = 7;
        int colW   = 10;

        // Header
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + stateW + "s", "State"));
        header.append("| ");
        // ACTION columns
        for (String t : terminals) {
            header.append(String.format("%-" + colW + "s", t));
        }
        header.append("| ");
        // GOTO columns (skip augmented start)
        for (String nt : nonTerminals) {
            header.append(String.format("%-" + colW + "s", nt));
        }
        System.out.println(header);
        System.out.println("-".repeat(header.length()));

        for (int s = 0; s < numStates; s++) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-" + stateW + "s", "I" + s));
            row.append("| ");
            for (String t : terminals) {
                List<Action> acts = getAllActions(s, t);
                String cell = acts.isEmpty() ? "" : formatActions(acts);
                row.append(String.format("%-" + colW + "s", cell));
            }
            row.append("| ");
            for (String nt : nonTerminals) {
                int g = getGoto(s, nt);
                row.append(String.format("%-" + colW + "s", g == -1 ? "" : String.valueOf(g)));
            }
            System.out.println(row);
        }
        System.out.println();

        // Print production numbers used in reduce actions
        System.out.println("--- Productions (for reduce actions) ---");
        for (int i = 0; i < productions.size(); i++) {
            System.out.println("  r" + i + ": " + productions.get(i));
        }
        System.out.println();

        // Conflicts
        if (hasConflicts()) {
            System.out.println("*** CONFLICTS DETECTED ***");
            for (String c : conflicts) System.out.println("  " + c);
            System.out.println("=> Grammar is NOT " + parserName);
        } else {
            System.out.println("=> Grammar IS " + parserName + " (no conflicts)");
        }
        System.out.println();
    }

    private String formatActions(List<Action> acts) {
        if (acts.size() == 1) return acts.get(0).toString();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < acts.size(); i++) {
            if (i > 0) sb.append("/");
            sb.append(acts.get(i));
        }
        return sb.toString();
    }

    /**
     * Saves parsing table to a file.
     */
    public void save(String filename, List<Grammar.Production> productions) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename));
        pw.println("=== " + parserName + " Parsing Table ===\n");

        int stateW = 7, colW = 12;
        StringBuilder header = new StringBuilder();
        header.append(String.format("%-" + stateW + "s", "State"));
        header.append("| ACTION ");
        for (String t : terminals) header.append(String.format("%-" + colW + "s", t));
        header.append("| GOTO ");
        for (String nt : nonTerminals) header.append(String.format("%-" + colW + "s", nt));
        pw.println(header);
        pw.println("-".repeat(Math.max(header.length(), 60)));

        for (int s = 0; s < numStates; s++) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-" + stateW + "s", "I" + s));
            row.append("|        ");
            for (String t : terminals) {
                List<Action> acts = getAllActions(s, t);
                String cell = acts.isEmpty() ? "" : formatActions(acts);
                row.append(String.format("%-" + colW + "s", cell));
            }
            row.append("|      ");
            for (String nt : nonTerminals) {
                int g = getGoto(s, nt);
                row.append(String.format("%-" + colW + "s", g == -1 ? "" : String.valueOf(g)));
            }
            pw.println(row);
        }

        pw.println();
        pw.println("--- Productions ---");
        for (int i = 0; i < productions.size(); i++) {
            pw.println("  r" + i + ": " + productions.get(i));
        }
        pw.println();

        if (hasConflicts()) {
            pw.println("*** CONFLICTS DETECTED ***");
            for (String c : conflicts) pw.println("  " + c);
            pw.println("=> Grammar is NOT " + parserName);
        } else {
            pw.println("=> Grammar IS " + parserName + " (no conflicts)");
        }
        pw.close();
    }

    // Getters
    public int getNumStates()        { return numStates; }
    public List<String> getTerminals()    { return terminals; }
    public List<String> getNonTerminals() { return nonTerminals; }
    public String getParserName()    { return parserName; }
}