import java.util.*;
import java.io.*;

/**
 * Tree.java
 * Represents a parse tree and provides utilities for building and displaying it.
 *
 * The parse tree is built during bottom-up parsing:
 * - Leaves = terminals from the input
 * - Internal nodes = non-terminals created during reduce steps
 * - Root = the start symbol
 */
public class Tree {


    public static class TreeNode {
        public final String symbol;           // grammar symbol this node represents
        public final List<TreeNode> children; // left-to-right children
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

    // During parsing we maintain a node stack parallel to the parser stack.
    // Each element is the TreeNode corresponding to that stack symbol.
    private final Deque<TreeNode> nodeStack;

    public Tree() {
        this.nodeStack = new ArrayDeque<>();
        this.root      = null;
    }


    /**
     * Called when the parser performs a SHIFT on terminal `symbol`.
     * Pushes a leaf node onto the node stack.
     */
    public void shift(String symbol) {
        nodeStack.push(new TreeNode(symbol, true));
    }

    /**
     * Called when the parser performs a REDUCE using production lhs -> rhs.
     * Pops |rhs| nodes from the node stack, creates an internal node for lhs,
     * and pushes it back.
     *
     * @param lhs     the non-terminal being reduced to
     * @param rhsSize the length of the RHS (number of symbols to pop)
     * @param isEps   true if this is an epsilon production
     */
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

    /**
     * Returns the root of the parse tree (valid after successful parse).
     */
    public TreeNode getRoot() {
        return root;
    }


    /**
     * Prints the parse tree to stdout in a readable indented format.
     */
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

    /**
     * Saves the parse tree to a file.
     */
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

    /**
     * Returns a bracket-notation string of the tree.
     * E.g., [Expr [Expr [Term [Factor [id]]]] [+] [Term [Factor [id]]]]
     */
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