package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProfileImageChangeRequest (

	@NotNull
	String viewUrl,

    @NotNull
    Status status

	){

		public enum Status {
        DELETE,
        UPDATE
    }

}


