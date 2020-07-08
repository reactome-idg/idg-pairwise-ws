package org.reactome.idg.pairwise.web.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * @author brunsont
 *
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerError extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 316926435659593174L;

}
