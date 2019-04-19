//package lflist;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Driver version 4.0, Lock-Free version 3.0
 * @authors Marcus Sooter, Michael Harris
 */

public class TestDriver {
    public static void main(String[] args) {
        
        // configurable options for benchmarking and granularity
    	boolean RUN_BENCHMARKS = true;
        boolean VERBOSE_BENCHMARKS = true;
        
        TestDriver st = new TestDriver();
        if (st.runUnitTesting()) {
            System.out.print("all unit tests pass");
            
            if (RUN_BENCHMARKS) {
                System.out.println(", running benchmarks now");
                st.benchmark(VERBOSE_BENCHMARKS);
            }
            else
                System.out.println();
        }
    }
    
    
    // method testing
    LFlist s;
    
    // Thread handling data
    LFlist threadPointer;
    
    // List of threads, accessed from primary thread
    private ArrayList<LFlist> threadList;
    AtomicReference<Node> head;
    boolean sub;
    int i;
    
    /*
    methods
    */
    
    public boolean threadTester(boolean VERBOSE, int numThreads, int inputRange,
                                            int lookupPercentage, int numInstr) {
        int instrPerThread = numInstr/numThreads;
        if (inputRange < 0) {
                System.out.println("Invalid Input Range");
                return false;
        }
        else if (lookupPercentage > 100 || lookupPercentage < 0) {
                System.out.println("Invalid Lookup Percentage");
                return false;
        }
        else if (numInstr < 0) {
                System.out.println("Invalid Run Time (ms)");
                return false;
        }

        threadList = new ArrayList<LFlist>();

        head = new AtomicReference<Node>();
        head.set(new Node());

        // Fill up the new list for testing
        // (insert w/ random numbers until list size is achieved)
        int a[] = {0};
        int b[] = {0};
        Random random;

        LFlist p = new LFlist(head, lookupPercentage, instrPerThread, a, b, new AtomicInteger());

        int i = 0;
        /*
        random = new Random();
        while (i < 500) {
                p.insert(random.nextInt() % inputRange + 1);
                i++;
        }
        */

        for (i = 1; i < inputRange/2; i++) {
                p.insert(i);
        }

        int j;
        // Initialize threads

        int ops[];
        int keys[];
        AtomicInteger t;
        for (i = 0; i < numThreads; i++) {
                ops = new int[instrPerThread];
                keys = new int[instrPerThread];
                t = new AtomicInteger();
                // Generate random data for each thread
                random = new Random();
                for (j = 0; j < instrPerThread; j++) {
                        ops[j] = random.nextInt(101);
                        keys[j] = random.nextInt(inputRange);
                }



                //

                threadPointer = new LFlist(head, lookupPercentage, instrPerThread, ops, keys, t);

                // Kills threads if main thread dies
                threadPointer.setDaemon(true);

                threadList.add(threadPointer);
            }

            long start = System.currentTimeMillis();
            // Starting threads
            for (i = 0; i < threadList.size(); i++) {
                    threadList.get(i).start();
            }


            for (i = 0; i < threadList.size(); i++) {
                    // get number of completed operations from each thread?
                    try {
                            threadList.get(i).join();
                    } catch (InterruptedException ex) {
                            ex.printStackTrace();
                    }
            }

            long end = System.currentTimeMillis();

            System.out.println((end - start) + "ms");
///thread info for benchmarks		
            if (VERBOSE) System.out.println("Times per thread:");
            int sum = 0;
            for (i = 0; i < threadList.size(); i++)
            {
                    if (VERBOSE) System.out.println("t" + (i + 1) + ": " + threadList.get(i).t.get());
                    sum += threadList.get(i).t.get();
            }
            int avg = sum/threadList.size();
            if (VERBOSE) System.out.println("avg time: " + (avg));
            System.out.println("ops/ms: " + (numInstr/avg));
            return true;
    }
	
    // unit testing for list functionality 
    public boolean runUnitTesting() {
        s = new LFlist();
        
        // keep track of if we have seen a bug so far
        boolean nobugs = true;
        
        // this ensures we are always testing new values, and not repeated values
        int testingValue = 1;
        
        s.enlist(new Node(212121, 0, null, null, Thread.currentThread().getId()));
        if (!s.contains(212121)) {
            nobugs = false;
            System.out.println("contains failed unit test 1 - contains doesn't detect enlisted node");
        }

        
        s.enlist(new Node(646464, 0, null, null, Thread.currentThread().getId()));
        if (!s.contains(646464)) {
            nobugs = false;
            System.out.println("contains failed unit test 2 - contains doesn't detect enlisted node");
        }

        
        if (s.contains(testingValue + 12345)){
            nobugs = false;
            System.out.println("contains failed unit test 3 - contains detects false value");
        }

        
        s.remove(646464);
        if (s.contains(646464)) {
            nobugs = false;
            System.out.println("remove failed unit test 4 - remove failure");
        }

        
        s.remove(212121);
        if (s.contains(212121)) {
            nobugs = false;
            System.out.println("remove failed unit test 5 - remove failure");
        }

        
        
        s.enlist(new Node(testingValue, 0, null, null, Thread.currentThread().getId()));
        s.insert(testingValue * 90000 + 15000);
        if (!s.contains(testingValue * 90000 + 15000)) {
            nobugs = false;
            System.out.println("insert failed unit test 6 - insert() into a non-empty stack");
        }
        s.remove(testingValue * 90000 + 15000);
        s.remove(testingValue++);

        
        s.insert(testingValue);
        if (!s.contains(testingValue++)) {
            nobugs = false;
            System.out.println("insert failed unit test 7 - insert() into an empty stack");
        }

//        put new tests here
//        s.insert(testingValue);
//        if (!s.contains(testingValue++)) {
//            nobugs = false;
//            System.out.println("insert failed unit test 8 - description here");
//        }
//        s.insert(testingValue);
//        if (!s.contains(testingValue++)) {
//            nobugs = false;
//            System.out.println("insert failed unit test 8 description here");
//        }
        
        
        //System.out.println(testingValue);
        
        return nobugs;
    }
    
    
    //for the parameters -> (a, b, c, 5mil) | a = 1, 2, 4, 8; b = 512, 1000, 2000; c = 0, 34, 100
    //(adding, threads for 16, 32, 64 threads)
    public void benchmark(boolean b) {
        int count=1, x, y, z, numOps=5000000;
        
        int[] threads = new int[]{1,2,4,8,16,32,64};
        int[] keysize = new int[]{512,1000,2000};
        int[] lookup = new int[]{0,34,100};
        
        int tlist_size = threads.length;
        int klist_size = keysize.length;
        int llist_size = lookup.length;
        
        for (z = 0; z < llist_size; z++) {
            for (y = 0; y < klist_size; y++) {
                for (x = 0; x < tlist_size; x++) {
                    System.out.println("\nInitiating test " + count++ + ": threads: " + threads[x]
                    + ", keysize: " + keysize[y] + ", lookup ratio: " + lookup[z] + "%, with " + numOps + 
                            " operations.");
                    this.threadTester(b, threads[x], keysize[y], lookup[z], numOps);
                }
            }
        }
    }
    
    
}
