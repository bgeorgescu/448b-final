<?php

if($argc != 3) {
	echo "usage: php ".$argv[0]." naive_1gram.php /path/to/dir/to/index/\n";
	die();
}

// http://www.recessframework.org/page/map-reduce-anonymous-functions-lambdas-php
function sumWordCounts($countsL, $countsR) {  
	// Get all the words  
	$words = array_merge(array_keys($countsL), array_keys($countsR));  
	$out = array();  
	// Put them in a new (Array: Word => Count)  
	foreach($words as $word) {  
		// Sum their counts  
		$out[$word] = isset($countsL[$word]) ? $countsL[$word] : 0;  
		$out[$word] += isset($countsR[$word]) ? $countsR[$word] : 0;  
	}  
	return $out;  
};  

/*
Load the paragraph handler specified on the command line.
A paragraph handler must contain a function handleParagraph that
returns a map containing ngrams and their counts in the paragraph.
*/
require($argv[1]);

/*
This is a bit gross but I'm too lazy to figure out the exact
xpath to get only the first result
*/
function getFirstMatchAsString(&$doc, $xpath) {
	$m = $doc->xpath($xpath);
	if(!empty($m)) {
		$val = "".$m[0];
		unset($m);
		return $val;
	}
	unset($m);
	return false;
}

/*
Mapping of document properties we are interested in
to xpath query to extract those properties
*/
$xpath_mappings = array(
	"pub_id" => "/DMLDOC/pmdt/pmid",
	"section_raw" => "/DMLDOC/docdt/docsec",
	"date" => "/DMLDOC/pcdt/pcdtn",
	"doc_id" => "/DMLDOC/docdt/docid",
	"title" => "/DMLDOC/docdt/doctitle",
	"subtitle" => "/DMLDOC/docdt/docsubt",
	"page" => "/DMLDOC/docdt/docpgn"
);

function handleFile($filename) {
	global $xpath_mappings;
	
	$doc = new SimpleXMLElement($filename, null, true);
	$docinfo = array();
	
	foreach($xpath_mappings as $key => $xpath) {
		$docinfo[$key] = getFirstMatchAsString($doc, $xpath);
	}
		
	$paragraphs = $doc->xpath('/DMLDOC/txtdt/text/paragraph');
	$emit = array();
	
	foreach($paragraphs as $para)
		$emit[] = handleParagraph("".$para);

	// TODO: should the title also be treated as a paragraph?
	// $emit[] = handleParagraph($docinfo['title']);

	unset($paragraphs);	
	$totals = array_reduce($emit, sumWordCounts, array()); 
	unset($emit);
	unset($doc);
	
	
	// TODO: save output to database instead of echoing it
	
	print_r($totals);
	print_r($docinfo);
}

/*
Attempts to handle all the XML files in the given directory and its descendants
*/
function handleDir($path) {
	//Inspired by comments on http://php.net/manual/en/class.recursivedirectoryiterator.php
	
	$Directory = new RecursiveDirectoryIterator(realpath($path));
	$Iterator = new RecursiveIteratorIterator($Directory);
	$Regex = new RegexIterator($Iterator, '/^.+\.xml$/i', RecursiveRegexIterator::GET_MATCH);

	foreach($Regex as $name => $object){
	    handleFile($name);
	}
	
	unset($Regex);
	unset($Iterator);
	unset($Directory);
}

handleDir($argv[2]);


?>