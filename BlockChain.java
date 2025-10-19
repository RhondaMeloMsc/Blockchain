import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * BlockChain class implementation for Assignment 3.
 * * Manages a tree of blocks, a global transaction pool, and UTXO pools
 * for each block. It prunes old branches to satisfy memory constraints.
 *
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

        public BlockNode(Block block, BlockNode parent, int height, UTXOPool utxoPool) {
            this.block = block;
            this.parent = parent;
            this.height = height;
            this.utxoPool = utxoPool;
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
        
        // 1. Check if it's a