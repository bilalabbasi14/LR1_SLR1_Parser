import java.util.*;
import java.io.*;

public class Tree {

    public static class TreeNode {
        public final String symbol;
        public final List<TreeNode> children;
        public boolean isTerminal;

        public TreeNode(String symbol, boolean isTerminal) {
            this.symbol     = symbol;
            this.isTerminal = isTerminal;
            this.children   = new ArrayList<>();
        }

        public void addChild(TreeNode child) {
            children.add(child);
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    private TreeNode root;

    private final Deque<TreeNode> nodeStack;

    public Tree() {
        this.nodeStack = new ArrayDeque<>();
        this.root      = null;
    }

    public void shift(String symbol) {
        nodeStack.push(new TreeNode(symbol, true));
    }

    public void reduce(String lhs, int rhsSize, boolean isEps) {
        TreeNode parent = new TreeNode(lhs, false);

        if (isEps) {
            // Epsilon production: add an epsilon leaf
            parent.addChild(new TreeNode(Grammar.EPSILON, true));
        } else {
            // Pop rhsSize nodes (they are in reverse order on the stack)
            List<TreeNode> children = new ArrayList<>();
            for (int i = 0; i < rhsSize; i++) {
                if (!nodeStack.isEmpty()) {
                    children.add(nodeStack.pop());
                }
            }
            // Reverse to get left-to-right order
            Collections.reverse(children);
            for (TreeNode child : children) {
                parent.addChild(child);
            }
        }

        nodeStack.push(parent);
        root = parent; // keep track of last reduced node (will be root at end)
    }

    public TreeNode getRoot() {
        return root;
    }

    public void print() {
        if (root == null) {
            System.out.println("[No parse tree available]");
            return;
        }
        System.out.println("=== Parse Tree ===");
        printNode(root, "", true);
        System.out.println();
    }

    private void printNode(TreeNode node, String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        System.out.println(prefix + connector + node.symbol
                + (node.isTerminal ? " [terminal]" : " [" + node.symbol + "]"));

        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < node.children.size(); i++) {
            printNode(node.children.get(i), childPrefix, i == node.children.size() - 1);
        }
    }

    public void save(String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename, true)); // append mode
        pw.println("=== Parse Tree ===");
        if (root == null) {
            pw.println("[No parse tree available]");
        } else {
            saveNode(pw, root, "", true);
        }
        pw.println();
        pw.close();
    }

    private void saveNode(PrintWriter pw, TreeNode node, String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        pw.println(prefix + connector + node.symbol
                + (node.isTerminal ? " [terminal]" : ""));
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < node.children.size(); i++) {
            saveNode(pw, node.children.get(i), childPrefix, i == node.children.size() - 1);
        }
    }

    public String toBracketNotation() {
        if (root == null) return "[]";
        return toBracketNode(root);
    }

    private String toBracketNode(TreeNode node) {
        if (node.isLeaf()) return "[" + node.symbol + "]";
        StringBuilder sb = new StringBuilder("[" + node.symbol);
        for (TreeNode child : node.children) {
            sb.append(" ").append(toBracketNode(child));
        }
        sb.append("]");
        return sb.toString();
    }

    public void reset() {
        nodeStack.clear();
        root = null;
    }
}