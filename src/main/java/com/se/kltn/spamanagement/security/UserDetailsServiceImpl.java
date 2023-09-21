package com.se.kltn.spamanagement.security;

import com.se.kltn.spamanagement.model.Account;
import com.se.kltn.spamanagement.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findAccountByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("account cannot found"));
        return UserPrinciple.createUser(account);
    }
}
