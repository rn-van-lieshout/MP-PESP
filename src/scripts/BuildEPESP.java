package scripts;

import java.io.IOException;

import instance.Instance_TT;

public class BuildEPESP {

	public static void main(String[] args) throws IOException {
		String[] instances = {"Schweiz_Fernverkehr","toy_2","grid","Erding_NDP_S020","Erding_NDP_S021","metroFixed","regional","Stuttgart"};
		
		for(int i = 7; i<=7; i++) {
			String name = instances[i];
			Instance_TT baseInst = Instance_TT.readInstance(name);
			//baseInst.completeActivities();
			Instance_TT reduced = baseInst.getMPPESPfromPESP();
			reduced.computeRouting(false);
			reduced.writeEPESPinstance();
		}
		

		
	}

}
