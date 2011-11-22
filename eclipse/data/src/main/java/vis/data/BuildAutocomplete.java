package vis.data;

//build an autocomplete table (maybe the regular index works ok.. however, for phrases, 
//you want to be able to autocomplete to later parts of the phrase (i.e. entry per word)
public class BuildAutocomplete {
	public static void main(String[] args) {
		String cmd = "";
		for(String s : args)
		    cmd += s + " ";
		System.out.println("cmd: " + cmd);
	}
}
