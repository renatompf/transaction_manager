package io.renatofreire.transaction_manager.repository;

import io.renatofreire.transaction_manager.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmailIgnoreCase(String email);

    Optional<Account> findByIdAndDeletedIsFalse(Long id);

    Page<Account> findAllByDeletedIsFalse(Pageable pageable);

}
