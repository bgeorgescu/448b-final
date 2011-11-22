package vis.data;

//tag all parts of speech for each document for later processing of phrase queries
public class PartsOfSpeech {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
