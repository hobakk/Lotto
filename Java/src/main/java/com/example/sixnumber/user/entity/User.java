package com.example.sixnumber.user.entity;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.sixnumber.user.dto.SignupRequest;
import com.example.sixnumber.user.type.Status;
import com.example.sixnumber.user.type.UserRole;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "Users")
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	@Column(name = "email", nullable = false, unique = true, length = 50)
	private String email;
	@Column(name = "password", nullable = false, length = 60)
	private String password;
	@Column(name = "nickname", nullable = false, unique = true, length = 10)
	private String nickname;
	@Column(name = "cash")
	private int cash;
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 12)
	private UserRole role;
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private Status status;
	@Column(name = "paymentDate")
	private String paymentDate;
	@Column(name = "withdrawExpiration")
	private LocalDate withdrawExpiration;
	@ElementCollection
	@OrderColumn(name = "statement_index", nullable = false)
	private List<String> statement;
	@Column(name = "chargingCount")
	private int chargingCount;

	public User(SignupRequest request, String password) {
		this.email = request.getEmail();
		this.password = password;
		this.nickname = request.getNickname();
		this.role = UserRole.ROLE_USER;
		this.status = Status.ACTIVE;
		this.cash = 1000;
		this.statement = new ArrayList<>();
		this.chargingCount = 0;
	}

	public void update(List<String> list) {
		this.email = list.get(0);
		this.password = list.get(1);
		this.nickname = list.get(2);
	}

	public void setPaymentDate(String yearMonth) {
		this.paymentDate = yearMonth;
	}
	public void setCash(String sign, int cash) {
		switch (sign) {
			case "+" -> this.cash += cash;
			case "-" -> this.cash -= cash;
		}
	}
	public void setRole(String role) {
		switch (role) {
			case "USER" -> this.role = UserRole.ROLE_USER;
			case "PAID" -> this.role = UserRole.ROLE_PAID;
		}
	}

	public void setAdmin() {
		this.role = UserRole.ROLE_ADMIN;
	}

	public void setStatus(String status) {
		switch (status) {
			case "ACTIVE" -> this.status = Status.ACTIVE;
			case "SUSPENDED" -> this.status = Status.SUSPENDED;
			case "DORMANT" -> this.status = Status.DORMANT;
		}
	}

	public void setWithdrawExpiration(LocalDate localDate) {
		this.withdrawExpiration = localDate;
	}

	public void setStatement(String str) {
		this.statement.add(str);
	}

	public void setChargingCount(int num) {
		if (num == 0) this.chargingCount = 0;
		else this.chargingCount += num;
	}

	public void setNickname(String str) {
		this.nickname = str;
	}

	// test code
	public void setId(Long userId) {
		this.id = userId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Collection<GrantedAuthority> authorities = new HashSet<>();
		for (UserRole eachRole : UserRole.values()) {
			authorities.add(new SimpleGrantedAuthority(eachRole.name()));
		}
		return authorities;
	}

	@Override
	public String getUsername() {
		return this.email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return false;
	}

	@Override
	public boolean isAccountNonLocked() {
		return false;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return false;
	}

	@Override
	public boolean isEnabled() {
		return false;
	}
}
