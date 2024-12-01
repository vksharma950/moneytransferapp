package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.dto.TransferResponse;
import com.dws.challenge.exception.AccountNotExistException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.OverDraftException;
import com.dws.challenge.service.AccountsService;

import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

	@Autowired
	private AccountsService accountsService;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
		log.info("Creating account {}", account);

		try {
			accountsService.createAccount(account);
		} catch (DuplicateAccountIdException daie) {
			return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping(path = "/{accountId}")
	public ResponseEntity<Account> getAccount(@PathVariable String accountId) {
		log.info("Retrieving account for id {}", accountId);
		Account account =  accountsService.getAccount(accountId);
		return new ResponseEntity<>(account, HttpStatus.OK);
	}
	
	//for transfer Amount
	@PutMapping("/transferAmount")
	public ResponseEntity<TransferResponse> transferAmount(@RequestBody @Valid TransferRequest transferRequest) throws DuplicateAccountIdException, InterruptedException {
	  try {
		  TransferResponse result = accountsService.transferAmount(transferRequest);
		  result.setBalanceAfterTransfer(this.accountsService.checkBalance(transferRequest.getAccountFromId()));
		  return new ResponseEntity<>(result, HttpStatus.OK);
	  }catch (AccountNotExistException | OverDraftException e) {
			log.error("Fail to transfer balances, please check with system administrator.");
			throw e;
	}
	  
	  
  }

}
