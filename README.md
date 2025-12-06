# Lox Interpreter

A complete implementation of the Lox programming language interpreter in Kotlin, following the principles from "Crafting Interpreters".

## Features

- **Lexical Analysis**: Full tokenization with keyword and identifier distinction
- **Parsing**: Recursive descent parser building an abstract syntax tree
- **Variables**: Dynamic typing with `var` declarations and lexical scoping
- **Functions**: First-class functions with named (`fun`) and anonymous (`fn`) syntax, closures, and proper tail behavior
- **Classes**: Object-oriented programming with inheritance, instance fields, and methods
- **Static Methods**: Class-level methods accessible directly on the class
- **Native Functions**: Built-in functions for I/O and timing (clock, println, scan, input)
- **Control Flow**: if/else statements and while loops
- **Operators**: All standard arithmetic, comparison, logical, and assignment operators
- **Scope Resolution**: Static analysis pass that pre-computes variable binding distances for efficient runtime lookup
- **Error Handling**: Proper runtime error reporting with token information

## Language Syntax Basics

Variables and assignment:
```
var x = 10;
x = 20;
```

Functions:
```
fun greet(name) {
  println("Hello, " + name);
}

greet("Alice");
```

Anonymous functions:
```
var add = fn(a, b) { a + b };
println(add(3, 4));
```

Classes and inheritance:
```
class Animal {
  init(name) {
    this.name = name;
  }
  
  speak() {
    println(this.name + " makes a sound");
  }
}

class Dog > Animal {
  speak() {
    println(this.name + " barks");
  }
}

var dog = Dog("Buddy");
dog.speak();
```

## Building and Running

Compile the Kotlin code and run the interpreter:

```bash
kotlinc -include-runtime -d lox.jar src/*.kt
java -jar lox.jar script.lox
```

Or use the REPL for interactive mode.

## Implementation Highlights

- **Visitor Pattern**: AST traversal for both parsing and interpretation
- **Environment Chains**: Linked scopes for variable storage and lookup
- **Resolver**: Two-pass compilation approach (resolve then execute) for scope analysis
- **LoxCallable**: Interface for both user-defined and native functions
- **Exception-based Control Flow**: RuntimeReturn for return statements, RuntimeError for errors

## Project Structure

```
src/main/kotlin/com/aymanetech/
├── Lox.kt                      # Main entry point and REPL
│
├── lexer/                      # Lexical analysis
│   ├── Scanner.kt
│   ├── Token.kt
│   └── TokenType.kt
│
├── parser/                     # Syntax analysis
│   └── Parser.kt
│
├── ast/                        # Abstract Syntax Tree
│   ├── Expr.kt
│   └── Stmt.kt
│
├── resolver/                   # Semantic analysis
│   └── Resolver.kt
│
├── interpreter/                # Runtime execution
│   ├── Interpreter.kt
│   ├── Environment.kt
│   └── NativeFunctions.kt
│
└── runtime/                    # Runtime types and errors
    ├── LoxCallable.kt
    ├── LoxClass.kt
    ├── LoxFunction.kt
    ├── LoxInstance.kt
    └── errors/
        ├── ErrorHandler.kt
        ├── DiagnosticReporter.kt
        ├── RuntimeError.kt
        └── RuntimeReturn.kt
```

## References

This implementation closely follows the Lox language design and interpreter architecture from "Crafting Interpreters" by Robert Nystrom, adapted to Kotlin's modern language features.
