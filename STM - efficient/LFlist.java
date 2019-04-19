package STMeff;

import org.deuce.*;
import org.deuce.transaction.TransactionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
//import java.util.concurrent.atomic.AtomicReferenceFieldUpdater; <- deuce does not seem to like these

/**
 * STM-efficient.1 - forked from 2.0 - directly CASing fields by having state be atomic
 * @authors Marcus Sooter, Michael Harris (STM)
 * Credit to DeuceSTM and Guy Korland
 */

//enum state {INS, REM, DAT, INV};
//             0    1    2    3

public class LFlist extends Thread {
    AtomicReference<Node> head;
    
    
    
 // Testing data
    int lookupPercentage;
    int numInstr;
    int[] ops;
    int[] keys;
    
    AtomicInteger t;
    LFlist()
    { 
    	head = new AtomicReference<>();
    	// This line is critical to preventing NPEs, 
    	// and ensures non-zero items must be stored in our stack.
    	// contains(0) will always return true.
    	head.set(new Node());
    }

    LFlist(AtomicReference<Node> head, int lookupPercentage, int numInstr, 
            int[] ops, int[] keys, AtomicInteger t) {
	
        this.head = head;
        this.lookupPercentage = lookupPercentage;
        this.numInstr = numInstr;
        this.ops = ops;
        this.keys = keys;
        this.t = t;
    }
    
    
    
    public void run() {
    	long s = System.currentTimeMillis(), e;
    	int op, key;
    	
    	// Pull from random data to decide whether to insert/remove/lookup
    	// Pull from random data for keys to insert/remove/lookup
    	int i;
    	for (i = 0; i < ops.length; i++) {
    		
            if (ops[i] < lookupPercentage)
                    op = 2;
            else
                    op = ops[i]%2;

            // Insert
            if (op == 0) {
                    enlist(new Node(keys[i], 0, null, null, Thread.currentThread().getId()));
//    			insert(keys[i]);
            }
            // Remove
            else if (op == 1) {
                    remove(keys[i]);
            }
            // Contains
            else {
                    contains(keys[i]);
            }
    	}
    	t.set((int) (System.currentTimeMillis() - s));
    }

    
    /*
    methods
    */
    
    
    // lock-free enlist
    public void enlist(Node h) {
            Node old = new Node();

            enlistDoAtomic(h, old);
        }
    
    @Atomic
    public void enlistDoAtomic(Node h, Node old) throws TransactionException {
        old = head.get();
        h.next = old;

        try {
            if (head.get() == old) {
                head.set(h);
            }
            else
                throw new TransactionException();
            }
        catch (TransactionException t) {
            old = head.get();
            h.next = old;
        }
    }

    
    
    
    
    public boolean remove(int k) {
        boolean b = false;

        Node h = new Node(k, 1, null, null, Thread.currentThread().getId());

        enlist(h);

        

        return removeDoAtomic(h, k);
    }
    
    @Atomic
    public boolean removeDoAtomic(Node h, int k) throws TransactionException {
        boolean b = false;
        try {
            b = helpRemove(h, k);
            h.state.set(3); // INV
        }
        catch (TransactionException t){
            ;
        }
        return b;
    }

    
    
    
    
    // helpRemove
    public boolean helpRemove(Node h, int k) {
        Node pred = h;
        AtomicReference<Node> curr = new AtomicReference<Node>(pred.next);
        
        Node tempCurr, succ;
        int s;
        while ((tempCurr = curr.get()) != null) {
        	s = tempCurr.state.get();
            if (s == 3) {
                succ = curr.get().next;
                pred.next = succ;
                curr.set(succ);
            }
            else if (tempCurr.key != k) {
                pred = curr.get();
                curr.set(curr.get().next);
            }
            else if (s == 1) {
                return false;
            }
            else if (s == 0) {
            	
                if (helpRemoveDoAtomic(tempCurr)) {
                    return true;
                }
            }
            else if (s == 2) {
                Node newCurr = curr.get();
                newCurr.state.set(3);
                
                return true;
            }
        }
        return false;
        
    }    
    
    @Atomic
    public boolean helpRemoveDoAtomic(Node tempCurr) throws TransactionException {
        boolean b = false;
        try {
            if (tempCurr.state.get() == 0) {
                tempCurr.state.set(1);
                b = true;
            }
//            else {
//                throw new TransactionException(); //this block isnt really necessary because there's no loop on the CAS.
                  //if it failed, it means that something else succeeded, a retry will only continue to fail.
//            }
        }
        catch (TransactionException t){
            ;
        }
        return b;
    }
    
    
    
    
    
    // insert
    public boolean insert(int k) {
        boolean b;
        
        Node h = new Node(k, 0, null, null, Thread.currentThread().getId());
        
        enlist(h);
        
        b = helpInsert(h, k);
        
        
//        if (!(h.state.compareAndSet(0, (b ? 2 : 3)))) {
//            helpRemove(h, k);
//            h.state.set(3);
//        } //replacing this method with this one below:
        if (!(insertDoAtomic(h, b))) {
            helpRemove(h, k);
            h.state.set(3);
        }
        
        return b;
    }
    
    @Atomic
    public boolean insertDoAtomic(Node h, boolean b) throws TransactionException {
        boolean retval = false;
        
        try {
            if (h.state.get() == 0) {
                h.state.set(b?2:3);
                retval = true;
            }
        }
        catch (TransactionException t){
            ;//dont need to roll back or prepare for a roll back
        }
        return retval;
    }
    
    
    
    // helpInsert
    public boolean helpInsert(Node h, int k) {
        Node pred = h;
        AtomicReference<Node> curr = new AtomicReference<>(pred.next);
        
        Node tempCurr, succ;
        int s;
        
        while ((tempCurr = curr.get()) != null) {
        	s = tempCurr.state.get();
            // INV
            if (s == 3) {
                succ = curr.get().next;
                pred.next = succ;
                curr.set(succ);
            }
            // key doesn't match
            else if (tempCurr.key != k) {
                pred = tempCurr;//curr.get();
                curr.set(tempCurr.next);//curr.get().next);
            }
            // REM
            else if (s == 1) {
                return true;
            }
            // INS || DAT
            else if (s == 0 || s == 2) {
                return false;
            }
        }
        return true;
    }
    // @Atomic isn't really viable here, everything happens inside the loop atomically already
    
    
    
    
    
    // contains
    public boolean contains(int k) {
        Node curr = head.get();
        int s;
        
        while (curr != null) {
            
            if (curr.key == k) {
                if (containsDoAtomic(curr)) {
                    return true;
                }
            }
            curr = curr.next;
        }
        return false;
    }
    
    @Atomic
    public boolean containsDoAtomic(Node curr) throws TransactionException {
        boolean b = false;
        try {
            if (curr.state.get() != 3 && curr.state.get() != 1)
                b = true;
        }
        catch (TransactionException t){
            ;
        }
        return b;
    }
}