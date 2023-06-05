package com.example.sixnumber.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.sixnumber.fixture.TestDataFactory;
import com.example.sixnumber.global.dto.ApiResponse;
import com.example.sixnumber.global.dto.ListApiResponse;
import com.example.sixnumber.global.exception.BreakTheRulesException;
import com.example.sixnumber.global.exception.UserNotFoundException;
import com.example.sixnumber.global.util.JwtProvider;
import com.example.sixnumber.user.dto.ChargingRequest;
import com.example.sixnumber.user.dto.ChargingResponse;
import com.example.sixnumber.user.dto.SigninRequest;
import com.example.sixnumber.user.dto.SignupRequest;
import com.example.sixnumber.user.dto.OnlyMsgRequest;
import com.example.sixnumber.user.dto.StatementResponse;
import com.example.sixnumber.user.entity.User;
import com.example.sixnumber.user.exception.OverlapException;
import com.example.sixnumber.user.exception.StatusNotActiveException;
import com.example.sixnumber.user.repository.UserRepository;
import com.example.sixnumber.user.type.Status;
import com.example.sixnumber.user.type.UserRole;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepository userRepository;
	@Mock
	private JwtProvider jwtProvider;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private RedisTemplate<String, String> redisTemplate;

	private User saveUser;
	private ValueOperations<String, String> valueOperations;

	@BeforeEach
	public void setup() {
		// MockitoAnnotations.openMocks(this);
		saveUser = TestDataFactory.user();
		valueOperations = mock(ValueOperations.class);
	}

	@Test
	void signup_success() {
		SignupRequest signupRequest = TestDataFactory.signupRequest();

		when(userRepository.existsUserByEmail(anyString())).thenReturn(false);
		when(userRepository.existsUserByNickname(anyString())).thenReturn(false);

		String encodedPassword = "ePassword";
		when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn(encodedPassword);

		ApiResponse response = userService.signUp(signupRequest);

		verify(userRepository).existsUserByEmail(anyString());
		verify(userRepository).existsUserByNickname(anyString());
		verify(userRepository).save(any(User.class));
		assertEquals(201, response.getCode());
		assertEquals("회원가입 완료", response.getMsg());
	}

	@Test
	void signup_success_setActive() {
		SignupRequest request = TestDataFactory.signupRequest();

		saveUser.setStatus("DORMANT");

		when(userRepository.findByStatusAndEmail(eq(Status.DORMANT), anyString())).thenReturn(Optional.of(saveUser));

		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

		ApiResponse response = userService.signUp(request);

		verify(userRepository).findByStatusAndEmail(eq(Status.DORMANT), anyString());
		verify(passwordEncoder).matches(anyString(), anyString());
		verify(userRepository).save(any(User.class));
		assertEquals(saveUser.getStatus(), Status.ACTIVE);
		assertNull(saveUser.getWithdrawExpiration());
		assertEquals(response.getCode(), 200);
		assertEquals(response.getMsg(), "재가입 완료");
	}

	@Test
	void signup_EmailOverlapException() {
		SignupRequest signupRequest = TestDataFactory.signupRequest();

		when(userRepository.existsUserByEmail(anyString())).thenReturn(true);

		Exception exception = assertThrows(OverlapException.class,
			() -> userService.signUp(signupRequest));

		verify(userRepository).existsUserByEmail(anyString());
		assertEquals(exception.getMessage(), "중복된 이메일입니다");
	}

	@Test
	void signup_NicknameOverlapException() {
		SignupRequest signupRequest = TestDataFactory.signupRequest();

		when(userRepository.existsUserByNickname(anyString())).thenReturn(true);

		Exception exception = assertThrows(OverlapException.class,
			() -> userService.signUp(signupRequest));

		verify(userRepository).existsUserByNickname(anyString());
		assertEquals(exception.getMessage(), "중복된 닉네임입니다");
	}

	@Test
	void signin_success() {
		SigninRequest signinRequest = TestDataFactory.signinRequest();

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

		when(jwtProvider.refreshToken(saveUser.getEmail(), saveUser.getId())).thenReturn("sampleRT");
		when(jwtProvider.accessToken(saveUser.getEmail(), saveUser.getId())).thenReturn("sampleAT");

		String accessToken = userService.signIn(signinRequest);

		verify(userRepository).findByEmail(signinRequest.getEmail());
		verify(valueOperations).get(anyString());
		verify(valueOperations).set(anyString(), anyString());
		verify(passwordEncoder).matches(anyString(), anyString());
		verify(jwtProvider).refreshToken(saveUser.getEmail(), saveUser.getId());
		verify(jwtProvider).accessToken(saveUser.getEmail(), saveUser.getId());
		assertEquals(accessToken, "sampleAT");
		assertEquals(saveUser.getStatus(), Status.ACTIVE);
	}

	@Test
	void signin_UserNotFoundException() {
		SigninRequest signinRequest = TestDataFactory.signinRequest();

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		Assertions.assertThrows(UserNotFoundException.class, () -> userService.signIn(signinRequest));

		verify(userRepository).findByEmail(anyString());
	}

	@Test
	void signin_LoginOverlapException() {
		SigninRequest signinRequest = TestDataFactory.signinRequest();

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn("RTV");

		Assertions.assertThrows(OverlapException.class,
			() -> userService.signIn(signinRequest));

		verify(userRepository).findByEmail(anyString());
		verify(valueOperations).get(anyString());
		verify(redisTemplate).delete(anyString());
	}

	@ParameterizedTest
	@MethodSource("com.example.sixnumber.fixture.TestDataFactory#statusTestData")
	void signin_fail_Status(Status status) {
		SigninRequest signinRequest = TestDataFactory.signinRequest();

		User user = mock(User.class);
		when(user.getStatus()).thenReturn(status);

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(null);

		Assertions.assertThrows(StatusNotActiveException.class,
			() -> userService.signIn(signinRequest));

		verify(userRepository).findByEmail(anyString());
		verify(valueOperations).get(anyString());
	}

	@Test
	void signin_fail_incorrectPW() {
		SigninRequest signinRequest = TestDataFactory.signinRequest();

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(null);

		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

		Assertions.assertThrows(IllegalArgumentException.class,
			() -> userService.signIn(signinRequest));

		verify(userRepository).findByEmail(anyString());
		verify(valueOperations).get(anyString());
		verify(passwordEncoder).matches(anyString(), anyString());
	}

	@Test
	void logout() {
		ApiResponse response = userService.logout(saveUser);

		assertEquals(response.getCode(), 200);
		assertEquals(response.getMsg(), "로그아웃 성공");
		verify(redisTemplate).delete(anyString());
	}

	@Test
	void withdraw_success() {
		OnlyMsgRequest request = mock(OnlyMsgRequest.class);
		when(request.getMsg()).thenReturn("회원탈퇴");

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		ApiResponse response = userService.withdraw(request, saveUser.getEmail());

		verify(redisTemplate).delete(anyString());
		assertEquals(saveUser.getStatus(), Status.DORMANT);
		assertNotNull(saveUser.getWithdrawExpiration());
		assertEquals(response.getCode(), 200);
		assertEquals(response.getMsg(), "회원 탈퇴 완료");
	}

	@Test
	void withdraw_fail_incorrectMsg() {
		OnlyMsgRequest request = mock(OnlyMsgRequest.class);
		when(request.getMsg()).thenReturn("incorrectMsg");

		Assertions.assertThrows(IllegalArgumentException.class, () -> userService.withdraw(request, saveUser.getEmail()));

		verify(request).getMsg();
	}

	@Test
	void setUser_success() {
		OnlyMsgRequest request = mock(OnlyMsgRequest.class);
		when(request.getMsg()).thenReturn("월정액 해지");

		saveUser.setRole("PAID");

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		ApiResponse response = userService.setPaid(request, saveUser.getEmail());

		assertEquals(saveUser.getPaymentDate(), "월정액 해지");
		assertEquals(response.getCode(), 200);
		assertEquals(response.getMsg(), "해지 신청 성공");
	}

	@Test
	void setUser_fail_USER() {
		OnlyMsgRequest request = mock(OnlyMsgRequest.class);
		when(request.getMsg()).thenReturn("월정액 해지");

		// saveUser.getRole() = UserRole.USER
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		Assertions.assertThrows(IllegalArgumentException.class, () -> userService.setPaid(request, saveUser.getEmail()));

		verify(request).getMsg();
		verify(userRepository).findByEmail(anyString());
	}

	@Test
	void setPaid_success() {
		OnlyMsgRequest request = mock(OnlyMsgRequest.class);
		when(request.getMsg()).thenReturn("false");

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		ApiResponse response = userService.setPaid(request, saveUser.getEmail());

		verify(userRepository).findByEmail(anyString());
		assertEquals(saveUser.getCash(), 1000);
		assertEquals(saveUser.getRole(), UserRole.ROLE_PAID);
		assertNotNull(saveUser.getPaymentDate());
		assertNotNull(saveUser.getStatement());
		assertEquals(response.getCode(), 200);
		assertEquals(response.getMsg(), "권한 변경 성공");
	}

	@ParameterizedTest
	@MethodSource("com.example.sixnumber.fixture.TestDataFactory#setPaidTestData")
	void setPaid_fail_lowCash_Or_Role(int cash, UserRole role) {
		OnlyMsgRequest request = mock(OnlyMsgRequest.class);
		when(request.getMsg()).thenReturn("false");

		User user = mock(User.class);
		when(user.getEmail()).thenReturn("test@email.com");
		when(user.getCash()).thenReturn(cash);
		// 분명하게 필요한 정보인데 스터빙 오류가 계속 떠서 lenient() 적용함
		lenient().when(user.getRole()).thenReturn(role);

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

		Assertions.assertThrows(IllegalArgumentException.class, () -> userService.setPaid(request, user.getEmail()));

		verify(request).getMsg();
		verify(userRepository).findByEmail(anyString());
	}

	@Test
	void getCash() {
		int response = userService.getCash(saveUser);

		assertEquals(saveUser.getCash(), response);
	}

	@Test
	void charging_success() {
		ChargingRequest request = TestDataFactory.chargingRequest();

		Set<String> set = new HashSet<>(List.of("STMT: 7-1"));
		when(redisTemplate.keys("*STMT: 7-*")).thenReturn(set);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		ApiResponse response = userService.charging(request, saveUser);

		verify(redisTemplate, times(2)).keys(anyString());
		verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
		verify(userRepository).findByEmail(anyString());
		assertEquals(saveUser.getChargingCount(), 1);
		assertEquals(response.getCode(), 200);
		assertEquals(response.getMsg(), "요청 성공");
	}

	@Test
	void charging_fail_manyCharges() {
		ChargingRequest request = TestDataFactory.chargingRequest();

		Set<String> keys = TestDataFactory.keys();
		when(redisTemplate.keys(anyString())).thenReturn(keys);

		Assertions.assertThrows(IllegalArgumentException.class, () -> userService.charging(request, saveUser));

		verify(redisTemplate).keys(anyString());
	}

	@Test
	void charging_fail_KeyOverlapException() {
		ChargingRequest request = TestDataFactory.chargingRequest();

		Set<String> set = new HashSet<>(List.of("Msg-5000"));
		when(redisTemplate.keys(anyString())).thenReturn(set);

		Assertions.assertThrows(OverlapException.class, () -> userService.charging(request, saveUser));

		verify(redisTemplate, times(2)).keys(anyString());
	}

	@Test
	void charging_BreakTheRulesException() {
		ChargingRequest request = TestDataFactory.chargingRequest();

		saveUser.setChargingCount(4);
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(saveUser));

		Assertions.assertThrows(BreakTheRulesException.class, () -> userService.charging(request, saveUser));

		verify(userRepository).findByEmail(anyString());
	}

	// AdminServiceTest getCharges 와 성공 code가 동일함 삭제해도되나 ?
	@Test
	void getCharges_success() {
		Set<String> keys = TestDataFactory.keys();
		List<String> values = TestDataFactory.values();

		when(redisTemplate.keys(anyString())).thenReturn(keys);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.multiGet(keys)).thenReturn(values);

		ListApiResponse<ChargingResponse> response = userService.getCharges(saveUser.getId());

		verify(redisTemplate).keys(anyString());
		verify(valueOperations).multiGet(keys);
		assertEquals(response.getData().size(), 3);
		assertEquals(response.getCode(), 200);
		assertEquals(response.getMsg(), "신청 리스트 조회 성공");
	}

	@Test
	void getCharges_fail_noData() {
		when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

		Assertions.assertThrows(IllegalArgumentException.class, () -> userService.getCharges(0L));

		verify(redisTemplate).keys(anyString());
	}

	@Test
	void getStatement_success() {
		saveUser.setStatement(LocalDate.now() + ",5000" );

		when(userRepository.findById(anyLong())).thenReturn(Optional.of(saveUser));

		ListApiResponse<StatementResponse> response = userService.getStatement(saveUser.getEmail());

		verify(userRepository).findById(anyLong());
		assertEquals(response.getData().size(), 1);
		assertEquals(response.getCode(), 200);
		assertEquals(response.getMsg(), "거래내역 조회 완료");
	}

	@Test
	void getStatement_fail_lowSize() {
		when(userRepository.findById(anyLong())).thenReturn(Optional.of(saveUser));

		Assertions.assertThrows(IllegalArgumentException.class, () -> userService.getStatement(saveUser.getEmail()));

		verify(userRepository).findById(anyLong());
	}
}
