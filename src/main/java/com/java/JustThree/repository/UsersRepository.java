package com.java.JustThree.repository;

import com.java.JustThree.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface UsersRepository extends JpaRepository<Users,Long> {

}
