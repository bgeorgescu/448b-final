package vis.data.util;

//basically this talks to the db and caches the word to id mapping.
//it will automatically add new words, so no one else should mess with
//this table while the cache is active
public class WordCache {
	public WordCache() {
	}
	int getIdForWord(String word) {
		throw new RuntimeException("fat chance");
	}
}
