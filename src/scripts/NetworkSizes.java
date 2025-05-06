package scripts;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import instance.Instance_TT;
import pesp.EAN;

public class NetworkSizes {

	public static void main(String[] args) throws IOException {
		String[] instances = {"toy_2","grid","regional","metroFixed","Erding_NDP_S020","Schweiz_Fernverkehr","Stuttgart"};
		
		PrintWriter pw = new PrintWriter("results/networkSizes.txt");
		pw.println("Instance,Nodes,Arcs,Nodes,Arcs,Nodes,Arcs");
		for(String inst: instances) {
			String name = inst+"-EPESP-1.0";
			Instance_TT inst_tt = Instance_TT.readMPPESPInstance(name);
			
			Instance_TT full = inst_tt.getPESPfromMPPESP();
			
			EAN ean = new EAN(full);
			int eventsFull = ean.getNumberOfNodes();
			int activitiesFull = ean.getNumberOfArcs();
			
			EAN red = new EAN(inst_tt);
			int eventsRed = red.getNumberOfNodes();
			int activitiesRed = red.getNumberOfArcs();
			red.determineSubgraphPerPeriod();
			if(!red.isRooted()) {
				red.makeRooted();
			}
			int eventsNice = red.getNumberOfNodes();
			int activitiesNice = red.getNumberOfArcs();
			pw.println(inst+","+eventsFull+","+activitiesFull+","+eventsRed+","+activitiesRed+","+eventsNice+","+activitiesNice);
		}
		pw.close();
	}

}
