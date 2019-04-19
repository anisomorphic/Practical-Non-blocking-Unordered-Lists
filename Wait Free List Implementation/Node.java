import java.util.concurrent.atomic.AtomicReference;

/**
 * 3.0 - AtomicFieldUpdaters, using int to represent state, no open issues. Lock-free enlist
 * @authors Marcus Sooter, Michael Harris
 */

//enum state {INS, REM, DAT, INV};
//             0    1    2    3

public class Node {
    
    int key;
    
    int state;
    
    Node next;
    AtomicReference<Node> prev;
    long tid;
	
    Node(int k, int s, Node n, AtomicReference<Node> p, long t) {
        key = k;
        this.state = s;
        next = n;
        prev = p;
        tid = t;
    }
	
    Node() {
        key = 0;
        this.state = 0; // INS
        next = null;
        prev = null;
        tid = -1;
    }
    
}