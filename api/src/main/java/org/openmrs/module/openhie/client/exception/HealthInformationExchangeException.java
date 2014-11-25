package org.openmrs.module.openhie.client.exception;


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

	/**
	 * Create health information exception
	 * @param string
	 */
	public HealthInformationExchangeException(String message) {
		super(message);
	}
	
}
