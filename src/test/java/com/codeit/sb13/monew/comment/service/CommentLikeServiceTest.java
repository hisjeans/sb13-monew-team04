package com.codeit.sb13.monew.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.codeit.sb13.monew.article.domain.Article;
import com.codeit.sb13.monew.article.domain.ArticleSource;
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
import org.junit.jupiter.api.BeforeEach;
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

  private Comment comment;
  private User likedBy;
  private User commentUser;
  private Article article;
  private LocalDateTime commentCreatedAt;
  private LocalDateTime likeCreatedAt;


  @BeforeEach
  void setUp() {
    // given
    article = Article.create("기사 제목", "기사 요약", "https://test.com/article",
        LocalDateTime.now(), ArticleSource.NAVER);
    commentUser = User.builder()
        .email("comment@test.com")
        .nickname("댓글 작성자")
        .password("Abcd!")
        .build();
    likedBy = User.builder()
        .email("like@test.com")
        .nickname("좋아요한 사용자")
        .password("Abcd!")
        .build();
    comment=Comment.builder()
        .article(article)
        .user(commentUser)
        .content("테스트 댓글")
        .build();

    // 생성 시각 기대값 고정
    commentCreatedAt = LocalDateTime.of(2026, 8, 21, 10, 11);
    likeCreatedAt = LocalDateTime.of(2026, 8, 21, 13, 20);

    ReflectionTestUtils.setField(likedBy, "id", UUID.randomUUID()); // 좋아요 요청한 사용자 객체에 id 필드 설정
    ReflectionTestUtils.setField(commentUser, "id", UUID.randomUUID()); // 댓글 작성자 객체에 id 필드 설정
    ReflectionTestUtils.setField(article, "id", UUID.randomUUID()); // 기사 객체에
    ReflectionTestUtils.setField(comment, "id", UUID.randomUUID()); // 댓글 객체에 id 필드 설정
    ReflectionTestUtils.setField(comment, "createdAt", commentCreatedAt); // 댓글 객체에 createdAt 필드 설정
  }


  @Test
  @DisplayName("댓글 좋아요 생성 성공 - GREEN")
  void 댓글_좋아요_생성_성공() {

    // given
    UUID commentLikeId = UUID.randomUUID();

    CommentLikeRegisterCommand command=new CommentLikeRegisterCommand(comment.getId(), likedBy.getId());

    given(userRepository.findById(likedBy.getId())).willReturn(java.util.Optional.of(
        likedBy));
    given(commentRepository.findActiveByIdForUpdate(comment.getId())).willReturn(java.util.Optional.of(comment));
    given(commentLikeRepository.findByCommentAndLikedBy(comment, likedBy)).willReturn(java.util.Optional.empty());
    given(commentLikeRepository.save(any(CommentLike.class))).willAnswer(invocation -> {
      CommentLike commentLike = invocation.getArgument(0);
      ReflectionTestUtils.setField(commentLike, "id", commentLikeId);
      ReflectionTestUtils.setField(commentLike, "createdAt", likeCreatedAt);
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
        () -> assertThat(result.createdAt()).isEqualTo(likeCreatedAt),
        () -> assertThat(result.commentId()).isEqualTo(comment.getId()),
        () -> assertThat(result.articleId()).isEqualTo(article.getId()),
        () -> assertThat(result.commentUserId()).isEqualTo(commentUser.getId()),
        () -> assertThat(result.commentUserNickname()).isEqualTo("댓글 작성자"),
        () -> assertThat(result.commentContent()).isEqualTo("테스트 댓글"),
        () -> assertThat(result.commentLikeCount()).isEqualTo(1L),
        () -> assertThat(result.commentCreatedAt()).isEqualTo(commentCreatedAt)
    );
  }


  @Test
  @DisplayName("이미 좋아요한 댓글에 대해 다시 좋아요 요청 시 기존 좋아요 정보 반환 - GREEN")
  void 중복_좋아요_기존_좋아요_반환() {
    // given
    CommentLike existingLike = CommentLike.builder()
        .comment(comment)
        .likedBy(likedBy)
        .build();
    ReflectionTestUtils.setField(existingLike, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(existingLike, "createdAt", likeCreatedAt);

    CommentLikeRegisterCommand command = new CommentLikeRegisterCommand(comment.getId(), likedBy.getId());

    given(commentRepository.findActiveByIdForUpdate(comment.getId())).willReturn(java.util.Optional.of(comment));
    given(commentLikeRepository.findByCommentAndLikedBy(comment, likedBy)).willReturn(java.util.Optional.of(existingLike));
    given(userRepository.findById(likedBy.getId())).willReturn(java.util.Optional.of(likedBy));
    given(commentLikeRepository.countByCommentId(comment.getId())).willReturn(1L);

    // when
    CommentLikeDto result = commentLikeService.likeComment(command);

    // then
    Assertions.assertAll(
        ()->assertThat(result.id()).isEqualTo(existingLike.getId()),
        ()->assertThat(result.likedBy()).isEqualTo(likedBy.getId()),
        ()->assertThat(result.createdAt()).isEqualTo(likeCreatedAt),
        ()->assertThat(result.commentId()).isEqualTo(comment.getId()),
        ()->assertThat(result.articleId()).isEqualTo(article.getId()),
        ()->assertThat(result.commentUserId()).isEqualTo(commentUser.getId()),
        ()->assertThat(result.commentUserNickname()).isEqualTo(commentUser.getNickname()),
        ()->assertThat(result.commentContent()).isEqualTo(comment.getContent()),
        ()->assertThat(result.commentLikeCount()).isEqualTo(1L),
        ()->assertThat(result.commentCreatedAt()).isEqualTo(commentCreatedAt)
    );
    then(commentRepository).should(times(1)).findActiveByIdForUpdate(comment.getId());
    then(userRepository).should(times(1)).findById(likedBy.getId());
    then(commentLikeRepository).should(never()).save(any(CommentLike.class));
    then(commentLikeRepository).should(times(1)).countByCommentId(comment.getId());


  }

}
