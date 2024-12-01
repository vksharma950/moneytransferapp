package com.dws.challenge.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TransferResponse {

	private String accountFromId;
	
    private String accountToId;
	
	private BigDecimal amount;
	
	private BigDecimal balanceAfterTransfer;
	
	private TransferStatus status;
}
