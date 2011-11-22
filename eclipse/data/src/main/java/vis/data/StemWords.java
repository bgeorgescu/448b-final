package vis.data;

//chop words up to find the stems as a crappy form of resolving words against each other
public class StemWords {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
