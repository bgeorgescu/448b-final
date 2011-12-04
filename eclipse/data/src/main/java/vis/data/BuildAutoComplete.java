package vis.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;

import org.apache.commons.dbutils.DbUtils;

import vis.data.model.AutoComplete;
import vis.data.model.AutoComplete.Type;
import vis.data.model.Term;
import vis.data.model.meta.AutoCompleteAccessor;
import vis.data.model.meta.EntityAccessor;
import vis.data.model.meta.EntityAccessor.ScoredEntity;
import vis.data.model.meta.LemmaAccessor;
import vis.data.model.meta.LemmaAccessor.ScoredLemma;
import vis.data.model.meta.TermInsertionCache;
import vis.data.util.ExceptionHandler;
import vis.data.util.SQL;
import vis.data.util.StopWords;

public class BuildAutoComplete {

	public static void main(String[] args) {
		ExceptionHandler.terminateOnUncaught();
		
		if(SQL.tableExists(AutoComplete.TABLE))
			return;
		try {
			SQL.createTable(SQL.forThread(), AutoComplete.class, false);
		} catch (SQLException e) {
			throw new RuntimeException("failed to create autocomplete table", e);
		}
		
		Connection second = SQL.open();
		TermInsertionCache tic = TermInsertionCache.getInstance();
		try {
			AutoCompleteAccessor aca = new AutoCompleteAccessor();
			try {
				EntityAccessor ea = new EntityAccessor(second);
				EntityAccessor.ScoredResultSetIterator entities = ea.entityIteratorWithScore();
				ScoredEntity entity;
				HashSet<String> uniques = new HashSet<String>();
				while((entity = entities.nextEntity()) != null) {
					String[] parts = entity.entity_.split("\\s+");
					uniques.clear();
					for(String part : parts) {
						//this could create duplicates... but it is a pretty long length
						if(part.length() > Term.TERM_LENGTH)
							part = part.substring(0, Term.TERM_LENGTH);
						uniques.add(part);
					}
					for(String s : uniques) {
						if(StopWords.STOP_WORDS.contains(s))
							continue;
						int id = tic.getOrAddTerm(s);
						aca.addAutoComplete(id, Type.ENTITY, entity.id_, entity.score_);
					}
				}
			} catch (SQLException e) {
				throw new RuntimeException("failed to scan entities table", e);
			}
	
			try {
				LemmaAccessor la = new LemmaAccessor(second);
				LemmaAccessor.ScoredResultSetIterator lemmas = la.lemmaIteratorWithScore();
				ScoredLemma lemma;
				while((lemma = lemmas.nextLemma()) != null) {
					String part = lemma.lemma_;
					if(part.length() > Term.TERM_LENGTH)
						part = part.substring(0, Term.TERM_LENGTH);
					int id = tic.getOrAddTerm(part);
					aca.addAutoComplete(id, Type.LEMMA, lemma.id_, lemma.score_);
				}
			} catch (SQLException e) {
				throw new RuntimeException("failed to scan lemma table", e);
			}	
			SQL.createAllIndexes(SQL.forThread(), AutoComplete.class);
		} catch (SQLException e) {
			throw new RuntimeException("failed to add autocomplete entries", e);
		} finally {
			DbUtils.closeQuietly(second);
			DbUtils.closeQuietly(SQL.forThread());
		}
	}
}
