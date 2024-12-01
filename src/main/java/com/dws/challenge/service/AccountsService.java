package com.dws.challenge.service;

import com.dws.challenge.constant.ErrorCode;
import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.dto.TransferResponse;
import com.dws.challenge.dto.TransferStatus;
import com.dws.challenge.exception.AccountNotExistException;
import com.dws.challenge.exception.BusinessException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.OverDraftException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Service
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	@Getter
	private final NotificationService notificationService;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	public TransferResponse transferAmount(TransferRequest transferRequest)
			throws AccountNotExistException, DuplicateAccountIdException {
		
		Account accountFrom = this.accountsRepository.getAccount(transferRequest.getAccountFromId());
		Account accountTo = this.accountsRepository.getAccount(transferRequest.getAccountToId());

		TransferResponse response = new TransferResponse();
		response.setAccountFromId(transferRequest.getAccountFromId());
		response.setAccountToId(transferRequest.getAccountToId());
		response.setAmount(transferRequest.getAmount());
		if(ObjectUtils.isEmpty(accountFrom) || ObjectUtils.isEmpty(accountTo)) {
			response.setStatus(TransferStatus.FAILED);
			throw new AccountNotExistException("AccountFrom or AccountTo is not available.", ErrorCode.ACCOUNT_ERROR);
		}
		
		if(accountFrom.getAccountId().compareTo(accountTo.getAccountId()) > 0) {
			synchronized (accountFrom) {
				synchronized (accountTo) {
						transfer(accountTo, accountFrom, transferRequest, response);	
				}	
			}
		}else {
			synchronized (accountTo) {
				synchronized (accountFrom) {
						transfer(accountTo, accountFrom, transferRequest, response);
				}	
			}
		}
		if(response.getStatus().equals(TransferStatus.COMPLETED)) {
			  this.notificationService.notifyAboutTransfer(accountFrom, "Amount "+transferRequest.getAmount()+"is successfully transfered to "+accountTo);
			  this.notificationService.notifyAboutTransfer(accountTo, "You have recieved amount "+transferRequest.getAmount()+"from acount"+accountFrom);
		  }
		
		return response;
	}
	
	//Perform amount settlement in both account.
	private void transfer(Account accountFrom, Account accountTo, TransferRequest request, TransferResponse response) {
		if (accountFrom.getBalance().compareTo(request.getAmount()) < 0) {
			response.setStatus(TransferStatus.FAILED);
			throw new OverDraftException(
					"Account with id:" + accountFrom.getAccountId() + " does not have enough balance to transfer.",
					ErrorCode.ACCOUNT_ERROR);
		}
		depositAccountBalance(accountTo, accountTo.getBalance(), request);
		withdrawAccountBalance(accountFrom, accountFrom.getBalance(), request);
		response.setStatus(TransferStatus.COMPLETED);
	}

	// performs calculation of final amount after deposit and then persist the
	// amount in account
	private void depositAccountBalance(Account account, BigDecimal initialDepositAccountBalance,
			TransferRequest request) throws BusinessException {
		BigDecimal finalWithdrawAccountBalance = initialDepositAccountBalance.add(request.getAmount());
		account.setBalance(finalWithdrawAccountBalance);

	}

	// performs calculation of final amount after withdraw and then persist the
	// amount in account
	private void withdrawAccountBalance(Account account, BigDecimal initialWithdrawAccountBalance,
			TransferRequest request) throws BusinessException {
		BigDecimal finalWithdrawAccountBalance = initialWithdrawAccountBalance.subtract(request.getAmount());
		account.setBalance(finalWithdrawAccountBalance);

	}


	public BigDecimal checkBalance(String accountId) {
		Account account = this.accountsRepository.getAccount(accountId);
		BigDecimal amount = account.getBalance();
		return amount;
	}
}
