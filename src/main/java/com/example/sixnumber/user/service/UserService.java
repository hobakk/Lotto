package com.example.sixnumber.user.service;

import java.time.YearMonth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sixnumber.global.dto.ApiResponse;
import com.example.sixnumber.global.util.JwtProvider;
import com.example.sixnumber.user.dto.ChargingRequest;
import com.example.sixnumber.user.dto.ReleasePaidRequest;
import com.example.sixnumber.user.dto.SigninRequest;
import com.example.sixnumber.user.dto.SignupRequest;
import com.example.sixnumber.user.dto.WithdrawRequest;
import com.example.sixnumber.user.entity.Cash;
import com.example.sixnumber.user.entity.User;
import com.example.sixnumber.user.repository.CashRepository;
import com.example.sixnumber.user.repository.UserRepository;
import com.example.sixnumber.user.type.Status;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final CashRepository cashRepository;
	private final PasswordEncoder passwordEncoder;

	public ApiResponse signUp(SignupRequest request) {
		if (userRepository.existsUserByEmail(request.getEmail())) {
			throw new IllegalArgumentException("이메일 중복");
		}
		if (userRepository.existsUserByNickname(request.getNickname())) {
			throw new IllegalArgumentException("닉네임 중복");
		}

		String password = passwordEncoder.encode(request.getPassword());
		User user = new User(request, password);
		userRepository.save(user);
		return ApiResponse.create("회원가입 완료");
	}

	public String signIn(SigninRequest request) {
		User user = findByUser(request.getEmail());
		if (user.getStatus().equals(Status.DORMANT)) {
			throw new IllegalArgumentException("탈퇴한 계정입니다");
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("아이디 또는 비밀번호를 잘못 입력하셨습니다");
		}
		return JwtProvider.accessToken(user.getEmail(), user.getId());
	}

	public void logout(User user) {  }

	public ApiResponse withdraw(WithdrawRequest request, String email) {
		String withdrawMsg = "회원탈퇴";

		if (!request.getMsg().equals(withdrawMsg)) {
			throw new IllegalArgumentException("잘못된 문자열 입력");
		}
		User user = findByUser(email);
		user.setStatus("DORMANT");
		return ApiResponse.ok("회원 탈퇴 완료");
	}

	public ApiResponse setPaid(ReleasePaidRequest request, String email) {
		User user = findByUser(email);

		if (request == null) {
			if (user.getCash() < 5000) {
				throw new IllegalArgumentException("금액이 부족합니다");
			}
			user.setCash("-", 5000);
			user.setRole("PAID");
			user.setPaymentDate(YearMonth.now().toString());
		} else {
			user.setPaymentDate(request.getMsg());
			return ApiResponse.ok("해지 신청 성공");
		}

		return ApiResponse.ok("권한 변경 성공");
	}

	public int getCash(User user) {
		return user.getCash();
	}

	public ApiResponse charging(ChargingRequest chargingRequest, Long userId) {
		Cash cash = new Cash(userId, chargingRequest);
		cashRepository.save(cash);
		return ApiResponse.ok("요청 성공");
	}

	private User findByUser(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(()-> new IllegalArgumentException("아이디 또는 비밀번호를 잘못 입력하셨습니다"));
	}
}
