package com.autosetai.backend.auth.domain.repository;

import com.autosetai.backend.auth.domain.model.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySocialId(String socialId);
}
