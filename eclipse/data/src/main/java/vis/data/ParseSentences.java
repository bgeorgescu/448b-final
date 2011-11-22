package vis.data;

//parse sentences for a later pass of n-gram generation
public class ParseSentences {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
