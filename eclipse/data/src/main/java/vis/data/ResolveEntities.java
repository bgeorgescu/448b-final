package vis.data;

//look through the intermediate entity table and generate a resolved set of entities, 
//e.g. President Obama, President Barack Obama are the same
public class ResolveEntities {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
