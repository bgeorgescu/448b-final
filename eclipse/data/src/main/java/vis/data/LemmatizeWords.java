package vis.data;

//use wordnet to lemmatize words in a nicer way
public class LemmatizeWords {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
