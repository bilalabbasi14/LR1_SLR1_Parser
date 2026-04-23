import java.util.*;

/**
 * Stack.java
 * A generic stack used during LR parsing.
 * Each entry holds a (symbol, state) pair reflecting the parser stack.
 *
 * The LR parsing stack alternates between grammar symbols and state numbers.
 * This class models that as a list of StackEntry objects for easy display.
 */
public class Stack {


    public static class StackEntry {
        public final String symbol; // grammar symbol (or "" for the initial empty slot)
        public final int state;

        public StackEntry(String symbol, int state) {
            this.symbol = symbol;
            this.state  = state;
        }

        @Override
        public String toString() {
            if (symbol.isEmpty()) return String.valueOf(state);
            return symbol + " " + state;
        }
    }


    private final Deque<StackEntry> stack;


    public Stack() {
        this.stack = new ArrayDeque<>();
    }


    /**
     * Pushes a (symbol, state) entry onto the stack.
     */
    public void push(String symbol, int state) {
        stack.push(new StackEntry(symbol, state));
    }

    /**
     * Pops the top entry from the stack.
     */
    public StackEntry pop() {
        if (stack.isEmpty()) throw new EmptyStackException();
        return stack.pop();
    }

    /**
     * Peeks at the top entry without removing it.
     */
    public StackEntry peek() {
        if (stack.isEmpty()) throw new EmptyStackException();
        return stack.peek();
    }

    /**
     * Returns the state at the top of the stack.
     */
    public int topState() {
        return peek().state;
    }

    /**
     * Pops n entries (used during reduce: pop 2 * |beta| symbols).
     * Actually we model one entry per symbol (symbol+state pair), so pop n entries.
     */
    public void popN(int n) {
        for (int i = 0; i < n; i++) pop();
    }

    public boolean isEmpty() { return stack.isEmpty(); }
    public int size()        { return stack.size(); }


    /**
     * Returns a human-readable representation of the stack (bottom to top).
     */
    public String toDisplayString() {
        // Stack is stored top-first (Deque); reverse for bottom-to-top display
        List<StackEntry> entries = new ArrayList<>(stack);
        Collections.reverse(entries);

        StringBuilder sb = new StringBuilder();
        for (StackEntry e : entries) {
            sb.append(e.toString()).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Returns only the state numbers in bottom-to-top order.
     */
    public String statesToString() {
        List<StackEntry> entries = new ArrayList<>(stack);
        Collections.reverse(entries);
        StringBuilder sb = new StringBuilder();
        for (StackEntry e : entries) {
            sb.append(e.state).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Returns only the symbols (non-state part) bottom-to-top.
     */
    public String symbolsToString() {
        List<StackEntry> entries = new ArrayList<>(stack);
        Collections.reverse(entries);
        StringBuilder sb = new StringBuilder();
        for (StackEntry e : entries) {
            if (!e.symbol.isEmpty()) sb.append(e.symbol).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}