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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CommentLikeServiceImpl implements CommentLikeService {

  private final CommentLikeRepository commentLikeRepository;
  private final CommentRepository commentRepository;
  private final UserRepository userRepository;

  @Transactional
  @Override
  public CommentLikeDto likeComment(@Valid CommentLikeRegisterCommand command) {
    log.debug("댓글 좋아요 등록 시작 - 댓글 아이디: {}", command.commentId());
    Comment comment = commentRepository.findByIdAndDeletedAtIsNull(command.commentId()).orElseThrow(()-> new CommentNotFoundException(command.commentId()));
    User likedBy = userRepository.findById(command.requestUserId()).orElseThrow(()->new UserNotFoundException(command.requestUserId()));

    // 중복 좋아요 방지 - 좋아요가 없을 때만 새로 생성, 이미 좋아요가 있을 경우 기존 좋아요 객체 반환
    return commentLikeRepository.findByCommentAndLikedBy(command.commentId(), command.requestUserId())
        .map(this::toDto)
        .orElseGet(() -> {
          try {
            commentLikeRepository.saveAndFlush(CommentLike.builder()
                .comment(comment)
                .likedBy(likedBy)
                .build());
            log.info("댓글 좋아요 등록 완료 - 댓글 아이디: {}", comment.getId());
          } catch (DataIntegrityViolationException e) {
            log.debug("댓글 좋아요 중복 감지 - 기존 댓글 좋아요 반환 - 댓글 아이디: {}", comment.getId());
          }

          return commentLikeRepository.findByCommentAndLikedBy(comment.getId(), likedBy.getId())
              .map(this::toDto)
              .orElseThrow(()->new IllegalStateException("댓글 좋아요 등록 후 조회 실패 - 댓글 아이디: " + comment.getId()));
        });
  }

  private CommentLikeDto toDto(CommentLike commentLike) {
    Long likeCount = commentLikeRepository.countByCommentId(commentLike.getComment().getId());
    return CommentLikeDto.from(commentLike, likeCount);
  }
}
