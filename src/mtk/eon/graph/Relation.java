package mtk.eon.graph;

import java.util.List;

import mtk.eon.utils.collections.InsertionSortList;


public class Relation<N, L, P extends Path<N>> {
	
	public final N nodeA;
	public final N nodeB;
	L link;
	List<Path<N>> paths = new InsertionSortList<Path<N>>();
	
	Relation(N nodeA, N nodeB) {
		if (nodeA.equals(nodeB)) throw new GraphException("A node cannot have a relation with itself!");
		this.nodeA = nodeA.hashCode() < nodeB.hashCode() ? nodeA : nodeB;
		this.nodeB = nodeB.hashCode() > nodeB.hashCode() ? nodeA : nodeB;
	}
	
	public boolean hasLink() {
		return link != null;
	}
	
	public L getLink() {
		return link;
	}
	
	@SuppressWarnings("unchecked")
	public List<P> getPaths() {
		return (List<P>) paths;
	}
	
	@Override
	public int hashCode() {
		return nodeB.hashCode() * (nodeB.hashCode() - 1) / 2 + nodeA.hashCode();
	}
	
	public static int hash(int idA, int idB) {
		long min = Math.min(idA, idB);
		long max = Math.max(idA, idB);
		return (int) (max * (max - 1) / 2 + min);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return hashCode() == obj.hashCode();
	}
	
	public void print() { // TODO remove
		System.out.println(nodeA + ":=:" + nodeB + " - " + (hasLink() ? "link " : "nolink ") + "{");
//		for (Path path : paths)
//			System.out.println("    " + path);
		System.out.println("}");
	}
}