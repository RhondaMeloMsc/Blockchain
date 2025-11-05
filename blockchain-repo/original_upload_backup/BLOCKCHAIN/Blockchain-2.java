import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * BlockChain class implementation for Assignment 3.
 * Manages a tree of blocks, a global transaction pool, and UTXO pools
 * for each block. It prunes old branches to satisfy memory constraints.
 */
public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    // A HashMap to store all block nodes, keyed by their hash.
    private HashMap<ByteArrayWrapper, BlockNode> blockStore;
    
    // The node corresponding to the block with the greatest height.
    private BlockNode maxHeightNode;
    
    // A single, global transaction pool for the whole chain.
    private TransactionPool globalTxPool;

    /**
     * Internal class to represent a node in the block tree.
     * Contains the block, its parent, its height, and the
     * UTXOPool after this block is applied.
     */
    private class BlockNode {
        public Block block;
        public BlockNode parent;
        public int height;
        public UTXOPool utxoPool;
        // Using System.nanoTime() for a simple "timestamp" to
        // resolve ties for the "oldest" block at the same height.
        public long timestamp;

        public BlockNode(Block block, BlockNode parent, int height, UTXOPool utxoPool) {
            this.block = block;
            this.parent = parent;
            this.height = height;
            this.utxoPool = utxoPool;
            this.timestamp = System.nanoTime();
        }
    }

    /**
     * create an empty block chain with just a genesis block. Assume
     * {@code genesisBlock} is a valid block
     */
    public BlockChain(Block genesisBlock) {
        this.blockStore = new HashMap<>();
        this.globalTxPool = new TransactionPool();

        // Create the UTXO pool for the genesis block
        UTXOPool genesisPool = new UTXOPool();
        
        // Add the genesis block's coinbase transaction to the pool
        Transaction coinbase = genesisBlock.getCoinbase();
        UTXO coinbaseUTXO = new UTXO(coinbase.getHash(), 0);
        genesisPool.addUTXO(coinbaseUTXO, coinbase.getOutput(0));

        // Create and store the genesis node
        BlockNode genesisNode = new BlockNode(genesisBlock, null, 1, genesisPool);
        this.blockStore.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
        this.maxHeightNode = genesisNode;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return this.maxHeightNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // Return a copy to prevent modification of the node's internal pool
        return new UTXOPool(this.maxHeightNode.utxoPool);
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // Return a copy so the caller can process transactions without
        // affecting the global pool until a block is added.
        return new TransactionPool(this.globalTxPool);
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all
     * transactions should be valid and block should be at
     * {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        byte[] prevHash = block.getPrevBlockHash();
        
        // 1. Check if it's a genesis block (which is not allowed)
        if (prevHash == null) {
            return false;
        }

        // 2. Check if the parent block exists in our store
        BlockNode parentNode = this.blockStore.get(new ByteArrayWrapper(prevHash));
        if (parentNode == null) {
            return false; // Parent is unknown or has been pruned
        }

        int newHeight = parentNode.height + 1;
        int currentMaxHeight = this.maxHeightNode.height;

        // 3. Check the CUT_OFF_AGE rule
        if (newHeight <= currentMaxHeight - CUT_OFF_AGE) {
            return false;
        }

        // 4. Validate all transactions in the block
        // Use a TxHandler initialized with the *parent's* UTXO pool.
        TxHandler handler = new TxHandler(new UTXOPool(parentNode.utxoPool));
        ArrayList<Transaction> txs = block.getTransactions();
        Transaction[] txsArray = txs.toArray(new Transaction[0]);
        
        Transaction[] validTxs = handler.handleTxs(txsArray);

        // If the block contains any transaction that is not valid
        // (i.e., handleTxs did not return all of them), reject the block.
        if (validTxs.length != txsArray.length) {
            return false;
        }

        // 5. If valid, create the new UTXO pool for this block
        UTXOPool newUTXOPool = handler.getUTXOPool();
        
        // Add this block's coinbase transaction to the new pool
        Transaction coinbase = block.getCoinbase();
        UTXO coinbaseUTXO = new UTXO(coinbase.getHash(), 0);
        newUTXOPool.addUTXO(coinbaseUTXO, coinbase.getOutput(0));

        // 6. Create and store the new block node
        BlockNode newNode = new BlockNode(block, parentNode, newHeight, newUTXOPool);
        this.blockStore.put(new ByteArrayWrapper(block.getHash()), newNode);

        // 7. Update max height if this block is a new chain leader
        boolean maxHeightUpdated = false;
        if (newHeight > currentMaxHeight) {
            this.maxHeightNode = newNode;
            maxHeightUpdated = true;
        } else if (newHeight == currentMaxHeight) {
            // Hint: "If there are multiple blocks at the same height,
            // return the oldest block".
            // We only replace the maxHeightNode if the new one is *not*
            // older (i.e., its timestamp is larger). This means the
            // existing one is older.
            if (newNode.timestamp < this.maxHeightNode.timestamp) {
                this.maxHeightNode = newNode;
                maxHeightUpdated = true; // A new (older) main branch
            }
        }


        // 8. Remove included transactions from the global pool
        // (Coinbase is not in the pool, so no need to remove it)
        for (Transaction tx : txs) {
            this.globalTxPool.removeTransaction(tx.getHash());
        }

        // 9. Prune old branches if max height was updated
        if (maxHeightUpdated) {
            pruneBlockChain();
        }

        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        this.globalTxPool.addTransaction(tx);
    }

    /**
     * Private helper method to prune the block tree.
     * This iterates through all nodes and keeps only those that are
     * ancestors of a "recent" block (height >= maxHeight - CUT_OFF_AGE).
     * This satisfies the memory constraint.
     */
    private void pruneBlockChain() {
        int currentMaxHeight = this.maxHeightNode.height;
        int cutoffHeight = currentMaxHeight - CUT_OFF_AGE;
        
        // Don't prune if we are not past the cutoff age
        if (cutoffHeight <= 1) {
            return;
        }

        HashMap<ByteArrayWrapper, BlockNode> newBlockStore = new HashMap<>();
        HashSet<BlockNode> nodesToKeep = new HashSet<>();

        // Find all "active" nodes (those recent enough)
        for (BlockNode node : this.blockStore.values()) {
            if (node.height >= cutoffHeight) {
                // This node is recent. We must keep it and all its parents.
                BlockNode curr = node;
                while (curr != null && !nodesToKeep.contains(curr)) {
                    nodesToKeep.add(curr);
                    curr = curr.parent;
                }
            }
        }

        // Rebuild the block store with only the nodes we need to keep
        for (BlockNode node : nodesToKeep) {
            newBlockStore.put(new ByteArrayWrapper(node.block.getHash()), node);
        }

        this.blockStore = newBlockStore;
    }
}