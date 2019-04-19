//package lflist;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 3.0 - AtomicFieldUpdaters, using int to represent state, no open issues. Lock-free enlist
 * @authors Marcus Sooter, Michael Harris
 */

//enum state {INS, REM, DAT, INV};
//             0    1    2    3

public class LFlist extends Thread {
    AtomicReference<Node> head;
    
    public static final AtomicReferenceFieldUpdater<Node, Integer> stateUpdater =
    AtomicReferenceFieldUpdater.newUpdater(Node.class, Integer.class, "state");
    
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
    	long s = System.currentTimeMillis();
    	int op;
    	
    	// Pull from random data to decide whether to insert/remove/lookup
    	// Pull from random data for keys to insert/remove/lookup
    	int i;
    	for (i = 0; i < ops.length; i++) {

            if (ops[i] < lookupPercentage)
                op = 2;
            else
                op = ops[i]%2;

            // Insert
            if (op == 0)
                insert(keys[i]);
            // Remove
            else if (op == 1)
                remove(keys[i]);
            // Contains
            else
                contains(keys[i]);
    	}
    	t.set((int) (System.currentTimeMillis() - s));
    }

    
    // lock-free enlist
    public void enlist(Node h) {
        Node old = new Node();
        
        while(true) {
            old = head.get();
            h.next = old;
            
            if (head.compareAndSet(old, h)) {
                return;
            }
        }
    }
    
    
    // remove
    public boolean remove(int k) {
        boolean b = false;
        
        Node h = new Node(k, 1, null, null, Thread.currentThread().getId());
        
        enlist(h);
        
        b = helpRemove(h, k);
        h.state = 3; // INV
        
        return b;
    }
    
    // helpRemove
    public boolean helpRemove(Node h, int k) {
        Node pred = h;
        Node curr = pred.next;
        
        Node succ;
        while (curr != null) {
            if (curr.state == 3) {
                succ = curr.next;
                pred.next = succ;
                curr = succ;
            }
            else if (curr.key != k) {
                pred = curr;
                curr = curr.next;
            }
            else if (curr.state == 1) {
                return false;
            }
            else if (curr.state == 0) {
                if (stateUpdater.compareAndSet(curr, 0, 1)) { 
                    return true;
                }
            }
            else if (curr.state == 2) {
                curr.state = 3;
                
                return true;
            }
        }
        return false;
        
    }    
    
    
    // insert
    public boolean insert(int k) {
        boolean b;
        Node h = new Node(k, 0, null, null, Thread.currentThread().getId());
        
        enlist(h);
        b = helpInsert(h, k);
        
        if (!stateUpdater.compareAndSet(h, 0, (b ? 2 : 3))) {
            helpRemove(h, k);
            h.state = 3;
        }
        
        return b;
    }
    
    
    // helpInsert
    public boolean helpInsert(Node h, int k) {
    	Node pred = h;
        Node curr = pred.next;
        Node succ;
        
        while (curr != null) {
            // INV
            if (curr.state == 3) {
            	
                succ = curr.next;
                pred.next = succ;
                curr = succ;
            }
            // key doesn't match, skip over
            else if (curr.key != k) {
                pred = curr;
                curr = curr.next;
            }
            // REM
            else if (curr.state == 1) {
                return true;
            }
            // INS || DAT
            else if (curr.state == 0 || curr.state == 2) {
                return false;
            }
        }
        return true;
    }
    
    //contains
    public boolean contains(int k) {
        Node curr = head.get();
        
        while (curr != null) {
            if (curr.key == k)                
                if (curr.state != 3 && curr.state != 1)
                    return true;
            
            curr = curr.next;
        }
        return false;
    }
    
    
    
    /*
    debug methods
    */
    //ignore state, determine if it exists in the list in any state
    public boolean DEBUG_containsAtALL(int k) {
        Node curr = head.get();
        
        while (curr != null) {
            
            if (curr.key == k) {
                //s = curr.state;
                
                //if (s != 3 && s != 1) {
                    return true;
                //}
            }
            curr = curr.next;
        }
        return false;
    }
}
