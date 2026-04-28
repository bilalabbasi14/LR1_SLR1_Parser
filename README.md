
# Bottom-Up Parser: SLR(1) and LR(1) Parsing

---

## Team Members

**Name:** 
Ahmad Bilal , Muhammad Ali 


---

## Programming Language

**Java** (JDK 17 or higher recommended; tested with JDK 21)

---

## Project Structure

```
LR1_SLR1_Parser/
│
├── src/
│   ├── Main.java           # Entry point; runs both parsers and parsing traces
│   ├── Grammar.java        # CFG reading, augmentation, FIRST/FOLLOW sets
│   ├── Items.java          # LR(0) and LR(1) items, CLOSURE, GOTO, canonical collections
│   ├── SLRParser.java      # SLR(1) parsing table construction
│   ├── LR1Parser.java      # LR(1) parsing table construction
│   ├── ParsingTable.java   # ACTION/GOTO table; conflict detection
│   ├── Stack.java          # LR parser stack (symbol+state pairs)
│   └── Tree.java           # Parse tree construction and display
│
├── input/
│   ├── grammar1.txt              # Simple expression grammar
│   ├── grammar2.txt              # Full arithmetic grammar (with precedence)
│   ├── grammar3.txt              # Dangling-else grammar
│   ├── grammar_with_conflict.txt # Classic LR(1)-but-not-SLR(1) grammar
│   ├── input_valid.txt           # Valid input strings for testing
│   └── input_invalid.txt         # Invalid input strings for testing
│
├── output/
│   ├── augmented_grammar.txt
│   ├── slr_items.txt
│   ├── slr_parsing_table.txt
│   ├── slr_trace.txt
│   ├── lr1_items.txt
│   ├── lr1_parsing_table.txt
│   ├── lr1_trace.txt
│   ├── comparison.txt
│   └── parse_trees.txt
│
├── docs/
│   └── report.pdf
│   └── report.docs
|         
└── README.md
```

---

## Input File Format

### Grammar File

One production rule per line. Alternatives separated by `|`.

```
NonTerminal -> production1 | production2 | ...
```

**Rules:**
- Non-terminals: multi-character names starting with an uppercase letter (e.g., `Expr`, `Term`, `Factor`)
- Single-character non-terminals (`E`, `T`, `F`) are **NOT** allowed
- Terminals: lowercase letters, operators, keywords (e.g., `id`, `+`, `*`, `(`, `)`)
- Epsilon: use `epsilon` or `@`
- Arrow symbol: `->` (with spaces around it is fine)

**Example (`grammar2.txt`):**
```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

### Input String Files

One input string per line. Tokens separated by spaces.

```
id + id * id
( id + id ) * id
id + * id
```

---

## Compilation Instructions

### Method 1 — Command Line (javac)

```bash
# Navigate to the src directory
cd src/

# Compile all Java files
javac *.java
```

### Method 2 — IDE (IntelliJ IDEA / Eclipse)

1. Open the project root in your IDE.
2. Mark `src/` as the Sources Root.
3. Build the project (Ctrl+F9 in IntelliJ).

---

## Execution Instructions

### Basic Run (uses grammar.txt in the working directory)

```bash
# From inside src/ after compiling:
java Main
```

The default `Main.java` reads `grammar.txt` from the current directory and parses the hard-coded test string `"id + id * id"`. To parse different inputs or grammars, modify the `main()` method or extend it to accept command-line arguments.

### Run SLR(1) Parser on a specific grammar

1. Copy your grammar file to `src/grammar.txt` (or change the filename in `Main.java`).
2. Compile and run as above.
3. The SLR(1) table and parsing trace will be printed to stdout.

### Run LR(1) Parser on a specific grammar

Same as above — both parsers run in a single execution. The output sections are clearly labelled:
```
=== SLR(1) Parsing Table ===
...
=== LR(1) Parsing Table ===
...
```

### Example Commands

```bash
# Grammar 2 (full arithmetic)
cp ../input/grammar2.txt grammar.txt
java Main

# Conflict grammar
cp ../input/grammar_with_conflict.txt grammar.txt
java Main

# Redirect output to file
java Main > ../output/slr_trace.txt
```

---

## Sample Grammar Files

### grammar1.txt – Simple Expressions
```
Expr -> Expr + Term | Term
Term -> Factor
Factor -> id
```

### grammar2.txt – Full Arithmetic
```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

### grammar_with_conflict.txt – SLR(1) Conflict Demo
```
Start -> L = R | R
L -> * R | id
R -> L
```
> This grammar is **LR(1) but NOT SLR(1)**. Running it demonstrates the shift/reduce conflict that SLR(1) cannot resolve but LR(1) handles correctly.

### grammar3.txt – Dangling Else
```
Stmt -> if Expr then Stmt | if Expr then Stmt else Stmt | other
Expr -> id
```

---

## What the Program Outputs

For each grammar, the program prints:

1. **Augmented Grammar** – with the new start symbol (`<StartSymbol>Prime`)
2. **FIRST and FOLLOW Sets** – for all non-terminals
3. **SLR(1) Parsing Table** – with shift, reduce, accept, and any conflicts
4. **LR(1) Parsing Table** – with lookahead-driven reduce actions
5. **Parsing Trace** – step-by-step stack/input/action table
6. **Parse Tree** – Unicode box-drawing tree for accepted strings
7. **Conflict Report** – lists all shift/reduce and reduce/reduce conflicts

---

## Known Limitations

- **Single-character non-terminals** are not supported (use `Expr` instead of `E`).
- The parser reads the input string from a hard-coded variable in `Main.java`; there is no interactive prompt or file-based input string reading in the current version.
- **Ambiguous grammars** (e.g., dangling-else) will produce conflicts in both parsers; the implementation resolves conflicts by preferring the first-entered action (shift-preference by construction order).
- No error recovery: parsing halts on the first unrecognised token.
- Very large grammars may be slow due to O(n) state-equality checks in the canonical collection builder.

---

