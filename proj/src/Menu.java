import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import jade.core.Runtime; 
import jade.wrapper.*; 

public class Menu {

	static int nextAgent = 0;
	static int nRounds;
	static String item;
	static String nome;
	static int max_delivery_days;
	static int min_delivery_days;
	static int stock;
	static int min_sell_price;
	static int selling_price;
	static int price_unit;
	static int desired_price;
	static int wanted_quantity;
	static int min_quantity;
	static int utility;
	static double ratio;
	static Object arg[];
	static AgentController creator;
	static Runtime rt;
	static jade.core.Profile p;
	static String host;

	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);
		System.out.println("Bem vindo!\nPretende iniciar um agente:\n\t1 - Gestor (Host)\n\t2 - Comprador/Vendedor (Client)\n\t0 - Sair");
		int flag = scanner.nextInt();
		rt = Runtime.instance();
		switch(flag){
		case 1:
			host = "localhost";
			p = new jade.core.ProfileImpl();
			p.setParameter(jade.core.ProfileImpl.MAIN_HOST, host);
			p.setParameter(jade.core.ProfileImpl.DETECT_MAIN,"true");
			p.setParameter(jade.core.ProfileImpl.GUI,"true");
			ContainerController cc = rt.createMainContainer(p);

			try {//TODO fazer a parte relativa ao gestor
				creator = cc.createNewAgent("gestor", "Gestor", arg);
				creator.start();
			} catch (StaleProxyException e1) {
				e1.printStackTrace();
			}

			break;
		case 2:
			System.out.println("Insira IP do gestor:");
			host = scanner.next();
			int loop = 1;
			while(loop != 0){

				System.out.println("Deseja comprar ou vender itens?\n\t1 - Comprador\n\t2 - Vendedor\n\t3 - Correr ficheiro de testes");
				nextAgent = scanner.nextInt();
				if(nextAgent!=3){
					System.out.println("Introduza os seguintes dados:");
					System.out.println("\n\tNome do agente:");
					nome = scanner.next();
					System.out.println("\n\tNome do item:");
					item = scanner.next();
				}
				else{
					item = "x";
				}
				if(item != null){
					switch(nextAgent){
					case 1:
						System.out.println("\n\tNumero de rondas de negociacao maximo:");
						nRounds = scanner.nextInt();
						System.out.println("\n\tTempo de entrega maximo (em dias):");
						max_delivery_days = scanner.nextInt();
						System.out.println("\n\tTempo de entrega razoavel (em dias):");
						min_delivery_days = scanner.nextInt();
						System.out.println("\n\tQuantidade pretendida:");
						wanted_quantity = scanner.nextInt();
						System.out.println("\n\tQuantidade minima:");
						min_quantity = scanner.nextInt();
						System.out.println("\n\tPreco por unidade desejado:");
						desired_price = scanner.nextInt();
						System.out.println("\n\tPreco maximo que esta disposto a pagar por unidade:");
						price_unit = scanner.nextInt();
						System.out.println("\n\tUtilidade minima (0-100):");
						utility = scanner.nextInt();
						System.out.println("\n\tMinimo de reputacao que\n\testa disposto a aceitar (0-1, Exemplo 0,5):" );
						ratio = scanner.nextDouble();

						arg = new Object[10];
						arg[0] = item;
						arg[1] = max_delivery_days;
						arg[2] = min_delivery_days;
						arg[3] = wanted_quantity;
						arg[4] = min_quantity;
						arg[5] = price_unit;
						arg[6] = desired_price;
						arg[7] = utility;
						arg[8] = ratio;
						arg[9] = nRounds;

						p = new jade.core.ProfileImpl();
						p.setParameter(jade.core.ProfileImpl.MAIN_HOST, host);
						p.setParameter(jade.core.ProfileImpl.CONTAINER_NAME, "container");
						ContainerController cc1 = rt.createAgentContainer(p);

						try {
							creator = cc1.createNewAgent(nome,  
									"Buyer", arg);
							// Fire up the agent 
							creator.start(); 
						} catch (StaleProxyException e) {
							e.printStackTrace();
						} 

						break;


					case 2:
						System.out.println("\n\tTempo de entrega maximo (em dias):");
						max_delivery_days = scanner.nextInt();
						System.out.println("\n\tTempo de entrega minimo (em dias):");
						min_delivery_days = scanner.nextInt();
						System.out.println("\n\tStock:");
						stock = scanner.nextInt();
						System.out.println("\n\tPreco de venda inicial: ");
						selling_price = scanner.nextInt();
						System.out.println("\n\tPreco min que esta disposto a vender: ");
						min_sell_price = scanner.nextInt();
						System.out.println("\n\tMinimo de reputacao que\n\testa disposto a aceitar (0-1, Exemplo 0,5):" );
						ratio = scanner.nextDouble();

						arg = new Object[7]; 
						arg[0] = item;
						arg[1] = max_delivery_days;
						arg[2] = min_delivery_days;
						arg[3] = stock;
						arg[4] = selling_price;
						arg[5] = min_sell_price;
						arg[6] = ratio;

						p = new jade.core.ProfileImpl();
						p.setParameter(jade.core.ProfileImpl.MAIN_HOST, host);
						p.setParameter(jade.core.ProfileImpl.CONTAINER_NAME, "container");
						ContainerController cc2 = rt.createAgentContainer(p);

						try {
							creator = cc2.createNewAgent(nome,  
									"Seller", arg);
							// Fire up the agent 
							creator.start(); 
						} catch (StaleProxyException e) {
							e.printStackTrace();
						} 

						break;
					case 3:						
						try{
							FileInputStream fstream = new FileInputStream("testes.txt");
							DataInputStream in = new DataInputStream(fstream);
							BufferedReader br = new BufferedReader(new InputStreamReader(in));
							String strLine;
							while ((strLine = br.readLine()) != null)   {
								String[] temp;
								String delimiter = "!";
								temp = strLine.split(delimiter);
								arg = new Object[10];

								for(int i = 2; i < temp.length ; i++){
									arg[i-2] = temp[i];
								}
								p = new jade.core.ProfileImpl();
								p.setParameter(jade.core.ProfileImpl.MAIN_HOST, host);
								p.setParameter(jade.core.ProfileImpl.CONTAINER_NAME, "container");
								ContainerController cc3 = rt.createAgentContainer(p);
								nome = temp[1];
								if(temp[0].equals("v")){
									try {
										arg[1] = Integer.parseInt((String) arg[1]);
										arg[2] = Integer.parseInt((String) arg[2]);;
										arg[3] = Integer.parseInt((String) arg[3]);;
										arg[4] = Integer.parseInt((String) arg[4]);;
										arg[5] = Integer.parseInt((String) arg[5]);
										arg[6] = Double.parseDouble((String) arg[6]);
										creator = cc3.createNewAgent(nome,  
												"Seller", arg);
										creator.start(); 
									} catch (StaleProxyException e) {
										e.printStackTrace();
									} 
								}
								else{
									try {
						
										arg[1] = Integer.parseInt((String) arg[1]);
										arg[2] = Integer.parseInt((String) arg[2]);;
										arg[3] = Integer.parseInt((String) arg[3]);;
										arg[4] = Integer.parseInt((String) arg[4]);;
										arg[5] = Integer.parseInt((String) arg[5]);
										arg[6] = Integer.parseInt((String) arg[6]);
										arg[7] = Integer.parseInt((String) arg[7]);
										arg[8] = Double.parseDouble((String) arg[8]);
										arg[9] = Integer.parseInt((String) arg[9]);
										creator = cc3.createNewAgent(nome,  
												"Buyer", arg);
										creator.start(); 
									} catch (StaleProxyException e) {
										e.printStackTrace();
									} 
								}
							}
							in.close();
						}catch (Exception e){//Catch exception if any
							System.err.println("Error: " + e.getMessage());
						}

						break;
					}

				}
				System.out.println("0 - Sair\n1 - Criar outro agente");
				loop = scanner.nextInt();
			}
			break;
		case 0:
			System.out.println("Adeus!");
			break;
		}
		scanner.close();

	}

}
