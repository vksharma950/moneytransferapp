package com.dws.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TransferRequest {
	
	@NotNull
	@NotEmpty
	private String accountFromId;
	
	@NotNull
	@NotEmpty
	private String accountToId;
	
	@NotNull
	@Min(value = 0, message = "Transfer amount can not be less then zero")
	private BigDecimal amount;

}
