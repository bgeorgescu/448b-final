package vis.data;

public class All {
	public static void main(String[] args) {
		//loads all the 
		LoadXML.main(args);

		//direct use of sql to store word hit tuples, too many rows...
		//so doing the presorted list of precomputed hits instead
		//DocWords.main(new String[]);
		//RawHits.main(new String[]);
		
		//extract all entities and lemmas using stanford corenlp
		DocLemmasEntities.main(new String[0]);
		
		//load up all of the extracted list that are stored per document
		//and convert them into lists of documents for each entity/lemma
		EntityDocs.main(new String[0]);
		LemmaDocs.main(new String[0]);

		//takes too long... ~20days ... need to use amazon
		//not parallel without multiple jvm instances because of global
		//state in the stanford nlp library
		//Coreferences.main(new String[0]);
		
		//loads the word lists for sentiment
		GeneralInquirerSentiment.main(null);

		//do basic resolution of entities via wikipedia redirects
		WikipediaEntityResolution.main(null);
		
		//load wordnet databases
		LoadWordNet.main(null);
		
		//build an autocomplete database table
		BuildAutoComplete.main(null);
		
		//optimize tables now that we are done
		OptimizeTables.main(null);
	}
}
