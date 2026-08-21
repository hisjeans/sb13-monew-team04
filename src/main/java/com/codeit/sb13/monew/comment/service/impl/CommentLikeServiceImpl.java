package com.codeit.sb13.monew.comment.service.impl;

import com.codeit.sb13.monew.comment.domain.Comment;
import com.codeit.sb13.monew.comment.domain.CommentLike;
import com.codeit.sb13.monew.comment.repository.CommentLikeRepository;
import com.codeit.sb13.monew.comment.repository.CommentRepository;
import com.codeit.sb13.monew.comment.service.CommentLikeService;
import com.codeit.sb13.monew.comment.service.dto.CommentLikeDto;
import com.codeit.sb13.monew.comment.service.dto.CommentLikeRegisterCommand;
import com.codeit.sb13.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.sb13.monew.global.exception.user.UserNotFoundException;
import com.codeit.sb13.monew.user.domain.User;
import com.codeit.sb13.monew.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
@Service
public class CommentLikeServiceImpl implements CommentLikeService {

  private final CommentLikeRepository commentLikeRepository;
  private final CommentRepository commentRepository;
  private final UserRepository userRepository;
  private final EntityManager entityManager;
  private final TransactionTemplate likeWriteTransactionTemplate;

  public CommentLikeServiceImpl(CommentLikeRepository commentLikeRepository,
      CommentRepository commentRepository, UserRepository userRepository,
      EntityManager entityManager,
      PlatformTransactionManager transactionManager) {
    this.commentLikeRepository = commentLikeRepository;
    this.commentRepository = commentRepository;
    this.userRepository = userRepository;
    this.entityManager = entityManager;

    // UNIQUE 제약 조건 위반 시 실패하는 INSERT 트랜잭션만 롤백되도록 REQUIRES_NEW 로 분리
    this.likeWriteTransactionTemplate = new TransactionTemplate(transactionManager);
    this.likeWriteTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  // 댓글, 사용자 존재 여부 확인 -> 기존 좋아요가 없다면 새로운 좋아요 등록, 이미 좋아요가 있다면 기존 좋아요 반환
  @Transactional
  @Override
  public CommentLikeDto likeComment(@Valid CommentLikeRegisterCommand command) {
    UUID commentId = command.commentId();
    UUID likedById = command.requestUserId();

    log.debug("댓글 좋아요 등록 시작 - 댓글 아이디: {}", commentId);

    commentRepository.findByIdAndDeletedAtIsNull(commentId).orElseThrow(()-> new CommentNotFoundException(commentId));
    userRepository.findById(likedById).orElseThrow(()->new UserNotFoundException(likedById));

    return commentLikeRepository.findByCommentAndLikedBy(commentId, likedById)
        .map(this::toDto)
        .orElseGet(() -> createOrReturnExisting(commentId, likedById));
  }

  // 새로운 트랜잭션으로 댓글 좋아요 등록 시도 -> 동시 중복 요청 발생으로 UNIQUE 제약을 위반하면 기존 좋아요 반환
  private CommentLikeDto createOrReturnExisting(UUID commentId, UUID likedById) {
    try {
      likeWriteTransactionTemplate.executeWithoutResult(status ->
        commentLikeRepository.saveAndFlush(CommentLike.builder()
            .comment(entityManager.getReference(Comment.class, commentId))
            .likedBy(entityManager.getReference(User.class, likedById))
            .build()));
      log.info("댓글 좋아요 등록 완료 - 댓글 아이디: {}", commentId);

    } catch (DataIntegrityViolationException e) {
      log.debug("댓글 좋아요 중복 감지 -> 기존 댓글 좋아요 반환 - 댓글 아이디: {}", commentId);

      return commentLikeRepository.findByCommentAndLikedBy(commentId, likedById)
            .map(this::toDto)
            .orElseThrow(()->e);
    }

    return commentLikeRepository.findByCommentAndLikedBy(commentId, likedById)
        .map(this::toDto)
        .orElseThrow(()->new IllegalStateException("댓글 좋아요 등록 후 조회 실패 - 댓글 아이디: " + commentId));
  }

  // 댓글 좋아요가 성공적으로 등록되면 재조회하여 좋아요 수를 포함한 DTO 반환
  private CommentLikeDto toDto(CommentLike commentLike) {
    Long likeCount = commentLikeRepository.countByCommentId(commentLike.getComment().getId());
    return CommentLikeDto.from(commentLike, likeCount);
  }
}
