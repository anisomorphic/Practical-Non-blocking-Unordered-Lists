package STMeff;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * STM-efficient.1 - forked from 2.0 - directly CASing fields by having state be atomic
 * @authors Marcus Sooter, Michael Harris (STM)
 * Credit to DeuceSTM and Guy Korland
 */

public class Node
{
   
	int key;
	AtomicInteger state;
	Node next;
	Node prev;
	long tid;
	
	Node(int k, int s, Node n, Node p, long t)
    {
            key = k;
            this.state = new AtomicInteger(s);
            next = n;
            prev = p;
            tid = t;
	}
	
	Node()
	{
            key = 0;
            this.state = new AtomicInteger(0); // INS
            next = null;
            prev = null;
            tid = -1;
	}
}