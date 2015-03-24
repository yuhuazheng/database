import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class RunDatabaseWithFile {

public static void main(String[] args)throws IOException{
	
	if(args.length<1){
		System.out.println("Usage: java RunDatabaseWithFile <filename>");
		System.exit(0);
	}
		
	try{
			SimpleDatabase sdb = new SimpleDatabase();
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
			String curCmd;
			while((curCmd=br.readLine())!=null){
				System.out.println(curCmd);
				if(!sdb.executeCmd(curCmd,false)){
					br.close();
					System.exit(0);
				}
			}
			br.close();
		}catch(IOException io){
			io.printStackTrace();
		}
	}
}
