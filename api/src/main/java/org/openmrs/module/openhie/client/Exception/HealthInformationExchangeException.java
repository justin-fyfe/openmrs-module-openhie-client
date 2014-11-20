package org.openmrs.module.openhie.client.Exception;


/**
 * Health information exchange communication base exception
 * @author Justin
 *
 */
public class HealthInformationExchangeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * HIE Exception
	 */
	public HealthInformationExchangeException() {
		
	}
	
	/**
	 * Creates a new HIE exception
	 */
	public HealthInformationExchangeException(Exception cause)
	{
		super(cause);
	}
	
}
