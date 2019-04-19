import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TestDriver {
	// method testing
    WFlist s;
    
    // Thread handling data
    WFlist threadPointer;
    // List of threads, accessed from primary thread
    private ArrayList<WFlist> threadList;
    AtomicReference<Node> head;
    boolean sub;
    int i;
    
    public static void main(String[] args) {
    	TestDriver st = new TestDriver();
        
        if (st.runUnitTesting())
            System.out.println("all unit tests pass");
        else
            return;
        
        int count = 1;
        st.threadTester(4, 512, 100, 5000000);
    }
    
	public boolean threadTester(int numThreads,       int inputRange,
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
		
		threadList = new ArrayList<WFlist>();
		
		head = new AtomicReference<Node>();
		head.set(new Node());
		
		// Fill up the new list for testing
		// (insert w/ random numbers until list size is achieved)
		int a[] = {0};
		int b[] = {0};
		Random random;
		
		AtomicReference<Desc>[] s;
		AtomicInteger c;
		Node d;
		int id = 0;
		
		WFlist p = new WFlist(head, lookupPercentage, instrPerThread, a, b, new AtomicInteger(),
							  );
		
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
			
			threadPointer = new WFlist(head, lookupPercentage, instrPerThread, ops, keys, t);
			
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
		System.out.println("Times per thread:");
		int sum = 0;
		for (i = 0; i < threadList.size(); i++)
		{
			System.out.println("t" + (i + 1) + ": " + threadList.get(i).t.get());
			sum += threadList.get(i).t.get();
		}
		int avg = sum/threadList.size();
		System.out.println("avg time: " + (avg));
		System.out.println("ops/ms: " + (numInstr/avg));
		return true;
	}
	
	// Unit testing on a single thread
	public boolean runUnitTesting() {
		s = new WFlist();
        boolean nobugs = true;
        
        s.enlist(new Node(2, 0, null, null, Thread.currentThread().getId()));
        if (!s.contains(2)) {
            nobugs = false;
            System.out.println("contains failed unit test 1");
        }
        
        s.enlist(new Node(64, 0, null, null, Thread.currentThread().getId()));
        if (!s.contains(64)) {
            nobugs = false;
            System.out.println("contains failed unit test 2");
        }
        
        if (s.contains(34257)){
            nobugs = false;
            System.out.println("contains failed unit test 3");
        }
        
        s.remove(64);
        if (s.contains(64)) {
            nobugs = false;
            System.out.println("remove failed unit test 4");
        }
        
        s.remove(2);
        if (s.contains(2)) {
            nobugs = false;
            System.out.println("remove failed unit test 5");
        }
        
        s.enlist(new Node(90, 0, null, null, Thread.currentThread().getId()));
        s.insert(15);
        if (!s.contains(15)) {
            nobugs = false;
            System.out.println("insert failed unit test 6 - inserting into a non-empty stack");
        }
        s.remove(15);
        s.remove(90);
        
        s.insert(55);
        if (!s.contains(55)) {
            nobugs = false;
            System.out.println("insert failed unit test 7 - inserting into an empty stack");
        }
        
        return nobugs;
    }
}