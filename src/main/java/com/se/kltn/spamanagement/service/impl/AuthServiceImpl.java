package com.se.kltn.spamanagement.service.impl;

import com.se.kltn.spamanagement.dto.request.AccountRequest;
import com.se.kltn.spamanagement.dto.response.AccountResponse;
import com.se.kltn.spamanagement.exception.BadRequestException;
import com.se.kltn.spamanagement.exception.ResourceNotFoundException;
import com.se.kltn.spamanagement.model.Account;
import com.se.kltn.spamanagement.constants.enums.Role;
import com.se.kltn.spamanagement.model.Customer;
import com.se.kltn.spamanagement.model.Employee;
import com.se.kltn.spamanagement.repository.AccountRepository;
import com.se.kltn.spamanagement.repository.CustomerRepository;
import com.se.kltn.spamanagement.repository.EmployeeRepository;
import com.se.kltn.spamanagement.security.jwt.JwtProvider;
import com.se.kltn.spamanagement.service.AuthService;
import com.se.kltn.spamanagement.utils.MappingData;
import lombok.extern.log4j.Log4j2;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static com.se.kltn.spamanagement.constants.ErrorMessage.EMPLOYEE_NOT_FOUND;

@Service
@Log4j2
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String sender;

    @Autowired
    public AuthServiceImpl(AccountRepository accountRepository, EmployeeRepository employeeRepository,
                           CustomerRepository customerRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtProvider jwtProvider, JavaMailSender javaMailSender) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.javaMailSender = javaMailSender;
    }

    @Override
    public Map<String, String> login(String username, String password) {
        log.debug("login account: " + username);
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            String token = jwtProvider.createToken(username, role);
            Map<String, String> response = new HashMap<>();
            response.put("username", username);
            response.put("token", token);
            response.put("role", role);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceNotFoundException("incorrect username or password or account is not enable");
        }
    }

    @Override
    public void registerCustomer(AccountRequest accountRequest, String email, String siteUrl) throws MessagingException {
        log.debug("register new account for customer");
        checkAccountRequest(accountRequest);
        Customer customer = this.customerRepository.save(new Customer());
        Account account = MappingData.mapObject(accountRequest, Account.class);
        account.setPassword(passwordEncoder.encode(accountRequest.getPassword()));
        account.setRole(Role.CUSTOMER);
        account.setCustomer(customer);
        account.setEmailVerificationCode(RandomString.make(64));
        account.setEnabled(false);
        Account accountRegister = this.accountRepository.save(account);
        sendVerificationEmail(accountRegister, email, siteUrl);
    }

    @Override
    public AccountResponse registerEmployee(AccountRequest accountRequest, Long idEmployee) {
        log.debug("register new account for employee");
        checkAccountRequest(accountRequest);
        Employee employee = this.employeeRepository.findById(idEmployee).orElseThrow(
                () -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        Account account = MappingData.mapObject(accountRequest, Account.class);
        account.setPassword(passwordEncoder.encode(accountRequest.getPassword()));
        account.setRole(Role.STAFF);
        account.setEmployee(employee);
        Account accountRegister = this.accountRepository.save(account);
        return MappingData.mapObject(accountRegister, AccountResponse.class);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        log.debug("logout account");
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, null);
    }

    @Override
    public boolean verify(String verificationCode) {
        log.debug("verify account");
        Account account = this.accountRepository.findAccountByEmailVerificationCode(verificationCode).orElse(null);
        if (account == null || account.getEnabled()) {
            return false;
        } else {
            account.setEnabled(true);
            account.setEmailVerificationCode(null);
            this.accountRepository.save(account);
            return true;
        }
    }

    private void sendVerificationEmail(Account accountRegister, String email, String siteUrl) throws MessagingException {
        log.debug("send verification email");
        MimeMessage mimeMailMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMailMessage);
        try {
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setFrom(sender);
            mimeMessageHelper.setSubject("Complete Registration!");
            String message = processMessage(accountRegister, siteUrl);
            mimeMessageHelper.setText(message, true /* isHtml */);
            javaMailSender.send(mimeMailMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MessagingException("Error sending email");
        }
    }

    private static String processMessage(Account accountRegister, String siteUrl) {
        String content = "Dear [[name]],<br>" +
                "Please click the link below to verify your registration:<br>\n" +
                "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>\n" +
                "Thank you,<br>\n" +
                "VH Spa.";

        String verifyURL = siteUrl + "/api/auth/verify?code=" + accountRegister.getEmailVerificationCode();
        String message = content.replace("[[name]]", accountRegister.getUsername());
        message = message.replace("[[URL]]", verifyURL);
        return message;
    }

    private void checkAccountRequest(AccountRequest accountRequest) {
        if (!accountRequest.getPasswordConfirm().equals(accountRequest.getPassword())) {
            throw new BadRequestException("password confirm is not same");
        } else if (accountRepository.findAccountByUsername(accountRequest.getUsername()).isPresent()) {
            throw new BadRequestException("username already exist");
        }
    }
}

