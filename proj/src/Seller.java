import java.io.IOException;
import java.util.ArrayList;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class Seller extends Agent{

	private static final long serialVersionUID = 1L;
	String item;
	int max_delivery_days;
	int min_delivery_days;
	int stock;
	int selling_price;
	int min_sell_price;
	double ratio;

	protected boolean managerFlag;
	protected AID manager;
	private ArrayList<AID> blackList = new ArrayList<AID>();
	private int reportFlag = 0;
	private int firstFlag = 0;
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
			this.stock = (Integer) args[3];
			this.selling_price = (Integer) args[4];
			this.min_sell_price = (Integer) args[5];
			this.ratio = (Double) args[6];
			System.out.println("Agente vendedor "+getAID().getName()+ " criado!");
		}

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("seller");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new GetManager());//identificar quem é o gestor
		addBehaviour(new GetReputationFromManager());//adiciona um agente comprador a lista negra d
		addBehaviour(new ReportToManager());
		addBehaviour(new FirstProposal());// receber a necessidade e responder com proposta inicial
		addBehaviour(new GenerateCounterProps());// recebe avaliações e gera contra propostas
		addBehaviour(new GetCancelFromBuyer());// recebe cancelamento do comprador por numero de rondas de negociação excedidas
		addBehaviour(new ConfirmDeal());// envia confirmação de negocio.

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

	private class GetReputationFromManager extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		public void action() {
			ACLMessage msg = myAgent.receive();
			if(msg != null){
				if(msg.getPerformative() == ACLMessage.QUERY_REF){
					try {
						Reputation r = (Reputation) msg.getContentObject();
						if(r.reputation < ratio){
							System.out.println("Vendedor "+getAID().getName() + " ignorou: " + r.agent.getName() + ", reputacao baixa: " + r.reputation);
							blackList.add(r.agent);
						}
						else{
							//System.out.println("Vendedor "+getAID().getName() + " aceitou a reputacao de "+ r.reputation+ " do agente comprador " + r.agent.getName());
						}
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
					firstFlag = 1;
				}
				else if(msg.getPerformative() == ACLMessage.NOT_UNDERSTOOD){
					String err = msg.getContent();
					System.out.println(err);
				}
				else
					block();
			}
			else{
				block();
			}
		}
	}

	private class FirstProposal extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);

			if(msg != null){
				ACLMessage rep = new ACLMessage(ACLMessage.REQUEST_WHEN);
				rep.addReceiver(manager);
				try {
					rep.setContentObject(msg.getSender());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				myAgent.send(rep);

				if(!blackList.contains(msg.getSender()) && firstFlag == 1){
					try{
						Proposal buyer_need = (Proposal) msg.getContentObject();
						buyer_need.agent = msg.getSender();
						ACLMessage reply;
						if(item.equals(buyer_need.item)){
							//cria proposta inicial com o que é melhor para o vendedor
							Proposal np;
							if(stock < buyer_need.quantity){
								np = new Proposal(item, stock, max_delivery_days, selling_price*stock);
							}
							else{
								np = new Proposal(item, buyer_need.quantity, max_delivery_days, selling_price*buyer_need.quantity);
							}
							reply = new ACLMessage(ACLMessage.PROPOSE);
							reply.setContentObject(np);
						}
						else{
							//não vende o item pedido
							reply = new ACLMessage(ACLMessage.REFUSE);
							reply.setContent("Não vendo " + buyer_need.item +"!");
						}
						//reply.setConversationId("need");
						reply.addReceiver(buyer_need.agent);
						myAgent.send(reply);
						firstFlag = 0;
					} catch(UnreadableException | IOException e){
						e.printStackTrace();
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

	private class GenerateCounterProps extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){

				ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
				reply.addReceiver(msg.getSender());
				//reply.setConversationId("trading-market");

				try {
					Proposal p = (Proposal) msg.getContentObject();
					p = generateBetterProp(p);
					reply.setContentObject(p);
					System.out.println("\n\n-----------------> PROPOSTA ENVIADA <------------------");
					System.out.println("agente: " + getAID().getName());
					System.out.println("item: " + p.item);
					System.out.println("quandidade: " + p.quantity);
					System.out.println("dias de entrega: " + p.delivery_days);
					System.out.println("preço: " + p.proposed_value);
					System.out.println("---------------------------------------------------------");
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

	private class GetCancelFromBuyer extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				System.out.println(msg.getSender().getName() + " cancelou a negociacao. Numero de tentativas excedido!" );
			}
			else
				block();
		}

	}

	private class ConfirmDeal extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;


		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				ACLMessage conf = null;
				try {
					Proposal p = (Proposal) msg.getContentObject();
					p.agent = msg.getSender();
					if(p.quantity <= stock){
						conf = new ACLMessage(ACLMessage.CONFIRM);
						conf.setContent("Vendedor - " + getAID().getName() + " Produto vendido a: " + msg.getSender().getName() +"\n");
						stock -= p.quantity;
						managerFlag = true;
						System.out.println("Stock actual do Vendedor " + getAID().getName() + ": " + stock );
		
					}
					else{
						conf = new ACLMessage(ACLMessage.FAILURE);
						conf.setContent("Falha: produto vendido a outro agente entretanto :(!");
						managerFlag = false;
					}

				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				reportFlag = 1;
				conf.addReceiver(msg.getSender());
				//conf.setConversationId("trading-market");
				myAgent.send(conf);
				if(stock == 0){
					try {
						Thread.sleep(3000);
						myAgent.doDelete();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				}
			}
			else
				block();

		}

	}



	private class ReportToManager extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		public void action() {
			if(reportFlag == 1){
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM_REF);
				msg.addReceiver(manager);
				if(managerFlag){
					msg.setContent("1");
				}
				else{
					msg.setContent("0");
				}
				myAgent.send(msg);
				reportFlag = 0;
			}

		}
	}


	protected void takeDown() {
		System.out.println("Agente Vendedor " + getAID().getName() + " a terminar.");
		try { DFService.deregister(this); }
		catch (Exception e) {}
	}

	public Proposal generateBetterProp(Proposal p) {
		Proposal newProp = new Proposal();
		newProp = p;

		if(p.delivery_days_eval != "bom"){
			if(p.delivery_days > min_delivery_days){
				newProp.delivery_days = (int) Math.floor((p.delivery_days - min_delivery_days)*0.5);
			}
		}
		else{
			newProp.delivery_days = p.delivery_days;
		}

		if(stock < p.quantity){
			newProp.quantity = stock;
		}
		else{
			newProp.quantity = p.quantity;
		}

		if(p.proposed_value_eval != "bom"){
			float badpu = p.proposed_value/p.quantity;
			if(badpu > min_sell_price){
				newProp.proposed_value = (float) ((badpu-(badpu-min_sell_price)*0.5) * newProp.quantity);
			}
		}
		else{
			newProp.proposed_value = p.proposed_value;
		}


		return newProp;
	}


	public boolean done() {
		return true;
	}
}
