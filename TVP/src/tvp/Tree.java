package tvp;

import java.util.List;

public class Tree {
	int[] treeArr = {0,1,2,3,4,5,6};
	//int[] response = {3,4,5,6};
	List<Integer> response = null;
	public static void main(String[] args) {
		Tree t = new Tree();
		TreeNode rootNode = t.buildTree(1, null);
		System.out.println(" Test: "+t.testTree(rootNode));
	}
	
	public boolean testTree(TreeNode node){
		boolean myVal = false;
		for (int i = 0; i < response.size(); i++) {
			if(response.get(i) == node.value){
				myVal = true;
			}
		}
		if(node.lChild !=null && node.rChild != null){
			return ((myVal && testTree(node.lChild)) || (myVal && testTree(node.rChild)) || (testTree(node.lChild) && testTree(node.rChild)));
		}else if(node.lChild !=null){
			return ((myVal && testTree(node.lChild)) || (myVal && true) || (testTree(node.lChild) && true));
		}else if(node.rChild != null){
			return ((myVal && true) || (myVal && testTree(node.rChild)) || (true && testTree(node.rChild)));
		}
		return myVal;
	}
	
	public TreeNode buildTree(int i, TreeNode parent){
		TreeNode node = null;
		try {
			node = new TreeNode();
			node.value = treeArr[i-1];
			node.parent = parent;
			node.lChild = buildTree(2*i, node);
			node.rChild = buildTree((2*i)+1, node);
			/*System.out.println("     "+node.value+"     ");
			if(node.lChild != null){
				System.out.println("   "+node.lChild.value+"    "+node.rChild.value+"   ");
			}*/
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
		return node;
	}
}
