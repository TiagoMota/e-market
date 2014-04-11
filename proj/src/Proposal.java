import jade.core.AID;
import jade.util.leap.Serializable;


public class Proposal implements Serializable {

	private static final long serialVersionUID = 1L;

	String item;
	int quantity;
	int delivery_days;
	float proposed_value;
	AID agent;
	String quantity_eval;
	String delivery_days_eval;
	String proposed_value_eval;
	Integer currRound;
	
	float final_eval;
	
	public float getFinalEval() {	
		return final_eval;
	}
	
	public float globalEval(){
		int q = globalEvalAux(this.quantity_eval);
		int dd = globalEvalAux(this.delivery_days_eval);
		int pv = globalEvalAux(this.proposed_value_eval);
		
		return (this.final_eval = (q + dd + pv) * 100)/9 ;
	}

	private int globalEvalAux(String eval) {
		if(eval.equals("bom"))
			return 3;
		else if(eval.equals("suficiente"))
			return 2;
		else
			return 1;
	}
	
	public Proposal(){
		currRound = 0;
	}

	public Proposal(String item, int q, int dd, float pv){
		currRound = 0;
		this.item=item;
		this.quantity=q;
		this.delivery_days=dd;
		this.proposed_value = pv;
	}
	
	
	
}
