package vis.data;

//use sentence parsing intermediates to make n-grams
public class SyntacticNGrams {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
