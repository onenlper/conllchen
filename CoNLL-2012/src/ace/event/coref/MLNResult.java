package ace.event.coref;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class MLNResult {

	public static class Node implements Comparable{
		String idx;
		HashSet<Node> linkTo;
		int mark = 0;

		int clusterId = -1;
		
		Node antecedent;

		public Node(String idx) {
			this.idx = idx;
			linkTo = new HashSet<Node>();
		}

		public void link(Node node) {
			this.linkTo.add(node);
			node.linkTo.add(this);
		}
		
		Node ant;

		@Override
		public int compareTo(Object node2) {
			return Integer.parseInt(this.idx) - Integer.parseInt(((Node) node2).idx);
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

		for (String file : files) {
			HashMap<String, String> mentionMap = Common.readFile2Map2(file + ".mention");

			ArrayList<String> eventLines = Common.getLines(file + ".eventLines");
			ArrayList<String> entityLines = Common.getLines(file + ".entityLines");

			HashSet<String> mentionLines = new HashSet<String>();
			mentionLines.addAll(eventLines);
			mentionLines.addAll(entityLines);
//			System.err.println(mentionLines.iterator().next() + "#");
			HashMap<String, String> reverseMap = new HashMap<String, String>();
			for (String key : mentionMap.keySet()) {
				reverseMap.put(mentionMap.get(key), key);
			}
			ArrayList<String> outputs = new ArrayList<String>();

			if ((new File(file + ".mln")).exists()) {
				ArrayList<String> mlnResult = Common.getLines(file + ".mln");
				HashMap<String, Node> nodeMap = new HashMap<String, Node>();
				for (int i = 0; i < mlnResult.size(); i++) {
					String line = mlnResult.get(i);
					if (line.equals(">coref") || line.equals(">nCoref")) {
						i++;
						while (i < mlnResult.size() && !mlnResult.get(i).startsWith(">")) {
							String tokens[] = mlnResult.get(i).split("\\s+");

							Node node1 = nodeMap.get(tokens[0]);
							if (node1 == null) {
								node1 = new Node(tokens[0]);
								nodeMap.put(tokens[0], node1);
							}
							Node node2 = nodeMap.get(tokens[1]);
							if (node2 == null) {
								node2 = new Node(tokens[1]);
								nodeMap.put(tokens[1], node2);
							}
							
							// nearest best
							if(node2.ant!=null) {
								int ant = Integer.parseInt(node2.ant.idx);
								int n1 = Integer.parseInt(node1.idx);
								if(n1>ant) {
									node2.ant = node1;
								}
							} else {
								node2.ant = node1;
							}
							node1.link(node2);
							i++;
						}
					}
				}
//				ArrayList<ArrayList<Node>> clusters = positiveAll(nodeMap);
				ArrayList<ArrayList<Node>> clusters = nearestAnt(nodeMap);
				
				for (ArrayList<Node> temp : clusters) {
					StringBuilder sb = new StringBuilder();
					for (Node node : temp) {
						String m = reverseMap.get(node.idx).replace("#", ",");
						if(mentionLines.contains(m)) {
							sb.append(m).append(" ");
						}
						reverseMap.remove(node.idx);
					}
					if(!sb.toString().isEmpty()) {
						outputs.add(sb.toString());
					}
				}
			}
			// add singletons
			for(String key : reverseMap.keySet()) {
				StringBuilder sb = new StringBuilder();
				sb.append(reverseMap.get(key).replace("#", ","));
				if(mentionLines.contains(sb.toString())) {
					outputs.add(sb.toString());
				}
			}
			Common.outputLines(outputs, file + ".entities.mln");
		}
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<ArrayList<Node>> nearestAnt(HashMap<String, Node> nodeMap) {
		ArrayList<ArrayList<Node>> clusters = new ArrayList<ArrayList<Node>>();
		ArrayList<Node> allNodes = new ArrayList<Node>();
		allNodes.addAll(nodeMap.values());
		Collections.sort(allNodes);
		for(int i=0;i<allNodes.size();i++) {
			Node n = allNodes.get(i);
			Node ant = n.ant;
			if(ant==null) {
				ArrayList<Node> cluster = new ArrayList<Node>();
				n.clusterId = clusters.size();
				cluster.add(n);
				clusters.add(cluster);
			} else {
				int clusterId = ant.clusterId;
				n.clusterId = clusterId;
				clusters.get(clusterId).add(n);
			}
		}
		return clusters;
	}

	private static ArrayList<ArrayList<Node>> positiveAll(HashMap<String, Node> nodeMap) {
		ArrayList<ArrayList<Node>> clusters = new ArrayList<ArrayList<Node>>();
		ArrayList<Node> cluster = new ArrayList<Node>();
		for (Node node : nodeMap.values()) {
			if (node.mark == 0) {
				visitNode(node, cluster);
				clusters.add(cluster);
				cluster = new ArrayList<Node>();
			}
		}
		return clusters;
	}
	


	// Load the mln
	

	

	// Load a test corpus 
	

	/* Just loading makes the corpus available for
	   the processors within thebeast but not for random
	   access through the shell. This is achieved using ... */


	
	
	
}
