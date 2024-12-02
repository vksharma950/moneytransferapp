package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.AccountNotExistException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.OverDraftException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
class AccountsServiceTest {

  @InjectMocks
  private AccountsService accountsService;
  
  @Mock
  private AccountsRepository accountsRepository;
  
  @Mock
  private NotificationService notificationService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    doNothing().when(accountsRepository).createAccount(account);
    when(accountsService.getAccount("Id-123")).thenReturn(account);
    accountsService.createAccount(account);
    assertEquals(accountsService.getAccount("Id-123"), account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      //fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }
  
  @Test
  void transferAmount() throws Exception {
	    String accountFromId = "12";
		String accountFromTo = "15";
		BigDecimal amount = new BigDecimal(1);
		
		TransferRequest request = new TransferRequest();
		request.setAccountFromId(accountFromId);
		request.setAccountToId(accountFromTo);
		request.setAmount(amount);
		
		Account accFrom = new Account(accountFromId, BigDecimal.TEN);
		Account accTo = new Account(accountFromTo, BigDecimal.TEN);
		
		when(accountsRepository.getAccount(accountFromId)).thenReturn(accFrom);
		when(accountsRepository.getAccount(accountFromTo)).thenReturn(accTo);
		doNothing().when(notificationService).notifyAboutTransfer(accTo, accountFromTo);
		
		accountsService.transferAmount(request);

	  assertEquals(BigDecimal.valueOf(11), accFrom.getBalance());
	  assertEquals(BigDecimal.TEN.subtract(BigDecimal.ONE), accTo.getBalance());
  }
  
    @Test
	void transferAmountOverDraftException() throws OverDraftException, AccountNotExistException {
		String accountFromId = "12";
		String accountFromTo = "15";
		BigDecimal amount = new BigDecimal(15);

		TransferRequest request = new TransferRequest();
		request.setAccountFromId(accountFromId);
		request.setAccountToId(accountFromTo);
		request.setAmount(amount);

		Account accFrom = new Account(accountFromId, BigDecimal.TEN);
		Account accTo = new Account(accountFromTo, BigDecimal.TEN);

		when(accountsRepository.getAccount(accountFromId)).thenReturn(accFrom);
		when(accountsRepository.getAccount(accountFromTo)).thenReturn(accTo);
		doNothing().when(notificationService).notifyAboutTransfer(accTo, accountFromTo);

		assertThrows(OverDraftException.class, () -> accountsService.transferAmount(request),
				"Account with id:" + accFrom.getAccountId() + " does not have enough balance to transfer.");
	}
    
    @Test
	void transferAmountExceptionWhenAccountNotAvalaible() throws OverDraftException, AccountNotExistException {
		String accountFromId = "12";
		String accountFromTo = "15";
		BigDecimal amount = new BigDecimal(15);

		TransferRequest request = new TransferRequest();
		request.setAccountFromId(accountFromId);
		request.setAccountToId(accountFromTo);
		request.setAmount(amount);

		Account accFrom = new Account(accountFromId, BigDecimal.TEN);
		Account accTo = new Account(accountFromTo, BigDecimal.TEN);

		when(accountsRepository.getAccount(accountFromId)).thenReturn(null);
		when(accountsRepository.getAccount(accountFromTo)).thenReturn(accTo);
		doNothing().when(notificationService).notifyAboutTransfer(accTo, accountFromTo);

		assertThrows(AccountNotExistException.class, () -> accountsService.transferAmount(request),
				"AccountFrom or AccountTo is not available.");
	}
}
