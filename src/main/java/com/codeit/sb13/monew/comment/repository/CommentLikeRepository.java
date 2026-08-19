package com.codeit.sb13.monew.comment.repository;

import com.codeit.sb13.monew.comment.domain.CommentLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

}
