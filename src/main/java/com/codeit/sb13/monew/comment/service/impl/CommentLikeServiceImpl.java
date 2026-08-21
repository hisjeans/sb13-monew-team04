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
    Comment comment = commentRepository.findActiveByIdForUpdate(command.commentId()).orElseThrow(()-> new CommentNotFoundException(command.commentId()));
    User likedBy = userRepository.findById(command.requestUserId()).orElseThrow(()->new UserNotFoundException(command.requestUserId()));

    // 중복 좋아요 방지 - 좋아요가 없을 때만 새로 생성, 이미 좋아요가 있을 경우 기존 좋아요 객체 반환
    return commentLikeRepository.findByCommentAndLikedBy(comment, likedBy)
        .map(existingLike -> {
          Long likeCount = commentLikeRepository.countByCommentId(comment.getId());
          log.debug("이미 존재하는 댓글 좋아요 반환 - 댓글 아이디: {}", comment.getId());
          return CommentLikeDto.from(existingLike, likeCount);
        })
        .orElseGet(() -> {
          CommentLike commentLike = CommentLike.builder()
              .comment(comment)
              .likedBy(likedBy)
              .build();

          CommentLike savedCommentLike = commentLikeRepository.save(commentLike);
          Long likeCount = commentLikeRepository.countByCommentId(comment.getId());

          log.info("댓글 좋아요 등록 완료 - 댓글 아이디: {}, 좋아요 객체 ID: {}",
              savedCommentLike.getComment().getId(), savedCommentLike.getId());
          return CommentLikeDto.from(savedCommentLike, likeCount);
        });
  }
}
