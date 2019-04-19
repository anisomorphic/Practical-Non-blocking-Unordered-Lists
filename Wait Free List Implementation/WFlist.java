import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 3.0 - AtomicFieldUpdaters, using int to represent state, no open issues. Lock-free enlist
 * @authors Marcus Sooter, Michael Harris
 */

//enum state {INS, REM, DAT, INV};
//             0    1    2    3

public class WFlist extends Thread {
    AtomicReference<Node> head;
    
    Node dummy;
    AtomicInteger counter;
    //AtomicReference<ArrayList<Desc>> status;
    
    AtomicReference<Desc>[] status;
    int id, len;
    
    
    public static final AtomicReferenceFieldUpdater<Node, Integer> stateUpdater =
    AtomicReferenceFieldUpdater.newUpdater(Node.class, Integer.class, "state");
    
 // Testing data
    int lookupPercentage;
    int numInstr;
    int[] ops;
    int[] keys;
    
    AtomicInteger t;
    WFlist(AtomicReference<Desc>[] s,          AtomicInteger c,
    	   Node d, 		   					   int id,
    	   AtomicReference<Node> head) {
    	
    	this.status = s;
    	this.counter = c;
    	this.dummy = d;
    	this.id = id;
    	
    	len = status.length;
    	//head = new AtomicReference<>();
    	//head.set(new Node(-1, 1, null, null, -1));
        //dummy = new AtomicReference<>();
        //dummy.set(new Node());
        //counter.set(0);
        /*
        for (Desc d : status.get()) {
            d = new Desc(-1, false, null);
        }
        */
    }

    WFlist(AtomicReference<Node> head, int lookupPercentage, int numInstr, 
           int[] ops, 				   int[] keys,           AtomicInteger t,
           AtomicReference<Desc>[] s,				 AtomicInteger c,
    	   Node d,    				   int id) {
    	
    	this.status = s;
    	this.counter = c;
    	this.dummy = d;
    	this.id = id;
    	
    	len = status.length;
    	
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

    
    
    public void enlist(Node h) {
    	int phase = counter.getAndIncrement();
    	status[id].set(new Desc(phase, true, h));
    	
    	int i;
    	for (i = 0; i < len; i++) {
    		helpEnlist(i, phase);
    	}
    	helpFinish();
    }
    
    public boolean isPending(int tid, int phase) {
    	Desc d = status[tid].get();
    	return  d.pending && (d.phase < phase);
    }
    
    public void helpEnlist(int tid, int phase) {
    	AtomicReference<Node> curr;
    	Node pred;
    	
    	Node n;
    	while (isPending(tid, phase)) {
    		curr = head;
    		pred = curr.get().prev.get();
    		
    		if (curr.equals(head)) {
    			if (pred == null) {
    				if (isPending(tid, phase)) {
    					n = status[tid].get().node;
    					
    					if (curr.get().prev.compareAndSet(null, n)) {
    						helpFinish();
    						return;
    					}
    				}
    			}
    			else {
    				helpFinish();
    			}
    		}
    	}
    }
    
    public void helpFinish() {
    	int tid;
    	Desc d, dPrime;
    	Node curr = head.get();
    	Node pred = curr.prev.get();
    	
    	if (pred != null && pred != dummy) {
    		tid = (int)pred.tid;
    		d = status[tid].get();
    		if (curr == head.get() && pred == d.node) {
    			
    			dPrime = new Desc(d.phase, false, d.node);
    			status[tid].compareAndSet(d, dPrime);
    					
    			pred.next = curr;
    			head.compareAndSet(curr, pred);
    			curr.prev.set(dummy);
    		}
    	}
    }
    
    // remove
    public boolean remove(int k) {
        boolean b = false;
        
        Node h = new Node(k, 1, null, null, id);
        
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
        Node h = new Node(k, 0, null, null, id);
        
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
}