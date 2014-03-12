package ace.event.mln;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class Inspector {

	public static class Tripe {
		public Tripe(Node node1, Node node2, String role) {
			this.node1 = node1;
			this.node2 = node2;
			this.role = role;
		}
		Node node1;
		Node node2;
		String role;
	}

	public static class Node {
		String idx;
		HashSet<Node> linkTo;
		int mark = 0;
		
		int clusterID;

		boolean event = true;

		Node antecedent;

		public Node(String idx) {
			this.idx = idx;
			linkTo = new HashSet<Node>();
		}

		public void link(Node node) {
			this.linkTo.add(node);
			node.linkTo.add(this);
		}
	}

	public static void visitNode(Node node, ArrayList<Node> cluster) {
		cluster.add(node);
		node.mark = 1;
		// qualify mention
		for (Node tmp : node.linkTo) {
			if (tmp.mark == 0) {
				visitNode(tmp, cluster);
			}
		}
		node.mark = 2;
	}

	public static void main(String args[]) {

		ArrayList<String> files = Common.getLines("/users/yzcchen/chen3/conll12/chinese/ACE_test/all.txt");

		for (int i=0;i<files.size();i++) {
			String file = files.get(i);
			System.err.println(file);
			if (!(new File(file + ".mln")).exists()) {
				continue;
			}
			ArrayList<String> mlnResult = Common.getLines(file + ".mln");
			HashMap<String, Node> nodeMap = readInMentions(mlnResult);
			ArrayList<Tripe> tripes = readInPerArgs(mlnResult, nodeMap);
			
			ArrayList<ArrayList<Node>> clusters = new ArrayList<ArrayList<Node>>();
			ArrayList<Node> cluster = new ArrayList<Node>();
			for (Node node : nodeMap.values()) {
				if (node.mark == 0) {
					visitNode(node, cluster);
					clusters.add(cluster);
					// set cluster ID
					for(Node temp : cluster) {
						temp.clusterID = clusters.size();
					}
					cluster = new ArrayList<Node>();
				}
			}

			for(Tripe t1 : tripes) {
				Node m1 = t1.node1;
				Node n1 = t1.node2;
				String role1 = t1.role; 
				for(Tripe t2 : tripes) {
					Node m2 = t2.node1;
					Node n2 = t2.node2;
					String role2 = t2.role;
					if(Integer.parseInt(m1.idx)<Integer.parseInt(m2.idx) && role1.equals(role2)) {
						
						boolean left = !n1.linkTo.contains(n2);
						boolean right = !m1.linkTo.contains(m2);
						
						if(left && !right) {
							System.err.println("GEE");
						}
						
//						System.err.println(left + "=>" + right);
					}
					
				}
			}
			
			// factor: for Int m1, Int n1, Int m2, Int n2, Role role
			// if singlePerArg_(m1, n1, role) & singlePerArg_(m2, n2, role) &
			// commonBV_(m1, m2) & m1<m2 :
			// !nCoref(n1, n2) & !nCoref(n2, n1) => !coref(m1, m2);

		}

	}

	private static ArrayList<Tripe> readInPerArgs(ArrayList<String> mlnResult, HashMap<String, Node> nodeMap) {
		ArrayList<Tripe> tripes = new ArrayList<Tripe>();
		for (int i = 0; i < mlnResult.size(); i++) {
			String line = mlnResult.get(i);
			if (line.equals(">singlePerArg_")) {
				i++;
				while (i < mlnResult.size() && !mlnResult.get(i).startsWith(">")) {
					String tokens[] = mlnResult.get(i).split("\\s+");
					Node node1 = nodeMap.get(tokens[0]);
					if (node1 == null) {
						node1 = new Node(tokens[0]);
						node1.event = true;
						nodeMap.put(tokens[0], node1);
					}
					Node node2 = nodeMap.get(tokens[1]);
					if (node2 == null) {
						node2 = new Node(tokens[1]);
						node2.event = false;
						nodeMap.put(tokens[1], node2);
					}
					String role = tokens[2];
					Tripe tripe = new Tripe(node1, node2, role);
					tripes.add(tripe);
					i++;
				}
			}
		}
		return tripes;
	}

	private static HashMap<String, Node> readInMentions(ArrayList<String> mlnResult) {
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();
		for (int i = 0; i < mlnResult.size(); i++) {
			String line = mlnResult.get(i);
			boolean event = false;
			if (line.equals(">coref") || line.equals(">nCoref")) {
				if (line.equals(">nCoref")) {
					event = false;
				} else {
					event = true;
				}
				i++;
				while (i < mlnResult.size() && !mlnResult.get(i).startsWith(">")) {
					String tokens[] = mlnResult.get(i).split("\\s+");
					Node node1 = nodeMap.get(tokens[0]);
					if (node1 == null) {
						node1 = new Node(tokens[0]);
						node1.event = event;
						nodeMap.put(tokens[0], node1);
					}
					Node node2 = nodeMap.get(tokens[1]);
					if (node2 == null) {
						node2 = new Node(tokens[1]);
						node2.event = event;
						nodeMap.put(tokens[1], node2);
					}
					node1.link(node2);
					i++;
				}
			}
		}
		return nodeMap;
	}
}
