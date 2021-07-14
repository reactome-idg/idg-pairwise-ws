package org.reactome.idg.pairwise.web.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * @author brunsont
 *
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException{

	/**
	 * needed when extending RuntimeException
	 */
	private static final long serialVersionUID = 6265130588876988385L;

	public ResourceNotFoundException(String message) {
		super(message);
	}
	
}
