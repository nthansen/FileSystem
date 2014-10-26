// Norman Hansen, Tenzin Choephel, Konstantin Stekhov
// Final Assignment
// CSS430B
// SuperBlock class

public class SuperBlock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; // disk blocks
    public int inodeBlocks; // inodes
    public int freeList;    // free list's head

    // constructor
    public SuperBlock(int diskSize) {
        // reading the superblock and initiating
        // all of the variables
        byte[] block = new byte[Disk.blockSize];
        SysLib.rawread(0, block);
        totalBlocks = SysLib.bytes2int(block, 0);
        inodeBlocks = SysLib.bytes2int(block, 4);
        freeList = SysLib.bytes2int(block, 8);

        // if disk data is valid then return
        if (totalBlocks == diskSize && inodeBlocks > 0 && freeList >= 2) {
            return;
        }
        // if it is not valid then format the disk
        totalBlocks = diskSize;
        SysLib.cerr("default format( 64 )\n");
        format();
    }

    // This function writes back totalBlocks, inodeBlocks, and freeList
    // back to the disk
    public void sync() {
        byte[] block = new byte[Disk.blockSize];
        SysLib.int2bytes(totalBlocks, block, 0);
        SysLib.int2bytes(inodeBlocks, block, 4);
        SysLib.int2bytes(freeList, block, 8);
        SysLib.rawwrite(0, block);
        SysLib.cerr("Superblock synchronized\n");
    }

    public void format() {
        format(defaultInodeBlocks);
    }

    // This function formats the superblock and inodes based on
    // the number of inodes
    public void format(int files) {

        inodeBlocks = files;

        // initiating the unused inodes to the disk
        for(short j = 0; j < inodeBlocks; j++) {
            Inode inode = new Inode();
            inode.flag = 0;
            inode.toDisk(j);
        }

        // setting the free list based of the number of
        // inodes and the disk block size
        freeList = 2 + (inodeBlocks * 32) / Disk.blockSize;

        // making free blocks and writing them to the disk
        for(int i = freeList; i < totalBlocks; i++) {
            byte[] data = new byte[Disk.blockSize];

            for(int j = 0; j < Disk.blockSize; j++) {
                data[j] = 0;
            }

            SysLib.int2bytes(i + 1, data, 0);
            SysLib.rawwrite(i, data);
        }

        // writing back totalBlocks,totalInodes,
        // and freeList to the disk
        sync();
    }


    // This function dequeues the top block from
    // the free list
    public int getFreeBlock() {
        int cur = freeList; // current free block

        // if there is a free block
        if (cur != -1) {
            // create a new block
            byte[] block = new byte[Disk.blockSize];
            SysLib.rawread(cur, block); // get the data for the block
            freeList = SysLib.bytes2int(block, 0);
            SysLib.int2bytes(0, block, 0); // block from the freelist
            SysLib.rawwrite(cur, block); // write back to the disk
        }
        return cur;
    }

    // This function enqueues a block with the input block number
    // to the end of the list
    public boolean returnBlock(int blockNum) {
        // if valid block number
        if (blockNum >= 0) {

            byte[] data = new byte[Disk.blockSize];

            for (int i = 0; i < Disk.blockSize; i++) {
                data[i] = 0;
            }
            // add the new block to the end of the freelist
            SysLib.int2bytes(freeList, data, 0);
            // write the contents to the new block
            SysLib.rawwrite(blockNum, data);
            freeList = blockNum; // setting freelist to this block
            return true;
        }
        return false;
    }
}