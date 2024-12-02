package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.AccountNotExistException;
import com.dws.challenge.exception.OverDraftException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;
  
  @Mock
  private AccountsRepository accountsRepository;

  @InjectMocks
  private AccountsService accountsService;
  
  @Mock
  private NotificationService notificationService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
	  String uniqueAccountId = "Id-" + "123";
	  Account account = new Account(uniqueAccountId, new BigDecimal("1000"));
	  doNothing().when(accountsRepository).createAccount(account);
	  accountsService.createAccount(account);
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + "123";
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    when(accountsRepository.getAccount(uniqueAccountId)).thenReturn(account);
    Account acnt = accountsService.getAccount(uniqueAccountId);
    assertEquals(account.getAccountId(), acnt.getAccountId());
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
