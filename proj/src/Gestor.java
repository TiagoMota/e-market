import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class Gestor extends Agent{

	private static final long serialVersionUID = 1L;

	HashMap<AID, Reputation> reputations = new HashMap<AID, Reputation>();
	private AID[] sellerAgents;

	protected void setup(){

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("manager");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new TickerBehaviour(this, 8000) {//envia a sua identificação aos agentes da rede de 10 em 10 segundos

			private static final long serialVersionUID = 1L;

			protected void onTick() {
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("seller");
				template.addServices(sd);

				DFAgentDescription[] result;
				try {

					result = DFService.search(myAgent, template);
					sellerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						sellerAgents[i] = result[i].getName();
					}

				} catch (FIPAException e) {
					e.printStackTrace();
				}

				ACLMessage msg = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					msg.addReceiver(sellerAgents[i]);
				}
				msg.setContent("none");
				myAgent.send(msg);
			}
		});

		addBehaviour(new TickerBehaviour(this, 3000) {

			private static final long serialVersionUID = 1L;

			protected void onTick() {
				
				if(!reputations.isEmpty()){
					System.out.println("****************************************\n****************************************\n");
					for (Map.Entry<AID, Reputation> entry : reputations.entrySet()) {
					    AID key = entry.getKey();
					    Reputation value = entry.getValue();
					    System.out.println(key.getName() + " está cotado com a reputacao: " + value.reputation);
					}
					System.out.println("\n\n");
				}
				else{
					block();
				}
			}
		});
		addBehaviour(new EvaluateAgentsOnNetwork());//behaviour para avaliar os agentes presentes na rede
		addBehaviour(new RequestSellerRep());//behaviour para retornar a reputaçao de determinado agente na rede

	}

	private class EvaluateAgentsOnNetwork extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				AID asker = msg.getSender();
				String opt = msg.getContent();
				if(reputations.containsKey(asker)){
					reputations.get(asker).calcRep(opt);
				}
				else{
					Reputation r = new Reputation(asker, opt);
					r.calcRep(opt);
					reputations.put(msg.getSender(), r);
				}
			}
			else{
				block();
			}
		}

	}

	private class RequestSellerRep extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		public void action() {
			ACLMessage msg = myAgent.receive();
			if(msg != null){
				AID requested;
				if(msg.getPerformative() == ACLMessage.REQUEST_WHEN){
					try {
						requested = (AID) msg.getContentObject();
						ACLMessage reply;
						if(reputations.containsKey(requested)){
							reply = new ACLMessage(ACLMessage.QUERY_REF);
							reply.setContentObject(reputations.get(requested));
						}
						else{
							reply = new ACLMessage(ACLMessage.QUERY_REF);
							Reputation rp = new Reputation(requested, "2");
							reputations.put(requested, rp);
							reply.setContentObject(rp);
							/*
							reply = new ACLMessage(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("Agente comprador alvo: " + requested.getName() + " ainda não avaliado pelo gestor!");
							Reputation rp = new Reputation(requested, "2");
							reputations.put(requested, rp);
							*/
						}
						reply.addReceiver(msg.getSender());
						myAgent.send(reply);
					} catch (UnreadableException | IOException e1) {
						e1.printStackTrace();
					}
				}
				else if(msg.getPerformative() == ACLMessage.REQUEST){
					try {
						requested = (AID) msg.getContentObject();
						ACLMessage reply;
						if(reputations.containsKey(requested)){
							reply = new ACLMessage(ACLMessage.QUERY_IF);
							reply.setContentObject(reputations.get(requested));
							
						}
						else{
							reply = new ACLMessage(ACLMessage.QUERY_IF);
							Reputation rp = new Reputation(requested, "2");
							reputations.put(requested, rp);
							reply.setContentObject(rp);
							/*
							reply = new ACLMessage(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("Agente vendedor alvo: " + requested.getName() + " ainda não avaliado pelo gestor!");
							Reputation rp = new Reputation(requested, "2");
							reputations.put(requested, rp);
							*/
						}
						reply.addReceiver(msg.getSender());
						myAgent.send(reply);
					} catch (UnreadableException | IOException e1) {
						e1.printStackTrace();
					}
				}
				else
					block();
			}
			else{
				block();
			}
		}
	}

	protected void takeDown(){
		System.out.println("Agente gestor a terminar!");
		try { DFService.deregister(this); }
		catch (Exception e) {}
	}


}
