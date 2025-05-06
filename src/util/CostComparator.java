package util;

import java.util.Comparator;

import digraph.DirectedGraphArc;


public class CostComparator<V,A> implements Comparator<DirectedGraphArc<V, A>>
{
	@Override
	public int compare(DirectedGraphArc<V, A> o1, DirectedGraphArc<V, A> o2)
	{
		// TODO Auto-generated method stub SHOULD BE CAREFUL HERE WITH NON-INTEGER DATA
		return Double.compare(o1.getCost(),o2.getCost());
//		if(o1.getCost()<o2.getCost()) {
//			return -1;
//		} else 
//			return 1;
	}
}
