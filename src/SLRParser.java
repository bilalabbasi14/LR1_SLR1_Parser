import java.util.*;
import java.io.*;

public class SLRParser {
    private Grammar grammar;
    private Items itemsHandler;
    private ParsingTable table;
    private List<Set<Items.LR0Item>> canonicalCollection;

    public SLRParser(Grammar grammar) {
        this.grammar = grammar;
        this.itemsHandler = new Items(grammar);
    }

    public ParsingTable buildTable() {
        canonicalCollection = itemsHandler.buildCanonicalLR0();
        int numStates = canonicalCollection.size();

        List<String> terminals = new ArrayList<>(grammar.getTerminals());
        List<String> nonTerminals = new ArrayList<>(grammar.getNonTerminals());
        nonTerminals.remove(grammar.getAugmentedStart());

        table = new ParsingTable(numStates, terminals, nonTerminals, "SLR(1)");

        for (int i = 0; i < numStates; i++) {
            Set<Items.LR0Item> stateItems = canonicalCollection.get(i);

            for (Items.LR0Item item : stateItems) {
                String symbolAfterDot = item.symbolAfterDot();

                // 1. Shift Actions [cite: 77]
                if (symbolAfterDot != null && grammar.isTerminal(symbolAfterDot)) {
                    Set<Items.LR0Item> nextStateItems = itemsHandler.goto0(stateItems, symbolAfterDot);
                    int nextState = itemsHandler.indexOf0(canonicalCollection, nextStateItems);
                    table.setAction(i, symbolAfterDot, new ParsingTable.Action(ParsingTable.ActionType.SHIFT, nextState));
                }

                // 2. Reduce Actions (Using FOLLOW sets) [cite: 81, 84]
                else if (item.isComplete()) {
                    // Accept Action [cite: 86]
                    if (item.production.lhs.equals(grammar.getAugmentedStart())) {
                        table.setAction(i, "$", new ParsingTable.Action(ParsingTable.ActionType.ACCEPT, 0));
                    } else {
                        int prodIdx = itemsHandler.productionIndex(item.production);
                        Set<String> followA = grammar.getFollowSets().get(item.production.lhs);
                        for (String a : followA) {
                            table.setAction(i, a, new ParsingTable.Action(ParsingTable.ActionType.REDUCE, prodIdx));
                        }
                    }
                }

                // 3. GOTO Actions [cite: 90]
                for (String nt : nonTerminals) {
                    Set<Items.LR0Item> nextStateItems = itemsHandler.goto0(stateItems, nt);
                    if (!nextStateItems.isEmpty()) {
                        int nextState = itemsHandler.indexOf0(canonicalCollection, nextStateItems);
                        table.setGoto(i, nt, nextState);
                    }
                }
            }
        }
        return table;
    }

    public List<Set<Items.LR0Item>> getCanonicalCollection() { return canonicalCollection; }
}