package com.codeit.sb13.monew.comment.repository;


import com.codeit.sb13.monew.comment.domain.Comment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  // 중복 좋아요 방지를 위해 댓글을 조회할 때 PESSIMISTIC_WRITE 잠금을 사용하여 동시성 문제 방지
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select c 
      from Comment c 
      where c.id = :commentId 
      and c.deletedAt is null
      """)
  Optional<Comment> findActiveByIdForUpdate(@Param("commentId") UUID commentId);
}
