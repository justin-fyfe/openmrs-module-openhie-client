package org.openmrs.module.openhie.client.cda.section.impl;

import java.util.ArrayList;
import java.util.UUID;

import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.doc.StructDocElementNode;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedAuthor;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organization;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.openmrs.module.openhie.client.cda.section.SectionBuilder;
import org.openmrs.module.openhie.client.util.CdaDataUtil;
import org.openmrs.module.openhie.client.util.CdaTextUtil;

/**
 * Generic clinical section builder
 * @author JustinFyfe
 *
 */
public abstract class SectionBuilderImpl implements SectionBuilder {

	// CDA Text utility
	private CdaTextUtil m_cdaTextUtil = CdaTextUtil.getInstance();
	private CdaDataUtil m_cdaDataUtil = CdaDataUtil.getInstance();
	
	/**
	 * Generate the Level 3 content text
	 */
	protected SD generateLevel3Text(Section section)
	{
		SD retVal = new SD();
		Class<? extends ClinicalStatement> previousStatementType = null;
		StructDocElementNode context = null; 
		for(Entry ent : section.getEntry())
		{
			// Is this different than the previous?
			if(!ent.getClinicalStatement().getClass().equals(previousStatementType))
			{
				// Add existing context node before generating another
				if(context!=null)
					retVal.getContent().add(context);
				// Force the generation of new context
				context = null;
			}
			StructDocElementNode genNode = this.m_cdaTextUtil.generateText(ent.getClinicalStatement(), context);
			
			// Set the context node
			if(context == null)
				context = genNode;
			previousStatementType = ent.getClinicalStatement().getClass();
			
			ent.setTypeCode(x_ActRelationshipEntry.DRIV);
		}
		if(context != null && !retVal.getContent().contains(context))
			retVal.getContent().add(context);
		return retVal;
	}
	
	/**
	 * Generate section
	 */
	public Section generate(Entry... entries) {
		Section retVal = new Section();
		retVal.setId(UUID.randomUUID());
		
		for(Entry ent : entries)
		{
			retVal.getEntry().add(ent);
		}
		
		// Generate the text
		retVal.setText(this.generateLevel3Text(retVal));
		
		// Minify authors
		for(Entry ent : entries)
			this.minifyAuthors(ent.getClinicalStatement(), retVal.getAuthor());

		return retVal;
	}

	/**
	 * Minify authors
	 */
	private void minifyAuthors(ClinicalStatement clinicalStatement, ArrayList<Author> author) {
		
		for(Author aut : clinicalStatement.getAuthor())
		{
			if(!this.m_cdaDataUtil.containsAuthor(author, aut))
				author.add(aut);
			aut.setAssignedAuthor(new AssignedAuthor(aut.getAssignedAuthor().getId()));
			
		}
		
		for(EntryRelationship er : clinicalStatement.getEntryRelationship())
			this.minifyAuthors(er.getClinicalStatement(), author);
		
		if(clinicalStatement instanceof Organizer)
			for(Component4 comp4 : ((Organizer)clinicalStatement).getComponent())
				this.minifyAuthors(comp4.getClinicalStatement(), author);
		
	}

	
	
	
}
