package vis.data;

//run the whole data processing setup
public class All 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        
        LoadXML.main(null);
        PartsOfSpeech.main(null);
        ProperPhrases.main(null);
        ResolveEntities.main(null);
        StemWords.main(null);
        LemmatizeWords.main(null);
        PatternNGrams.main(null);
        ParseSentences.main(null);
        SyntacticNGrams.main(null);
        BuildAutocomplete.main(null);
    }
}
