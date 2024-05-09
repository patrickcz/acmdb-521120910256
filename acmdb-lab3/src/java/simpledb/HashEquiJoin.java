package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class HashEquiJoin extends Operator {

    private static final long serialVersionUID = 1L;
    private final JoinPredicate predicate;
    private DbIterator child1;
    private DbIterator child2;
    private final TupleDesc mergeTd;
    transient private Tuple t1 = null;
    transient private Tuple t2 = null;
    
    final Map<Object, List<Tuple>> map = new HashMap<>();
    public final static int MAP_SIZE = 20000;


    /**
     * Constructor. Accepts to children to join and the predicate to join them
     * on
     * 
     * @param p
     *            The predicate to use to join the children
     * @param child1
     *            Iterator for the left(outer) relation to join
     * @param child2
     *            Iterator for the right(inner) relation to join
     */
    public HashEquiJoin(JoinPredicate p, DbIterator child1, DbIterator child2) {
        // some code goes here
        this.predicate = p;
        this.child1 = child1;
        this.child2 = child2;
        this.mergeTd = TupleDesc.merge(child1.getTupleDesc(), child2.getTupleDesc());

    }

    public JoinPredicate getJoinPredicate() {
        // some code goes here
        return this.predicate;
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return this.mergeTd;
    }
    
    public String getJoinField1Name()
    {
        // some code goes here
	return this.child1.getTupleDesc().getFieldName(this.predicate.getField1());
    }

    public String getJoinField2Name()
    {
        // some code goes here
        return this.child2.getTupleDesc().getFieldName(this.predicate.getField2());
    }
    
    // add new function
    private boolean loadMap() throws DbException, TransactionAbortedException {
        int cnt = 0;
        map.clear();
        while (child1.hasNext()) {
            t1 = child1.next();
            List<Tuple> list = map.computeIfAbsent(t1.getField(predicate.getField1()), k -> new ArrayList<>());
            list.add(t1);
            if (cnt++ == MAP_SIZE)
                return true;
        }
        return cnt > 0;

    }

    
    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        child1.open();
        child2.open();
        loadMap();
        super.open();

        
    }

    public void close() {
        // some code goes here
        super.close();
        child2.close();
        child1.close();
        this.t1=null;
        this.t2=null;
        this.listIt=null;
        this.map.clear();

    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        child1.rewind();
        child2.rewind();

    }

    transient Iterator<Tuple> listIt = null;
    
    private Tuple processList() {
        t1 = listIt.next();

        int td1n = t1.getTupleDesc().numFields();
        int td2n = t2.getTupleDesc().numFields();

        // set fields in combined tuple
        Tuple t = new Tuple(mergeTd);
        for (int i = 0; i < td1n; i++)
            t.setField(i, t1.getField(i));
        for (int i = 0; i < td2n; i++)
            t.setField(td1n + i, t2.getField(i));
        return t;

    }


    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, there will be two copies of the join attribute in
     * the results. (Removing such duplicate columns can be done with an
     * additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     * 
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if (listIt != null && listIt.hasNext()) {
            return processList();
        }

        // loop around child2
        while (child2.hasNext()) {
            t2 = child2.next();

            // if match, create a combined tuple and fill it with the values
            // from both tuples
            List<Tuple> l = map.get(t2.getField(predicate.getField2()));
            if (l == null)
                continue;
            listIt = l.iterator();

            return processList();

        }

        // child2 is done: advance child1
        child2.rewind();
        if (loadMap()) {
            return fetchNext();
        }

        return null;

    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        return new DbIterator[]{this.child1, this.child2};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
        this.child1 = children[0];
        this.child2 = children[1];

    }
    
}
