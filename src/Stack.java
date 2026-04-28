import java.util.*;

public class Stack {


    public static class StackEntry {
        public final String symbol;
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


    public void push(String symbol, int state) {
        stack.push(new StackEntry(symbol, state));
    }

    public StackEntry pop() {
        if (stack.isEmpty()) throw new EmptyStackException();
        return stack.pop();
    }

    public StackEntry peek() {
        if (stack.isEmpty()) throw new EmptyStackException();
        return stack.peek();
    }

    public int topState() {
        return peek().state;
    }

    public void popN(int n) {
        for (int i = 0; i < n; i++) pop();
    }

    public boolean isEmpty() { return stack.isEmpty(); }
    public int size()        { return stack.size(); }


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

    public String statesToString() {
        List<StackEntry> entries = new ArrayList<>(stack);
        Collections.reverse(entries);
        StringBuilder sb = new StringBuilder();
        for (StackEntry e : entries) {
            sb.append(e.state).append(" ");
        }
        return sb.toString().trim();
    }

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