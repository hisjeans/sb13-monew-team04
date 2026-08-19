package com.codeit.sb13.monew.comment.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 좋아요 엔티티 - TDD")
public class CommentLikesTest {

  @Test
  @DisplayName("댓글 좋아요 생성 성공 - RED")
  void 댓글_좋아요_생성_성공() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID likedBy = UUID.randomUUID();

    // when
    CommentLikes commentLikes=new CommentLikes(commentId, likedBy);

    // then
    Assertions.assertAll(
        ()-> assertThat(commentLikes.getCommentId()).isEqualTo(commentId),
        ()-> assertThat(commentLikes.getLikedBy()).isEqualTo(likedBy)
    );

  }
}
