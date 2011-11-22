package vis.data;

//match patterns of parts of speech to generate n-grams
public class PatternNGrams {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
