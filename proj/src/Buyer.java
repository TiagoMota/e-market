import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class Buyer extends Agent{
	private static final long serialVersionUID = 1L;

	String item;
	int max_delivery_days;
	int min_delivery_days;
	int wanted_quantity;
	int min_quantity;
	int max_price_unit;
	int desired_price_unit;
	int utility;
	double minRatio;
	int nRounds;




	protected AID manager;
	private ArrayList<AID> auxAgents = new ArrayList<AID>();// agentes que já negociou
	private ArrayList<AID> sellerAgents = new ArrayList<AID>();
	Hashtable<AID, Proposal> proposals = new Hashtable<AID, Proposal>();

	public void evalSellerProp(Proposal p){
		if(p.quantity >= wanted_quantity){
			p.quantity_eval = "bom";
		}
		else if(p.quantity > min_quantity){
			p.quantity_eval = "suficiente";
		}
		else
			p.quantity_eval = "mau";

		if(p.delivery_days <= min_delivery_days){
			p.delivery_days_eval = "bom";
		}
		else if(p.delivery_days > min_delivery_days && p.delivery_days < max_delivery_days){
			p.delivery_days_eval = "suficiente";
		}
		else{
			p.delivery_days_eval = "mau";
		}

		if(p.proposed_value <= desired_price_unit*p.quantity){
			p.proposed_value_eval = "bom";
		}
		else if(p.proposed_value > desired_price_unit*p.quantity && p.proposed_value < max_price_unit*p.quantity){
			p.proposed_value_eval = "suficiente";
			return;
		}
		else{
			p.proposed_value_eval = "mau";
		}
	}

	protected void setup(){
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			this.item = (String) args[0];
			this.max_delivery_days = (Integer) args[1];
			this.min_delivery_days = (Integer) args[2];
			this.wanted_quantity = (Integer) args[3];
			this.min_quantity = (Integer) args[4];
			this.max_price_unit = (Integer) args[5];
			this.desired_price_unit = (Integer) args[6];
			this.utility = (Integer) args[7];
			this.minRatio = (Double) args[8];
			this.nRounds = (Integer) args[9];
			System.out.println("Agente comprador "+getAID().getName()+ " criado!");
		}

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("buyer");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new GetManager());
		addBehaviour(new GetReputationFromManager());
		addBehaviour(new TickerBehaviour(this, 10000) { //parte da necessidade

			private static final long serialVersionUID = 1L;

			protected void onTick() {

				// Actualiza a lista seller agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("seller");
				template.addServices(sd);
				DFAgentDescription[] result;

				try {


					result = DFService.search(myAgent, template);
					for (int i = 0; i < result.length; ++i) {
						sellerAgents.add(result[i].getName());
					}
				} catch (FIPAException e) {
					e.printStackTrace();
				}

				//System.out.println("\n\nA procura de agentes vendedores do item: "+ item);

				ACLMessage toMan = new ACLMessage(ACLMessage.REQUEST);
				toMan.addReceiver(manager);
				//System.out.println("Agentes vendedores encontados:");
				for (int i=0; i < sellerAgents.size(); ++i){
					try {
						toMan.setContentObject(sellerAgents.get(i));
						//System.out.println(sellerAgents.get(i).getName());
						myAgent.send(toMan);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				//System.out.println("\n");

				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				for (int i = 0; i < sellerAgents.size(); ++i) {
					if(!auxAgents.contains(sellerAgents.get(i)))
						msg.addReceiver(sellerAgents.get(i));
				}
				//"proposta" apenas com item e quantidade
				Proposal np = new Proposal(item, wanted_quantity, 0, 0);
				try{
					msg.setContentObject(np);
				} catch (IOException e){
					e.printStackTrace();
				}

				//msg.setConversationId("need");
				myAgent.send(msg);
			
		}
	});

		addBehaviour(new EvaluateProposals());// parte de receber e avaliar propostas do vendedor, se forem acima da utilidade aceita
		addBehaviour(new TradeConfirm());//parte de receber a confirmação do negocio e terminar
}

private class GetReputationFromManager extends CyclicBehaviour{

	private static final long serialVersionUID = 1L;

	public void action() {
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
		ACLMessage msg = myAgent.receive(mt);
		if(msg != null){
			try {
				Reputation r = (Reputation) msg.getContentObject();
				if(r.reputation < minRatio){
					System.out.println("Comprador: " + getAID().getName() + " ignorou vendedor: "+ r.agent.getName() + " por reputacao baixa: " + r.reputation);
					sellerAgents.remove(r.agent);
				}
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
		else{
			block();
		}
	}

}

private class EvaluateProposals extends CyclicBehaviour{

	private static final long serialVersionUID = 1L;

	public void action() {
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
		ACLMessage msg = myAgent.receive(mt);
		
		if(msg != null){			
			Proposal p;
			
			ACLMessage reply;
			try {
				p = (Proposal) msg.getContentObject();
				
				
				
				p.agent = msg.getSender();
				evalSellerProp(p);
				float util = p.globalEval();
				proposals.put(p.agent, p); 
				
				System.out.println("\n\n-----------------> PROPOSTA RECEBIDA <------------------");
				System.out.println("agente: " + getAID().getName());
				System.out.println("item: " + p.item);
				System.out.println("quandidade: " + p.quantity);
				System.out.println("dias de entrega: " + p.delivery_days);
				System.out.println("preço: " + p.proposed_value);
				System.out.println("utilidade: " + util);
				System.out.println("---------------------------------------------------------");
				
				if(util >= utility){

					reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);

				}
				else if(nRounds  == p.currRound){// ao passarem o numero de rondas o comprador vai perder 5% do seu ratio.
					reply = new ACLMessage(ACLMessage.CANCEL);
					ACLMessage bad = new ACLMessage(ACLMessage.INFORM_REF);
					bad.addReceiver(manager);
					bad.setContent("2");
					myAgent.send(bad);

				}
				else{
					reply = new ACLMessage(ACLMessage.PROPOSE);
					p.currRound+=1;
				}
				reply.setContentObject(p);
				reply.addReceiver(p.agent);
				if(!auxAgents.contains(msg.getSender())){
					auxAgents.add(msg.getSender());
				}
				myAgent.send(reply);
				Thread.sleep(1000);
			} catch (UnreadableException | IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}

private class TradeConfirm extends CyclicBehaviour{

	private static final long serialVersionUID = 1L;

	public void action() {
		ACLMessage msg = myAgent.receive();
		if(msg != null){
			if(msg.getPerformative() == ACLMessage.CONFIRM){
				String finalMessage = msg.getContent();
				System.out.println(finalMessage);
				myAgent.doDelete();
			}
			else if(msg.getPerformative() == ACLMessage.FAILURE){
				String reason = msg.getContent();
				System.out.println(reason);
			}
		}
		else
			block();

	}

}

private class GetManager extends SimpleBehaviour{

	private static final long serialVersionUID = 1L;

	public void action() {
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
		ACLMessage msg = myAgent.receive(mt);
		if(msg!=null){
			manager = msg.getSender();
			System.out.println(getAID().getName() +" identificou agente gestor: " + manager.getName());
		}
		else{
			block();
		}
	}

	public boolean done() {
		return (manager != null);
	}

}


protected void takeDown() {
	System.out.println("Agente comprador " + getAID().getName() + " a terminar.");
	try { DFService.deregister(this); }
	catch (Exception e) {}
}

public boolean done() {
	return true;
}
}
