import jade.core.AID;
import jade.util.leap.Serializable;


public class Reputation implements Serializable{

	private static final long serialVersionUID = 1L;

	AID agent;
	int succSells;
	int failSells;
	double reputation;

	public Reputation(){

	}

	public Reputation(AID a,String opt){
		this.agent = a;
		if(opt.equals("1")){
			this.succSells = 1;
			this.failSells = 0;
		}
		else if(opt.equals("2")){
			this.reputation = 1;
		}
		else{
			this.succSells = 0;
			this.failSells = 1;
		}
	}

	public void calcRep(String opt) {
		if(opt.equals("1")){
			this.succSells += 1;
		}
		else if(opt.endsWith("2")){
			this.reputation *= 0.95;
			return;
		}
		else{
			this.failSells += 1;
		}
		this.reputation = succSells/(succSells+failSells);
	}

}
