package com.portfolio.drivecare.security;

import com.portfolio.drivecare.user.AppUser;
import com.portfolio.drivecare.user.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DriveCareUserDetailsService implements UserDetailsService {

	private final AppUserRepository users;

	public DriveCareUserDetailsService(AppUserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user = users.findByEmail(username.toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
		return User.builder()
				.username(user.getEmail())
				.password(user.getPasswordHash())
				.authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
				.build();
	}
}
