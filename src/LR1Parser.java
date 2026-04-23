import java.util.*;

public class LR1Parser {
    private Grammar grammar;
    private Items itemsHandler;
    private ParsingTable table;
    private List<Set<Items.LR1Item>> canonicalCollection;

    public LR1Parser(Grammar grammar) {
        this.grammar = grammar;
        this.itemsHandler = new Items(grammar);
    }

    public ParsingTable buildTable() {
        canonicalCollection = itemsHandler.buildCanonicalLR1();
        int numStates = canonicalCollection.size();

        List<String> terminals = new ArrayList<>(grammar.getTerminals());
        List<String> nonTerminals = new ArrayList<>(grammar.getNonTerminals());
        nonTerminals.remove(grammar.getAugmentedStart());

        table = new ParsingTable(numStates, terminals, nonTerminals, "LR(1)");

        for (int i = 0; i < numStates; i++) {
            Set<Items.LR1Item> stateItems = canonicalCollection.get(i);

            for (Items.LR1Item item : stateItems) {
                String symbolAfterDot = item.symbolAfterDot();

                // 1. Shift Actions [cite: 162]
                if (symbolAfterDot != null && grammar.isTerminal(symbolAfterDot)) {
                    Set<Items.LR1Item> nextStateItems = itemsHandler.goto1(stateItems, symbolAfterDot);
                    int nextState = itemsHandler.indexOf1(canonicalCollection, nextStateItems);
                    table.setAction(i, symbolAfterDot, new ParsingTable.Action(ParsingTable.ActionType.SHIFT, nextState));
                }

                // 2. Reduce Actions (Using specific lookahead) [cite: 166, 172]
                else if (item.isComplete()) {
                    if (item.production.lhs.equals(grammar.getAugmentedStart()) && item.lookahead.equals("$")) {
                        table.setAction(i, "$", new ParsingTable.Action(ParsingTable.ActionType.ACCEPT, 0));
                    } else {
                        int prodIdx = itemsHandler.productionIndex(item.production);
                        table.setAction(i, item.lookahead, new ParsingTable.Action(ParsingTable.ActionType.REDUCE, prodIdx));
                    }
                }

                // 3. GOTO Actions [cite: 178]
                for (String nt : nonTerminals) {
                    Set<Items.LR1Item> nextStateItems = itemsHandler.goto1(stateItems, nt);
                    if (!nextStateItems.isEmpty()) {
                        int nextState = itemsHandler.indexOf1(canonicalCollection, nextStateItems);
                        table.setGoto(i, nt, nextState);
                    }
                }
            }
        }
        return table;
    }

    public List<Set<Items.LR1Item>> getCanonicalCollection() { return canonicalCollection; }
}