package com.example.sixnumber.lotto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.sixnumber.lotto.entity.SixNumber;

public interface SixNumberRepository extends JpaRepository<SixNumber, Long> {

	@Query(value = "SELECT s FROM SixNumber s WHERE YEAR(s.buyDate) = :year AND MONTH(s.buyDate) = :month")
	List<SixNumber> findAllByBuyDate(@Param("year") int year, @Param("month") int month);
}
