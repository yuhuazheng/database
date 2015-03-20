import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;


public class SimpleDatabase {

	//global hashmaps to hold official data
	HashMap<String, Integer> nameValueMap;
	HashMap<Integer, Integer> valueCountMap;
	//hashmaps for tracking transactions
	HashMap<String, Integer> nameValueMap_tran;
	HashMap<Integer, Integer> valueCountMap_tran;
	HashSet<String> unsetN_tran; //to track the unset name in trans
	HashSet<Integer> emptyV_tran; //to track the zero value in trans
	Stack<String> cmds_tran; // to track the reverse cmd in trans
	boolean inTran;
	
	//constructor to initialize attributes
	public SimpleDatabase(){
		nameValueMap = new HashMap<String, Integer>() ;
		valueCountMap = new HashMap<Integer, Integer>();
		nameValueMap_tran = new HashMap<String, Integer>();
		valueCountMap_tran = new HashMap<Integer, Integer>();
		unsetN_tran = new HashSet<String>();
		emptyV_tran = new HashSet<Integer>();
		cmds_tran = new Stack<String>();
		inTran = false;
	}
	
	public static void main(String[] args)throws IOException{
		
		try{
			SimpleDatabase sdb = new SimpleDatabase();
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String curCmd;
			while((curCmd=br.readLine())!=null){
				if(!sdb.executeCmd(curCmd,false)){
					System.exit(0);
				}
			}
		}catch(IOException io){
			io.printStackTrace();
		}
	}
	
	//if in tran, read and write tran hashmaps, and keep reverse cmds in stack
	//when rollback, pop reverse cmds til meet "begin"
	//when commit, apply all tran hashmap/set
	//unset-tran to keep the unset variables in trans
	private boolean executeCmd(String cmd, boolean inRollback){ // assume cmd is valid
		//if not in tran, read and write global hashmaps
		String[] words = cmd.split(" ");
		if(words[0].toLowerCase().equals("end")){
			return false;
		}
		
		else if(words[0].toLowerCase().equals("begin")){
			inTran=true;
			if(!inRollback){
				cmds_tran.push("begin");
			}
			return true;
		}
		
		else if(words[0].toLowerCase().equals("set")){
			String k = words[1].toLowerCase();
			int v = Integer.parseInt(words[2]);
			if(inTran){
				if(unsetN_tran.contains(k)){
					unsetN_tran.remove(k);
				}
				if(emptyV_tran.contains(v)){
					emptyV_tran.remove(v);
				}
				boolean existedBeforeTran = false;
				int v_old=0;
				//update hashmaps
				if(nameValueMap_tran.containsKey(k)){
					existedBeforeTran=true;
					v_old = nameValueMap_tran.get(k);
					int c_old = valueCountMap_tran.get(v_old);
					if(c_old==1){
						valueCountMap_tran.remove(v_old);
					}
					else{
						valueCountMap_tran.put(v_old, c_old-1);
					}
				}
				else{
					//inherit value_count info from main maps
					if(nameValueMap.containsKey(k)){
						//value count need to update
						existedBeforeTran=true;
						v_old = nameValueMap.get(k);
						int c_old = valueCountMap.get(v_old);
						if(c_old==1){
							valueCountMap_tran.put(v_old, 0);
						}
						else{
							valueCountMap_tran.put(v_old, c_old-1);
						}
					}
				}
				nameValueMap_tran.put(k, v);
				if(valueCountMap_tran.containsKey(v)){
					valueCountMap_tran.put(v, valueCountMap_tran.get(v)+1);
				}
				else{
					valueCountMap_tran.put(v, 1);
				}
				//save the scene
				if(!inRollback){
					if(existedBeforeTran){
						cmds_tran.push("set "+k+" "+ Integer.toString(v_old));
					}
					else{
						cmds_tran.push("unset "+k);
					}
				}
			}
			else{
				if(nameValueMap.containsKey(k)){
					int v_old = nameValueMap.get(k);
					int c_old = valueCountMap.get(v_old);
					//update value count
					if(c_old==1){
						valueCountMap.remove(v_old);
					}
					else{
						valueCountMap.put(v_old, c_old-1);
					}
				}
				//udpate name value map
				nameValueMap.put(k, v);
				if(valueCountMap.containsKey(v)){
					valueCountMap.put(v, valueCountMap.get(v)+1);
				}
				else{
					valueCountMap.put(v, 1);
				}
			}
		}
		
		else if(words[0].toLowerCase().equals("get")){
			String k = words[1].toLowerCase();
			if(inTran){
				if(unsetN_tran.contains(k)){
					System.out.println("NULL");
				}
				else if(nameValueMap_tran.containsKey(k)){
					System.out.println(nameValueMap_tran.get(k));
				}
				else if(nameValueMap.containsKey(k)){
					System.out.println(nameValueMap.get(k));
				}
				else{
					System.out.println("NULL");
				}
			}
			else{
				if(nameValueMap.containsKey(k)){
					System.out.println(nameValueMap.get(k));
				}
				else{
					System.out.println("NULL");
				}
			}
		}
		
		else if(words[0].toLowerCase().equals("unset")){
			String k = words[1].toLowerCase();
			if(inTran){
				if(unsetN_tran.contains(k)){
					//
				}
				else if(nameValueMap_tran.containsKey(k)){
					//update name_value
					int tranV=nameValueMap_tran.get(k);
					nameValueMap_tran.remove(k);
					//update value_count
					int tranC=valueCountMap_tran.get(tranV);
					if(tranC==1){
						valueCountMap_tran.remove(tranV);
						emptyV_tran.add(tranV);
					}
					else{
						valueCountMap_tran.put(tranV, tranC-1);
					}
					//update unset map
					unsetN_tran.add(k);
					//save scene
					if(!inRollback){
						cmds_tran.push("set "+k+" "+Integer.toString(tranV));
					}
				}
				else{
					if(nameValueMap.containsKey(k)){
						int tranV=nameValueMap.get(k);
						//update value_count
						if(valueCountMap_tran.containsKey(tranV)){
							int tranC = valueCountMap_tran.get(tranV);
							if(tranC==1){
								valueCountMap_tran.remove(tranV);
								emptyV_tran.add(tranV);
							}
							else{
								valueCountMap_tran.put(tranV, tranC-1);
							}
						}
						else{
							int normC = valueCountMap.get(tranV);
							if(normC==1){
								emptyV_tran.add(tranV);
							}
							else{
								valueCountMap_tran.put(tranV, normC-1);
							}
						}
						unsetN_tran.add(k);
						if(!inRollback){
							cmds_tran.push("set "+"k "+Integer.toString(tranV));
						}
					}
				}
			}
			else{
				if(nameValueMap.containsKey(k)){
					int v_old = nameValueMap.get(k);
					int c_old = valueCountMap.get(v_old);
					//update value count
					if(c_old==1){
						valueCountMap.remove(v_old);
					}
					else{
						valueCountMap.put(v_old, c_old-1);
					}
					nameValueMap.remove(k);
				}
			}
		}
		
		else if(words[0].toLowerCase().equals("numequalto")){
			int v = Integer.parseInt(words[1]);
			if(inTran){
				if(emptyV_tran.contains(v)){
					System.out.println(0);
				}
				else if(valueCountMap_tran.containsKey(v)){
					System.out.println(valueCountMap_tran.get(v));
				}
				else if(valueCountMap.containsKey(v)){
					System.out.println(valueCountMap.get(v));
				}
				else{
					System.out.println(0);
				}
			}
			else{
				if(valueCountMap.containsKey(v)){
					System.out.println(valueCountMap.get(v));
				}
				else{
					System.out.println(0);
				}
			}
		}
		
		else if(words[0].toLowerCase().equals("rollback")){
			if(inTran){
				if(cmds_tran.isEmpty()){
					System.out.println("NO TRANSACTION");
				}
				else{
					String cd = cmds_tran.pop();
					while(!cd.toLowerCase().equals("begin")){
						executeCmd(cd,true);
						cd=cmds_tran.pop();
					}
				}
			}
			else{
				System.out.println("NO TRANSACTION");
			}
		}
		
		else if(words[0].toLowerCase().equals("commit")){
			if(inTran){
				if(cmds_tran.isEmpty()){
					System.out.println("NO TRANSACTION");
				}
				else{	
					//remove unset
					for(String k : unsetN_tran){
						int v = nameValueMap.get(k);
						nameValueMap.remove(k);
						int c = valueCountMap.get(v);
						if(c==1){
							valueCountMap.remove(v);
						}
						else{
							valueCountMap.put(v, c-1);
						}
					}
				    //	update name value in tran
					for(String k : nameValueMap_tran.keySet()){
						int v= nameValueMap_tran.get(k);
						if(nameValueMap.containsKey(k)){
							int v_old = nameValueMap.get(k);
							int c_old = valueCountMap.get(v_old);
							if(c_old==1){
								valueCountMap.remove(v_old);
							}
							else{
								valueCountMap.put(v_old, c_old-1);
							}
						}
						nameValueMap.put(k, v);
						if(valueCountMap.containsKey(v)){
							valueCountMap.put(v, valueCountMap.get(v)+1);
						}
						else{
							valueCountMap.put(v, 1);
						}
					}
					//clean tran maps
					cmds_tran.clear();
					nameValueMap_tran.clear();
					valueCountMap_tran.clear();
					emptyV_tran.clear();
					unsetN_tran.clear();
				}
			}
			else{
				System.out.println("NO TRANSACTION");
			}
		}
		
		else{
			System.out.println("Not a valid command. Only support set, unset, get, numequalto, begin, rollback, commit, end");
		}
		return true;
	}
}
