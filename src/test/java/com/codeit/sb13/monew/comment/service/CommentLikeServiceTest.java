package com.codeit.sb13.monew.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.codeit.sb13.monew.article.domain.Article;
import com.codeit.sb13.monew.comment.domain.Comment;
import com.codeit.sb13.monew.comment.domain.CommentLike;
import com.codeit.sb13.monew.comment.repository.CommentLikeRepository;
import com.codeit.sb13.monew.comment.repository.CommentRepository;
import com.codeit.sb13.monew.comment.service.dto.CommentLikeDto;
import com.codeit.sb13.monew.comment.service.dto.CommentLikeRegisterCommand;
import com.codeit.sb13.monew.comment.service.impl.CommentLikeServiceImpl;
import com.codeit.sb13.monew.user.domain.User;
import com.codeit.sb13.monew.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("댓글 좋아요 서비스 - TDD")
@ExtendWith(MockitoExtension.class)
public class CommentLikeServiceTest {

  @Mock
  CommentLikeRepository commentLikeRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  CommentRepository commentRepository;

  @InjectMocks
  CommentLikeServiceImpl commentLikeService;

  @Test
  @DisplayName("댓글 좋아요 생성 성공 - GREEN")
  void 댓글_좋아요_생성_성공() {

    // given
    Article article = new Article("기사 제목", "기사 요약", "https://test.com/article", LocalDateTime.now(), "기사 출처");
    User commentUser = User.builder()
        .email("comment@test.com")
        .nickname("댓글 작성자")
        .password("Abcd!")
        .build();
    User likedBy = User.builder()
        .email("like@test.com")
        .nickname("좋아요한 사용자")
        .password("Abcd!")
        .build();
    Comment comment=Comment.builder()
        .article(article)
        .user(commentUser)
        .content("테스트 댓글")
        .build();

    ReflectionTestUtils.setField(commentUser, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(likedBy, "id", UUID.randomUUID());
    UUID commentLikeId = UUID.randomUUID();

    CommentLikeRegisterCommand command=new CommentLikeRegisterCommand(comment.getId(), likedBy.getId());

    given(userRepository.findById(likedBy.getId())).willReturn(java.util.Optional.of(
        likedBy));
    given(commentRepository.findById(comment.getId())).willReturn(java.util.Optional.of(comment));
    given(commentLikeRepository.findByCommentAndLikedBy(comment, likedBy)).willReturn(java.util.Optional.empty());
    given(commentLikeRepository.save(any(CommentLike.class))).willAnswer(invocation -> {
      CommentLike commentLike = invocation.getArgument(0);
      ReflectionTestUtils.setField(commentLike, "id", commentLikeId);
      return commentLike;
    });
    given(commentLikeRepository.countByCommentId(comment.getId())).willReturn(1L);

    // when
    CommentLikeDto result = commentLikeService.likeComment(command);

    // then
    ArgumentCaptor<CommentLike> captor = ArgumentCaptor.forClass(CommentLike.class);

    then(commentLikeRepository).should(times(1)).save(captor.capture());
    then(commentLikeRepository).should(times(1)).findByCommentAndLikedBy(comment, likedBy);
    then(commentLikeRepository).should(times(1)).countByCommentId(comment.getId());
    CommentLike savedCommentLike = captor.getValue();
    Assertions.assertAll(
        () -> assertThat(savedCommentLike.getComment()).isEqualTo(comment),
        () -> assertThat(savedCommentLike.getLikedBy()).isEqualTo(likedBy),
        () -> assertThat(result.id()).isEqualTo(commentLikeId),
        () -> assertThat(result.likedBy()).isEqualTo(likedBy.getId()),
        () -> assertThat(result.commentId()).isEqualTo(comment.getId()),
        () -> assertThat(result.articleId()).isEqualTo(article.getId()),
        () -> assertThat(result.commentUserId()).isEqualTo(commentUser.getId()),
        () -> assertThat(result.commentUserNickname()).isEqualTo("댓글 작성자"),
        () -> assertThat(result.commentContent()).isEqualTo("테스트 댓글"),
        () -> assertThat(result.commentLikeCount()).isEqualTo(1L)
    );
  }
}
