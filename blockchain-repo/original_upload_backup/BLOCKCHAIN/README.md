# Java Blockchain Implementation (Assignment 3)

This repository contains a Java-based implementation of a blockchain node, as part of a programming assignment on cryptocurrency technologies. The core of the project is the `BlockChain.java` class, which manages a blockchain as a tree, handles forks, validates new blocks, and maintains a transaction pool.

This implementation successfully handles:
* **Block Tree Management**: The blockchain is stored as a tree of `BlockNode` objects, allowing for multiple competing forks.
* **UTXO State per Block**: Each block node maintains its own `UTXOPool` to represent the state of the ledger after its transactions are applied.
* **Transaction Validation**: Uses the `TxHandler` class (from a previous assignment) to validate all transactions within a new block against its parent's UTXO state.
* **Longest Chain Rule**: Correctly identifies the longest valid chain, using the "oldest block" as a tie-breaker.
* **Global Transaction Pool**: Manages a single `TransactionPool` for all pending transactions waiting to be mined.
* **Pruning**: Implements a `CUT_OFF_AGE` rule to prune old, abandoned forks and save memory.

## Core Files Implemented

### `BlockChain.java`
This is the main file for the assignment. It contains all the logic for adding blocks, managing the chain, and providing the necessary state (max height block, UTXO pool) for mining new blocks.

### `TxHandler.java`
This file is a required dependency from a previous assignment. It is responsible for validating individual transactions and sets of transactions based on 5 key rules:
1.  All claimed outputs are in the current UTXO pool.
2.  Signatures on each input are valid.
3.  No UTXO is claimed multiple times in a single transaction.
4.  All output values are non-negative.
5.  The sum of input values is greater than or equal to the sum of output values.

## Provided Files
This project also uses several provided files that form the basic data structures for the blockchain:
* `Block.java`
* `Transaction.java`
* `UTXO.java`
* `UTXOPool.java`
* `TransactionPool.java`
* `ByteArrayWrapper.java`
* `Crypto.java`
* `BlockHandler.java` (Uses `BlockChain` to create and process blocks)

## How to Use
This project is a library and is intended to be compiled as part of a larger Java application. You can compile the `.java` files using a standard Java compiler (e.g., `javac`).

```bash
# Compile all Java files
javac *.java
