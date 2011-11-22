package vis.data;

//scan the full text for proper noun phrases and put them in an intermediate table
public class ProperPhrases {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
