// descriptor class for wait-free version
public class Desc
{
    int phase;
    boolean pending;
    Node node;
    
    Desc(int p, boolean b, Node n) {
        phase = p;
        pending = b;
        node = n;
    }
}